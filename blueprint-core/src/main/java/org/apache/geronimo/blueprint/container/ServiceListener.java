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
package org.apache.geronimo.blueprint.container;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.apache.geronimo.blueprint.utils.ReflectionUtils;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceListener.class);
    
    private Object listener;
    private String registerMethod;
    private String unregisterMethod;

    private List<Method> registerMethods;
    private List<Method> unregisterMethods;
    private boolean initialized = false;

    public void setListener(Object listener) {
        this.listener = listener;
    }

    public void setRegisterMethod(String method) {
        this.registerMethod = method;
    }
    
    public void setUnregisterMethod(String method) {
        this.unregisterMethod = method;
    }
    
    public void register(Object service, Map properties) {
        init(service);
        invokeMethod(registerMethods, service, properties);
    }

    public void unregister(Object service, Map properties) {
        invokeMethod(unregisterMethods, service, properties);
    }

    private synchronized void init(Object service) {
        if (initialized) {
            return;
        }
        Class[] paramTypes = new Class[] { service != null ? service.getClass() : null, Map.class };
        Class listenerClass = listener.getClass();

        if (registerMethod != null) {
            registerMethods = ReflectionUtils.findCompatibleMethods(listenerClass, registerMethod, paramTypes);
            if (registerMethods.size() == 0) {
                throw new ComponentDefinitionException("No matching methods found for listener registration method: " + registerMethod);
            }
        }
        if (unregisterMethod != null) {
            unregisterMethods = ReflectionUtils.findCompatibleMethods(listenerClass, unregisterMethod, paramTypes);
            if (unregisterMethods.size() == 0) {
                throw new ComponentDefinitionException("No matching methods found for listener unregistration method: " + unregisterMethod);
            }
        }
        initialized = true;
    }

    private void invokeMethod(List<Method> methods, Object service, Map properties) {
        if (methods == null || methods.isEmpty()) {
            return;
        }
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

