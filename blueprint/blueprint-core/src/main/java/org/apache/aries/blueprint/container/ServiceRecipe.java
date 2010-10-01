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
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.aries.blueprint.BlueprintConstants;
import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.blueprint.ServiceProcessor;
import org.apache.aries.blueprint.di.AbstractRecipe;
import org.apache.aries.blueprint.di.CollectionRecipe;
import org.apache.aries.blueprint.di.MapRecipe;
import org.apache.aries.blueprint.di.Recipe;
import org.apache.aries.blueprint.di.Repository;
import org.apache.aries.blueprint.proxy.AsmInterceptorWrapper;
import org.apache.aries.blueprint.utils.JavaUtils;
import org.apache.aries.blueprint.utils.ReflectionUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
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

    private final ExtendedBlueprintContainer blueprintContainer;
    private final ServiceMetadata metadata;
    private final Recipe serviceRecipe;
    private final CollectionRecipe listenersRecipe;
    private final MapRecipe propertiesRecipe;
    private final List<Recipe> explicitDependencies;

    private Map properties;
    private final AtomicBoolean registered = new AtomicBoolean();
    private final AtomicReference<ServiceRegistration> registration = new AtomicReference<ServiceRegistration>();
    private Map registrationProperties;
    private List<ServiceListener> listeners;
    private volatile Object service;
    private int activeCalls;
    private boolean quiesce;
    private DestroyCallback destroyCallback;
    
    public ServiceRecipe(String name,
                         ExtendedBlueprintContainer blueprintContainer,
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
        return registered.get();
    }

    public void register() {
        int state = blueprintContainer.getBundleContext().getBundle().getState();
        if (state != Bundle.ACTIVE && state != Bundle.STARTING) {
            return;
        }
        if (registered.compareAndSet(false, true)) {
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

            registration.set(blueprintContainer.registerService(classArray, new TriggerServiceFactory(this, metadata), props));            
        }
    }

    public void unregister() {
        if (registered.compareAndSet(true, false)) {
            LOGGER.debug("Unregistering service {}", name);
            // This method needs to allow reentrance, so if we need to make sure the registration is
            // set to null before actually unregistering the service
            ServiceRegistration reg = registration.get();
            if (listeners != null) {
                LOGGER.debug("Calling listeners for service unregistration");
                for (ServiceListener listener : listeners) {
                    listener.unregister(service, registrationProperties);
                }
            }
            if (reg != null) {
                reg.unregister();
            }
            
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
     * We need to synchronize the access to the repository,
     * but not on this ServiceRecipe instance to avoid deadlock.
     * When using internalCreate(), no other lock but the on the repository
     * should be held.
     *
     * @param bundle
     * @param registration
     * @return
     */
    private Object internalGetService(Bundle bundle, ServiceRegistration registration) {
        LOGGER.debug("Retrieving service for bundle {} and service registration {}", bundle, registration);
        if (this.service == null) {
            synchronized (blueprintContainer.getRepository().getInstanceLock()) {
                if (this.service == null) {
                    createService();
                }
            }
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
            service = createRecipe(serviceRecipe);
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
                if (registered.get()) {
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
    	synchronized(this) 
    	{
    		activeCalls++;	
		}
    }
    
	protected void decrementActiveCalls() 
	{
		
    	synchronized(this) 
    	{
    		activeCalls--;

			if (quiesce && activeCalls == 0)
			{
				destroyCallback.callback(service);
			}
    	}
	}
	
    public void quiesce(DestroyCallback destroyCallback)
    {
    	this.destroyCallback = destroyCallback;
    	quiesce = true;
    	unregister();
    	if(activeCalls == 0)
		{
			destroyCallback.callback(service);
		}
    }
     
    private class TriggerServiceFactory implements ServiceFactory 
    {
    	private QuiesceInterceptor interceptor;
    	private ServiceRecipe serviceRecipe;
    	private ComponentMetadata cm;
    	private ServiceMetadata sm;
    	public TriggerServiceFactory(ServiceRecipe serviceRecipe, ServiceMetadata cm)
    	{
    		this.serviceRecipe = serviceRecipe;
    		this.cm = cm;
    		this.sm = cm;
    	}
    	
        public Object getService(Bundle bundle, ServiceRegistration registration) {
            Object original = ServiceRecipe.this.getService(bundle,
                    registration);
            LOGGER.debug(LOG_ENTRY, "getService", original);
            Object intercepted;

            if (interceptor == null) {
                interceptor = new QuiesceInterceptor(serviceRecipe);
            }

            List<Interceptor> interceptors = new ArrayList<Interceptor>();
            interceptors.add(interceptor);

            //check for any registered interceptors for this metadata
            ComponentDefinitionRegistry reg = blueprintContainer.getComponentDefinitionRegistry();
            List<Interceptor> registeredInterceptors = reg.getInterceptors(cm);
            //add the registered interceptors to the list of interceptors
            if (registeredInterceptors != null && registeredInterceptors.size()>0){
              interceptors.addAll(registeredInterceptors);
            }
            
            try {
                // Try load load an asm class (to make sure it's actually
                // available)
                getClass().getClassLoader().loadClass(
                        "org.objectweb.asm.ClassVisitor");
                LOGGER.debug("asm available for interceptors");
            } catch (Throwable t) {
                LOGGER
                        .info("A problem occurred trying to create a proxy object. Returning the original object instead.");
                LOGGER.debug(LOG_EXIT, "getService", original);
                return original;
            }

            try {
                Set<String> interfaces = getClasses();

                // check for the case where interfaces is null or empty
                if (interfaces == null || interfaces.isEmpty()) {
                    intercepted = AsmInterceptorWrapper.createProxyObject(
                            original.getClass().getClassLoader(), cm,
                            interceptors, AsmInterceptorWrapper.passThrough(original),
                            original.getClass());
                    LOGGER.debug(LOG_EXIT, "getService", intercepted);
                    return intercepted;
                }
                Class[] classesToProxy = new Class[interfaces.size()];
                Iterator<String> it = interfaces.iterator();
                for (int i = 0; i < interfaces.size(); i++) {
                    classesToProxy[i] = Class.forName(it.next(),
                            true, original.getClass().getClassLoader());
                }

                // if asm is available we can proxy the original object with
                // the AsmInterceptorWrapper
                intercepted = AsmInterceptorWrapper.createProxyObject(
                        original.getClass().getClassLoader(), cm,
                        interceptors, AsmInterceptorWrapper.passThrough(original),
                        classesToProxy);
            } catch (Throwable u) {
                LOGGER
                        .info("A problem occurred trying to create a proxy object. Returning the original object instead.");
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

}
