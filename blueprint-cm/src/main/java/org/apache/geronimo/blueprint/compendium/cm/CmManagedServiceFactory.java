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
package org.apache.geronimo.blueprint.compendium.cm;

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

import org.apache.geronimo.blueprint.BeanProcessor;
import org.apache.geronimo.blueprint.ExtendedBlueprintContainer;
import org.apache.geronimo.blueprint.utils.ReflectionUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
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
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 766508 $, $Date: 2009-04-19 22:09:27 +0200 (Sun, 19 Apr 2009) $
 */
public class CmManagedServiceFactory {

    static final int CONFIGURATION_ADMIN_OBJECT_DELETED = 1;

    static final int BUNDLE_STOPPING = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(CmManagedServiceFactory.class);
    
    private ExtendedBlueprintContainer blueprintContainer;
    private ConfigurationAdmin configAdmin;
    private String factoryPid;
    private List<String> interfaces;
    private int autoExport;
    private int ranking;
    private Map serviceProperties;
    private String managedComponentName;
    private String componentDestroyMethod;
    private final Object lock = new Object();

    private ServiceRegistration registration;
    private Map<String, ServiceRegistration> pids = new ConcurrentHashMap<String, ServiceRegistration>();
    private Map<ServiceRegistration, Object> services = new ConcurrentHashMap<ServiceRegistration, Object>();

    public void init() {
        LOGGER.debug("Initializing CmManagedServiceFactory for factoryPid={}", factoryPid);
        Properties props = new Properties();
        props.put(Constants.SERVICE_PID, factoryPid);
        Bundle bundle = blueprintContainer.getBundleContext().getBundle();
        props.put(Constants.BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());
        props.put(Constants.BUNDLE_VERSION, bundle.getHeaders().get(Constants.BUNDLE_VERSION));
        
        synchronized(lock) {
            registration = blueprintContainer.getBundleContext().registerService(ManagedServiceFactory.class.getName(), new ConfigurationWatcher(), props);
        
            String filter = '(' + ConfigurationAdmin.SERVICE_FACTORYPID + '=' + this.factoryPid + ')';
            try {
                Configuration[] configs = configAdmin.listConfigurations(filter);
                if (configs != null) {
                    for (Configuration config : configs) {
                        updated(config.getPid(), config.getProperties());
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Unable to retrieve initial configurations for factoryPid={}", factoryPid, e);
            }
        }
    }

    public void destroy() {
        if (registration != null) {
            registration.unregister();
        }
        for (Map.Entry<ServiceRegistration, Object> entry : services.entrySet()) {
            destroyComponent(entry.getValue(), BUNDLE_STOPPING);
            entry.getKey().unregister();
        }
        services.clear();
        pids.clear();
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
        ServiceRegistration reg = pids.get(pid);
        if (reg == null) {      
            updateComponentProperties(props);

            Object component = blueprintContainer.getComponentInstance(managedComponentName);
            //  TODO: init instance, call listeners, etc...
        
            Hashtable regProps = new Hashtable();
            if (serviceProperties != null) {
                regProps.putAll(serviceProperties);
            }
            regProps.put(Constants.SERVICE_PID, pid);
            regProps.put(Constants.SERVICE_RANKING, ranking);
            Set<String> classes = getClasses(component);
            String[] classArray = classes.toArray(new String[classes.size()]);
            reg = blueprintContainer.getBundleContext().registerService(classArray, component, regProps);
            LOGGER.debug("Service {} registered with interfaces {} and properties {}", new Object [] { component, classes, regProps });
            
            services.put(reg, component);
            pids.put(pid, reg);
        } else {
            updateComponentProperties(props);
        }
    }

    private void updateComponentProperties(Dictionary props) {
        CmManagedProperties cm = findManagedProperties();
        if (cm != null) {
            cm.updated(props);
        }
    }
    
    private CmManagedProperties findManagedProperties() {
        for (BeanProcessor beanProcessor : blueprintContainer.getBeanProcessors()) {
            if (beanProcessor instanceof CmManagedProperties) {
                CmManagedProperties cm = (CmManagedProperties) beanProcessor;
                if (managedComponentName.equals(cm.getBeanName())) {
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
            if (methods != null & !methods.isEmpty()) {
                method = methods.get(0);
            }
        }
        return method;
    }
    
    protected void deleted(String pid) {
        LOGGER.debug("Deleted configuration {}", pid);
        ServiceRegistration reg = pids.remove(pid);
        if (reg != null) {
            // TODO: destroy instance, etc...
            Object component = services.remove(reg);
            destroyComponent(component, CONFIGURATION_ADMIN_OBJECT_DELETED);
            reg.unregister();
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

}
