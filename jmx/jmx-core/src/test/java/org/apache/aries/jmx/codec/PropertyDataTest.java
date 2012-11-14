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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.osgi.jmx.JmxConstants.BIGINTEGER;
import static org.osgi.jmx.JmxConstants.BOOLEAN;
import static org.osgi.jmx.JmxConstants.CHARACTER;
import static org.osgi.jmx.JmxConstants.DOUBLE;
import static org.osgi.jmx.JmxConstants.INTEGER;
import static org.osgi.jmx.JmxConstants.KEY;
import static org.osgi.jmx.JmxConstants.PROPERTY_TYPE;
import static org.osgi.jmx.JmxConstants.P_BOOLEAN;
import static org.osgi.jmx.JmxConstants.P_CHAR;
import static org.osgi.jmx.JmxConstants.P_DOUBLE;
import static org.osgi.jmx.JmxConstants.P_INT;
import static org.osgi.jmx.JmxConstants.STRING;
import static org.osgi.jmx.JmxConstants.TYPE;
import static org.osgi.jmx.JmxConstants.VALUE;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;

import org.junit.Test;

/**
 *
 *
 * @version $Rev$ $Date$
 */
public class PropertyDataTest {


    @Test
    public void testToCompositeDataForPrimitiveTypes() throws Exception {

        PropertyData<Integer> intData = PropertyData.newInstance("test", 1);
        CompositeData intCData = intData.toCompositeData();
        assertEquals("test", intCData.get(KEY));
        assertEquals("1", intCData.get(VALUE));
        assertEquals(P_INT, intCData.get(TYPE));

        PropertyData<Double> doubleData = PropertyData.newInstance("test", 1.0);
        CompositeData doubleCData = doubleData.toCompositeData();
        assertEquals("test", doubleCData.get(KEY));
        assertEquals("1.0", doubleCData.get(VALUE));
        assertEquals(P_DOUBLE, doubleCData.get(TYPE));

        PropertyData<Character> charData = PropertyData.newInstance("test", 'c');
        CompositeData charCData = charData.toCompositeData();
        assertEquals("test", charCData.get(KEY));
        assertEquals("c", charCData.get(VALUE));
        assertEquals(P_CHAR, charCData.get(TYPE));

        PropertyData<Boolean> booleanData = PropertyData.newInstance("test", true);
        CompositeData booleanCData = booleanData.toCompositeData();
        assertEquals("test", booleanCData.get(KEY));
        assertEquals("true", booleanCData.get(VALUE));
        assertEquals(P_BOOLEAN, booleanCData.get(TYPE));
    }

    @Test
    public void testFromCompositeDataForPrimitiveTypes() throws Exception {

        Map<String, Object> items = new HashMap<String, Object>();
        items.put(KEY, "key");
        items.put(VALUE, "1");
        items.put(TYPE, P_INT);
        CompositeData compositeData = new CompositeDataSupport(PROPERTY_TYPE, items);

        PropertyData<Integer> intData = PropertyData.from(compositeData);
        assertEquals("key", intData.getKey());
        assertEquals(new Integer(1), intData.getValue());
        assertEquals(P_INT, intData.getEncodedType());
        assertTrue(intData.isEncodingPrimitive());

        items.clear();
        items.put(KEY, "key");
        items.put(VALUE, "1.0");
        items.put(TYPE, P_DOUBLE);
        compositeData = new CompositeDataSupport(PROPERTY_TYPE, items);

        PropertyData<Double> doubleData = PropertyData.from(compositeData);
        assertEquals("key", doubleData.getKey());
        assertEquals(Double.valueOf(1.0), doubleData.getValue());
        assertEquals(P_DOUBLE, doubleData.getEncodedType());
        assertTrue(doubleData.isEncodingPrimitive());

        items.clear();
        items.put(KEY, "key");
        items.put(VALUE, "a");
        items.put(TYPE, P_CHAR);
        compositeData = new CompositeDataSupport(PROPERTY_TYPE, items);

        PropertyData<Character> charData = PropertyData.from(compositeData);
        assertEquals("key", charData.getKey());
        assertEquals(Character.valueOf('a'), charData.getValue());
        assertEquals(P_CHAR, charData.getEncodedType());
        assertTrue(charData.isEncodingPrimitive());

        items.clear();
        items.put(KEY, "key");
        items.put(VALUE, "true");
        items.put(TYPE, P_BOOLEAN);
        compositeData = new CompositeDataSupport(PROPERTY_TYPE, items);

        PropertyData<Boolean> booleanData = PropertyData.from(compositeData);
        assertEquals("key", booleanData.getKey());
        assertTrue(booleanData.getValue());
        assertEquals(P_BOOLEAN, booleanData.getEncodedType());
        assertTrue(booleanData.isEncodingPrimitive());

    }

