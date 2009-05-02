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
package org.apache.geronimo.blueprint.context;

import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.geronimo.blueprint.BlueprintConstants;
import org.apache.geronimo.blueprint.utils.ReflectionUtils;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.context.BlueprintContext;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.RegistrationListener;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/** 
 * TODO: javadoc
 */
public class ServiceRegistrationProxy implements ServiceRegistration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistrationProxy.class);

    private BlueprintContext blueprintContext;
    private Object service;
    private Map serviceProperties;
    private List<Listener> listeners;
    private ServiceMetadata metadata;

    private ServiceRegistration registration = null;
    private Map registrationProperties = null;

    protected Object getService() {
        return service;
    }
        
    protected Map getRegistrationProperties() {
        return registrationProperties;
    }

    public boolean isRegistered() {
        return registration != null;
    }
    
    public synchronized void register() {
        if (registration != null) {
            return;
        }
                
        Class serviceClass = service.getClass();
        if (service instanceof BundleScopeServiceFactory) {
            serviceClass = ((BundleScopeServiceFactory) service).getServiceClass();
        }
        Set<String> classes;
        switch (metadata.getAutoExportMode()) {
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
                classes = new HashSet<String>(metadata.getInterfaceNames());
                break;
        }
                
        Hashtable props = new Hashtable();
        if (serviceProperties != null) {
            props.putAll(serviceProperties);
        }
        props.put(Constants.SERVICE_RANKING, metadata.getRanking());
        String componentName = getComponentName();
        if (componentName != null) {
            props.put(BlueprintConstants.COMPONENT_NAME_PROPERTY, componentName);
        }
        String[] classesArray = classes.toArray(new String[classes.size()]);
        registration = blueprintContext.getBundleContext().registerService(classesArray, service, props);
        registrationProperties = props;
        
        LOGGER.debug("Service {} registered with interfaces {}", service, classes);
        
        if (listeners != null) {
            for (Listener listener : listeners) {
                listener.register(this);
            }
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
    
    public String toString() {
        return service + " " + serviceProperties + " " + listeners;
    }

    // ServiceRegistation methods
        
    public synchronized void unregister() {   
        if (registration != null) {
            registration.unregister();
            
            LOGGER.debug("Service {} unregistered", service);

            if (listeners != null) {
                for (Listener listener : listeners) {
                    listener.unregister(this);
                }
            }
            
            registration = null;
            registrationProperties = null;
        }
    }
    
    public ServiceReference getReference() {
        if (registration == null) {
            throw new IllegalStateException();
        } else {
            return registration.getReference();
        }
    }

    public void setProperties(Dictionary props) {
        if (registration == null) {
            throw new IllegalStateException();
        } else {
            registration.setProperties(props);     
            // TODO: set serviceProperties? convert somehow? should listeners be notified of this?
        }
    }
    
    public static class Listener {
        
        private Object listener;
        private RegistrationListener metadata;
        
        private List<Method> registerMethods;
        private List<Method> unregisterMethods;
        private boolean initialized = false;
        
        private synchronized void init(ServiceRegistrationProxy registration) {
            if (initialized) {
                return;
            }
            
            Object service = registration.getService();
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
        
        public void register(ServiceRegistrationProxy registration) {
            init(registration);
            invokeMethod(registerMethods, registration);
        }
        
        public void unregister(ServiceRegistrationProxy registration) {
            invokeMethod(unregisterMethods, registration);
        }
                
        private void invokeMethod(List<Method> methods, ServiceRegistrationProxy registration) {
            if (methods == null || methods.isEmpty()) {
                return;
            }
            Object service = registration.getService();
            Map properties = registration.getRegistrationProperties();
            Object[] args = new Object[] { service, properties };
            for (Method method : methods) {
                try {
                    method.invoke(listener, args);
                } catch (Exception e) {
                    LOGGER.info("Error calling listener", e);
                }
            }
        }
                           
    }
}
