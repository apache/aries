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

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
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
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.osgi.framework.BundleReference;
import org.osgi.service.blueprint.container.ComponentDefinitionException;

/**
 * TODO: javadoc
 *
 * @version $Rev$, $Date$
 */
public class ReflectionUtils {

    private static Map<Class<?>, WeakReference<Method[]>> publicMethods = Collections.synchronizedMap(new WeakHashMap<Class<?>, WeakReference<Method[]>>());
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
    
    public static Set<Class<?>> getImplementedInterfacesAsClasses(Set<Class<?>> classes, Class<?> clazz) {
        if (clazz != null && clazz != Object.class) {
            for (Class<?> itf : clazz.getInterfaces()) {
                if (Modifier.isPublic(itf.getModifiers())) {
                    classes.add(itf);
                }
                getImplementedInterfacesAsClasses(classes, itf);
            }
            getImplementedInterfacesAsClasses(classes, clazz.getSuperclass());
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
            for (Method method : getPublicMethods(clazz)) {
                if (method.getName().equals(name)
                        && method.getParameterTypes().length == 0
                        && Void.TYPE.equals(method.getReturnType())) {
                    return method;
                }
            }
        }
        return null;
    }

    public static Method[] getPublicMethods(Class clazz) {
        WeakReference<Method[]> ref = publicMethods.get(clazz);
        Method[] methods = ref != null ? ref.get() : null;
        if (methods == null) {
            ArrayList<Method> array = new ArrayList<Method>();
            doGetPublicMethods(clazz, array);
            methods = array.toArray(new Method[array.size()]);
            publicMethods.put(clazz, new WeakReference<Method[]>(methods));
        }
        return methods;
    }

    private static void doGetPublicMethods(Class clazz, ArrayList<Method> methods) {
        Class parent = clazz.getSuperclass();
        if (parent != null) {
            doGetPublicMethods(parent, methods);
        }
        for (Class interf : clazz.getInterfaces()) {
            doGetPublicMethods(interf, methods);
        }
        if (Modifier.isPublic(clazz.getModifiers())) {
            for (Method mth : clazz.getMethods()) {
                removeByNameAndSignature(methods, mth);
                methods.add(mth);
            }
        }
    }

    private static void removeByNameAndSignature(ArrayList<Method> methods, Method toRemove) {
        for (int i = 0; i < methods.size(); i++) {
            Method m = methods.get(i);
            if (m != null &&
                    m.getReturnType() == toRemove.getReturnType() &&
                    m.getName() == toRemove.getName() &&
                    arrayContentsEq(m.getParameterTypes(),
                            toRemove.getParameterTypes())) {
                methods.remove(i--);
            }
        }
    }

    private static boolean arrayContentsEq(Object[] a1, Object[] a2) {
        if (a1 == null) {
            return a2 == null || a2.length == 0;
        }
        if (a2 == null) {
            return a1.length == 0;
        }
        if (a1.length != a2.length) {
            return false;
        }
        for (int i = 0; i < a1.length; i++) {
            if (a1[i] != a2[i]) {
                return false;
            }
        }
        return true;
    }

