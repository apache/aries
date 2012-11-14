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
package org.apache.aries.jmx.codec;

import static org.apache.aries.jmx.util.TypeUtils.fromString;
import static org.apache.aries.jmx.util.TypeUtils.primitiveTypes;
import static org.apache.aries.jmx.util.TypeUtils.types;
import static org.osgi.jmx.JmxConstants.ARRAY_OF;
import static org.osgi.jmx.JmxConstants.KEY;
import static org.osgi.jmx.JmxConstants.PROPERTY_TYPE;
import static org.osgi.jmx.JmxConstants.P_BOOLEAN;
import static org.osgi.jmx.JmxConstants.P_BYTE;
import static org.osgi.jmx.JmxConstants.P_CHAR;
import static org.osgi.jmx.JmxConstants.P_DOUBLE;
import static org.osgi.jmx.JmxConstants.P_FLOAT;
import static org.osgi.jmx.JmxConstants.P_INT;
import static org.osgi.jmx.JmxConstants.P_LONG;
import static org.osgi.jmx.JmxConstants.TYPE;
import static org.osgi.jmx.JmxConstants.VALUE;
import static org.osgi.jmx.JmxConstants.VECTOR_OF;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;

import org.osgi.jmx.JmxConstants;

/**
 * <p>
 * <tt>PropertyData</tt> represents Property Type @see {@link JmxConstants#PROPERTY_TYPE}. It is a codec for the
 * <code>CompositeData</code> representing a Property with an associated Type and Value.
 * </p>
 *
 * @version $Rev$ $Date$
 */
public class PropertyData<T> {

    /**
     * @see JmxConstants#KEY_ITEM
     */
    private String key;

    /**
     * @see JmxConstants#SCALAR
     */
    private T value;

    /**
     * @see JmxConstants#VALUE_ITEM
     */
    private String encodedValue;

    /**
     * @see JmxConstants#TYPE_ITEM
     */
    private String encodedType;

    private PropertyData() {
        super();
    }


    @SuppressWarnings("unchecked")
    private PropertyData(String key, T value, String preservedBaseType) throws IllegalArgumentException {
        if (key == null) {
            throw new IllegalArgumentException("Argument key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Argument value cannot be null");
        }
        this.key = key;
        this.value = value;
        Class<T> type = (Class<T>) value.getClass();
        if (type.isArray()) {
            this.encodedType = ARRAY_OF + type.getComponentType().getSimpleName();
            StringBuilder builder = new StringBuilder();
            int length = Array.getLength(value);
            boolean useDelimiter = false;
            for (int i = 0; i < length; i++) {
                if (useDelimiter) {
                    builder.append(",");
                } else {
                    useDelimiter = true;
                }
                builder.append(Array.get(value, i));
            }
            this.encodedValue = builder.toString();
        } else if (type.equals(Vector.class)) {
            Vector<?> vector = (Vector<?>) value;
            Class<? extends Object> componentType = Object.class;
            if (vector.size() > 0) {
                componentType = vector.firstElement().getClass();
            }
            this.encodedType = VECTOR_OF + componentType.getSimpleName();
            StringBuilder builder = new StringBuilder();
            Vector<?> valueVector = (Vector<?>) value;
            boolean useDelimiter = false;
            for (Object val : valueVector) {
                if (useDelimiter) {
                    builder.append(",");
                } else {
                    useDelimiter = true;
                }
                builder.append(val);
            }
            this.encodedValue = builder.toString();
        } else if (List.class.isAssignableFrom(type)) {
            // Lists are encoded as Arrays...
            List<?> list = (List<?>) value;
            Class<?> componentType = Object.class;
            if (list.size() > 0)
                componentType = list.get(0).getClass();

            this.encodedType = ARRAY_OF + componentType.getSimpleName();
            StringBuilder builder = new StringBuilder();
            boolean useDelimiter = false;
            for (Object o : list) {
                if (useDelimiter) {
                    builder.append(",");
                } else {
                    useDelimiter = true;
                }
                builder.append(o);
            }
            this.encodedValue = builder.toString();
        } else {
            this.encodedType = (preservedBaseType == null) ? type.getSimpleName() : preservedBaseType;
            this.encodedValue = value.toString();
        }
    }

    /**
     * Static factory method for <code>PropertyData</code> instance parameterized by value's type
     * @param <T>
     * @param key
     * @param value an instance of {@link JmxConstants#SCALAR}
     * @return
     * @throws IllegalArgumentException if key or value are null or value's type cannot be encoded
     */
    public static <T> PropertyData<T> newInstance(String key, T value) throws IllegalArgumentException {
        return new PropertyData<T>(key, value, null);
    }

    /**
     * Static factory method for <code>PropertyData</code> instance which preserves encoded type
     * information for primitive int type
     * @param key
     * @param value
     * @return
     * @throws IllegalArgumentException if key or value are null or value's type cannot be encoded
     */
    public static PropertyData<Integer> newInstance(String key, int value) throws IllegalArgumentException {
        return new PropertyData<Integer>(key, value, P_INT);
    }

    /**
     * Static factory method for <code>PropertyData</code> instance which preserves encoded type
     * information for primitive long type
     * @param key
     * @param value
     * @return
     * @throws IllegalArgumentException if key or value are null or value's type cannot be encoded
     */
    public static PropertyData<Long> newInstance(String key, long value) throws IllegalArgumentException {
        return new PropertyData<Long>(key, value, P_LONG);
    }

    /**
     * Static factory method for <code>PropertyData</code> instance which preserves encoded type
     * information for primitive float type
     * @param key
     * @param value
     * @return
     * @throws IllegalArgumentException if key or value are null or value's type cannot be encoded
     */
    public static PropertyData<Float> newInstance(String key, float value) throws IllegalArgumentException {
        return new PropertyData<Float>(key, value, P_FLOAT);
    }

