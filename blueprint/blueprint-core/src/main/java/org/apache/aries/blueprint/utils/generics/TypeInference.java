/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.utils.generics;

import org.apache.aries.blueprint.utils.ReflectionUtils;

import java.lang.reflect.*;
import java.util.*;

public class TypeInference {

    public static class Match<E> {
        final E e;
        final List<TypedObject> args;
        final Map<TypeVariable<?>, Type> inferred;
        final int score;

        Match(E e, List<TypedObject> args, Map<TypeVariable<?>, Type> inferred, int score) {
            this.e = e;
            this.args = args;
            this.inferred = inferred;
            this.score = score;
        }

        public E getMember() {
            return e;
        }

        public List<TypedObject> getArgs() {
            return args;
        }

        public Map<TypeVariable<?>, Type> getInferred() {
            return inferred;
        }

        public int getScore() {
            return score;
        }
    }

    public static class TypedObject {
        final Type type;
        final Object value;

        public TypedObject(Type type, Object value) {
            this.type = type;
            this.value = value;
        }

        public Type getType() {
            return type;
        }

        public Object getValue() {
            return value;
        }
    }

    public interface Converter {
        TypedObject convert(TypedObject from, Type to) throws Exception;
    }

    private interface Executable<T> {
        T getMember();
        Type[] getGenericParameterTypes();
    }

    private static class ConstructorExecutable implements Executable<Constructor<?>> {

        private final Constructor<?> constructor;

        private ConstructorExecutable(Constructor<?> constructor) {
            this.constructor = constructor;
        }

        @Override
        public Constructor<?> getMember() {
            return constructor;
        }

        @Override
        public Type[] getGenericParameterTypes() {
            return constructor.getGenericParameterTypes();
        }
    }

    private static class MethodExecutable implements Executable<Method> {

        private final Method method;

        private MethodExecutable(Method method) {
            this.method = method;
        }

        @Override
        public Method getMember() {
            return method;
        }

        @Override
        public Type[] getGenericParameterTypes() {
            return method.getGenericParameterTypes();
        }
    }

    public static List<Match<Constructor<?>>> findMatchingConstructors(Type clazz, List<TypedObject> args, Converter converter, boolean reorder) {
        List<Executable<Constructor<?>>> executables = findConstructors(clazz, args.size());
        return findMatching(executables, args, converter, reorder);
    }

    public static List<Match<Method>> findMatchingMethods(Type clazz, String name, List<TypedObject> args, Converter converter, boolean reorder) {
        List<Executable<Method>> executables = findMethods(clazz, name, true, args.size());
        return findMatching(executables, args, converter, reorder);
    }

    public static List<Match<Method>> findMatchingStatics(Type clazz, String name, List<TypedObject> args, Converter converter, boolean reorder) {
        List<Executable<Method>> executables = findMethods(clazz, name, false, args.size());
        return findMatching(executables, args, converter, reorder);
    }

    private static List<Method> applyStaticHidingRules(Collection<Method> methods) {
        List<Method> result = new ArrayList<Method>(methods.size());
        for (Method m : methods) {
            boolean toBeAdded = true;

            Iterator<Method> it = result.iterator();
            while (it.hasNext()) {
                Method other = it.next();
                if (hasIdenticalParameters(m, other)) {
                    Class<?> mClass = m.getDeclaringClass();
                    Class<?> otherClass = other.getDeclaringClass();

                    if (mClass.isAssignableFrom(otherClass)) {
                        toBeAdded = false;
                        break;
                    } else if (otherClass.isAssignableFrom(mClass)) {
                        it.remove();
                    }
                }
            }

            if (toBeAdded) result.add(m);
        }

        return result;
    }

    private static boolean hasIdenticalParameters(Method one, Method two) {
        Class<?>[] oneTypes = one.getParameterTypes();
        Class<?>[] twoTypes = two.getParameterTypes();

        if (oneTypes.length != twoTypes.length) return false;

        for (int i = 0; i < oneTypes.length; i++) {
            if (!oneTypes[i].equals(twoTypes[i])) return false;
        }

        return true;
    }

    private static List<Executable<Constructor<?>>> findConstructors(Type clazz, int size) {
        List<Constructor<?>> constructors = new ArrayList<Constructor<?>>();
        for (Constructor<?> constructor : ClassUtil.getClass(clazz).getConstructors()) {
            if (constructor.getGenericParameterTypes().length != size) {
                continue;
            }
            constructors.add(constructor);
        }
        List<Executable<Constructor<?>>> executables = new ArrayList<Executable<Constructor<?>>>();
        for (Constructor<?> constructor : constructors) {
            executables.add(new ConstructorExecutable(constructor));
        }
        return executables;
    }