    @Test
    public void testToCompositeDataForWrapperTypes() {

        PropertyData<Integer> intData = PropertyData.newInstance("test", new Integer(1));
        CompositeData intCData = intData.toCompositeData();
        assertEquals("test", intCData.get(KEY));
        assertEquals("1", intCData.get(VALUE));
        assertEquals(INTEGER, intCData.get(TYPE));

        PropertyData<Double> doubleData = PropertyData.newInstance("test", new Double(1.0));
        CompositeData doubleCData = doubleData.toCompositeData();
        assertEquals("test", doubleCData.get(KEY));
        assertEquals("1.0", doubleCData.get(VALUE));
        assertEquals(DOUBLE, doubleCData.get(TYPE));

        PropertyData<Character> charData = PropertyData.newInstance("test", Character.valueOf('c'));
        CompositeData charCData = charData.toCompositeData();
        assertEquals("test", charCData.get(KEY));
        assertEquals("c", charCData.get(VALUE));
        assertEquals(CHARACTER, charCData.get(TYPE));

        PropertyData<Boolean> booleanData = PropertyData.newInstance("test", Boolean.TRUE);
        CompositeData booleanCData = booleanData.toCompositeData();
        assertEquals("test", booleanCData.get(KEY));
        assertEquals("true", booleanCData.get(VALUE));
        assertEquals(BOOLEAN, booleanCData.get(TYPE));

    }

    @Test
    public void testFromCompositeDataForWrapperTypes() throws Exception {

        Map<String, Object> items = new HashMap<String, Object>();
        items.put(KEY, "key");
        items.put(VALUE, "1");
        items.put(TYPE, INTEGER);
        CompositeData compositeData = new CompositeDataSupport(PROPERTY_TYPE, items);

        PropertyData<Integer> intData = PropertyData.from(compositeData);
        assertEquals("key", intData.getKey());
        assertEquals(new Integer(1), intData.getValue());
        assertEquals(INTEGER, intData.getEncodedType());
        assertFalse(intData.isEncodingPrimitive());

        items.clear();
        items.put(KEY, "key");
        items.put(VALUE, "1.0");
        items.put(TYPE, DOUBLE);
        compositeData = new CompositeDataSupport(PROPERTY_TYPE, items);

        PropertyData<Double> doubleData = PropertyData.from(compositeData);
        assertEquals("key", doubleData.getKey());
        assertEquals(Double.valueOf(1.0), doubleData.getValue());
        assertEquals(DOUBLE, doubleData.getEncodedType());
        assertFalse(doubleData.isEncodingPrimitive());

        items.clear();
        items.put(KEY, "key");
        items.put(VALUE, "a");
        items.put(TYPE, CHARACTER);
        compositeData = new CompositeDataSupport(PROPERTY_TYPE, items);

        PropertyData<Character> charData = PropertyData.from(compositeData);
        assertEquals("key", charData.getKey());
        assertEquals(Character.valueOf('a'), charData.getValue());
        assertEquals(CHARACTER, charData.getEncodedType());
        assertFalse(charData.isEncodingPrimitive());

        items.clear();
        items.put(KEY, "key");
        items.put(VALUE, "true");
        items.put(TYPE, BOOLEAN);
        compositeData = new CompositeDataSupport(PROPERTY_TYPE, items);

        PropertyData<Boolean> booleanData = PropertyData.from(compositeData);
        assertEquals("key", booleanData.getKey());
        assertTrue(booleanData.getValue());
        assertEquals(BOOLEAN, booleanData.getEncodedType());
        assertFalse(booleanData.isEncodingPrimitive());

    }

    @Test
    public void testToFromCompositeDataForAdditionalTypes() {

        PropertyData<String> stringData = PropertyData.newInstance("test", "value");

        CompositeData stringCData = stringData.toCompositeData();
        assertEquals("test", stringCData.get(KEY));
        assertEquals("value", stringCData.get(VALUE));
        assertEquals(STRING, stringCData.get(TYPE));

        stringData = PropertyData.from(stringCData);
        assertEquals("test", stringData.getKey());
        assertEquals("value", stringData.getValue());
        assertEquals(STRING, stringData.getEncodedType());

        PropertyData<BigInteger> bigIntData = PropertyData.newInstance("test", new BigInteger("1"));

        CompositeData bigIntCData = bigIntData.toCompositeData();
        assertEquals("test", bigIntCData.get(KEY));
        assertEquals("1", bigIntCData.get(VALUE));
        assertEquals(BIGINTEGER, bigIntCData.get(TYPE));

        bigIntData = PropertyData.from(bigIntCData);
        assertEquals("test", bigIntData.getKey());
        assertEquals(new BigInteger("1"), bigIntData.getValue());
        assertEquals(BIGINTEGER, bigIntData.getEncodedType());

    }

