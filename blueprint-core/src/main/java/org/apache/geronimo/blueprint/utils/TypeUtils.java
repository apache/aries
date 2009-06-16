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
package org.apache.geronimo.blueprint.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.HashMap;

import org.osgi.framework.Bundle;
import org.apache.geronimo.blueprint.di.ExecutionContext;

/**
 * @version $Rev: 6687 $ $Date: 2005-12-28T21:08:56.733437Z $
 */
public final class TypeUtils {
    private TypeUtils() {
    }

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

    public static boolean isInstance(Type t, Object instance) {
        Class type = toClass(t);
        if (type.isPrimitive()) {
            // for primitives the insance can't be null
            if (instance == null) {
                return false;
            }

            // verify instance is the correct wrapper type
            if (type.equals(boolean.class)) {
                return instance instanceof Boolean;
            } else if (type.equals(char.class)) {
                return instance instanceof Character;
            } else if (type.equals(byte.class)) {
                return instance instanceof Byte;
            } else if (type.equals(short.class)) {
                return instance instanceof Short;
            } else if (type.equals(int.class)) {
                return instance instanceof Integer;
            } else if (type.equals(long.class)) {
                return instance instanceof Long;
            } else if (type.equals(float.class)) {
                return instance instanceof Float;
            } else if (type.equals(double.class)) {
                return instance instanceof Double;
            } else {
                throw new AssertionError("Invalid primitve type: " + type);
            }
        }

        return instance == null || type.isInstance(instance);
    }

    public static boolean isAssignable(Type expectedType, Type actualType) {
        Class expectedClass = toClass(expectedType);
        Class actualClass = toClass(actualType);
        return expectedClass.isAssignableFrom(actualClass);
    }

    public static Class toClass(Type type) {
        // GenericArrayType, ParameterizedType, TypeVariable<D>, WildcardType
        if (type instanceof Class) {
            Class clazz = (Class) type;
            return clazz;
        } else if (type instanceof GenericArrayType) {
            GenericArrayType arrayType = (GenericArrayType) type;
            Class componentType = toClass(arrayType.getGenericComponentType());
            return Array.newInstance(componentType, 0).getClass();
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return toClass(parameterizedType.getRawType());
        } else {
            return Object.class;
        }
    }

    public static Type[] getTypeParameters(Class desiredType, Type type) {
        if (type instanceof Class) {
            Class rawClass = (Class) type;

            // if this is the collection class we're done
            if (desiredType.equals(type)) {
                return null;
            }

            for (Type intf : rawClass.getGenericInterfaces()) {
                Type[] collectionType = getTypeParameters(desiredType, intf);
                if (collectionType != null) {
                    return collectionType;
                }
            }

            Type[] collectionType = getTypeParameters(desiredType, rawClass.getGenericSuperclass());
            return collectionType;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            Type rawType = parameterizedType.getRawType();
            if (desiredType.equals(rawType)) {
                Type[] argument = parameterizedType.getActualTypeArguments();
                return argument;
            }
            Type[] collectionTypes = getTypeParameters(desiredType,rawType);
            if (collectionTypes != null) {
                for (int i = 0; i < collectionTypes.length; i++) {
                    if (collectionTypes[i] instanceof TypeVariable) {
                        TypeVariable typeVariable = (TypeVariable) collectionTypes[i];
                        TypeVariable[] rawTypeParams = ((Class) rawType).getTypeParameters();
                        for (int j = 0; j < rawTypeParams.length; j++) {
                            if (typeVariable.getName().equals(rawTypeParams[j].getName())) {
                                collectionTypes[i] = parameterizedType.getActualTypeArguments()[j];
                            }
                        }
                    }
                }
            }
            return collectionTypes;
        }
        return null;
    }

    private static Map<String, Class> primitiveClasses = new HashMap<String, Class>();

    static {
        primitiveClasses.put("int", int.class);
        primitiveClasses.put("short", short.class);
        primitiveClasses.put("long", long.class);
        primitiveClasses.put("byte", byte.class);
        primitiveClasses.put("char", char.class);
        primitiveClasses.put("float", float.class);
        primitiveClasses.put("double", double.class);
        primitiveClasses.put("boolean", boolean.class);
    }

    public static Type parseJavaType(String type, Object loader) throws ClassNotFoundException {
        type = type.trim();
        // Check if this is an array
        if (type.endsWith("[]")) {
            return new GenericArrayTypeImpl(parseJavaType(type.substring(0, type.length() - 2), loader));
        }
        // Check if this is a generic
        int genericIndex = type.indexOf('<');
        if (genericIndex > 0) {
            if (!type.endsWith(">")) {
                throw new IllegalArgumentException("Can not load type: " + type);
            }
            Type base = parseJavaType(type.substring(0, genericIndex), loader);
            String[] params = type.substring(genericIndex + 1, type.length() - 1).split(",");
            Type[] types = new Type[params.length];
            for (int i = 0; i < params.length; i++) {
                types[i] = parseJavaType(params[i], loader);
            }
            return new ParameterizedTypeImpl(base, types);
        }
        // Primitive
        if (primitiveClasses.containsKey(type)) {
            return primitiveClasses.get(type);
        }
        // Class
        if (loader instanceof ClassLoader) {
            return ((ClassLoader) loader).loadClass(type);
        } else if (loader instanceof Bundle) {
            return ((Bundle) loader).loadClass(type);
        } else if (loader instanceof ExecutionContext) {
            return ((ExecutionContext) loader).loadClass(type);
        } else {
            throw new IllegalArgumentException("Unsupported loader: " + loader);
        }
    }

    private static class ParameterizedTypeImpl implements ParameterizedType {

        private final Type base;
        private final Type[] params;

        private ParameterizedTypeImpl(Type base, Type[] params) {
            this.base = base;
            this.params = params;
        }

        public Type[] getActualTypeArguments() {
            return params;
        }

        public Type getRawType() {
            return base;
        }

        public Type getOwnerType() {
            return null;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getDescription(base));
            sb.append("<");
            for (int i = 0; i < params.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(getDescription(params[i]));
            }
            sb.append(">");
            return sb.toString();
        }
    }

    private static class GenericArrayTypeImpl implements GenericArrayType {

        private final Type genericComponentType;

        private GenericArrayTypeImpl(Type genericComponentType) {
            this.genericComponentType = genericComponentType;
        }

        public Type getGenericComponentType() {
            return genericComponentType;
        }

        @Override
        public String toString() {
            return getDescription(genericComponentType) + "[]";
        }
    }

    private static String getDescription(Type type) {
        if (type instanceof Class) {
            return ((Class) type).getName();
        } else {
            return type.toString();
        }
    }
}
