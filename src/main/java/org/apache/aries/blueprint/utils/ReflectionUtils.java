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
package org.apache.aries.blueprint.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * TODO: javadoc
 *
 * @version $Rev$, $Date$
 */
public class ReflectionUtils {

    // TODO: MLK: PropertyDescriptor holds a reference to Method which holds a reference to the Class itself
    private static Map<Class, PropertyDescriptor[]> beanInfos = Collections.synchronizedMap(new WeakHashMap<Class, PropertyDescriptor[]>());

    public static boolean hasDefaultConstructor(Class type) {
        if (!Modifier.isPublic(type.getModifiers())) {
            return false;
        }
        if (Modifier.isAbstract(type.getModifiers())) {
            return false;
        }
        Constructor[] constructors = type.getConstructors();
        for (Constructor constructor : constructors) {
            if (Modifier.isPublic(constructor.getModifiers()) &&
                    constructor.getParameterTypes().length == 0) {
                return true;
            }
        }
        return false;
    }
    
    public static Set<String> getImplementedInterfaces(Set<String> classes, Class clazz) {
        if (clazz != null && clazz != Object.class) {
            for (Class itf : clazz.getInterfaces()) {
                if (Modifier.isPublic(itf.getModifiers())) {
                    classes.add(itf.getName());
                }
                getImplementedInterfaces(classes, itf);
            }
            getImplementedInterfaces(classes, clazz.getSuperclass());
        }
        return classes;
    }

    public static Set<String> getSuperClasses(Set<String> classes, Class clazz) {
        if (clazz != null && clazz != Object.class) {
            if (Modifier.isPublic(clazz.getModifiers())) {
                classes.add(clazz.getName());
            }
            getSuperClasses(classes, clazz.getSuperclass());
        }
        return classes;
    }

    public static Method getLifecycleMethod(Class clazz, String name) {
        if (name != null) {
            try {
                Method method = clazz.getMethod(name);
                if (Void.TYPE.equals(method.getReturnType())) {
                    return method;
                }
            } catch (NoSuchMethodException e) {
                // fall thru
            }
        }
        return null;
    }
    
    public static List<Method> findCompatibleMethods(Class clazz, String name, Class[] paramTypes) {
        List<Method> methods = new ArrayList<Method>();
        for (Method method : clazz.getMethods()) {
            Class[] methodParams = method.getParameterTypes();
            if (name.equals(method.getName()) && Void.TYPE.equals(method.getReturnType()) && methodParams.length == paramTypes.length && !method.isBridge()) {
                boolean assignable = true;
                for (int i = 0; i < paramTypes.length && assignable; i++) {
                    assignable &= paramTypes[i] == null || methodParams[i].isAssignableFrom(paramTypes[i]);
                }
                if (assignable) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    public static PropertyDescriptor[] getPropertyDescriptors(Class clazz) {
        PropertyDescriptor[] properties = beanInfos.get(clazz);
        if (properties == null) {
            List<PropertyDescriptor> props = new ArrayList<PropertyDescriptor>();
            for (Method method : clazz.getMethods()) {
                if (Modifier.isStatic(method.getModifiers()) || method.isBridge()) {
                    continue;
                }
                String name = method.getName();
                Class argTypes[] = method.getParameterTypes();
                Class resultType = method.getReturnType();
                if (name.length() > 3 && name.startsWith("set") && resultType == Void.TYPE && argTypes.length == 1) {
                    props.add(new PropertyDescriptor(decapitalize(name.substring(3)), argTypes[0], null, method));

                } else if (name.length() > 3 && name.startsWith("get") && argTypes.length == 0) {
                    props.add(new PropertyDescriptor(decapitalize(name.substring(3)), resultType, method, null));
                } else if (name.length() > 2 && name.startsWith("is") && argTypes.length == 0 && resultType == boolean.class) {
                    props.add(new PropertyDescriptor(decapitalize(name.substring(2)), resultType, method, null));
                }
            }
            PropertyDescriptor[] pds = props.toArray(new PropertyDescriptor[props.size()]);
            for (int i = 0; i < pds.length - 1; i++) {
                boolean remove = false;
                for (int j = i + 1; j < pds.length; j++) {
                    if (pds[i] != null && pds[j] != null) {
                        if (pds[i].name.equals(pds[j].name)) {
                            if (remove || !pds[i].type.equals(pds[j].type)) {
                                remove = true;
                                pds[j] = null;
                                continue;
                            } else {
                                if (pds[j].getter != null) {
                                    if (pds[i].getter == null) {
                                        pds[i].getter = pds[j].getter;
                                    } else if (pds[i].getter != pds[j].getter) {
                                        remove = true;
                                        pds[j] = null;
                                        continue;
                                    }
                                }
                                if (pds[j].setter != null) {
                                    if (pds[i].setter == null) {
                                        pds[i].setter = pds[j].setter;
                                    } else if (pds[i].setter != pds[j].setter) {
                                        remove = true;
                                        pds[j] = null;
                                        continue;
                                    }
                                }
                            }
                        }
                    }
                }
                if (remove) {
                    pds[i] = null;
                }
            }
            props.clear();
            for (int i = 0; i < pds.length - 1; i++) {
                if (pds[i] != null) {
                    pds[i].type = null;
                    props.add(pds[i]);
                }
            }
            properties = props.toArray(new PropertyDescriptor[props.size()]);
            beanInfos.put(clazz, properties);
        }
        return properties;
    }

    private static String decapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
                Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    public static Object invoke(AccessControlContext acc, final Method method, final Object instance, final Object... args) throws Exception {
        if (acc == null) {
            return method.invoke(instance, args);
        } else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    public Object run() throws Exception {
                        return method.invoke(instance, args);
                    }            
                }, acc);
            } catch (PrivilegedActionException e) {
                throw e.getException();
            }
        }
    }
    
    public static Object newInstance(AccessControlContext acc, final Class clazz) throws Exception {
        if (acc == null) {
            return clazz.newInstance();
        } else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    public Object run() throws Exception {
                        return clazz.newInstance();
                    }            
                }, acc);
            } catch (PrivilegedActionException e) {
                throw e.getException();
            }
        }
    }
    
    public static Object newInstance(AccessControlContext acc, final Constructor constructor, final Object... args) throws Exception {
        if (acc == null) {
            return constructor.newInstance(args);
        } else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    public Object run() throws Exception {
                        return constructor.newInstance(args);
                    }            
                }, acc);
            } catch (PrivilegedActionException e) {
                throw e.getException();
            }
        }
    }
    
    public static class PropertyDescriptor {
        private String name;
        private Class type;
        private Method getter;
        private Method setter;

        public PropertyDescriptor(String name, Class type, Method getter, Method setter) {
            this.name = name;
            this.type = type;
            this.getter = getter;
            this.setter = setter;
        }

        public String getName() {
            return name;
        }

        public Method getGetter() {
            return getter;
        }

        public Method getSetter() {
            return setter;
        }
    }

    public static Throwable getRealCause(Throwable t) {
        if (t instanceof InvocationTargetException && t.getCause() != null) {
            return t.getCause();
        }
        return t;
    }

}