    @Test
    public void testToFromCompositeDataForArrayTypes() {

        //long[]
        long[] primitiveLongValues = new long[] { 1, 2, 3 };
        PropertyData<long[]> primitiveLongArrayData = PropertyData.newInstance("test", primitiveLongValues);
        CompositeData primitiveLongArrayCData = primitiveLongArrayData.toCompositeData();
        assertEquals("test", primitiveLongArrayCData.get(KEY));
        assertEquals("1,2,3", primitiveLongArrayCData.get(VALUE));
        assertEquals("Array of long", primitiveLongArrayCData.get(TYPE));
        primitiveLongArrayData = PropertyData.from(primitiveLongArrayCData);
        assertEquals("test", primitiveLongArrayData.getKey());
        assertEquals("Array of long", primitiveLongArrayData.getEncodedType());
        assertArrayEquals(primitiveLongValues, primitiveLongArrayData.getValue());

        //Long[]
        Long[] longValues = new Long[] { new Long(4), new Long(5), new Long(6) };
        PropertyData<Long[]> longArrayData = PropertyData.newInstance("test", longValues);
        CompositeData longArrayCData = longArrayData.toCompositeData();
        assertEquals("test", longArrayCData.get(KEY));
        assertEquals("4,5,6", longArrayCData.get(VALUE));
        assertEquals("Array of Long", longArrayCData.get(TYPE));
        longArrayData = PropertyData.from(longArrayCData);
        assertEquals("test", longArrayData.getKey());
        assertEquals("Array of Long", longArrayData.getEncodedType());
        assertArrayEquals(longValues, longArrayData.getValue());

        //char[]
        char[] primitiveCharValues = new char[] { 'a', 'b', 'c' };
        PropertyData<char[]> primitiveCharArrayData = PropertyData.newInstance("test", primitiveCharValues);
        CompositeData primitiveCharArrayCData = primitiveCharArrayData.toCompositeData();
        assertEquals("test", primitiveCharArrayCData.get(KEY));
        assertEquals("a,b,c", primitiveCharArrayCData.get(VALUE));
        assertEquals("Array of char", primitiveCharArrayCData.get(TYPE));
        primitiveCharArrayData = PropertyData.from(primitiveCharArrayCData);
        assertEquals("test", primitiveCharArrayData.getKey());
        assertEquals("Array of char", primitiveCharArrayData.getEncodedType());
        assertArrayEquals(primitiveCharValues, primitiveCharArrayData.getValue());

        //Character[]
        Character[] charValues = new Character[] { 'a', 'b', 'c' };
        PropertyData<Character[]> charArrayData = PropertyData.newInstance("test", charValues);
        CompositeData charArrayCData = charArrayData.toCompositeData();
        assertEquals("test", charArrayCData.get(KEY));
        assertEquals("a,b,c", charArrayCData.get(VALUE));
        assertEquals("Array of Character", charArrayCData.get(TYPE));
        charArrayData = PropertyData.from(charArrayCData);
        assertEquals("test", charArrayData.getKey());
        assertEquals("Array of Character", charArrayData.getEncodedType());
        assertArrayEquals(charValues, charArrayData.getValue());

    }

    @Test
    public void testToFromCompositeDataForVector() {

        Vector<Long> vector = new Vector<Long>();
        vector.add(new Long(40));
        vector.add(new Long(50));
        vector.add(new Long(60));

        PropertyData<Vector<Long>> vectorPropertyData = PropertyData.newInstance("test", vector);
        CompositeData vectorCompositeData = vectorPropertyData.toCompositeData();

        assertEquals("test", vectorCompositeData.get(KEY));
        assertEquals("40,50,60", vectorCompositeData.get(VALUE));
        assertEquals("Vector of Long", vectorCompositeData.get(TYPE));

        vectorPropertyData = PropertyData.from(vectorCompositeData);
        assertEquals("test", vectorPropertyData.getKey());
        assertEquals("Vector of Long", vectorPropertyData.getEncodedType());
        assertArrayEquals(vector.toArray(new Long[vector.size()]), vectorPropertyData.getValue().toArray(new Long[vector.size()]));

    }

    @Test
    public void testToFromCompositeDataForList() {
        List<String> sl = new ArrayList<String>();
        sl.add("A");
        sl.add("B");

        PropertyData<List<String>> pd = PropertyData.newInstance("test", sl);
        CompositeData cd = pd.toCompositeData();

        assertEquals("test", cd.get(KEY));
        assertEquals("A,B", cd.get(VALUE));
        assertEquals("Array of String", cd.get(TYPE));

        PropertyData<String []> pd2 = PropertyData.from(cd);
        assertEquals("test", pd2.getKey());
        assertEquals("Array of String", pd2.getEncodedType());
        assertArrayEquals(new String [] {"A", "B"}, pd2.getValue());
    }

    @Test
    public void testToFromCompositeDataForList2() {
        List<Long> sl = new ArrayList<Long>();
        sl.add(Long.MAX_VALUE);

        PropertyData<List<Long>> pd = PropertyData.newInstance("test", sl);
        CompositeData cd = pd.toCompositeData();

        assertEquals("test", cd.get(KEY));
        assertEquals(new Long(Long.MAX_VALUE).toString(), cd.get(VALUE));
        assertEquals("Array of Long", cd.get(TYPE));

        PropertyData<Long []> pd2 = PropertyData.from(cd);
        assertEquals("test", pd2.getKey());
        assertEquals("Array of Long", pd2.getEncodedType());
        assertArrayEquals(new Long [] {Long.MAX_VALUE}, pd2.getValue());
    }
}
