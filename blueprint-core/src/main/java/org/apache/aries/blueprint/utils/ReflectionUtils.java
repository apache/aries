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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
    private static Map<Class<?>, PropertyDescriptor[][]> beanInfos = Collections.synchronizedMap(new WeakHashMap<Class<?>, PropertyDescriptor[][]>());

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

    public static PropertyDescriptor[] getPropertyDescriptors(Class clazz, boolean allowFieldInjection) {
        PropertyDescriptor[][] properties = beanInfos.get(clazz);
        int index = allowFieldInjection ? 0 : 1;
        
        if (properties == null) {
            properties = new PropertyDescriptor[2][];
            beanInfos.put(clazz, properties);
        }
        
        if (properties[index] == null) {
            Map<String,PropertyDescriptor> props = new HashMap<String, PropertyDescriptor>();
            for (Method method : clazz.getMethods()) {
                if (Modifier.isStatic(method.getModifiers()) || method.isBridge()) {
                    continue;
                }
                String name = method.getName();
                Class<?> argTypes[] = method.getParameterTypes();
                Class<?> resultType = method.getReturnType();
                
                Class<?> argType = resultType;
                Method getter = null;
                Method setter = null;
                
                if (name.length() > 3 && name.startsWith("set") && resultType == Void.TYPE && argTypes.length == 1) {
                    name = decapitalize(name.substring(3));
                    setter = method;
                    argType = argTypes[0];
                } else if (name.length() > 3 && name.startsWith("get") && argTypes.length == 0) {
                    name = decapitalize(name.substring(3));
                    getter = method;
                } else if (name.length() > 2 && name.startsWith("is") && argTypes.length == 0 && resultType == boolean.class) {
                    name = decapitalize(name.substring(2));
                    getter = method;
                } else {
                    continue;
                }
                
                if (props.containsKey(name)) {
                    PropertyDescriptor pd = props.get(name);
                    if (pd != INVALID_PROPERTY) {
                        if (!argType.equals(pd.type)) {
                            props.put(name, INVALID_PROPERTY);
                        } else if (getter != null) {
                            if (pd.getter == null || pd.getter.equals(getter))
                                pd.getter = getter;
                            else
                                props.put(name, INVALID_PROPERTY);
                        } else if (setter != null) {
                            if (pd.setter == null || pd.setter.equals(setter)) 
                                pd.setter = setter;
                            else
                                props.put(name, INVALID_PROPERTY);
                        }
                    }
                } else {
                    props.put(name, new PropertyDescriptor(name, argType, getter, setter));
                }
            }
            
            if (allowFieldInjection) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    
                    String name = decapitalize(field.getName());
                    if (!props.containsKey(name)) {
                        props.put(name, new PropertyDescriptor(name, field.getType(), field));
                    } else {
                        PropertyDescriptor pd = props.get(name);
                        if (pd != INVALID_PROPERTY) {
                            if (pd.type.equals(field.getType())) {
                                pd.field = field;
                            } 
                            // no else, we don't require field implementations to have the same
                            // type as the getter and setter
                        }
                    }
                }
            }
            
            Iterator<PropertyDescriptor> it = props.values().iterator();
            while (it.hasNext()) {
                if (it.next() == INVALID_PROPERTY)
                    it.remove();
            }
            
            Collection<PropertyDescriptor> tmp = props.values();
            properties[index] = tmp.toArray(new PropertyDescriptor[tmp.size()]); 
        }
        return properties[index];
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
    
    private static final PropertyDescriptor INVALID_PROPERTY = new PropertyDescriptor(null, null, null, null);

    public static class PropertyDescriptor {
        private String name;
        private Class<?> type;
        private Method getter;
        private Method setter;
        private Field field;

        public PropertyDescriptor(String name, Class<?> type, Method getter, Method setter) {
            this.name = name;
            this.type = type;
            this.getter = getter;
            this.setter = setter;
        }
        
        public PropertyDescriptor(String name, Class<?> type, Field field) {
            this.name = name;
            this.type = type;
            this.field = field;
            this.getter = null;
            this.setter = null;
        }

        public String getName() {
            return name;
        }
        
        public boolean allowsGet() {
            return getter != null || field != null;
        }
        
        public boolean allowsSet() {
            return setter != null || field != null;
        }
        
        public Object get(final Object instance, AccessControlContext acc) throws Exception {            
            if (acc == null) {
                return internalGet(instance);
            } else {
                try {
                    return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                        public Object run() throws Exception {
                            return internalGet(instance);
                        }            
                    }, acc);
                } catch (PrivilegedActionException e) {
                    throw e.getException();
                }
            }
        }
            
        private Object internalGet(Object instance) 
                throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            if (getter != null) {
                return getter.invoke(instance);
            } else if (field != null) {
                field.setAccessible(true);
                return field.get(instance);
            } else {
                throw new UnsupportedOperationException();
            }
        }

        public void set(final Object instance, final Object value, AccessControlContext acc) throws Exception {
            if (acc == null) {
                internalSet(instance, value);
            } else {
                try {
                    AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                        public Object run() throws Exception {
                            internalSet(instance, value);
                            return null;
                        }            
                    }, acc);
                } catch (PrivilegedActionException e) {
                    throw e.getException();
                }
            }            
        }
        
        private void internalSet(Object instance, Object value) 
                throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            if (setter != null) {
                setter.invoke(instance, value);
            } else if (field != null) {
                field.setAccessible(true);
                field.set(instance, value);
            } else {
                throw new UnsupportedOperationException();
            }
        }
        
        public Type getGenericType() {
            if (setter != null)
                return setter.getGenericParameterTypes()[0];
            else if (getter != null)
                return getter.getGenericReturnType();
            else 
                return field.getGenericType();
                
        }
        
        public String toString() {
            return "PropertyDescriptor <name: "+name+", getter: "+getter+", setter: "+setter+
                ", field: "+field+">";
        }
    }

    public static Throwable getRealCause(Throwable t) {
        if (t instanceof InvocationTargetException && t.getCause() != null) {
            return t.getCause();
        }
        return t;
    }

}