    private static List<Executable<Method>> findMethods(Type clazz, String name, boolean instance, int size) {
        List<Method> methods = new ArrayList<Method>();
        for (Method method : ReflectionUtils.getPublicMethods(ClassUtil.getClass(clazz))) {
            if (instance == Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.isBridge()) {
                continue;
            }
            if (!method.getName().equals(name)) {
                continue;
            }
            if (method.getGenericParameterTypes().length != size) {
                continue;
            }
            methods.add(method);
        }
        methods = applyStaticHidingRules(methods);
        List<Executable<Method>> executables = new ArrayList<Executable<Method>>();
        for (Method method : methods) {
            executables.add(new MethodExecutable(method));
        }
        return executables;
    }

    private static final long COST_ASSIGN  =       1L;
    private static final long COST_CAST    =     100L;
    private static final long COST_CONVERT =   10000L;

    private static <E> List<Match<E>> findMatching(List<Executable<E>> executables, List<TypedObject> args, Converter converter, boolean reorder) {
        Comparator<Match<E>> comparator = new Comparator<Match<E>>() {
            @Override
            public int compare(Match<E> o1, Match<E> o2) {
                return o1.getScore() - o2.getScore();
            }
        };
        List<Match<E>> matches = new ArrayList<Match<E>>();
        for (Executable<E> e : executables) {
            Match<E> match = match(e, args, converter);
            if (match != null) {
                matches.add(match);
            }
        }
        Collections.sort(matches, comparator);
        if (matches.isEmpty() && reorder) {
            for (long p = 1, l = factorial(args.size()); p < l; p++) {
                List<TypedObject> pargs = permutation(p, args);
                for (Executable<E> e : executables) {
                    Match<E> match = match(e, pargs, converter);
                    if (match != null) {
                        matches.add(match);
                    }
                }
            }
            Collections.sort(matches, comparator);
        }
        return matches;
    }

