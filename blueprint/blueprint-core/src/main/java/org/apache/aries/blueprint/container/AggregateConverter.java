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
package org.apache.aries.blueprint.container;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.apache.aries.blueprint.container.BeanRecipe.UnwrapperedBeanHolder;
import org.apache.aries.blueprint.di.CollectionRecipe;
import org.apache.aries.blueprint.di.MapRecipe;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.utils.ReflectionUtils;
import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.ReifiedType;

import static org.apache.aries.blueprint.utils.ReflectionUtils.getRealCause;

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
 * @version $Rev$, $Date$
 */
public class AggregateConverter implements Converter {

    /**
     * Objects implementing this interface will bypass the default conversion rules
     * and be called directly to transform into the expected type.
     */
    public static interface Convertible {

        Object convert(ReifiedType type) throws Exception;
    }

    private static class ConversionResult {

        public final Converter converter;
        public final Object value;

        public ConversionResult(Converter converter, Object value) {
            this.converter = converter;
            this.value = value;
        }
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

    public boolean canConvert(Object fromValue, final ReifiedType toType) {
        if (fromValue == null) {
            return true;
        } else if (fromValue instanceof UnwrapperedBeanHolder) {
        	fromValue = ((UnwrapperedBeanHolder) fromValue).unwrapperedBean;
        }
        if (isAssignable(fromValue, toType)) {
            return true;
        }
        
        final Object toTest = fromValue;
        boolean canConvert = false;
        AccessControlContext acc = blueprintContainer.getAccessControlContext();
        if (acc == null) {
            canConvert = canConvertWithConverters(toTest, toType);
        } else {
            canConvert = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                public Boolean run() {
                    return canConvertWithConverters(toTest, toType);
                }            
            }, acc);
        }
        if (canConvert) {
            return true;
        }
        
