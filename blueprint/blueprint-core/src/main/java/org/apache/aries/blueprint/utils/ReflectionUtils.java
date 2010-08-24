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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.aries.blueprint.container.GenericType;
import org.apache.aries.blueprint.di.ExecutionContext;
import org.osgi.service.blueprint.container.ComponentDefinitionException;

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
            Set<String> propertyNames = new HashSet<String>();
            Map<String,Method> getters = new HashMap<String, Method>();
            Map<String,List<Method>> setters = new HashMap<String, List<Method>>();
            Set<String> illegalProperties = new HashSet<String>();
            
            for (Method method : clazz.getMethods()) {
                if (Modifier.isStatic(method.getModifiers()) || method.isBridge()) continue;
                
                String name = method.getName();
                Class<?> argTypes[] = method.getParameterTypes();
                Class<?> resultType = method.getReturnType();
                
                if (name.length() > 3 && name.startsWith("set") && resultType == Void.TYPE && argTypes.length == 1) {
                    name = decapitalize(name.substring(3));
                    if (!!!setters.containsKey(name)) setters.put(name, new ArrayList<Method>());
                    setters.get(name).add(method);
                    propertyNames.add(name);
                } else if (name.length() > 3 && name.startsWith("get") && resultType != Void.TYPE && argTypes.length == 0) {
                    name = decapitalize(name.substring(3));

                    if (getters.containsKey(name)) illegalProperties.add(name);
                    else propertyNames.add(name);
                    
                    getters.put(name, method);                    
                } else if (name.length() > 2 && name.startsWith("is") && argTypes.length == 0 && resultType == boolean.class) {
                    name = decapitalize(name.substring(2));

                    if (getters.containsKey(name)) illegalProperties.add(name);
                    else propertyNames.add(name);
                    
                    getters.put(name, method);                    
                }
                
            }

            Map<String, PropertyDescriptor> props = new HashMap<String, PropertyDescriptor>();
            for (String propName : propertyNames) {
                props.put(propName,
                        new MethodPropertyDescriptor(propName, getters.get(propName), setters.get(propName)));
            }            
            
            if (allowFieldInjection) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (!!!Modifier.isStatic(field.getModifiers())) {
                        String name = decapitalize(field.getName());
                        PropertyDescriptor desc = props.get(name);
                        if (desc == null) {
                            props.put(name, new FieldPropertyDescriptor(name, field));
                        } else if (desc instanceof MethodPropertyDescriptor) {
                            props.put(name, 
                                    new JointPropertyDescriptor((MethodPropertyDescriptor) desc, 
                                            new FieldPropertyDescriptor(name, field)));
                        } else {
                            illegalProperties.add(name);
                        }
                    }
                }
            }
            
            List<PropertyDescriptor> result = new ArrayList<PropertyDescriptor>();
            for (PropertyDescriptor prop : props.values()) {
                if (!!!illegalProperties.contains(prop.getName())) result.add(prop);
            }
            
            properties[index] = result.toArray(new PropertyDescriptor[result.size()]); 
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
    
    public static abstract class PropertyDescriptor {
        private final String name;
        
        public PropertyDescriptor(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public abstract boolean allowsGet();
        public abstract boolean allowsSet();
        
        protected abstract Object internalGet(Object instance) throws Exception;
        protected abstract void internalSet(Object instance, Object value) throws Exception;        
        
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
        
        protected Object convert(Object obj, Type type) throws Exception {
            return ExecutionContext.Holder.getContext().convert(obj, new GenericType(type));
        }
    }
    
    private static class JointPropertyDescriptor extends PropertyDescriptor {
        private final MethodPropertyDescriptor mpd;
        private final FieldPropertyDescriptor fpd;
        
        public JointPropertyDescriptor(MethodPropertyDescriptor mpd, FieldPropertyDescriptor fpd) {
            super(mpd.getName());
            this.mpd = mpd;
            this.fpd = fpd;
        }

        @Override
        public boolean allowsGet() {
            return mpd.allowsGet() || fpd.allowsGet();
        }

        @Override
        public boolean allowsSet() {
            return mpd.allowsSet() || fpd.allowsSet();
        }

        @Override
        protected Object internalGet(Object instance) throws Exception {
            if (mpd.allowsGet()) return mpd.internalGet(instance);
            else if (fpd.allowsGet()) return fpd.internalGet(instance);
            else throw new UnsupportedOperationException();
        }

        @Override
        protected void internalSet(Object instance, Object value) throws Exception {
            if (mpd.allowsSet()) mpd.internalSet(instance, value);
            else if (fpd.allowsSet()) fpd.internalSet(instance, value);
            else throw new UnsupportedOperationException();
        }
    }
    
    private static class FieldPropertyDescriptor extends PropertyDescriptor {
        private final Field field;
        
        public FieldPropertyDescriptor(String name, Field field) {
            super(name);
            this.field = field;
        }

        public boolean allowsGet() {
            return true;
        }

        public boolean allowsSet() {
            return true;
        }

        protected Object internalGet(Object instance) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            field.setAccessible(true);
            return field.get(instance);
        }

        protected void internalSet(Object instance, Object value) throws Exception {
            field.setAccessible(true);
            field.set(instance, convert(value, field.getGenericType()));
        }
    }
    
    private static class MethodPropertyDescriptor extends PropertyDescriptor {
        private final Method getter;
        private final Collection<Method> setters;

        private MethodPropertyDescriptor(String name, Method getter, Collection<Method> setters) {
            super(name);
            this.getter = getter;
            this.setters = (setters != null) ? setters : Collections.<Method>emptyList();
        }
        
        public boolean allowsGet() {
            return getter != null;
        }
        
        public boolean allowsSet() {
            return !!!setters.isEmpty();
        }
        
        protected Object internalGet(Object instance) 
                throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            if (getter != null) {
                return getter.invoke(instance);
            } else {
                throw new UnsupportedOperationException();
            }
        }
        
        protected void internalSet(Object instance, Object value) throws Exception {
            
            Method setterMethod = findSetter(value);

            if (setterMethod != null) {
                setterMethod.invoke(instance, convert(value, setterMethod.getGenericParameterTypes()[0]));
            } else {
                throw new ComponentDefinitionException(
                        "No converter available to convert value "+value+" into a form applicable for the " + 
                        "setters of property "+getName());
            }
        }
        
        private Method findSetter(Object value) {
            Class<?> valueType = (value == null) ? null : value.getClass();
            
            Method result = findMethodByClass(valueType);
            
            if (result == null) result = findMethodWithConversion(value);
                        
            return result;
        }
        
        private Method findMethodByClass(Class<?> arg)
                throws ComponentDefinitionException {
            Method result = null;

            if (!hasSameTypeSetter()) {
                throw new ComponentDefinitionException(
                        "At least one Setter method has to match the type of the Getter method for property "
                                + getName());
            }

            if (setters.size() == 1) {
                return setters.iterator().next();
            }
            
            for (Method m : setters) {
                Class<?> paramType = m.getParameterTypes()[0];

                if ((arg == null && Object.class.isAssignableFrom(paramType))
                        || (arg != null && paramType.isAssignableFrom(arg))) {

                    // pick the method that has the more specific parameter if
                    // any
                    if (result != null) {
                        Class<?> oldParamType = result.getParameterTypes()[0];
                        if (paramType.isAssignableFrom(oldParamType)) {
                            // do nothing, result is correct
                        } else if (oldParamType.isAssignableFrom(paramType)) {
                            result = m;
                        } else {
                            throw new ComponentDefinitionException(
                                    "Ambiguous setter method for property "
                                            + getName()
                                            + ". More than one method matches the  parameter type "
                                            + arg);
                        }
                    } else {
                        result = m;
                    }
                }
            }

            return result;
        }
        
        // ensure there is a setter that matches the type of the getter
        private boolean hasSameTypeSetter() {
            if (getter == null) {
                return true;
            }
            Iterator<Method> it = setters.iterator();
            while (it.hasNext()) {
                Method m = it.next();
                if (m.getParameterTypes()[0].equals(getter.getReturnType())) {
                    return true;
                }
            }
            return false;
        }

        private Method findMethodWithConversion(Object value) throws ComponentDefinitionException {
            ExecutionContext ctx = ExecutionContext.Holder.getContext();
            List<Method> matchingMethods = new ArrayList<Method>();
            for (Method m : setters) {
                Type paramType = m.getGenericParameterTypes()[0];
                if (ctx.canConvert(value, new GenericType(paramType))) matchingMethods.add(m);
            }
            
            if (matchingMethods.isEmpty()) return null;
            else if (matchingMethods.size() == 1) return matchingMethods.get(0);
            else throw new ComponentDefinitionException(
                    "Ambiguous setter method for property "+ getName() + 
                    ". More than one method matches the parameter "+value+" after applying conversion.");
        }
        
        public String toString() {
            return "PropertyDescriptor <name: "+getName()+", getter: "+getter+", setter: "+setters;
        }
    }

    public static Throwable getRealCause(Throwable t) {
        if (t instanceof InvocationTargetException && t.getCause() != null) {
            return t.getCause();
        }
        return t;
    }

}
