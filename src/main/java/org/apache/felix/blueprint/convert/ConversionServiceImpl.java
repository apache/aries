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
package org.apache.felix.blueprint.convert;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.osgi.service.blueprint.convert.ConversionService;
import org.osgi.service.blueprint.convert.Converter;

public class ConversionServiceImpl implements ConversionService {

    private Map<Class, List<Converter>> convertersMap = new HashMap<Class, List<Converter>>();

    public void registerConverter(Converter converter) {
        Class type = converter.getTargetClass();
        List<Converter> converters = convertersMap.get(type);
        if (converters == null) {
            converters = new ArrayList<Converter>();
            convertersMap.put(type, converters);
        }
        converters.add(converter);
    }

    public void unregisterConverter(Converter converter) {
        Class type = converter.getTargetClass();
        List<Converter> converters = convertersMap.get(type);
        if (converters != null) {
            converters.remove(converter);
        }
    }
    
    public Object convert(Object fromValue, Class toType) throws Exception {
        if (Object.class == toType) {
            return fromValue;
        }
        Converter converter = lookupConverter(toType);
        if (converter == null) {
            return convertDefault(fromValue, toType);
        } else {
            return converter.convert(fromValue);
        }
    }

    private Converter lookupConverter(Class toType) {
        // do explicit lookup
        List<Converter> converters = convertersMap.get(toType);
        if (converters != null && !converters.isEmpty()) {
            return converters.get(0);
        }

        // try to find converter that matches the type
        for (Map.Entry<Class, List<Converter>> entry : convertersMap.entrySet()) {
            Class converterClass = entry.getKey();
            if (toType.isAssignableFrom(converterClass)) {
                return entry.getValue().get(0);
            }
        }

        return null;
    }

    private Object convertDefault(Object fromValue, Class toType) throws Exception {
        if (!(fromValue instanceof String)) {
            throw new RuntimeException("Unable to convert non-String value: " + fromValue.getClass());
        }
        String value = (String)fromValue;
        if (Locale.class == toType) {
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
            if (value.length() == 1) {
                return Character.valueOf(value.charAt(0));
            } else {
                throw new Exception("Invalid value for character type: " + value);
            }
        } else if (Byte.class == toType || byte.class == toType) {
            return Byte.valueOf(value);
        } else {
            return createObject(value, toType);
        }
    }

    private Object createObject(String value, Class type) throws Exception {
        Constructor constructor = null;
        try {
            constructor = type.getConstructor(String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to convert to " + type);
        }
        try {
            return constructor.newInstance(value);
        } catch (InvocationTargetException e) {
            throw new Exception("Unable to convert ", e.getTargetException());
        } catch (Exception e) {
            throw new Exception("Unable to convert ", e);
        }
    }

}