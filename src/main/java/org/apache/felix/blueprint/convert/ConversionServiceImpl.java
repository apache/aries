/*
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
package org.apache.felix.blueprint.convert;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Constructor;
import java.io.StringReader;
import java.io.Reader;
import java.io.ByteArrayInputStream;

import org.osgi.service.blueprint.convert.ConversionService;
import org.osgi.service.blueprint.convert.Converter;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@felix.apache.org">Apache Felix Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class ConversionServiceImpl implements ConversionService {

    private static final Map<Class, Class> primitives;
    private List<Converter> converters = new ArrayList<Converter>();

    static {
        primitives = new HashMap<Class, Class>();
        primitives.put(byte.class, Byte.class);
        primitives.put(int.class, Integer.class);
        primitives.put(long.class, Long.class);
        primitives.put(float.class, Float.class);
        primitives.put(double.class, Double.class);
        primitives.put(short.class, Short.class);
        primitives.put(char.class, Character.class);
        primitives.put(boolean.class, Boolean.class);
    }

    public ConversionServiceImpl() {
    }

    public Object convert(Object fromValue, Class toType) throws Exception {
        if (fromValue == null) {
            return null;
        }
        // Check converters
        for (Converter converter : converters) {
            if (converter.getTargetClass().equals(toType)) {
                return converter.convert(fromValue);
            }
        }
        // Default conversion
        if (fromValue instanceof String) {
            String fromString = (String) fromValue;
            if (primitives.containsKey(toType)) {
                toType = primitives.get(toType);
            }
            // Boolean
            if (toType.equals(Boolean.class)) {
                fromString = fromString.toLowerCase();
                if (fromString.equals("true") || fromString.equals("yes") || fromString.equals("on")) {
                    return Boolean.TRUE;
                } else if (fromString.equals("false") || fromString.equals("no") || fromString.equals("off")) {
                    return Boolean.FALSE;
                } else {
                    throw new IllegalArgumentException("Illegal boolean value: " + fromString);
                }
            }
            // Character
            if (toType.equals(Character.class)) {
                if (fromString.length() == 1) {
                    return fromString.charAt(0);
                } else {
                    throw new IllegalArgumentException("Conversion from String to Character must have a strig argument of length equals to 1");
                }
            }
            // Locale
            if (toType.equals(Locale.class)) {
                // TODO
            }
            // Properties
            if (toType.equals(Properties.class)) {
                Properties props = new Properties();
                props.load(new ByteArrayInputStream(fromString.getBytes()));
                return props;
            }
            // Pattern
            if (toType.equals(Pattern.class)) {
                return Pattern.compile(fromString);
            }
            // Public constructor
            try {
                Constructor constructor = toType.getConstructor(String.class);
                return constructor.newInstance(fromString);
            } catch (NoSuchMethodException e) {
                // Ignore
            }
        }
        return null;
    }
}