    private static <E> Match<E> match(Executable<E> executable, List<TypedObject> args, Converter converter) {
        Map<TypeVariable<?>, Type> variables = new HashMap<TypeVariable<?>, Type>();
        Type[] parameterTypes = executable.getGenericParameterTypes();
        boolean allowCast = true;
        for (int i = 0; i < parameterTypes.length; i++) {
            TypedObject arg = args.get(i);
            Type needed = parameterTypes[i];
            if (GenericsUtil.containsTypeVariable(needed)
                    && ClassUtil.getClass(needed).isAssignableFrom(ClassUtil.getClass(arg.type))) {
                try {
                    Type[] neededTypes = getParameters(needed);
                    Type[] actualTypes = getParameters(ClassUtil.getClass(needed), arg.type);
                    for (int j = 0; j < neededTypes.length; j++) {
                        if (neededTypes[j] instanceof TypeVariable) {
                            TypeVariable tv = (TypeVariable) neededTypes[j];
                            Type t = variables.get(tv);
                            t = mergeBounds(t, actualTypes[j]);
                            variables.put(tv, t);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    allowCast = false;
                }
            }
        }

        int score = 0;
        List<TypedObject> converted = new ArrayList<TypedObject>();
        for (int i = 0; i < parameterTypes.length; i++) {
            TypedObject arg = args.get(i);
            Type needed = parameterTypes[i];
            long sc;
            if (needed == arg.type) {
                sc = COST_ASSIGN;
            } else if (allowCast && ClassUtil.getClass(needed).isAssignableFrom(ClassUtil.getClass(arg.type))) {
                sc = COST_CAST;
            } else {
                sc = COST_CONVERT;
            }
            try {
                Type real = mapVariables(needed, variables);
                converted.add(converter.convert(arg, real));
                score += sc;
            } catch (Exception e) {
                return null;
            }
        }
        return new Match<E>(executable.getMember(), converted, variables, score);
    }

    private static Type[] mapVariables(Type[] types, Map<TypeVariable<?>, Type> variables) {
        Type[] resolved = new Type[types.length];
        for (int i = 0; i < types.length; i++) {
            resolved[i] = mapVariables(types[i], variables);
        }
        return resolved;
    }

    private static Type mapVariables(Type type, Map<TypeVariable<?>, Type> variables) {
        if (type == null) {
            return null;
        } else if (type instanceof Class) {
            return type;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            return new OwbParametrizedTypeImpl(
                        mapVariables(pt.getOwnerType(), variables),
                        mapVariables(pt.getRawType(), variables),
                        mapVariables(pt.getActualTypeArguments(), variables));
        } else if (type instanceof TypeVariable) {
            return variables.containsKey(type) ? variables.get(type) : type;
        } else if (type instanceof WildcardType) {
            WildcardType wt = (WildcardType) type;
            return new OwbWildcardTypeImpl(
                    mapVariables(wt.getUpperBounds(), variables),
                    mapVariables(wt.getLowerBounds(), variables));
        } else if (type instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) type;
            return new OwbGenericArrayTypeImpl(mapVariables(gat.getGenericComponentType(), variables));
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static Type mergeBounds(Type t1, Type t2) {
        Set<Type> types = new HashSet<Type>();
        TypeVariable vt = null;
        if (t1 instanceof TypeVariable) {
            Collections.addAll(types, ((TypeVariable) t1).getBounds());
            vt = (TypeVariable) t1;
        } else if (t1 != null) {
            types.add(t1);
        }
        if (t2 instanceof TypeVariable) {
            Collections.addAll(types, ((TypeVariable) t2).getBounds());
            vt = (TypeVariable) t2;
        } else if (t2 != null) {
            types.add(t2);
        }
        List<Type> bounds = new ArrayList<Type>();
        Class cl = null;
        for (Type type : types) {
            if (isClass(type)) {
                cl = cl != null ? reduceClasses(cl, (Class) type) : (Class) type;
            }
        }
        if (cl != null) {
            bounds.add(cl);
        }
        List<Type> l = Collections.emptyList();
        for (Type type : types) {
            if (!isClass(type)) {
                l = reduceTypes(l, Collections.singletonList(type));
            }
        }
        bounds.addAll(l);
        if (bounds.size() == 1) {
            return bounds.get(0);
        } else {
            return OwbTypeVariableImpl.createTypeVariable(vt, bounds.toArray(new Type[bounds.size()]));
        }
    }

    private static boolean isClass(Type t) {
        Class c = ClassUtil.getClass(t);
        return !c.isInterface() && !c.isEnum();
    }

    private static List<Type> reduceTypes(List<Type> l1, List<Type> l2) {
        List<Type> types = new ArrayList<Type>();
        for (Type t1 : l1) {
            boolean discard = false;
            for (Type t2 : l2) {
                discard |= GenericsUtil.isAssignableFrom(false, false, t1, t2);
            }
            if (!discard) {
                types.add(t1);
            }
        }
        for (Type t2 : l2) {
            boolean discard = false;
            for (Type t1 : l1) {
                discard |= GenericsUtil.isAssignableFrom(false, false, t1, t2);
            }
            if (!discard) {
                types.add(t2);
            }
        }
        return types;
    }

    private static Class reduceClasses(Class<?> c1, Class<?> c2) {
        if (c1.isAssignableFrom(c2)) {
            return c1;
        } else if (c2.isAssignableFrom(c1)) {
            return c2;
        } else {
            throw new IllegalArgumentException("Illegal bounds: " + c1 + ", " + c2);
        }
    }

    private static Type[] getParameters(Class<?> neededClass, Type type) {
        return GenericsUtil.resolveTypes(getParameters(neededClass), type);
    }

    private static long factorial(int n) {
        if (n > 20 || n < 0) throw new IllegalArgumentException(n + " is out of range");
        long l = 1L;
        for (int i = 2; i <= n; i++) {
            l *= i;
        }
        return l;
    }

    private static <T> List<T> permutation(long no, List<T> items) {
        return permutationHelper(no, new LinkedList<T>(items), new ArrayList<T>());
    }

    private static <T> List<T> permutationHelper(long no, LinkedList<T> in, List<T> out) {
        if (in.isEmpty()) return out;
        long subFactorial = factorial(in.size() - 1);
        out.add(in.remove((int) (no / subFactorial)));
        return permutationHelper((int) (no % subFactorial), in, out);
    }

    private static Type[] getParameters(Type type) {
        if (type instanceof Class) {
            Class clazz = (Class) type;
            if (clazz.isArray()) {
                return clazz.getComponentType().getTypeParameters();
            } else {
                return clazz.getTypeParameters();
            }
        }
        if (type instanceof ParameterizedType) {
            return ((ParameterizedType) type).getActualTypeArguments();
        }
        throw new RuntimeException("Unknown type " + type);
    }

}