    public static List<Method> findCompatibleMethods(Class clazz, String name, Class[] paramTypes) {
        List<Method> methods = new ArrayList<Method>();
        for (Method method : getPublicMethods(clazz)) {
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
            
            for (Method method : getPublicMethods(clazz)) {
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
                for (Class cl = clazz; cl != null && cl != Object.class; cl = cl.getSuperclass()) {
                    for (Field field : cl.getDeclaredFields()) {
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
        
        protected abstract Object internalGet(ExtendedBlueprintContainer container, Object instance) throws Exception;
        protected abstract void internalSet(ExtendedBlueprintContainer container, Object instance, Object value) throws Exception;        
        
        public Object get(final Object instance, final ExtendedBlueprintContainer container) throws Exception {            
            if (container.getAccessControlContext() == null) {
                return internalGet(container, instance);
            } else {
                try {
                    return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                        public Object run() throws Exception {
                            return internalGet(container, instance);
                        }            
                    }, container.getAccessControlContext());
                } catch (PrivilegedActionException e) {
                    throw e.getException();
                }
            }
        }

        public void set(final Object instance, final Object value, final ExtendedBlueprintContainer container) throws Exception {
            if (container.getAccessControlContext() == null) {
                internalSet(container, instance, value);
            } else {
                try {
                    AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                        public Object run() throws Exception {
                            internalSet(container, instance, value);
                            return null;
                        }            
                    }, container.getAccessControlContext());
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
        protected Object internalGet(ExtendedBlueprintContainer container, Object instance) throws Exception {
            if (mpd.allowsGet()) return mpd.internalGet(container, instance);
            else if (fpd.allowsGet()) return fpd.internalGet(container, instance);
            else throw new UnsupportedOperationException();
        }

        @Override
        protected void internalSet(ExtendedBlueprintContainer container, Object instance, Object value) throws Exception {
            if (mpd.allowsSet()) mpd.internalSet(container, instance, value);
            else if (fpd.allowsSet()) fpd.internalSet(container, instance, value);
            else throw new UnsupportedOperationException();
        }
    }
    
    private static class FieldPropertyDescriptor extends PropertyDescriptor {
        // instead of holding on to the java.lang.reflect.Field objects we retrieve it every time. The reason is that PropertyDescriptors are 
        // used as values in a WeakHashMap with the class corresponding to the field as the key
        private final String fieldName;
        private final WeakReference<Class<?>> declaringClass;
        
        public FieldPropertyDescriptor(String name, Field field) {
            super(name);
            this.fieldName = field.getName();
            this.declaringClass = new WeakReference(field.getDeclaringClass());
        }

        public boolean allowsGet() {
            return true;
        }

        public boolean allowsSet() {
            return true;
        }
        
        private Field getField(ExtendedBlueprintContainer container) throws ClassNotFoundException, NoSuchFieldException {
            if (declaringClass.get() == null) throw new ClassNotFoundException("Declaring class was garbage collected");
            
            return declaringClass.get().getDeclaredField(fieldName);
        }

        protected Object internalGet(final ExtendedBlueprintContainer container, final Object instance) throws Exception {
            if (useContainersPermission(container)) {
                try {
                    return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                        public Object run() throws Exception {
                            return doInternalGet(container, instance);
                        }                        
                    });
                } catch (PrivilegedActionException pae) {
                    Exception e = pae.getException();
                    if (e instanceof IllegalAccessException) throw (IllegalAccessException) e;
                    else throw (RuntimeException) e;
                }
            } else {
                return doInternalGet(container, instance);
            }
        }
        
        private Object doInternalGet(ExtendedBlueprintContainer container, Object instance) throws Exception {
            Field field = getField(container);
            boolean isAccessible = field.isAccessible();
            field.setAccessible(true);
            try {
                return field.get(instance);
            } finally {
                field.setAccessible(isAccessible);
            }
        }

        protected void internalSet(final ExtendedBlueprintContainer container, final Object instance, final Object value) throws Exception {
            try {
                Boolean wasSet = AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
                    public Boolean run() throws Exception {
                      if (useContainersPermission(container)) {
                        doInternalSet(container, instance, value);
                        return Boolean.TRUE;
                      }
                      return Boolean.FALSE;
                    }                        
                });
                if(!!!wasSet) {
                  doInternalSet(container, instance, value);
                }
            } catch (PrivilegedActionException pae) {
                throw pae.getException();
            }
        }
        
        private void doInternalSet(ExtendedBlueprintContainer container, Object instance, Object value) throws Exception {
            Field field = getField(container);
            final Object convertedValue = convert(value, field.getGenericType());
            boolean isAccessible = field.isAccessible();
            field.setAccessible(true);
            try {
                field.set(instance, convertedValue);
            } finally {
                field.setAccessible(isAccessible);
            }
        }
        
