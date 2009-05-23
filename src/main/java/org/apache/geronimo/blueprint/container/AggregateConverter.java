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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.geronimo.blueprint.ExtendedBlueprintContainer;
import org.apache.geronimo.blueprint.utils.TypeUtils;
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

    public boolean canConvert(Object fromValue, Class toType) {
        if (fromValue == null) {
            return true;
        }
        if (TypeUtils.isInstance(toType, fromValue)) {
            return true;
        }
        for (Converter converter : converters) {
            if (converter.canConvert(fromValue, toType)) {
                return true;
            }
        }
        if (fromValue instanceof String) {

        }
        return false;
    }

    public Object convert(Object fromValue, Class toType) throws Exception {
        if (fromValue == null) {
            return null;
        }
        if (TypeUtils.isInstance(toType, fromValue)) {
            return fromValue;
        }
        Object value = doConvert(fromValue, toType);        
        if (value == null) {
            if (fromValue instanceof String) {
                return convertString((String) fromValue, toType);
            } else {
                throw new Exception("Unable to convert value " + fromValue + " to type: " + toType.getName());
            }
        } else {
            return value;
        }
    }

    private Object doConvert(Object source, Class type) throws Exception {
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

    private Object convertString(String value, Class toType) throws Exception {
        if (Class.class == toType) {
            try {
                return blueprintContainer.loadClass(value);
            } catch (ClassNotFoundException e) {
                throw new Exception("Unable to convert", e);
            }
        } else {
            return defaultConversion(value, toType);
        }
    }

    public static Object defaultConversion(String value, Class toType) throws Exception {
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
        } catch (InvocationTargetException e) {
            throw new Exception("Unable to convert ", e.getTargetException());
        } catch (Exception e) {
            throw new Exception("Unable to convert ", e);
        }
    }

}