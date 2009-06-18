/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.blueprint.container;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.geronimo.blueprint.ExtendedBlueprintContainer;
import org.apache.geronimo.blueprint.di.MapRecipe;
import org.apache.geronimo.blueprint.di.CollectionRecipe;
import static org.apache.geronimo.blueprint.utils.ReflectionUtils.getRealCause;
import org.apache.geronimo.blueprint.utils.TypeUtils;
import static org.apache.geronimo.blueprint.utils.TypeUtils.toClass;
import org.osgi.service.blueprint.container.Converter;

/**
 * Implementation of the Converter.
 *
 * This object contains all the registered Converters which can be registered
 * by using {@link #registerConverter(Converter)}
 * and unregistered using {@link #unregisterConverter(Converter)}.
 *
 * Each {@link org.osgi.service.blueprint.container.BlueprintContainer} has its own AggregateConverter
 * used to register converters defined by the related blueprint bundle.
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class AggregateConverter implements Converter {

    private ExtendedBlueprintContainer blueprintContainer;
    private List<Converter> converters = new ArrayList<Converter>();

    public AggregateConverter(ExtendedBlueprintContainer blueprintContainer) {
        this.blueprintContainer = blueprintContainer;
    }

    public void registerConverter(Converter converter) {
        converters.add(converter);
    }

    public void unregisterConverter(Converter converter) {
        converters.remove(converter);
    }

    public boolean canConvert(Object fromValue, Object toType) {
        if (fromValue == null) {
            return true;
        }
        Type type = (Type) toType;
        if (TypeUtils.isInstance(type, fromValue)) {
            return true;
        }
        for (Converter converter : converters) {
            if (converter.canConvert(fromValue, type)) {
                return true;
            }
        }
        if (fromValue instanceof String) {

        }
        return false;
    }

    public Object convert(Object fromValue, Object toType) throws Exception {
        // Discard null values
        if (fromValue == null) {
            return null;
        }
        Type type = (Type) toType;
        // First convert service proxies
        if (fromValue instanceof Convertible) {
            return ((Convertible) fromValue).convert(type);
        }
        // If the object is an instance of the type, just return it
        // We need to pass through for arrays / maps / collections because of genenrics
        if (TypeUtils.isInstance(type, fromValue)
                && !toClass(type).isArray()
                && !Map.class.isAssignableFrom(toClass(type))
                && !Collection.class.isAssignableFrom(toClass(type))) {
            return fromValue;
        }
        Object value = convertWithConverters(fromValue, type);
        if (value == null) {
            if (fromValue instanceof String && toType instanceof Class) {
                return convertFromString((String) fromValue, (Class) type, blueprintContainer);
            } else if (toClass(type).isArray()) {
                return convertArray(fromValue, type);
            } else if (Map.class.isAssignableFrom(toClass(type))) {
                return convertMap(fromValue, type);
            } else if (Collection.class.isAssignableFrom(toClass(type))) {
                return convertCollection(fromValue, type);
            } else {
                throw new Exception("Unable to convert value " + fromValue + " to type " + type);
            }
        } else {
            return value;
        }
    }

    private Object convertWithConverters(Object source, Type type) throws Exception {
        Object value = null;
        for (Converter converter : converters) {
            if (converter.canConvert(source, type)) {
                value = converter.convert(source, type);
                if (value != null) {
                    return value;
                }
            }
        }
        return value;
    }

    public static Object convertFromString(String value, Class toType, Object loader) throws Exception {
        if (Class.class == toType || Type.class == toType) {
            try {
                return TypeUtils.parseJavaType(value, loader);
            } catch (ClassNotFoundException e) {
                throw new Exception("Unable to convert", e);
            }
        } else if (Locale.class == toType) {
            String[] tokens = value.split("_");
            if (tokens.length == 1) {
                return new Locale(tokens[0]);
            } else if (tokens.length == 2) {
                return new Locale(tokens[0], tokens[1]);
            } else if (tokens.length == 3) {
                return new Locale(tokens[0], tokens[1], tokens[2]);
            } else {
                throw new Exception("Invalid locale string:" + value);
            }
        } else if (Pattern.class == toType) {
            return Pattern.compile(value);
        } else if (Properties.class == toType) {
            Properties props = new Properties();
            ByteArrayInputStream in = new ByteArrayInputStream(value.getBytes("UTF8"));
            props.load(in);
            return props;
        } else if (Boolean.class == toType || boolean.class == toType) {
            if ("yes".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)) {
                return Boolean.TRUE;
            } else if ("no".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value) || "off".equalsIgnoreCase(value)) {
                return Boolean.FALSE;
            } else {
                throw new RuntimeException("Invalid boolean value: " + value);
            }
        } else if (Integer.class == toType || int.class == toType) {
            return Integer.valueOf(value);
        } else if (Short.class == toType || short.class == toType) {
            return Short.valueOf(value);
        } else if (Long.class == toType || long.class == toType) {
            return Long.valueOf(value);
        } else if (Float.class == toType || float.class == toType) {
            return Float.valueOf(value);
        } else if (Double.class == toType || double.class == toType) {
            return Double.valueOf(value);
        } else if (Character.class == toType || char.class == toType) {
            if (value.length() == 6 && value.startsWith("\\u")) {
                int code = Integer.parseInt(value.substring(2), 16);
                return (char)code;
            } else if (value.length() == 1) {
                return value.charAt(0);
            } else {
                throw new Exception("Invalid value for character type: " + value);
            }
        } else if (Byte.class == toType || byte.class == toType) {
            return Byte.valueOf(value);
        } else if (Enum.class.isAssignableFrom(toType)) {
            return Enum.valueOf((Class<Enum>) toType, value);
        } else {
            return createObject(value, toType);
        }
    }

    private static Object createObject(String value, Class type) throws Exception {
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            throw new Exception("Unable to convert value " + value + " to type " + type + ". Type " + type + " is an interface or an abstract class");
        }
        Constructor constructor = null;
        try {
            constructor = type.getConstructor(String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to convert to " + type);
        }
        try {
            return constructor.newInstance(value);
        } catch (Exception e) {
            throw new Exception("Unable to convert ", getRealCause(e));
        }
    }

    private Object convertCollection(Object obj, Type type) throws Exception {
        Type valueType = Object.class;
        Type[] typeParameters = TypeUtils.getTypeParameters(Collection.class, type);
        if (typeParameters != null && typeParameters.length == 1) {
            valueType = typeParameters[0];
        }
        Collection newCol = (Collection) CollectionRecipe.getCollection(toClass(type)).newInstance();
        if (obj.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(obj); i++) {
                try {
                    newCol.add(convert(Array.get(obj, i), valueType));
                } catch (Exception t) {
                    throw new Exception("Unable to convert from " + obj + " to " + type + "(error converting array element)", t);
                }
            }
        } else {
            for (Object item : (Collection) obj) {
                try {
                    newCol.add(convert(item, valueType));
                } catch (Exception t) {
                    throw new Exception("Unable to convert from " + obj + " to " + type + "(error converting collection entry)", t);
                }
            }
        }
        return newCol;
    }

    private Object convertMap(Object obj, Type type) throws Exception {
        Type keyType = Object.class;
        Type valueType = Object.class;
        Type[] typeParameters = TypeUtils.getTypeParameters(Map.class, type);
        if (typeParameters != null && typeParameters.length == 2) {
            keyType = typeParameters[0];
            valueType = typeParameters[1];
        }
        Map newMap = (Map) MapRecipe.getMap(toClass(type)).newInstance();
        for (Map.Entry e : ((Map<Object,Object>) obj).entrySet()) {
            try {
                newMap.put(convert(e.getKey(), keyType), convert(e.getValue(), valueType));
            } catch (Exception t) {
                throw new Exception("Unable to convert from " + obj + " to " + type + "(error converting map entry)", t);
            }
        }
        return newMap;
    }

    private Object convertArray(Object obj, Type type) throws Exception {
        if (obj instanceof Collection) {
            obj = ((Collection) obj).toArray();
        }
        if (!obj.getClass().isArray()) {
            throw new Exception("Unable to convert from " + obj + " to " + type);
        }
        Type componentType = type instanceof GenericArrayType
                                    ? ((GenericArrayType) type).getGenericComponentType()
                                    : toClass(type).getComponentType();
        Object array = Array.newInstance(TypeUtils.toClass(componentType), Array.getLength(obj));
        for (int i = 0; i < Array.getLength(obj); i++) {
            try {
                Array.set(array, i, convert(Array.get(obj, i), componentType));
            } catch (Exception t) {
                throw new Exception("Unable to convert from " + obj + " to " + type + "(error converting array element)", t);
            }
        }
        return array;
    }

    /**
     * Objects implementing this interface will bypass the default conversion rules
     * and be called directly to transform into the expected type.
     */
    public static interface Convertible {

        Object convert(Type type) throws Exception;
    }
}