    /**
     * Static factory method for <code>PropertyData</code> instance which preserves encoded type
     * information for primitive double type
     * @param key
     * @param value
     * @return
     * @throws IllegalArgumentException if key or value are null or value's type cannot be encoded
     */
    public static PropertyData<Double> newInstance(String key, double value) throws IllegalArgumentException {
        return new PropertyData<Double>(key, value, P_DOUBLE);
    }

    /**
     * Static factory method for <code>PropertyData</code> instance which preserves encoded type
     * information for primitive byte type
     * @param key
     * @param value
     * @return
     * @throws IllegalArgumentException if key or value are null or value's type cannot be encoded
     */
    public static PropertyData<Byte> newInstance(String key, byte value) throws IllegalArgumentException {
        return new PropertyData<Byte>(key, value, P_BYTE);
    }

    /**
     * Static factory method for <code>PropertyData</code> instance which preserves encoded type
     * information for primitive char type
     * @param key
     * @param value
     * @return
     * @throws IllegalArgumentException if key or value are null or value's type cannot be encoded
     */
    public static PropertyData<Character> newInstance(String key, char value) throws IllegalArgumentException {
        return new PropertyData<Character>(key, value, P_CHAR);
    }

    /**
     * Static factory method for <code>PropertyData</code> instance which preserves encoded type
     * information for primitive boolean type
     * @param key
     * @param value
     * @return
     * @throws IllegalArgumentException if key or value are null or value's type cannot be encoded
     */
    public static PropertyData<Boolean> newInstance(String key, boolean value) throws IllegalArgumentException {
        return new PropertyData<Boolean>(key, value, P_BOOLEAN);
    }


    /**
     * Returns CompositeData representing a Property typed by {@link JmxConstants#PROPERTY_TYPE}.
     * @return
     */
    public CompositeData toCompositeData() {
        CompositeData result = null;
        Map<String, Object> items = new HashMap<String, Object>();
        items.put(KEY, this.key);
        items.put(VALUE, this.encodedValue);
        items.put(TYPE, this.encodedType);
        try {
            result = new CompositeDataSupport(PROPERTY_TYPE, items);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Failed to create CompositeData for Property [" + this.key + ":" + this.value + "]", e);
        }
        return result;
    }

    /**
     * Constructs a <code>PropertyData</code> object from the given <code>CompositeData</code>
     * @param compositeData
     * @return
     * @throws IlleglArgumentException if compositeData is null or not of type {@link JmxConstants#PROPERTY_TYPE}
     */
    @SuppressWarnings("unchecked")
    public static <T> PropertyData<T> from(CompositeData compositeData) throws IllegalArgumentException {
        if ( compositeData == null ) {
            throw new IllegalArgumentException("Argument compositeData cannot be null");
        }
        if (!compositeData.getCompositeType().equals(PROPERTY_TYPE)) {
            throw new IllegalArgumentException("Invalid CompositeType [" + compositeData.getCompositeType() + "]");
        }
        PropertyData propertyData = new PropertyData();
        propertyData.key = (String) compositeData.get(KEY);
        propertyData.encodedType = (String) compositeData.get(TYPE);
        propertyData.encodedValue = (String) compositeData.get(VALUE);
        if (propertyData.encodedType == null || propertyData.encodedType.length() < 1) {
            throw new IllegalArgumentException ("Cannot determine type from compositeData : " + compositeData);
        }
        StringTokenizer values = new StringTokenizer(propertyData.encodedValue, ",");
        int valuesLength = values.countTokens();
        if (propertyData.encodedType.startsWith(ARRAY_OF)) {
            String[] arrayTypeParts = propertyData.encodedType.split("\\s");
            if (arrayTypeParts.length < 3) {
                throw new IllegalArgumentException("Cannot parse Array type from type item : " + propertyData.encodedType);
            }
            String arrayTypeName = arrayTypeParts[2].trim();
            if (!types.containsKey(arrayTypeName)) {
                throw new IllegalArgumentException ("Cannot determine type from value : " + arrayTypeName);
            }
            Class<? extends Object> arrayType = types.get(arrayTypeName);
            propertyData.value = Array.newInstance(arrayType, valuesLength);
            int index = 0;
            while (values.hasMoreTokens()) {
                Array.set(propertyData.value, index++, fromString(arrayType, values.nextToken()));
            }
        } else if (propertyData.encodedType.startsWith(VECTOR_OF)) {
            String[] vectorTypeParts = propertyData.encodedType.split("\\s");
            if (vectorTypeParts.length < 3) {
                throw new IllegalArgumentException("Cannot parse Array type from type item : " + propertyData.encodedType);
            }
            String vectorTypeName = vectorTypeParts[2].trim();
            if (!types.containsKey(vectorTypeName)) {
                throw new IllegalArgumentException ("Cannot determine type from value : " + vectorTypeName);
            }
            Class<? extends Object> vectorType = types.get(vectorTypeName);
            Vector vector = new Vector();
            while (values.hasMoreTokens()) {
                vector.add(fromString(vectorType, values.nextToken()));
            }
            propertyData.value = vector;
        } else {
            if (!types.containsKey(propertyData.encodedType)) {
                throw new IllegalArgumentException ("Cannot determine type from value : " + propertyData.encodedType);
            }
            Class<? extends Object> valueType = types.get(propertyData.encodedType);
            propertyData.value = fromString(valueType, propertyData.encodedValue);
        }
        return propertyData;
    }


    public String getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }

    public String getEncodedType() {
        return encodedType;
    }

    public String getEncodedValue() {
        return encodedValue;
    }

    public boolean isEncodingPrimitive() {
        return primitiveTypes.containsKey(encodedType);
    }

}
