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
package org.apache.aries.jmx.util;

import static org.osgi.jmx.JmxConstants.BIGDECIMAL;
import static org.osgi.jmx.JmxConstants.BIGINTEGER;
import static org.osgi.jmx.JmxConstants.BOOLEAN;
import static org.osgi.jmx.JmxConstants.BYTE;
import static org.osgi.jmx.JmxConstants.CHARACTER;
import static org.osgi.jmx.JmxConstants.DOUBLE;
import static org.osgi.jmx.JmxConstants.FLOAT;
import static org.osgi.jmx.JmxConstants.INTEGER;
import static org.osgi.jmx.JmxConstants.LONG;
import static org.osgi.jmx.JmxConstants.P_BOOLEAN;
import static org.osgi.jmx.JmxConstants.P_BYTE;
import static org.osgi.jmx.JmxConstants.P_CHAR;
import static org.osgi.jmx.JmxConstants.P_DOUBLE;
import static org.osgi.jmx.JmxConstants.P_FLOAT;
import static org.osgi.jmx.JmxConstants.P_INT;
import static org.osgi.jmx.JmxConstants.P_LONG;
import static org.osgi.jmx.JmxConstants.P_SHORT;
import static org.osgi.jmx.JmxConstants.SHORT;
import static org.osgi.jmx.JmxConstants.STRING;
import static org.osgi.jmx.JmxConstants.VERSION;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Version;

/**
 * This class provides common utilities related to type conversions for the MBean implementations
 *
 * @version $Rev$ $Date$
 */
public class TypeUtils {

    private TypeUtils() {
        super();
    }

    public static Map<String, Class<? extends Object>> primitiveTypes = new HashMap<String, Class<? extends Object>>();
    public static Map<String, Class<? extends Object>> wrapperTypes = new HashMap<String, Class<? extends Object>>();
    public static Map<String, Class<? extends Object>> mathTypes = new HashMap<String, Class<? extends Object>>();
    public static Map<Class<? extends Object>, Class<? extends Object>> primitiveToWrapper = new HashMap<Class<? extends Object>, Class<? extends Object>>();
    public static Map<String, Class<? extends Object>> types = new HashMap<String, Class<? extends Object>>();

    static {
        primitiveTypes.put(P_FLOAT, Float.TYPE);
        primitiveTypes.put(P_INT, Integer.TYPE);
        primitiveTypes.put(P_LONG, Long.TYPE);
        primitiveTypes.put(P_DOUBLE, Double.TYPE);
        primitiveTypes.put(P_BYTE, Byte.TYPE);
        primitiveTypes.put(P_SHORT, Short.TYPE);
        primitiveTypes.put(P_CHAR, Character.TYPE);
        primitiveTypes.put(P_BOOLEAN, Boolean.TYPE);
        primitiveToWrapper.put(Float.TYPE, Float.class);
        primitiveToWrapper.put(Integer.TYPE, Integer.class);
        primitiveToWrapper.put(Long.TYPE, Long.class);
        primitiveToWrapper.put(Double.TYPE, Double.class);
        primitiveToWrapper.put(Byte.TYPE, Byte.class);
        primitiveToWrapper.put(Short.TYPE, Short.class);
        primitiveToWrapper.put(Boolean.TYPE, Boolean.class);
        wrapperTypes.put(INTEGER, Integer.class);
        wrapperTypes.put(FLOAT, Float.class);
        wrapperTypes.put(LONG, Long.class);
        wrapperTypes.put(DOUBLE, Double.class);
        wrapperTypes.put(BYTE, Byte.class);
        wrapperTypes.put(SHORT, Short.class);
        wrapperTypes.put(BOOLEAN, Boolean.class);
        wrapperTypes.put(CHARACTER, Character.class);
        wrapperTypes.put(VERSION, Version.class);
        mathTypes.put(BIGDECIMAL, BigDecimal.class);
        mathTypes.put(BIGINTEGER, BigInteger.class);
        types.put(STRING, String.class);
        types.putAll(primitiveTypes);
        types.putAll(wrapperTypes);
        types.putAll(mathTypes);
    }

    /**
     * Converts a <code>Dictionary</code> object to a <code>Map</code>
     *
     * @param dictionary
     * @return
     */
    public static Map<String, String> fromDictionary(Dictionary<String, String> dictionary) {
        Map<String, String> result = new HashMap<String, String>();
        Enumeration<String> keys = dictionary.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            result.put(key, dictionary.get(key));
        }
        return result;
    }

    /**
     * Converts primitive long[] array to Long[]
     *
     * @param array
     * @return
     */
    public static Long[] toLong(long[] array) {
        Long[] toArray = (array == null) ? new Long[0] : new Long[array.length];
        for (int i = 0; i < toArray.length; i++) {
            toArray[i] = array[i];
        }
        return toArray;
    }

    /**
     * Converts Long[] array to primitive
     *
     * @param array
     * @return
     */
    public static long[] toPrimitive(Long[] array) {
        long[] toArray = (array == null) ? new long[0] : new long[array.length];
        for (int i = 0; i < toArray.length; i++) {
            toArray[i] = array[i];
        }
        return toArray;
    }

    /**
     * Converts a String value to an Object of the specified type
     *
     * @param type
     *            one of types listed in {@link #types}
     * @param value
     * @return instance of class <code>type</code>
     * @throws IllegalArgumentException
     *             if type or value are null or if the Class type does not support a valueOf() or cannot be converted to
     *             a wrapper type
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromString(Class<T> type, String value) {
        if (type == null || !types.containsValue(type)) {
            throw new IllegalArgumentException("Cannot convert to type argument : " + type);
        }
        if (value == null || value.length() < 1) {
            throw new IllegalArgumentException("Argument value cannot be null or empty");
        }
        T result = null;
        try {
            if (type.equals(String.class)) {
                result = (T) value;
            } else if (type.equals(Character.class) || type.equals(Character.TYPE)) {
                result = (T) Character.valueOf(value.charAt(0));
            } else if (wrapperTypes.containsValue(type) || mathTypes.containsValue(type)) {
                Constructor<? extends Object> constructor = type.getConstructor(String.class);
                result = (T) constructor.newInstance(value);
            } else if (primitiveToWrapper.containsKey(type)) { // attempt to promote to wrapper and resolve to the base
                                                               // type
                Class<? extends Object> promotedType = primitiveToWrapper.get(type);
                char[] simpleTypeName = type.getName().toCharArray();
                simpleTypeName[0] = Character.toUpperCase(simpleTypeName[0]);
                String parseMethodName = "parse" + new String(simpleTypeName);
                Method parseMethod = promotedType.getDeclaredMethod(parseMethodName, String.class);
                result = (T) parseMethod.invoke(null, value);
            }
        } catch (SecurityException e) {
            throw new IllegalArgumentException("Cannot convert value [" + value + "] to type [" + type + "]", e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Cannot convert value [" + value + "] to type [" + type + "]", e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cannot convert value [" + value + "] to type [" + type + "]", e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Cannot convert value [" + value + "] to type [" + type + "]", e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Cannot convert value [" + value + "] to type [" + type + "]", e);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Cannot convert value [" + value + "] to type [" + type + "]", e);
        }
        return result;
    }
}
