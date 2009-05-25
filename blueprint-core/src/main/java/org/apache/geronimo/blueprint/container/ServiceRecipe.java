package org.apache.geronimo.blueprint.container;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.geronimo.blueprint.BlueprintConstants;
import org.apache.geronimo.blueprint.ExtendedBlueprintContainer;
import org.apache.geronimo.blueprint.di.AbstractRecipe;
import org.apache.geronimo.blueprint.di.CollectionRecipe;
import org.apache.geronimo.blueprint.di.DefaultExecutionContext;
import org.apache.geronimo.blueprint.di.DefaultRepository;
import org.apache.geronimo.blueprint.di.ExecutionContext;
import org.apache.geronimo.blueprint.di.MapRecipe;
import org.apache.geronimo.blueprint.di.Recipe;
import org.apache.geronimo.blueprint.di.RefRecipe;
import org.apache.geronimo.blueprint.di.Repository;
import org.apache.geronimo.blueprint.utils.ReflectionUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.RegistrationListener;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A <code>Recipe</code> to export services into the OSGi registry.
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 776360 $, $Date: 2009-05-19 17:40:47 +0200 (Tue, 19 May 2009) $
 */
public class ServiceRecipe extends AbstractRecipe implements ServiceRegistration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRecipe.class);

    private ExtendedBlueprintContainer blueprintContainer;
    private ServiceMetadata metadata;
    private Recipe serviceRecipe;
    private CollectionRecipe listenersRecipe;
    private MapRecipe propertiesRecipe;

    private Map properties;
    private ServiceRegistration registration;
    private Map registrationProperties;
    private List<Listener> listeners;
    private Map<Bundle, Entry> instanceMap = Collections.synchronizedMap(new HashMap<Bundle, Entry>());
    private Object service;
    private boolean bundleScope;

    public ServiceRecipe(String name,
                               ExtendedBlueprintContainer blueprintContainer,
                               ServiceMetadata metadata,
                               Recipe serviceRecipe,
                               CollectionRecipe listenersRecipe,
                               MapRecipe propertiesRecipe) {
        super(name);
        this.blueprintContainer = blueprintContainer;
        this.metadata = metadata;
        this.serviceRecipe = serviceRecipe;
        this.listenersRecipe = listenersRecipe;
        this.propertiesRecipe = propertiesRecipe;
    }

    protected Object internalCreate() throws ComponentDefinitionException {
        return this;
    }

    public synchronized void register() {
        if (registration != null) {
            return;
        }
        Hashtable props = new Hashtable();
        if (properties == null) {
            properties = (Map) createSimpleRecipe(propertiesRecipe);
        }
        props.putAll(properties);
        props.put(Constants.SERVICE_RANKING, metadata.getRanking());
        String componentName = getComponentName();
        if (componentName != null) {
            props.put(BlueprintConstants.COMPONENT_NAME_PROPERTY, componentName);
        }
        Set<String> classes = getClasses();
        String[] classArray = classes.toArray(new String[classes.size()]);
        registration = blueprintContainer.getBundleContext().registerService(classArray, new TriggerServiceFactory(), props);
        registrationProperties = props;

        LOGGER.debug("Trigger service {} registered with interfaces {} and properties {}",
                     new Object[] { this, classes, props });
    }

    public synchronized boolean isRegistered() {
        return registration != null;
    }

    public synchronized ServiceReference getReference() {
        if (registration == null) {
            throw new IllegalStateException();
        } else {
            return registration.getReference();
        }
    }

    public synchronized void setProperties(Dictionary props) {
        if (registration == null) {
            throw new IllegalStateException();
        } else {
            registration.setProperties(props);
            // TODO: set serviceProperties? convert somehow? should listeners be notified of this?
        }
    }


    public synchronized void unregister() {
        if (registration != null) {
            // TODO: shouldn't listeners be called before unregistering the service?
            registration.unregister();
            if (listeners != null) {
                LOGGER.debug("Calling listeners for service unregistration");
                for (Listener listener : listeners) {
                    listener.unregister(this);
                }
            }
            LOGGER.debug("Service {} unregistered", service);
            registration = null;
        }
    }

    public Object getService() {
        return getService(blueprintContainer.getBundleContext().getBundle(), null);
    }

    public Object getService(Bundle bundle, ServiceRegistration registration) {
        LOGGER.debug("Retrieving service for bundle {} and service registration {}", bundle, registration);
        // Create initial service
        if (this.service == null) {
            synchronized (this) {
                if (this.service == null) {
                    bundleScope = isBundleScope(metadata.getServiceComponent());
                    LOGGER.debug("Creating service instance (bundle scope = {})", bundleScope);
                    this.service = createInstance(false);
                    LOGGER.debug("Service created: {}", this.service);
                    // When the service is first requested, we need to create listeners and call them
                    if (listeners == null) {
                        LOGGER.debug("Creating listeners");
                        if (listenersRecipe != null) {
                            listeners = (List) createSimpleRecipe(listenersRecipe);
                        } else {
                            listeners = Collections.emptyList();
                        }
                        LOGGER.debug("Listeners created: {}", listeners);
                        LOGGER.debug("Calling listeners for service registration");
                        for (Listener listener : listeners) {
                            listener.register(this);
                        }
                    }
                }
            }
        }
        Object service = this.service;
        if (service instanceof ServiceFactory) {
            service = ((ServiceFactory) service).getService(bundle, registration);
        } else if (bundleScope) {
            Entry entry;
            synchronized(bundle) {
                entry = instanceMap.get(bundle);
                if (entry == null) {
                    entry = new Entry(createInstance(true));
                    LOGGER.debug("Created service instance for bundle: " + bundle + " " + entry.getServiceInstance().hashCode());
                    instanceMap.put(bundle, entry);
                }
                entry.addServiceRegistration(registration);
            }
            service = entry.getServiceInstance();
        }
        return service;
    }

    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        if (bundleScope) {
            synchronized(bundle) {
                Entry entry = instanceMap.get(bundle);
                if (entry != null) {
                    entry.removeServiceRegistration(registration);
                    if (!entry.hasServiceRegistrations()) {
                        destroyInstance(entry.getServiceInstance());
                        LOGGER.debug("Destroyed service instance for bundle: " + bundle);
                        instanceMap.remove(bundle);
                    }
                }
            }
        }
    }

    private Set<String> getClasses() {
        Set<String> classes;
        switch (metadata.getAutoExportMode()) {
            case ServiceMetadata.AUTO_EXPORT_INTERFACES:
                classes = ReflectionUtils.getImplementedInterfaces(new HashSet<String>(), getService().getClass());
                break;
            case ServiceMetadata.AUTO_EXPORT_CLASS_HIERARCHY:
                classes = ReflectionUtils.getSuperClasses(new HashSet<String>(), getService().getClass());
                break;
            case ServiceMetadata.AUTO_EXPORT_ALL_CLASSES:
                classes = ReflectionUtils.getSuperClasses(new HashSet<String>(), getService().getClass());
                classes = ReflectionUtils.getImplementedInterfaces(classes, getService().getClass());
                break;
            default:
                classes = new HashSet<String>(metadata.getInterfaceNames());
                break;
        }
        return classes;
    }

    private Object createInstance(boolean scoped) {
        if (scoped) {
            Recipe recipe = serviceRecipe;
            Repository objectRepository = blueprintContainer.getRepository();
            if (recipe instanceof RefRecipe) {
                recipe = (Recipe) objectRepository.get(((RefRecipe) recipe).getReferenceName());
            }
            DefaultRepository repository = new DefaultRepository((DefaultRepository) objectRepository);
            repository.putRecipe(recipe.getName(), recipe);
            BlueprintObjectInstantiator graph = new BlueprintObjectInstantiator(blueprintContainer, repository);
            return graph.create(recipe.getName());
        } else {
            return createSimpleRecipe(serviceRecipe);
        }
    }

    private Object createSimpleRecipe(Recipe recipe) {
        String name = recipe.getName();
        DefaultRepository repo = (DefaultRepository) blueprintContainer.getRepository();
        if (repo.getRecipe(name) == null) {
            repo.putRecipe(name, recipe);
        }
        BlueprintObjectInstantiator graph = new BlueprintObjectInstantiator(blueprintContainer, repo);
        return graph.create(name);
    }

    private void destroyInstance(Object instance) {
        Recipe recipe = serviceRecipe;
        Repository objectRepository = blueprintContainer.getRepository();
        if (recipe instanceof RefRecipe) {
            recipe = (Recipe) objectRepository.get(((RefRecipe) recipe).getReferenceName());
        }
        ((BeanRecipe) recipe).destroyInstance(instance);
    }

    private boolean isBundleScope(Metadata value) {
        ComponentMetadata metadata = null;
        if (value instanceof RefMetadata) {
            RefMetadata ref = (RefMetadata) value;
            metadata = blueprintContainer.getComponentDefinitionRegistry().getComponentDefinition(ref.getComponentId());
        } else if (value instanceof ComponentMetadata) {
            metadata = (ComponentMetadata) value;
        }
        if (metadata instanceof BeanMetadata) {
            BeanMetadata bean = (BeanMetadata) metadata;
            Class clazz = bean.getRuntimeClass();
            if (clazz == null) {
                boolean isNewContext = !ExecutionContext.isContextSet();
                if (isNewContext) {
                    ExecutionContext.setContext(new DefaultExecutionContext(blueprintContainer, blueprintContainer.getRepository()));
                }
                try {
                    clazz = loadClass(bean.getClassName());
                } finally {
                    if (isNewContext) {
                        ExecutionContext.setContext(null);
                    }
                }
            }
            if (ServiceFactory.class.isAssignableFrom(clazz)) {
                return false;
            } else {
                return BeanMetadata.SCOPE_BUNDLE.equals(bean.getScope());
            }
        } else {
            return false;
        }
    }

    private String getComponentName() {
        if (metadata.getServiceComponent() instanceof RefMetadata) {
            RefMetadata ref = (RefMetadata) metadata.getServiceComponent();
            return ref.getComponentId();
        } else {
            return null;
        }
    }

    private class TriggerServiceFactory implements ServiceFactory {

        public Object getService(Bundle bundle, ServiceRegistration registration) {
            return ServiceRecipe.this.getService(bundle, registration);
        }

        public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
            ServiceRecipe.this.ungetService(bundle, registration, service);
        }

    }

    public static class Listener {

        private Object listener;
        private RegistrationListener metadata;

        private List<Method> registerMethods;
        private List<Method> unregisterMethods;
        private boolean initialized = false;

        public void setListener(Object listener) {
            this.listener = listener;
        }

        public void setMetadata(RegistrationListener metadata) {
            this.metadata = metadata;
        }

        public void register(ServiceRecipe recipe) {
            init(recipe);
            invokeMethod(registerMethods, recipe);
        }

        public void unregister(ServiceRecipe recipe) {
            invokeMethod(unregisterMethods, recipe);
        }

        private synchronized void init(ServiceRecipe recipe) {
            if (initialized) {
                return;
            }
            Object service = recipe.getService();
            Class[] paramTypes = new Class[] { service.getClass(), Map.class };
            Class listenerClass = listener.getClass();

            if (metadata.getRegistrationMethodName() != null) {
                registerMethods = ReflectionUtils.findCompatibleMethods(listenerClass, metadata.getRegistrationMethodName(), paramTypes);
            }
            if (metadata.getUnregistrationMethodName() != null) {
                unregisterMethods = ReflectionUtils.findCompatibleMethods(listenerClass, metadata.getUnregistrationMethodName(), paramTypes);
            }
            initialized = true;
        }

        private void invokeMethod(List<Method> methods, ServiceRecipe recipe) {
            if (methods == null || methods.isEmpty()) {
                return;
            }
            Object service = recipe.getService();
            Map properties = recipe.registrationProperties;
            Object[] args = new Object[] { service, properties };
            for (Method method : methods) {
                try {
                    method.invoke(listener, args);
                } catch (Exception e) {
                    LOGGER.info("Error calling listener method " + method, e);
                }
            }
        }

    }

    private static class Entry {

        private final Object serviceInstance;
        private final Set<ServiceRegistration> registrations = new HashSet<ServiceRegistration>();

        public Entry(Object serviceInstance) {
            this.serviceInstance = serviceInstance;
        }

        public Object getServiceInstance() {
            return this.serviceInstance;
        }

        public boolean hasServiceRegistrations() {
            return !registrations.isEmpty();
        }

        public void addServiceRegistration(ServiceRegistration registration) {
            registrations.add(registration);
        }

        public void removeServiceRegistration(ServiceRegistration registration) {
            registrations.remove(registration);
        }

    }

}
