/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.blueprint.container;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.aries.blueprint.BlueprintConstants;
import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.blueprint.ServiceProcessor;
import org.apache.aries.blueprint.container.AggregateConverter.Convertible;
import org.apache.aries.blueprint.container.BeanRecipe.UnwrapperedBeanHolder;
import org.apache.aries.blueprint.di.AbstractRecipe;
import org.apache.aries.blueprint.di.CollectionRecipe;
import org.apache.aries.blueprint.di.ExecutionContext;
import org.apache.aries.blueprint.di.MapRecipe;
import org.apache.aries.blueprint.di.Recipe;
import org.apache.aries.blueprint.di.Repository;
import org.apache.aries.blueprint.proxy.Collaborator;
import org.apache.aries.blueprint.proxy.ProxyUtils;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.utils.JavaUtils;
import org.apache.aries.blueprint.utils.ReflectionUtils;
import org.apache.aries.blueprint.utils.ServiceListener;
import org.apache.aries.proxy.InvocationListener;
import org.apache.aries.util.AriesFrameworkUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.ReifiedType;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A <code>Recipe</code> to export services into the OSGi registry.
 *
 * @version $Rev$, $Date$
 */
public class ServiceRecipe extends AbstractRecipe {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRecipe.class);
    final static String LOG_ENTRY = "Method entry: {}, args {}";
    final static String LOG_EXIT = "Method exit: {}, returning {}";

    private final BlueprintContainerImpl blueprintContainer;
    private final ServiceMetadata metadata;
    private final Recipe serviceRecipe;
    private final CollectionRecipe listenersRecipe;
    private final MapRecipe propertiesRecipe;
    private final List<Recipe> explicitDependencies;

    private Map properties;
    private final AtomicReference<ServiceRegistration> registration = new AtomicReference<ServiceRegistration>();
    private Map registrationProperties;
    private List<ServiceListener> listeners;
    private volatile Object service;
    private final Object monitor = new Object();
    /** Only ever access when holding a lock on <code>monitor</code> */
    private int activeCalls;
    /** Only ever access when holding a lock on <code>monitor</code> */
    private boolean quiesce;
    private Collection<DestroyCallback> destroyCallbacks = new ArrayList<DestroyCallback>();
    
    public ServiceRecipe(String name,
                         BlueprintContainerImpl blueprintContainer,
                         ServiceMetadata metadata,
                         Recipe serviceRecipe,
                         CollectionRecipe listenersRecipe,
                         MapRecipe propertiesRecipe,
                         List<Recipe> explicitDependencies) {
        super(name);
        this.prototype = false;
        this.blueprintContainer = blueprintContainer;
        this.metadata = metadata;
        this.serviceRecipe = serviceRecipe;
        this.listenersRecipe = listenersRecipe;
        this.propertiesRecipe = propertiesRecipe;
        this.explicitDependencies = explicitDependencies;
    }
    
    public Recipe getServiceRecipe() {
        return serviceRecipe;
    }
    public CollectionRecipe getListenersRecipe() {
        return listenersRecipe;
    }

    @Override
    public List<Recipe> getConstructorDependencies() {
        List<Recipe> recipes = new ArrayList<Recipe>();
        if (explicitDependencies != null) {
            recipes.addAll(explicitDependencies);
        }
        return recipes;
    }
    
    public List<Recipe> getDependencies() {
        List<Recipe> recipes = new ArrayList<Recipe>();
        if (serviceRecipe != null) {
            recipes.add(serviceRecipe);
        }
        if (listenersRecipe != null) {
            recipes.add(listenersRecipe);
        }
        if (propertiesRecipe != null) {
            recipes.add(propertiesRecipe);
        }        
        recipes.addAll(getConstructorDependencies());        
        return recipes;
    }

    protected Object internalCreate() throws ComponentDefinitionException {
        ServiceRegistrationProxy proxy = new ServiceRegistrationProxy();
        addPartialObject(proxy);
        internalGetService(null, null); // null bundle means we don't want to retrieve the actual service when used with a ServiceFactory
        return proxy;
    }

    public boolean isRegistered() {
        return registration.get() != null;
    }

    public void register() {
        int state = blueprintContainer.getBundleContext().getBundle().getState();
        if (state != Bundle.ACTIVE && state != Bundle.STARTING) {
            return;
        }
        createExplicitDependencies();

        Hashtable props = new Hashtable();
        if (properties == null) {
            properties = (Map) createRecipe(propertiesRecipe);
        }
        props.putAll(properties);
        if (metadata.getRanking() == 0) {
            props.remove(Constants.SERVICE_RANKING);
        } else {
            props.put(Constants.SERVICE_RANKING, metadata.getRanking());
        }
        String componentName = getComponentName();
        if (componentName != null) {
            props.put(BlueprintConstants.COMPONENT_NAME_PROPERTY, componentName);
        } else {
            props.remove(BlueprintConstants.COMPONENT_NAME_PROPERTY);
        }
        for (ServiceProcessor processor : blueprintContainer.getProcessors(ServiceProcessor.class)) {
            processor.updateProperties(new PropertiesUpdater(), props);
        }

        registrationProperties = props;

        Set<String> classes = getClasses();
        String[] classArray = classes.toArray(new String[classes.size()]);

        LOGGER.debug("Registering service {} with interfaces {} and properties {}",
                     new Object[] { name, classes, props });

        if (registration.get() == null) {
            ServiceRegistration reg = blueprintContainer.registerService(classArray, new TriggerServiceFactory(this, metadata), props);
            if (!registration.compareAndSet(null, reg) && registration.get() != reg) {
                reg.unregister();
            } else {
                if (listeners != null) {
                    LOGGER.debug("Calling listeners for service registration");
                    for (ServiceListener listener : listeners) {
                        listener.register(service, registrationProperties);
                    }
                }
            }
        }
    }

    public void unregister() {
        ServiceRegistration reg = registration.get();
        if (reg != null) {
            LOGGER.debug("Unregistering service {}", name);
            // This method needs to allow reentrance, so if we need to make sure the registration is
            // set to null before actually unregistering the service
            if (listeners != null) {
                LOGGER.debug("Calling listeners for service unregistration");
                for (ServiceListener listener : listeners) {
                    listener.unregister(service, registrationProperties);
                }
            }
            AriesFrameworkUtil.safeUnregisterService(reg);
            
            registration.compareAndSet(reg, null);
        }
    }

    protected ServiceReference getReference() {
    	ServiceRegistration reg = registration.get();
        if (reg == null) {
            throw new IllegalStateException("Service is not registered");
        } else {
            return reg.getReference();
        }
    }

    protected void setProperties(Dictionary props) {
    	ServiceRegistration reg = registration.get();
        if (reg == null) {
            throw new IllegalStateException("Service is not registered");
        } else {
            reg.setProperties(props);
            // TODO: set serviceProperties? convert somehow? should listeners be notified of this?
        }
    }


    protected Object internalGetService() {
        return internalGetService(blueprintContainer.getBundleContext().getBundle(), null);
    }

    /**
     * Create the service object.
     *
     * @param bundle
     * @param registration
     * @return
     */
    private Object internalGetService(Bundle bundle, ServiceRegistration registration) {
        LOGGER.debug("Retrieving service for bundle {} and service registration {}", bundle, registration);
        if (this.service == null) {
            createService();
        }
        
        Object service = this.service;
        // We need the real service ...
        if (bundle != null) {
        	if (service instanceof ServiceFactory) {
        		service = ((ServiceFactory) service).getService(bundle, registration);
        	}
        	if (service == null) {
        		throw new IllegalStateException("service is null");
        	}
        	// Check if the service actually implement all the requested interfaces
        	validateClasses(service);
        	// We're not really interested in the service, but perform some sanity checks nonetheless
        } else {
        	if (!(service instanceof ServiceFactory)) {
        		// Check if the service actually implement all the requested interfaces
        		validateClasses(service);
        	}
        }
        
        return service;
    }

    private void createService() {
        try {
            LOGGER.debug("Creating service instance");
            //We can't use the BlueprintRepository because we don't know what interfaces
            //to use yet! We have to be a bit smarter.
            ExecutionContext old = ExecutionContext.Holder.setContext(blueprintContainer.getRepository());
           
            try {
            	Object o = serviceRecipe.create();
            
            	if (o instanceof Convertible) {
            		o = blueprintContainer.getRepository().convert(o, new ReifiedType(Object.class));
                    validateClasses(o);
            	} else if (o instanceof UnwrapperedBeanHolder) {
                    UnwrapperedBeanHolder holder = (UnwrapperedBeanHolder) o;
                    validateClasses(holder.unwrapperedBean);
                    o = BeanRecipe.wrap(holder, getClassesForProxying(holder.unwrapperedBean));
                } else {
                    validateClasses(o);
                }
            	service = o;
			} catch (Exception e) {
				LOGGER.error("Error retrieving service from " + this, e);
				throw new ComponentDefinitionException(e);
			} finally {
				ExecutionContext.Holder.setContext(old);
			}
            
            LOGGER.debug("Service created: {}", service);
            // When the service is first requested, we need to create listeners and call them
            if (listeners == null) {
                LOGGER.debug("Creating listeners");
                if (listenersRecipe != null) {
                    listeners = (List) createRecipe(listenersRecipe);
                } else {
                    listeners = Collections.emptyList();
                }
                LOGGER.debug("Listeners created: {}", listeners);
                if (registration.get() != null) {
                    LOGGER.debug("Calling listeners for initial service registration");
                    for (ServiceListener listener : listeners) {
                        listener.register(service, registrationProperties);
                    }
                } else {
                    LOGGER.debug("Calling listeners for initial service unregistration");
                    for (ServiceListener listener : listeners) {
                        listener.unregister(service, registrationProperties);
                    }
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("Error retrieving service from " + this, e);
            throw e;
        }
    }
    
    private void validateClasses(Object service) {
        // Check if the service actually implement all the requested interfaces
        if (metadata.getAutoExport() == ServiceMetadata.AUTO_EXPORT_DISABLED) {
            Set<String> allClasses = new HashSet<String>();
            ReflectionUtils.getSuperClasses(allClasses, service.getClass());
            ReflectionUtils.getImplementedInterfaces(allClasses, service.getClass());
            //This call is safe because we know that we don't need to call internalGet to get the answer
            Set<String> classes = getClasses();
            classes.removeAll(allClasses);
            if (!classes.isEmpty()) {
                throw new ComponentDefinitionException("The service implementation does not implement the required interfaces: " + classes);
            }
        }
    }

    public Object getService(Bundle bundle, ServiceRegistration registration) {
        /** getService() can get called before registerService() returns with the registration object.
         *  So we need to set the registration object in case registration listeners call 
         *  getServiceReference(). 
         */
    	this.registration.compareAndSet(null, registration);
        return internalGetService(bundle, registration);
    }

    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        if (this.service instanceof ServiceFactory) {
            ((ServiceFactory) this.service).ungetService(bundle, registration, service);
        }
    }

    /**
     * Be careful to avoid calling this method from internalGetService or createService before the service
     * field has been set. If you get this wrong you will get a StackOverflowError!
     * @return
     */
    private Set<String> getClasses() {
        Set<String> classes;
        switch (metadata.getAutoExport()) {
            case ServiceMetadata.AUTO_EXPORT_INTERFACES:
                classes = ReflectionUtils.getImplementedInterfaces(new HashSet<String>(), internalGetService().getClass());
                break;
            case ServiceMetadata.AUTO_EXPORT_CLASS_HIERARCHY:
                classes = ReflectionUtils.getSuperClasses(new HashSet<String>(), internalGetService().getClass());
                break;
            case ServiceMetadata.AUTO_EXPORT_ALL_CLASSES:
                classes = ReflectionUtils.getSuperClasses(new HashSet<String>(), internalGetService().getClass());
                classes = ReflectionUtils.getImplementedInterfaces(classes, internalGetService().getClass());
                break;
            default:
                classes = new HashSet<String>(metadata.getInterfaces());
                break;
        }
        return classes;
    }
    
    /**
     * Get the classes we need to proxy, for auto-export interfaces only, those 
     * will be just the interfaces implemented by the bean, for auto-export classes
     * or everything, then just proxying the real bean class will give us everything we
     * need, if none of the above then we need the class forms of the interfaces in
     * the metadata
     * 
     * Note that we use a template object here because we have already instantiated the bean
     * that we're going to proxy. We can't call internalGetService because it would Stack Overflow.
     * @return
     * @throws ClassNotFoundException
     */
    private Collection<Class<?>> getClassesForProxying(Object template) throws ClassNotFoundException {
      Collection<Class<?>> classes;
      switch (metadata.getAutoExport()) {
          case ServiceMetadata.AUTO_EXPORT_INTERFACES:
              classes = ReflectionUtils.getImplementedInterfacesAsClasses(new HashSet<Class<?>>(), template.getClass());
              break;
          case ServiceMetadata.AUTO_EXPORT_CLASS_HIERARCHY:
          case ServiceMetadata.AUTO_EXPORT_ALL_CLASSES:
            classes = ProxyUtils.asList(template.getClass());
              break;
          default:
              classes = new HashSet<Class<?>>(convertStringsToClasses(metadata.getInterfaces()));
              break;
      }
      return classes;
  }

    private Collection<? extends Class<?>> convertStringsToClasses(
        List<String> interfaces) throws ClassNotFoundException {
      Set<Class<?>> classes = new HashSet<Class<?>>();
      for(String s : interfaces) {
        classes.add(blueprintContainer.loadClass(s)); 
      }
      return classes;
    }

    private void createExplicitDependencies() {
        if (explicitDependencies != null) {
            for (Recipe recipe : explicitDependencies) {
                createRecipe(recipe);
            }
        }
    }
    
    private Object createRecipe(Recipe recipe) {
        String name = recipe.getName();
        Repository repo = blueprintContainer.getRepository();
        if (repo.getRecipe(name) != recipe) {
            repo.putRecipe(name, recipe);
        }
        return repo.create(name);
    }
    
    private String getComponentName() {
        if (metadata.getServiceComponent() instanceof RefMetadata) {
            RefMetadata ref = (RefMetadata) metadata.getServiceComponent();
            return ref.getComponentId();
        } else {
            return null;
        }
    }

    protected void incrementActiveCalls()
    {
    	  synchronized(monitor)
    	  {
    		    activeCalls++;	
		    }
    }
    
  	protected void decrementActiveCalls() 
  	{
  	    List<DestroyCallback> callbacksToCall = new ArrayList<DestroyCallback>();
      	synchronized(monitor)
      	{
      	    activeCalls--;
  			    if(quiesce && activeCalls == 0) {
  			        callbacksToCall.addAll(destroyCallbacks);
  			        destroyCallbacks.clear();
  			    }
      	}
      	if(!!!callbacksToCall.isEmpty()) {
      	    for(DestroyCallback cbk : callbacksToCall)
      	        cbk.callback();
      	}
  	}
	
    public void quiesce(DestroyCallback destroyCallback)
    {
    	  unregister();
    	  int calls;
    	  synchronized (monitor) {
            if(activeCalls != 0)
              destroyCallbacks.add(destroyCallback);
    	      quiesce = true;
            calls = activeCalls;
        }
    	  if(calls == 0) {
    	      destroyCallback.callback();
    	  }
    }
     
    private class TriggerServiceFactory implements ServiceFactory 
    {
    	private ServiceRecipe serviceRecipe;
    	private ComponentMetadata cm;
    	private ServiceMetadata sm;
        private boolean isQuiesceAvailable;
    	public TriggerServiceFactory(ServiceRecipe serviceRecipe, ServiceMetadata cm)
    	{
    		this.serviceRecipe = serviceRecipe;
    		this.cm = cm;
    		this.sm = cm;
            this.isQuiesceAvailable = isClassAvailable("org.apache.aries.quiesce.participant.QuiesceParticipant");
    	}
    	
        public Object getService(Bundle bundle, ServiceRegistration registration) {
            Object original = ServiceRecipe.this.getService(bundle, registration);
            LOGGER.debug(LOG_ENTRY, "getService", original);

            List<Interceptor> interceptors = new ArrayList<Interceptor>();
            ComponentDefinitionRegistry reg = blueprintContainer.getComponentDefinitionRegistry();
            List<Interceptor> registeredInterceptors = reg.getInterceptors(cm);
            if (registeredInterceptors != null) {
                interceptors.addAll(registeredInterceptors);
            }
            // Add quiesce interceptor if needed
            if (isQuiesceAvailable)
            {
                interceptors.add(new QuiesceInterceptor(serviceRecipe));
            }
            // Exit if no interceptors configured
            if (interceptors.isEmpty()) {
                return original;
            }

            Object intercepted;
            try {
                Bundle b = FrameworkUtil.getBundle(original.getClass());
                if (b == null) {
                  // we have a class from the framework parent, so use our bundle for proxying.
                  b = blueprintContainer.getBundleContext().getBundle();
                }
                InvocationListener collaborator = new Collaborator(cm, interceptors);

                intercepted = blueprintContainer.getProxyManager().createInterceptingProxy(b,
                        getClassesForProxying(original), original, collaborator);
            } catch (Exception u) {
                Bundle b = blueprintContainer.getBundleContext().getBundle();
                LOGGER.info("Unable to create a proxy object for the service " + getName() + " defined in bundle " + b.getSymbolicName() + " at version " + b.getVersion() + " with id " + b.getBundleId() + ". Returning the original object instead.", u);
                LOGGER.debug(LOG_EXIT, "getService", original);
                return original;
            }

            LOGGER.debug(LOG_EXIT, "getService", intercepted);
            return intercepted;
        }

        public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
            ServiceRecipe.this.ungetService(bundle, registration, service);
        }

    }

    private class ServiceRegistrationProxy implements ServiceRegistration {

        public ServiceReference getReference() {
            return ServiceRecipe.this.getReference();
        }

        public void setProperties(Dictionary properties) {
            ServiceRecipe.this.setProperties(properties);
        }

        public void unregister() {
            throw new UnsupportedOperationException();
        }
    }

    private class PropertiesUpdater implements ServiceProcessor.ServicePropertiesUpdater {

        public String getId() {
            return metadata.getId();
        }

        public void updateProperties(Dictionary properties) {
            Hashtable table = JavaUtils.getProperties(ServiceRecipe.this.getReference());
            JavaUtils.copy(table, properties);
            ServiceRecipe.this.setProperties(table);
        }        
    }

    private boolean isClassAvailable(String clazz) {
        try {
            getClass().getClassLoader().loadClass(clazz);
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
        catch (NoClassDefFoundError e) {
            return false;
        }
    }

}