        // TODO implement better logic ?!
        try {
            convert(toTest, toType);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Object convert(Object fromValue, final ReifiedType type) throws Exception {
        // Discard null values
        if (fromValue == null) {
            return null;
        }
        // First convert service proxies
        if (fromValue instanceof Convertible) {
            return ((Convertible) fromValue).convert(type);
        } else if (fromValue instanceof UnwrapperedBeanHolder) {
        	UnwrapperedBeanHolder holder = (UnwrapperedBeanHolder) fromValue;
        	if (isAssignable(holder.unwrapperedBean, type)) {
                return BeanRecipe.wrap(holder, type.getRawClass());
            } else {
            	fromValue = BeanRecipe.wrap(holder, Object.class);
            }
        } else if (isAssignable(fromValue, type)) {
        	 // If the object is an instance of the type, just return it
            return fromValue;
        }
        
        final Object finalFromValue = fromValue;
        ConversionResult result = null;
        AccessControlContext acc = blueprintContainer.getAccessControlContext();
        if (acc == null) {
            result = convertWithConverters(fromValue, type);
        } else {
            result = AccessController.doPrivileged(new PrivilegedExceptionAction<ConversionResult>() {
                public ConversionResult run() throws Exception {
                    return convertWithConverters(finalFromValue, type);
                }            
            }, acc);
        }
        if (result == null) {
            if (fromValue instanceof Number && Number.class.isAssignableFrom(unwrap(toClass(type)))) {
                return convertToNumber((Number) fromValue, toClass(type));
            } else if (fromValue instanceof String) {
                return convertFromString((String) fromValue, toClass(type), blueprintContainer);
            } else if (toClass(type).isArray() && (fromValue instanceof Collection || fromValue.getClass().isArray())) {
                return convertToArray(fromValue, type);
            } else if (Map.class.isAssignableFrom(toClass(type)) && (fromValue instanceof Map || fromValue instanceof Dictionary)) {
                return convertToMap(fromValue, type);
            } else if (Dictionary.class.isAssignableFrom(toClass(type)) && (fromValue instanceof Map || fromValue instanceof Dictionary)) {
                return convertToDictionary(fromValue, type);
            } else if (Collection.class.isAssignableFrom(toClass(type)) && (fromValue instanceof Collection || fromValue.getClass().isArray())) {
                return convertToCollection(fromValue, type);
            } else {
                throw new Exception("Unable to convert value " + fromValue + " to type " + type);
            }
        }
        return result.value;
    }

    private Converter selectMatchingConverter(Object source, ReifiedType type) {
        for (Converter converter : converters) {
            if (converter.canConvert(source, type)) {
                return converter;
            }
        }
        return null;
    }

    private boolean canConvertWithConverters(Object source, ReifiedType type) {
        return selectMatchingConverter(source,type) != null;
    }
    
    private ConversionResult convertWithConverters(Object source, ReifiedType type) throws Exception {

        Converter converter = selectMatchingConverter(source,type);

        if (converter == null)  return null;

        Object value = converter.convert(source, type);
        return new ConversionResult(converter,value);
    }
    
    public Object convertToNumber(Number value, Class toType) throws Exception {
        toType = unwrap(toType);
        if (AtomicInteger.class == toType) {
            return new AtomicInteger((Integer) convertToNumber(value, Integer.class));
        } else if (AtomicLong.class == toType) {
            return new AtomicLong((Long) convertToNumber(value, Long.class));
        } else if (Integer.class == toType) {
            return value.intValue();
        } else if (Short.class == toType) {
            return value.shortValue();
        } else if (Long.class == toType) {
            return value.longValue();
        } else if (Float.class == toType) {
            return value.floatValue();
        } else if (Double.class == toType) {
            return value.doubleValue();
        } else if (Byte.class == toType) {
            return value.byteValue();
        } else if (BigInteger.class == toType) {
            return new BigInteger(value.toString());
        } else if (BigDecimal.class == toType) {
            return new BigDecimal(value.toString());
        } else {
            throw new Exception("Unable to convert number " + value + " to " + toType);
        }
    }

    public Object convertFromString(String value, Class toType, Object loader) throws Exception {
        toType = unwrap(toType);
        if (ReifiedType.class == toType) {
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
        } else if (Boolean.class == toType) {
            if ("yes".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)) {
                return Boolean.TRUE;
            } else if ("no".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value) || "off".equalsIgnoreCase(value)) {
                return Boolean.FALSE;
            } else {
                throw new RuntimeException("Invalid boolean value: " + value);
            }
        } else if (Integer.class == toType) {
            return Integer.valueOf(value);
        } else if (Short.class == toType) {
            return Short.valueOf(value);
        } else if (Long.class == toType) {
            return Long.valueOf(value);
        } else if (Float.class == toType) {
            return Float.valueOf(value);
        } else if (Double.class == toType) {
            return Double.valueOf(value);
        } else if (Character.class == toType) {
            if (value.length() == 6 && value.startsWith("\\u")) {
                int code = Integer.parseInt(value.substring(2), 16);
                return (char)code;
            } else if (value.length() == 1) {
                return value.charAt(0);
            } else {
                throw new Exception("Invalid value for character type: " + value);
            }
        } else if (Byte.class == toType) {
            return Byte.valueOf(value);
        } else if (Enum.class.isAssignableFrom(toType)) {
            return Enum.valueOf((Class<Enum>) toType, value);
        } else {
            return createObject(value, toType);
        }
    }

    private Object createObject(String value, Class type) throws Exception {
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
            return ReflectionUtils.newInstance(blueprintContainer.getAccessControlContext(), constructor, value);
        } catch (Exception e) {
            throw new Exception("Unable to convert ", getRealCause(e));
        }
    }
    
    private Object convertToCollection(Object obj, ReifiedType type) throws Exception {
        ReifiedType valueType = type.getActualTypeArgument(0);
        Collection newCol = (Collection) ReflectionUtils.newInstance(blueprintContainer.getAccessControlContext(), 
                                                                     CollectionRecipe.getCollection(toClass(type)));
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

    private Object convertToDictionary(Object obj, ReifiedType type) throws Exception {
        ReifiedType keyType = type.getActualTypeArgument(0);
        ReifiedType valueType = type.getActualTypeArgument(1);
        Dictionary newDic = new Hashtable();
        if (obj instanceof Dictionary) {
            Dictionary dic = (Dictionary) obj;
            for (Enumeration keyEnum = dic.keys(); keyEnum.hasMoreElements();) {
                Object key = keyEnum.nextElement();
                try {
                    newDic.put(convert(key, keyType), convert(dic.get(key), valueType));
                } catch (Exception t) {
                    throw new Exception("Unable to convert from " + obj + " to " + type + "(error converting map entry)", t);
                }
            }
        } else {
            for (Map.Entry e : ((Map<Object,Object>) obj).entrySet()) {
                try {
                    newDic.put(convert(e.getKey(), keyType), convert(e.getValue(), valueType));
                } catch (Exception t) {
                    throw new Exception("Unable to convert from " + obj + " to " + type + "(error converting map entry)", t);
                }
            }
        }
        return newDic;
    }

    private Object convertToMap(Object obj, ReifiedType type) throws Exception {
        ReifiedType keyType = type.getActualTypeArgument(0);
        ReifiedType valueType = type.getActualTypeArgument(1);
        Map newMap = (Map) ReflectionUtils.newInstance(blueprintContainer.getAccessControlContext(), 
                                                       MapRecipe.getMap(toClass(type)));
        if (obj instanceof Dictionary) {
            Dictionary dic = (Dictionary) obj;
            for (Enumeration keyEnum = dic.keys(); keyEnum.hasMoreElements();) {
                Object key = keyEnum.nextElement();
                try {
                    newMap.put(convert(key, keyType), convert(dic.get(key), valueType));
                } catch (Exception t) {
                    throw new Exception("Unable to convert from " + obj + " to " + type + "(error converting map entry)", t);
                }
            }
        } else {
            for (Map.Entry e : ((Map<Object,Object>) obj).entrySet()) {
                try {
                    newMap.put(convert(e.getKey(), keyType), convert(e.getValue(), valueType));
                } catch (Exception t) {
                    throw new Exception("Unable to convert from " + obj + " to " + type + "(error converting map entry)", t);
                }
            }
        }
        return newMap;
    }

    private Object convertToArray(Object obj, ReifiedType type) throws Exception {
        if (obj instanceof Collection) {
            obj = ((Collection) obj).toArray();
        }
        if (!obj.getClass().isArray()) {
            throw new Exception("Unable to convert from " + obj + " to " + type);
        }
        ReifiedType componentType;
        if (type.size() > 0) {
            componentType = type.getActualTypeArgument(0);
        } else {
            componentType = new GenericType(type.getRawClass().getComponentType());
        }
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

    public static boolean isAssignable(Object source, ReifiedType target) {
        if (source == null) {
            return true;
        }
        if (target.size() == 0) {
            return unwrap(target.getRawClass()).isAssignableFrom(unwrap(source.getClass()));
        } else {
            return isTypeAssignable(new GenericType(source.getClass()), target);
        }
    }

    public static boolean isTypeAssignable(ReifiedType from, ReifiedType to) {
        if (from.equals(to)) {
            return true;
        }
        Type t = from.getRawClass().getGenericSuperclass();
        if (t != null && isTypeAssignable(new GenericType(t), to)) {
            return true;
        }
        for (Type ti : from.getRawClass().getGenericInterfaces()) {
            if (ti != null && isTypeAssignable(new GenericType(ti), to)) {
                return true;
            }
        }
        return false;
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

    private Class toClass(ReifiedType type) {
        return type.getRawClass();
    }
    
}