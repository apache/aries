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

import java.util.Hashtable;
import java.util.List;

import org.apache.geronimo.blueprint.BlueprintConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * TODO: javadoc
 */
public class TriggerService implements ServiceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(TriggerService.class);
    
    private ServiceMetadata metadata;
    private BlueprintContextImpl blueprintContext;  
    private ServiceRegistration registration;
    
    private Object service;
    
    public TriggerService(ServiceMetadata metadata, BlueprintContextImpl blueprintContext) {
        this.metadata = metadata;
        this.blueprintContext = blueprintContext;
    }
    
    public synchronized void register() {
        if (registration != null) {
            return;
        }
        
        Hashtable props = new Hashtable();
        // TODO: handle service properties?
        props.put(Constants.SERVICE_RANKING, metadata.getRanking());
        String componentName = getComponentName();
        if (componentName != null) {
            props.put(BlueprintConstants.COMPONENT_NAME_PROPERTY, componentName);
        }
        String[] classes = getClasses();
        registration = blueprintContext.getBundleContext().registerService(classes, this, props);
        
        LOGGER.debug("Trigger service {} registered with interfaces {}", this, classes);
    }
    
    private String getComponentName() {
        if (metadata.getServiceComponent() instanceof RefMetadata) {
            RefMetadata ref = (RefMetadata) metadata.getServiceComponent();
            return ref.getComponentId();
        } else {
            return null;
        }
    }
    
    private String[] getClasses() {
        List<String> interfaces = metadata.getInterfaceNames();
        return interfaces.toArray(new String[interfaces.size()]);
    }
    
    public synchronized void unregister() {   
        if (registration != null) {
            notifyAll();
            registration.unregister();
            registration = null;
        }
    }
    
    public Object getService(Bundle bundle, ServiceRegistration registration) {
        LOGGER.debug("Service requested on trigger service. Requesting bundle start");
        
        try {
            blueprintContext.forceActivation(true);
        } catch (BundleException e) {
            LOGGER.debug("Bundle activation failed", e);
            // TODO: throw some exception or return null?
            return null;
        }
        
        waitForService();
        
        if (service instanceof ServiceFactory) {
            return ((ServiceFactory) service).getService(bundle, registration);
        } else {
            return service;
        }
    }

    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        if (service instanceof ServiceFactory) {
            ((ServiceFactory) service).ungetService(bundle, registration, service);
        }
    }  
    
    public ServiceRegistration getRegistration() {
        return registration;
    }
    
    public synchronized void setService(Object service) {
        LOGGER.debug("Updated service object: {}", service);
        this.service = service;
        notifyAll();
    }
    
    private synchronized void waitForService() {
        try {
            while(service == null) {
                wait();
            }
        } catch (InterruptedException e) {
            // break out
        }
    }
    
}
