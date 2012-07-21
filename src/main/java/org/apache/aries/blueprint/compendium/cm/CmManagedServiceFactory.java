/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.compendium.cm;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.aries.blueprint.BeanProcessor;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.ServiceProcessor;
import org.apache.aries.blueprint.utils.JavaUtils;
import org.apache.aries.blueprint.utils.ReflectionUtils;
import org.apache.aries.blueprint.utils.ServiceListener;
import org.apache.aries.util.AriesFrameworkUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: if we need to make those exported services tied to their references as for other <service/> elements
 * TODO: it becomes a problem as currently we would have to create a specific recipe or something like that
 *
 * @version $Rev$, $Date$
 */
public class CmManagedServiceFactory {

    static final int CONFIGURATION_ADMIN_OBJECT_DELETED = 1;

    static final int BUNDLE_STOPPING = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(CmManagedServiceFactory.class);
    
    private ExtendedBlueprintContainer blueprintContainer;
    private ConfigurationAdmin configAdmin;
    private String id;
    private String factoryPid;
    private List<String> interfaces;
    private int autoExport;
    private int ranking;
    private Map serviceProperties;
    private String managedComponentName;
    private String componentDestroyMethod;
    private List<ServiceListener> listeners;
    private final Object lock = new Object();

    private ServiceRegistration registration;
    private final Map<String, ServiceRegistration> pids = new ConcurrentHashMap<String, ServiceRegistration>();
    private final Map<ServiceRegistration, Object> services = new ConcurrentHashMap<ServiceRegistration, Object>();

    public void init() throws Exception {
        LOGGER.debug("Initializing CmManagedServiceFactory for factoryPid={}", factoryPid);
        Properties props = new Properties();
        props.put(Constants.SERVICE_PID, factoryPid);
        Bundle bundle = blueprintContainer.getBundleContext().getBundle();
        props.put(Constants.BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());
        props.put(Constants.BUNDLE_VERSION, bundle.getHeaders().get(Constants.BUNDLE_VERSION));
        
        synchronized(lock) {
            registration = blueprintContainer.getBundleContext().registerService(ManagedServiceFactory.class.getName(), new ConfigurationWatcher(), props);
        
            String filter = '(' + ConfigurationAdmin.SERVICE_FACTORYPID + '=' + this.factoryPid + ')';
            Configuration[] configs = configAdmin.listConfigurations(filter);
            if (configs != null) {
                for (Configuration config : configs) {
                    updated(config.getPid(), config.getProperties());
                }
            }
        }
    }

    public void destroy() {
        AriesFrameworkUtil.safeUnregisterService(registration);
        for (Map.Entry<ServiceRegistration, Object> entry : services.entrySet()) {
            destroy(entry.getValue(), entry.getKey(), BUNDLE_STOPPING);
        }
        services.clear();
        pids.clear();
    }

    private void destroy(Object component, ServiceRegistration registration, int code) {
        if (listeners != null) {
            ServiceReference ref = registration.getReference();
            for (ServiceListener listener : listeners) {
                Hashtable props = JavaUtils.getProperties(ref);
                listener.unregister(component, props);
            }
        }
        destroyComponent(component, code);
        AriesFrameworkUtil.safeUnregisterService(registration);
    }
    
    public Map<ServiceRegistration, Object> getServiceMap() {
        return Collections.unmodifiableMap(services);
    }

