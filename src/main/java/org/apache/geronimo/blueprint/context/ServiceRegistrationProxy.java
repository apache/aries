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
import org.osgi.service.blueprint.context.ModuleContext;
import org.osgi.service.blueprint.reflect.ReferenceValue;
import org.osgi.service.blueprint.reflect.RegistrationListenerMetadata;
import org.osgi.service.blueprint.reflect.ServiceExportComponentMetadata;

/** 
 * TODO: javadoc
 */
public class ServiceRegistrationProxy implements ServiceRegistration {
  
    private ModuleContext moduleContext;
    private Object service;
    private Map serviceProperties;
    private List<Listener> listeners;
    private ServiceExportComponentMetadata metadata;

    private ServiceRegistration registration = null;
    private Map registrationProperties = null;
    
    protected Object getService() {
        return service;
    }
        
    protected Map getRegistrationProperties() {
        return registrationProperties;
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
            case ServiceExportComponentMetadata.EXPORT_MODE_INTERFACES:
                classes = ReflectionUtils.getImplementedInterfaces(new HashSet<String>(), serviceClass);
                break;
            case ServiceExportComponentMetadata.EXPORT_MODE_CLASS_HIERARCHY:
                classes = ReflectionUtils.getSuperClasses(new HashSet<String>(), serviceClass);
                break;
            case ServiceExportComponentMetadata.EXPORT_MODE_ALL:
                classes = ReflectionUtils.getSuperClasses(new HashSet<String>(), serviceClass);
                classes = ReflectionUtils.getImplementedInterfaces(classes, serviceClass);
                break;
            default:
                classes = metadata.getInterfaceNames();
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
        registration = moduleContext.getBundleContext().registerService(classesArray, service, props);
        registrationProperties = props;
        
        System.out.println("service registered: " + service + " " + classes);
        
        if (listeners != null) {
            for (Listener listener : listeners) {
                listener.register(this);
            }
        }
    }
                       
    private String getComponentName() {
        if (metadata.getExportedComponent() instanceof ReferenceValue) {
            ReferenceValue ref = (ReferenceValue) metadata.getExportedComponent();
            return ref.getComponentName();
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
            
            System.out.println("service unregistered: " + service);
            
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
        private RegistrationListenerMetadata metadata;
        
        private Method registerMethod;
        private Method unregisterMethod;
        private boolean initialized = false;
        
        private synchronized void init(ServiceRegistrationProxy registration) {
            if (initialized) {
                return;
            }
            
            Object service = registration.getService();
            Class[] paramTypes = new Class[] { service.getClass(), Map.class };
            Class listenerClass = listener.getClass();
            
            if (metadata.getRegistrationMethodName() != null) { 
                registerMethod = ReflectionUtils.findMethod(listenerClass, metadata.getRegistrationMethodName(), paramTypes);
            }
            if (metadata.getUnregistrationMethodName() != null) {
                unregisterMethod = ReflectionUtils.findMethod(listenerClass, metadata.getUnregistrationMethodName(), paramTypes);
            }
            
            initialized = true;
        }
        
        public void register(ServiceRegistrationProxy registration) {
            init(registration);
            invokeMethod(registerMethod, registration);
        }
        
        public void unregister(ServiceRegistrationProxy registration) {
            invokeMethod(unregisterMethod, registration);
        }
                
        private void invokeMethod(Method method, ServiceRegistrationProxy registration) {
            if (method == null) {
                return;
            }
            Object service = registration.getService();
            Map properties = registration.getRegistrationProperties();
            Object[] args = new Object[] { service, properties };
            try {
                method.invoke(listener, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
                           
    }
}
