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
package org.apache.geronimo.blueprint.utils;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.LinkedHashMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.geronimo.blueprint.utils.TypeUtils.getTypeParameters;
import static org.apache.geronimo.blueprint.utils.TypeUtils.toClass;
import org.osgi.service.blueprint.container.Converter;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 767120 $, $Date: 2009-04-21 13:53:32 +0200 (Tue, 21 Apr 2009) $
 */
public final class ConversionUtils {

    private ConversionUtils() { }

    public static interface Convertible {

        Object convert(Type type) throws Exception;
    }

    public static Object convert(Object obj, Type type, Converter converter) throws Exception {
        // First convert service proxies
        if (obj instanceof Convertible) {
            return ((Convertible) obj).convert(type);
        }
        // Handle arrays, collections and generics
        if (obj == null) {
            return null;
        } else if (type instanceof GenericArrayType || (type instanceof Class && ((Class) type).isArray())) {
            if (obj instanceof Collection) {
                obj = ((Collection) obj).toArray();
            }
            if (!obj.getClass().isArray()) {
                throw new Exception("Unable to convert from " + obj + " to " + type);
            }
            Type componentType = type instanceof GenericArrayType
                                        ? ((GenericArrayType) type).getGenericComponentType()
                                        : ((Class) type).getComponentType();
            Object array = Array.newInstance(toClass(componentType), Array.getLength(obj));
            for (int i = 0; i < Array.getLength(obj); i++) {
                try {
                    Array.set(array, i, convert(Array.get(obj, i), componentType, converter));
                } catch (Exception t) {
                    throw new Exception("Unable to convert from " + obj + " to " + type + "(error converting array element)", t);
                }
            }
            return array;
        // TODO: removing the second part of the test will allow conversion between collections, is this desired?
        } else if (type instanceof ParameterizedType /*&& toClass(type).isInstance(obj)*/) {
            Class cl = toClass(type);
            if (Map.class.isAssignableFrom(cl) && obj instanceof Map) {
                Type keyType = Object.class;
                Type valueType = Object.class;
                Type[] typeParameters = getTypeParameters(Map.class, type);
                if (typeParameters != null && typeParameters.length == 2) {
                    keyType = typeParameters[0];
                    valueType = typeParameters[1];
                }
                Map newMap = (Map) getMap(cl).newInstance();
                for (Map.Entry e : ((Map<Object,Object>) obj).entrySet()) {
                    try {
                        newMap.put(convert(e.getKey(), keyType, converter), convert(e.getValue(), valueType, converter));
                    } catch (Exception t) {
                        throw new Exception("Unable to convert from " + obj + " to " + type + "(error converting map entry)", t);
                    }
                }
                return newMap;
            } else if (Collection.class.isAssignableFrom(cl) && obj instanceof Collection) {
                Type valueType = Object.class;
                Type[] typeParameters = getTypeParameters(Collection.class, type);
                if (typeParameters != null && typeParameters.length == 1) {
                    valueType = typeParameters[0];
                }
                Collection newCol = (Collection) getCollection(cl).newInstance();
                for (Object item : (Collection) obj) {
                    try {
                        newCol.add(convert(item, valueType, converter));
                    } catch (Exception t) {
                        throw new Exception("Unable to convert from " + obj + " to " + type + "(error converting collection entry)", t);
                    }
                }
                return newCol;
            }
        }
        return converter.convert(obj, toClass(type));
    }

    public static Class getMap(Class type) {
        if (TypeUtils.hasDefaultConstructor(type)) {
            return type;
        } else if (SortedMap.class.isAssignableFrom(type)) {
            return TreeMap.class;
        } else if (ConcurrentMap.class.isAssignableFrom(type)) {
            return ConcurrentHashMap.class;
        } else {
            return LinkedHashMap.class;
        }
    }

    public static Class getCollection(Class type) {
        if (TypeUtils.hasDefaultConstructor(type)) {
            return type;
        } else if (SortedSet.class.isAssignableFrom(type)) {
            return TreeSet.class;
        } else if (Set.class.isAssignableFrom(type)) {
            return LinkedHashSet.class;
        } else if (List.class.isAssignableFrom(type)) {
            return ArrayList.class;
        } else {
            return ArrayList.class;
        }
    }

}
