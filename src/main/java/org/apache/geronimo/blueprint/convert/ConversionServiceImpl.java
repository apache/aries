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
package org.apache.geronimo.blueprint.convert;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.geronimo.blueprint.context.BlueprintContextImpl;
import org.apache.xbean.recipe.RecipeHelper;
import org.osgi.service.blueprint.convert.ConversionService;
import org.osgi.service.blueprint.convert.Converter;

/**
 * Implementation of the ConverterService.
 *
 * This object contains all the registered Converters which can be registered
 * by using {@link #registerConverter(org.osgi.service.blueprint.convert.Converter)}
 * and unregistered using {@link #unregisterConverter(org.osgi.service.blueprint.convert.Converter)}.
 *
 * Each {@link org.osgi.service.blueprint.context.BlueprintContext} has its own ConversionService
 * used to register converters defined by the related blueprint bundle.
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class ConversionServiceImpl implements ConversionService {

    private BlueprintContextImpl blueprintContext;
    private Map<Class, List<Converter>> convertersMap = new HashMap<Class, List<Converter>>();

    public ConversionServiceImpl(BlueprintContextImpl blueprintContext) {
        this.blueprintContext = blueprintContext;
    }
    
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
        if (RecipeHelper.isInstance(toType, fromValue)) {
            return fromValue;
        }
        Object value = doConvert(fromValue, toType);        
        if (value == null) {
            if (fromValue instanceof String) {
                return convertString( (String)fromValue, toType);
            } else {
                throw new Exception("Unable to convert value " + fromValue + " to type: " + toType.getName());
            }
        } else {
            return value;
        }
    }

    private Object doConvert(Object source, Class type) {
        Object value = null;
        
        // do explicit lookup
        List<Converter> converters = convertersMap.get(type);
        if (converters != null) {
            value = convert(converters, source);
            if (value != null) {
                return value;
            }
        }
                
        // try to find converter that matches the type
        for (Map.Entry<Class, List<Converter>> entry : convertersMap.entrySet()) {
            Class converterClass = entry.getKey();
            if (type.isAssignableFrom(converterClass)) {
                value = convert(entry.getValue(), source);
                if (value != null) {
                    return value;
                }
            }
        }        

        return null;
    }

    private Object convert(List<Converter> converters, Object source) {
        Object value = null;
        for (Converter converter : converters) {
            try {
                value = converter.convert(source);
                if (value != null) {
                    return value;
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return null;
    }
    
    private Object convertString(String value, Class toType) throws Exception {
        if (Class.class == toType) {
            try {
                return blueprintContext.getClassLoader().loadClass(value);
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
                return Character.valueOf(value.charAt(0));
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

    private Object createObject(String value, Class type) throws Exception {
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            throw new Exception("Unable to convert. Type class is an interface or is an abstract class");
        }
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