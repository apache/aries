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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.aries.blueprint.BeanProcessor;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.utils.ReflectionUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.blueprint.container.ReifiedType;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO
 *
 * @version $Rev$, $Date$
 */
public class CmManagedProperties implements ManagedObject, BeanProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmManagedProperties.class);

    private ExtendedBlueprintContainer blueprintContainer;
    private ConfigurationAdmin configAdmin;
    private ManagedObjectManager managedObjectManager;
    private String persistentId;
    private String updateStrategy;
    private String updateMethod;
    private String beanName;

    private final Object lock = new Object();
    private final Set<Object> beans = new HashSet<Object>();
    private Dictionary<String,Object> properties;
    private boolean initialized;

    public ExtendedBlueprintContainer getBlueprintContainer() {
        return blueprintContainer;
    }

    public void setBlueprintContainer(ExtendedBlueprintContainer blueprintContainer) {
        this.blueprintContainer = blueprintContainer;
    }

    public ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }

    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public void setManagedObjectManager(ManagedObjectManager managedObjectManager) {
        this.managedObjectManager = managedObjectManager;
    }
    
    public ManagedObjectManager getManagedObjectManager() {
        return managedObjectManager;
    }
    
    public Bundle getBundle() {
        return blueprintContainer.getBundleContext().getBundle();
    }
    
    public String getPersistentId() {
        return persistentId;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

    public String getUpdateStrategy() {
        return updateStrategy;
    }

    public void setUpdateStrategy(String updateStrategy) {
        this.updateStrategy = updateStrategy;
    }

    public String getUpdateMethod() {
        return updateMethod;
    }

    public void setUpdateMethod(String updateMethod) {
        this.updateMethod = updateMethod;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }
    
    public void init() throws Exception {
        LOGGER.debug("Initializing CmManagedProperties for bean={} / pid={}", beanName, persistentId);
        
        Properties props = new Properties();
        props.put(Constants.SERVICE_PID, persistentId);
        Bundle bundle = blueprintContainer.getBundleContext().getBundle();
        props.put(Constants.BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());
        props.put(Constants.BUNDLE_VERSION, bundle.getHeaders().get(Constants.BUNDLE_VERSION));
                
        synchronized (lock) {
            managedObjectManager.register(this, props);
        }
    }

    public void destroy() {
        managedObjectManager.unregister(this);
    }

    public void updated(final Dictionary props) {
        if (!initialized) {
            properties = props;
            initialized = true;
            return;
        }
        LOGGER.debug("Configuration updated for bean={} / pid={}", beanName, persistentId);
        synchronized (lock) {
            properties = props;
            for (Object bean : beans) {
                updated(bean, properties);
            }
        }
    }

    public void updated(Object bean, final Dictionary props) {
        LOGGER.debug("Configuration updated for bean={} / pid={}", beanName, persistentId);
        synchronized (lock) {
            properties = props;
            if (bean != null) {
                inject(bean, false);
            }
        }
    }

    public Object beforeInit(Object bean, String beanName, BeanCreator beanCreator, BeanMetadata beanData) {
        if (beanName != null && beanName.equals(this.beanName)) {
            LOGGER.debug("Adding bean for bean={} / pid={}", beanName, persistentId);
            synchronized (lock) {
                beans.add(bean);
                inject(bean, true);
            }
        }
        return bean;
    }

    public Object afterInit(Object bean, String beanName, BeanCreator beanCreator, BeanMetadata beanData) {
        return bean;
    }

    public void beforeDestroy(Object bean, String beanName) {
        if (beanName.equals(this.beanName)) {
            LOGGER.debug("Removing bean for bean={} / pid={}", beanName, persistentId);
            synchronized (lock) {
                beans.remove(bean);
            }
        }
    }

    public void afterDestroy(Object bean, String beanName) {
    }

    private void inject(Object bean, boolean initial) {
        LOGGER.debug("Injecting bean for bean={} / pid={}", beanName, persistentId);
        LOGGER.debug("Configuration: {}", properties);
        if (initial || "container-managed".equals(updateStrategy)) {
            if (properties != null) {
                for (Enumeration<String> e = properties.keys(); e.hasMoreElements();) {
                    String key = e.nextElement();
                    Object val = properties.get(key);
                    String setterName = "set" + Character.toUpperCase(key.charAt(0));
                    if (key.length() > 0) {
                        setterName += key.substring(1);
                    }
                    Set<Method> validSetters = new LinkedHashSet<Method>();
                    List<Method> methods = new ArrayList<Method>(Arrays.asList(bean.getClass().getMethods()));
                    methods.addAll(Arrays.asList(bean.getClass().getDeclaredMethods()));
                    for (Method method : methods) {
                        if (method.getName().equals(setterName)) {
                            if (shouldSkip(method)) {
                                continue;
                            }
                            Class methodParameterType = method.getParameterTypes()[0];
                            Object propertyValue;
                            try {
                                propertyValue = blueprintContainer.getConverter().convert(val, new ReifiedType(methodParameterType));
                            } catch (Throwable t) {
                                LOGGER.debug("Unable to convert value for setter: " + method, t);
                                continue;
                            }
                            if (methodParameterType.isPrimitive() && propertyValue == null) {
                                LOGGER.debug("Null can not be assigned to {}: {}", methodParameterType.getName(), method);
                                continue;
                            }
                            if (validSetters.add(method)) {
                                try {
                                    method.invoke(bean, propertyValue);
                                } catch (Exception t) {
                                    LOGGER.debug("Setter can not be invoked: " + method, getRealCause(t));
                                }
                            }
                        }
                    }
                    if (validSetters.isEmpty()) {
                        LOGGER.debug("Unable to find a valid setter method for property {} and value {}", key, val);
                    }
                }
            }
        } else if ("component-managed".equals(updateStrategy) && updateMethod != null) {
            List<Method> methods = ReflectionUtils.findCompatibleMethods(bean.getClass(), updateMethod, new Class[] { Map.class });
            Map map = null;
            if (properties != null) {
                map = new HashMap();
                for (Enumeration<String> e = properties.keys(); e.hasMoreElements();) {
                    String key = e.nextElement();
                    Object val = properties.get(key);
                    map.put(key, val);
                }
            }
            for (Method method : methods) {
                try {
                    method.invoke(bean, map);
                } catch (Throwable t) {
                    LOGGER.warn("Unable to call method " + method + " on bean " + beanName, getRealCause(t));
                }
            }
        }
    }

    private boolean shouldSkip(Method method) {
        String msg = null;
        if (method.getParameterTypes().length == 0) {
            msg = "takes no parameters";
        } else if (method.getParameterTypes().length > 1) {
            msg = "takes more than one parameter";
        } else if (method.getReturnType() != Void.TYPE) {
            msg = "returns a value";
        } else if (Modifier.isAbstract(method.getModifiers())) {
            msg = "is abstract";
        } else if (!Modifier.isPublic(method.getModifiers())) {
            msg = "is not public";
        } else if (Modifier.isStatic(method.getModifiers())) {
            msg = "is static";
        }
        if (msg != null) {
            LOGGER.debug("Skipping setter {} because it " + msg, method);
            return true;
        } else {
            return false;
        }
    }

    private static Throwable getRealCause(Throwable t) {
        if (t instanceof InvocationTargetException && t.getCause() != null) {
            return t.getCause();
        }
        return t;
    }

}
