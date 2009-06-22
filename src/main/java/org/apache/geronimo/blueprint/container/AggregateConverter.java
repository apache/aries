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
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.geronimo.blueprint.ExtendedBlueprintContainer;
import org.apache.geronimo.blueprint.di.CollectionRecipe;
import org.apache.geronimo.blueprint.di.MapRecipe;
import static org.apache.geronimo.blueprint.utils.ReflectionUtils.getRealCause;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.CollapsedType;
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

    /**
     * Objects implementing this interface will bypass the default conversion rules
     * and be called directly to transform into the expected type.
     */
    public static interface Convertible {

        Object convert(CollapsedType type) throws Exception;
    }

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

    public boolean canConvert(Object fromValue, CollapsedType toType) {
        if (fromValue == null) {
            return true;
        }
        if (isAssignable(fromValue, toType)) {
            return true;
        }
        for (Converter converter : converters) {
            if (converter.canConvert(fromValue, toType)) {
                return true;
            }
        }
        // TODO
        if (fromValue instanceof String) {
            //
        }
        return false;
    }

    public Object convert(Object fromValue, CollapsedType type) throws Exception {
        // Discard null values
        if (fromValue == null) {
            return null;
        }
        // First convert service proxies
        if (fromValue instanceof Convertible) {
            return ((Convertible) fromValue).convert(type);
        }
        // If the object is an instance of the type, just return it
        if (isAssignable(fromValue, type)) {
            return fromValue;
        }
        Object value = convertWithConverters(fromValue, type);
        if (value == null) {
            if (fromValue instanceof String) {
                return convertFromString((String) fromValue, toClass(type), blueprintContainer);
            } else if (toClass(type).isArray()) {
                return convertArray(fromValue, type);
            } else if (Map.class.isAssignableFrom(toClass(type))) {
                return convertMap(fromValue, type);
            } else if (Collection.class.isAssignableFrom(toClass(type))) {
                return convertCollection(fromValue, type);
            } else {
                throw new Exception("Unable to convert value " + fromValue + " to type " + type);
            }
        }
        return value;
    }

    private Object convertWithConverters(Object source, CollapsedType type) throws Exception {
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

    public Object convertFromString(String value, Class toType, Object loader) throws Exception {
        if (CollapsedType.class == toType && blueprintContainer.getCompliance() == BlueprintContainer.COMPLIANCE_LOOSE) {
            try {
                return GenericType.parse(value, loader);
            } catch (ClassNotFoundException e) {
                throw new Exception("Unable to convert", e);
            }
        } else if (Class.class == toType) {
            try {
                return GenericType.parse(value, loader).getRawClass();
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

    private Object convertCollection(Object obj, CollapsedType type) throws Exception {
        CollapsedType valueType = type.getActualTypeArgument(0);
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

    private Object convertMap(Object obj, CollapsedType type) throws Exception {
        CollapsedType keyType = type.getActualTypeArgument(0);
        CollapsedType valueType = type.getActualTypeArgument(1);
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

    private Object convertArray(Object obj, CollapsedType type) throws Exception {
        if (obj instanceof Collection) {
            obj = ((Collection) obj).toArray();
        }
        if (!obj.getClass().isArray()) {
            throw new Exception("Unable to convert from " + obj + " to " + type);
        }
        CollapsedType componentType = type.getActualTypeArgument(0);
        Object array = Array.newInstance(toClass(componentType), Array.getLength(obj));
        for (int i = 0; i < Array.getLength(obj); i++) {
            try {
                Array.set(array, i, convert(Array.get(obj, i), componentType));
            } catch (Exception t) {
                throw new Exception("Unable to convert from " + obj + " to " + type + "(error converting array element)", t);
            }
        }
        return array;
    }

    // TODO need to do proper disambiguation
    private <T> Constructor<T> getDisambiguatedConstructor(Class<T> t, Class<?> s) {
        for (Constructor<T> c : t.getConstructors()) {
            if (c.getParameterTypes().length == 1
                    && c.getParameterTypes()[0] == s) {
                return c;
            }
        }
        for (Constructor<T> c : t.getConstructors()) {
            if (c.getParameterTypes().length == 1
                    && c.getParameterTypes()[0].isAssignableFrom(s)) {
                return c;
            }
        }
        return null;
    }

    private Collection<?> getAsCollection(Object s) {
        if (s.getClass().isArray())
            return Arrays.asList((Object[]) s);
        else if (Collection.class.isAssignableFrom(s.getClass()))
            return (Collection<?>) s;
        else
            return null;
    }

    private boolean isAssignable(Object source, CollapsedType target) {
        return target.size() == 0
                && unwrap(target.getRawClass()).isAssignableFrom(unwrap(source.getClass()));
    }

    private static Class unwrap(Class c) {
        Class u = primitives.get(c);
        return u != null ? u : c;
    }

    private static final Map<Class, Class> primitives;
    static {
        primitives = new HashMap<Class, Class>();
        primitives.put(byte.class, Byte.class);
        primitives.put(short.class, Short.class);
        primitives.put(char.class, Character.class);
        primitives.put(int.class, Integer.class);
        primitives.put(long.class, Long.class);
        primitives.put(float.class, Float.class);
        primitives.put(double.class, Double.class);
        primitives.put(boolean.class, Boolean.class);
    }

    public Object convert(Object source, Type target) throws Exception {
        return convert( source, new GenericType(target));
    }


    private Class<?> loadClass(String s) throws Exception {

        return blueprintContainer.loadClass(s);
    }

    private Class toClass(CollapsedType type) {
        return type.getRawClass();
    }

}