    public void setBlueprintContainer(ExtendedBlueprintContainer blueprintContainer) {
        this.blueprintContainer = blueprintContainer;
    }

    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public void setListeners(List<ServiceListener> listeners) {
        this.listeners = listeners;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public void setFactoryPid(String factoryPid) {
        this.factoryPid = factoryPid;
    }

    public void setInterfaces(List<String> interfaces) {
        this.interfaces = interfaces;
    }

    public void setAutoExport(int autoExport) {
        this.autoExport = autoExport;
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
    }

    public void setServiceProperties(Map serviceProperties) {
        this.serviceProperties = serviceProperties;
    }
    
    public void setManagedComponentName(String managedComponentName) {
        this.managedComponentName = managedComponentName;
    }

    public void setComponentDestroyMethod(String componentDestroyMethod) {
        this.componentDestroyMethod = componentDestroyMethod;
    }
    
    protected void updated(String pid, Dictionary props) {
      LOGGER.debug("Updated configuration {} with props {}", pid, props);

      Hashtable regProps = null;
      Object component = null;

      // This method might be multithreaded, so synchronize checking and
      // creating the service
      final ServiceRegistration existingReg;
      synchronized (pids) {
         existingReg = pids.get(pid);
         if (existingReg == null) {
            updateComponentProperties(props);

            component = blueprintContainer.getComponentInstance(managedComponentName);

            // TODO: call listeners, etc...

            regProps = getRegistrationProperties(pid);
            CmProperties cm = findServiceProcessor();
            if (cm != null) {
               if ("".equals(cm.getPersistentId())) {
                  JavaUtils.copy(regProps, props);
               }
               cm.updateProperties(new PropertiesUpdater(pid), regProps);
            }

            Set<String> classes = getClasses(component);
            String[] classArray = classes.toArray(new String[classes.size()]);
            ServiceRegistration reg = blueprintContainer.getBundleContext().registerService(classArray, component, regProps);

            LOGGER.debug("Service {} registered with interfaces {} and properties {}", new Object[] { component, classes, regProps });

            services.put(reg, component);
            pids.put(pid, reg);
         }
        } // end of synchronization
        
        // If we just registered a service, do the slower stuff outside the synchronized block
        if (existingReg == null)
        {
            if (listeners != null) {
                for (ServiceListener listener : listeners) {
                    listener.register(component, regProps);
                }
            }
        } else {
            updateComponentProperties(props);
            
            CmProperties cm = findServiceProcessor();
            if (cm != null && "".equals(cm.getPersistentId())) {
                regProps = getRegistrationProperties(pid);    
                JavaUtils.copy(regProps, props);
                cm.updated(regProps);
            }
        }
    }

    private Hashtable getRegistrationProperties(String pid) {
        Hashtable regProps = new Hashtable();
        if (serviceProperties != null) {
            regProps.putAll(serviceProperties);
        }
        regProps.put(Constants.SERVICE_PID, pid);
        regProps.put(Constants.SERVICE_RANKING, ranking);
        return regProps;
    }
    
    private void updateComponentProperties(Dictionary props) {
        CmManagedProperties cm = findBeanProcessor();
        if (cm != null) {
            cm.updated(props);
        }
    }
    
    private CmManagedProperties findBeanProcessor() {
        for (BeanProcessor beanProcessor : blueprintContainer.getProcessors(BeanProcessor.class)) {
            if (beanProcessor instanceof CmManagedProperties) {
                CmManagedProperties cm = (CmManagedProperties) beanProcessor;
                if (managedComponentName.equals(cm.getBeanName()) && "".equals(cm.getPersistentId())) {
                    return cm;
                }
            }
        }
        return null;
    }
        
    private CmProperties findServiceProcessor() {
        for (ServiceProcessor processor : blueprintContainer.getProcessors(ServiceProcessor.class)) {
            if (processor instanceof CmProperties) {
                CmProperties cm = (CmProperties) processor;
                if (id.equals(cm.getServiceId())) {
                    return cm;
                }
            }
        }
        return null;
    }
        
    private void destroyComponent(Object instance, int reason) {
        Method method = findDestroyMethod(instance.getClass());
        if (method != null) {
            try {
                method.invoke(instance, new Object [] { reason });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private Method findDestroyMethod(Class clazz) {
        Method method = null;        
        if (componentDestroyMethod != null && componentDestroyMethod.length() > 0) {
            List<Method> methods = ReflectionUtils.findCompatibleMethods(clazz, componentDestroyMethod, new Class [] { int.class });
            if (methods != null && !methods.isEmpty()) {
                method = methods.get(0);
            }
        }
        return method;
    }
    
    protected void deleted(String pid) {
        LOGGER.debug("Deleted configuration {}", pid);
        ServiceRegistration reg = pids.remove(pid);
        if (reg != null) {
            Object component = services.remove(reg);
            destroy(component, reg, CONFIGURATION_ADMIN_OBJECT_DELETED);
        }
    }

    private Set<String> getClasses(Object service) {
        Class serviceClass = service.getClass();
        Set<String> classes;
        switch (autoExport) {
            case ServiceMetadata.AUTO_EXPORT_INTERFACES:
                classes = ReflectionUtils.getImplementedInterfaces(new HashSet<String>(), serviceClass);
                break;
            case ServiceMetadata.AUTO_EXPORT_CLASS_HIERARCHY:
                classes = ReflectionUtils.getSuperClasses(new HashSet<String>(), serviceClass);
                break;
            case ServiceMetadata.AUTO_EXPORT_ALL_CLASSES:
                classes = ReflectionUtils.getSuperClasses(new HashSet<String>(), serviceClass);
                classes = ReflectionUtils.getImplementedInterfaces(classes, serviceClass);
                break;
            default:
                classes = new HashSet<String>(interfaces);
                break;
        }
        return classes;
    }
    
    private class ConfigurationWatcher implements ManagedServiceFactory {

        public String getName() {
            return null;
        }

        public void updated(String pid, Dictionary props) throws ConfigurationException {
            CmManagedServiceFactory.this.updated(pid, props);
        }

        public void deleted(String pid) {
            CmManagedServiceFactory.this.deleted(pid);
        }
    }

    private class PropertiesUpdater implements ServiceProcessor.ServicePropertiesUpdater {

        private String pid;
        
        public PropertiesUpdater(String pid) {
            this.pid = pid;
        }
        
        public String getId() {
            return id;
        }

        public void updateProperties(Dictionary properties) {
            ServiceRegistration reg = pids.get(pid);
            if (reg != null) {
                ServiceReference ref = reg.getReference();
                if (ref != null) {
                    Hashtable table = JavaUtils.getProperties(ref);
                    JavaUtils.copy(table, properties);
                    reg.setProperties(table);
                }
            }
        }
    }
   
}