        /**
         * Determine whether the field access (in particular the call to {@link Field#setAccessible(boolean)} should be done with the Blueprint extender's
         * permissions, rather than the joint (more restrictive) permissions of the extender plus the Blueprint bundle.
         * 
         * We currently only allow this for classes that originate from inside the Blueprint bundle. Otherwise this would open a potential security hole.
         * @param container
         * @return
         */
        private boolean useContainersPermission(ExtendedBlueprintContainer container) throws ClassNotFoundException {
            if (declaringClass.get() == null) throw new ClassNotFoundException("Declaring class was garbage collected");
            ClassLoader loader = declaringClass.get().getClassLoader();
            
            if (loader == null) return false;
            
            if (loader instanceof BundleReference) {
                BundleReference ref = (BundleReference) loader;
                return ref.getBundle().equals(container.getBundleContext().getBundle());                
            }
            
            return false;
        }
    }
    
    private static class MethodDescriptor {
        private final String methodName;
        private final WeakReference<Class<?>> declaringClass;
        private final List<WeakReference<Class<?>>> argClasses;
        
        public MethodDescriptor(Method method) {
            methodName = method.getName();
            declaringClass = new WeakReference<Class<?>>(method.getDeclaringClass());
            
            List<WeakReference<Class<?>>> accumulator = new ArrayList<WeakReference<Class<?>>>();
            for (Class<?> c : method.getParameterTypes()) {
                accumulator.add(new WeakReference<Class<?>>(c));
            }
            argClasses = Collections.unmodifiableList(accumulator);
        }
        
        public Method getMethod(ExtendedBlueprintContainer container) throws ClassNotFoundException, NoSuchMethodException {
            Class<?>[] argumentClasses = new Class<?>[argClasses.size()];
            for (int i=0; i<argClasses.size(); i++) {
                argumentClasses[i] = argClasses.get(i).get();
                if (argumentClasses[i] == null) throw new ClassNotFoundException("Argument class was garbage collected");
            }
            
            if (declaringClass.get() == null) throw new ClassNotFoundException("Declaring class was garbage collected");
            
            return declaringClass.get().getMethod(methodName, argumentClasses);
        }
        
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(declaringClass.get()).append(".").append(methodName).append("(");
            
            boolean first = true;
            for (WeakReference<Class<?>> wcl : argClasses) {
                if (!!!first) builder.append(",");
                else first = false;
                
                builder.append(wcl.get());
            }

            builder.append(")");
            return builder.toString();
        }
    }
    
    private static class MethodPropertyDescriptor extends PropertyDescriptor {
        // instead of holding on to the java.lang.reflect.Method objects we retrieve it every time. The reason is that PropertyDescriptors are 
        // used as values in a WeakHashMap with the class corresponding to the methods as the key
        private final MethodDescriptor getter;
        private final Collection<MethodDescriptor> setters;

        private MethodPropertyDescriptor(String name, Method getter, Collection<Method> setters) {
            super(name);
            this.getter = (getter != null) ? new MethodDescriptor(getter) : null;
            
            if (setters != null) {
                Collection<MethodDescriptor> accumulator = new ArrayList<MethodDescriptor>();
                for (Method s : setters) accumulator.add(new MethodDescriptor(s));
                this.setters = Collections.unmodifiableCollection(accumulator);
            } else {
                this.setters = Collections.emptyList();
            }
        }
        
        public boolean allowsGet() {
            return getter != null;
        }
        
        public boolean allowsSet() {
            return !!!setters.isEmpty();
        }
        
        protected Object internalGet(ExtendedBlueprintContainer container, Object instance) 
                throws Exception {
            if (getter != null) {
                return getter.getMethod(container).invoke(instance);
            } else {
                throw new UnsupportedOperationException();
            }
        }
        
        protected void internalSet(ExtendedBlueprintContainer container, Object instance, Object value) throws Exception {
            
            Method setterMethod = findSetter(container, value);

            if (setterMethod != null) {
                setterMethod.invoke(instance, convert(value, resolveParameterType(instance.getClass(), setterMethod)));
            } else {
                throw new ComponentDefinitionException(
                        "No converter available to convert value "+value+" into a form applicable for the " + 
                        "setters of property "+getName());
            }
        }

        private Type resolveParameterType(Class<?> impl, Method setterMethod) {
            Type type = setterMethod.getGenericParameterTypes()[0];
            Class<?> declaringClass = setterMethod.getDeclaringClass();
            TypeVariable<?>[] declaredVariables = declaringClass.getTypeParameters();

            if (TypeVariable.class.isInstance(type)) {
                // e.g.: "T extends Serializable"
                TypeVariable variable = TypeVariable.class.cast(type);

                int index = 0;
                for (; index < declaredVariables.length; index++) {
                    // find the class declaration index...
                    if (variable == declaredVariables[index]) {
                        break;
                    }
                }

                if (index >= declaredVariables.length) {
                    // not found - now what...
                    return type;
                }

                // navigate from the implementation type up to the declaring super
                // class to find the real generic type...
                Class<?> c = impl;
                while (c != null && c != declaringClass) {
                    Type sup = c.getGenericSuperclass();
                    if (sup != null && ParameterizedType.class.isInstance(sup)) {
                        ParameterizedType pt = ParameterizedType.class.cast(sup);
                        if (declaringClass == pt.getRawType()) {
                            Type t = pt.getActualTypeArguments()[index];
                            return t;
                        }
                    }
                    c = c.getSuperclass();
                }
                return type;
            } else {
                // not a generic type...
                return type;
            }
        }

        private Method findSetter(ExtendedBlueprintContainer container, Object value) throws Exception {
            Class<?> valueType = (value == null) ? null : value.getClass();
            
            Method getterMethod = (getter != null) ? getter.getMethod(container) : null;
            Collection<Method> setterMethods = getSetters(container);
            
            Method result = findMethodByClass(getterMethod, setterMethods, valueType);
            
            if (result == null) result = findMethodWithConversion(setterMethods, value);
                        
            return result;
        }
        
        private Collection<Method> getSetters(ExtendedBlueprintContainer container) throws Exception {
            Collection<Method> result = new ArrayList<Method>();
            for (MethodDescriptor md : setters) result.add(md.getMethod(container));
            
            return result;
        }
        
        private Method findMethodByClass(Method getterMethod, Collection<Method> setterMethods, Class<?> arg)
                throws ComponentDefinitionException {
            Method result = null;

            if (!hasSameTypeSetter(getterMethod, setterMethods)) {
                throw new ComponentDefinitionException(
                        "At least one Setter method has to match the type of the Getter method for property "
                                + getName());
            }

            if (setterMethods.size() == 1) {
                return setterMethods.iterator().next();
            }
            
            for (Method m : setterMethods) {
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
        private boolean hasSameTypeSetter(Method getterMethod, Collection<Method> setterMethods) {
            if (getterMethod == null) {
                return true;
            }

            Iterator<Method> it = setterMethods.iterator();
            while (it.hasNext()) {
                Method m = it.next();
                if (m.getParameterTypes()[0].equals(getterMethod.getReturnType())) {
                    return true;
                }
            }
            return false;
        }

        private Method findMethodWithConversion(Collection<Method> setterMethods, Object value) throws Exception {
            ExecutionContext ctx = ExecutionContext.Holder.getContext();
            List<Method> matchingMethods = new ArrayList<Method>();
            for (Method m : setterMethods) {
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
