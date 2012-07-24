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

import org.apache.aries.blueprint.BeanProcessor;
import org.apache.aries.blueprint.ServiceProcessor;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.utils.JavaUtils;
import org.apache.aries.blueprint.utils.ReflectionUtils;
import org.apache.aries.blueprint.utils.ServiceListener;
import org.apache.aries.util.AriesFrameworkUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: if we need to make those exported services tied to their references as for other <service/> elements
 * TODO: it becomes a problem as currently we would have to create a specific recipe or something like that
 *
 * @version $Rev$, $Date$
 */
public class CmManagedServiceFactory extends BaseManagedServiceFactory<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmManagedServiceFactory.class);

    private ExtendedBlueprintContainer blueprintContainer;
    private String id;
    private String factoryPid;
    private List<String> interfaces;
    private int autoExport;
    private int ranking;
    private Map<Object,Object> serviceProperties;
    private String managedComponentName;
    private String componentDestroyMethod;
    private List<ServiceListener> listeners;

    private ServiceRegistration registration;

    public CmManagedServiceFactory(ExtendedBlueprintContainer blueprintContainer) {
        super(blueprintContainer.getBundleContext(), null);
        this.blueprintContainer = blueprintContainer;
    }

    public void init() throws Exception {
        LOGGER.debug("Initializing CmManagedServiceFactory for factoryPid={}", factoryPid);
        Properties props = new Properties();
        props.put(Constants.SERVICE_PID, factoryPid);
        Bundle bundle = blueprintContainer.getBundleContext().getBundle();
        props.put(Constants.BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());
        props.put(Constants.BUNDLE_VERSION, bundle.getHeaders().get(Constants.BUNDLE_VERSION));

        registration = blueprintContainer.getBundleContext().registerService(ManagedServiceFactory.class.getName(), this, props);
    }

    public void destroy() {
        AriesFrameworkUtil.safeUnregisterService(registration);
        super.destroy();
    }

    public Map<ServiceRegistration, Object> getServiceMap() {
        return Collections.unmodifiableMap(getServices());
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

    private void getRegistrationProperties(Dictionary properties, boolean update) {
        String pid = (String) properties.get(Constants.SERVICE_PID);
        CmProperties cm = findServiceProcessor();
        if (cm == null) {
            while (!properties.isEmpty()) {
                properties.remove(properties.keys().nextElement());
            }
        } else  {
            if (!cm.getUpdate()) {
                if (update) {
                    while (!properties.isEmpty()) {
                        properties.remove(properties.keys().nextElement());
                    }
                    for (Map.Entry entry : cm.getProperties().entrySet()) {
                        properties.put(entry.getKey(), entry.getValue());
                    }
                } else {
                    cm.updated(properties);
                }
            }
        }
        if (serviceProperties != null) {
            for (Map.Entry entry : serviceProperties.entrySet()) {
                properties.put(entry.getKey(), entry.getValue());
            }
        }
        properties.put(Constants.SERVICE_RANKING, ranking);
        properties.put(Constants.SERVICE_PID, pid);
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

    protected Object doCreate(Dictionary properties) throws Exception {
        updateComponentProperties(copy(properties));
        Object component = blueprintContainer.getComponentInstance(managedComponentName);
        getRegistrationProperties(properties, false);
        return component;
    }

    protected Object doUpdate(Object service, Dictionary properties) throws Exception {
        updateComponentProperties(copy(properties));
        getRegistrationProperties(properties, true);
        return service;
    }

    protected void doDestroy(Object service, Dictionary properties, int code) throws Exception {
        Method method = findDestroyMethod(service.getClass());
        if (method != null) {
            try {
                method.invoke(service, new Object [] { code });
            } catch (Exception e) {
                LOGGER.info("Error destroying component", e);
            }
        }
    }

    protected void postRegister(Object service, Dictionary properties, ServiceRegistration registration) {
        if (listeners != null && !listeners.isEmpty()) {
            Hashtable props = new Hashtable();
            JavaUtils.copy(properties, props);
            for (ServiceListener listener : listeners) {
                listener.register(service, props);
            }
        }
    }

    protected void preUnregister(Object service, Dictionary properties, ServiceRegistration registration) {
        if (listeners != null && !listeners.isEmpty()) {
            Hashtable props = new Hashtable();
            JavaUtils.copy(properties, props);
            for (ServiceListener listener : listeners) {
                listener.unregister(service, props);
            }
        }
    }

    protected String[] getExposedClasses(Object service) {
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
        return classes.toArray(new String[classes.size()]);
    }

    private Hashtable copy(Dictionary source) {
        Hashtable ht = new Hashtable();
        JavaUtils.copy(ht, source);
        return ht;
    }

}
