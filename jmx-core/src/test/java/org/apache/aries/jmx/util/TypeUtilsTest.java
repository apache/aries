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

import static org.apache.aries.jmx.util.TypeUtils.fromDictionary;
import static org.apache.aries.jmx.util.TypeUtils.fromString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.junit.Test;

public class TypeUtilsTest {

    
    @Test
    public void testMapFromDictionary() throws Exception{
        
        Dictionary<String, String> dictionary = new Hashtable<String, String>();
        dictionary.put("one", "1");
        dictionary.put("two", "2");
        
        Map<String,String> map = fromDictionary(dictionary);
        assertEquals(2, map.size());
        assertEquals("1", map.get("one"));
        assertEquals("2", map.get("two"));
        
    }
    
    @Test
    public void testFromString() throws Exception {
        
        String value;
        
        value = "1";
        Integer integerValue = fromString(Integer.class, value);
        assertEquals(new Integer(1), integerValue);
        
        int intValue = fromString(Integer.TYPE, value);
        assertEquals(1, intValue);
        
        Long wrappedLongValue = fromString(Long.class, value);
        assertEquals(Long.valueOf(1), wrappedLongValue);
        
        long longValue = fromString(Long.TYPE, value);
        assertEquals(1, longValue);
        
        Double wrappedDoubleValue = fromString(Double.class, value);
        assertEquals(Double.valueOf(1), wrappedDoubleValue);
        
        double doubleValue = fromString(Double.TYPE, value);
        assertEquals(1, doubleValue, 0);
        
        Float wrappedFloatValue = fromString(Float.class, value);
        assertEquals(Float.valueOf(1), wrappedFloatValue);
        
        float floatValue = fromString(Float.TYPE, value);
        assertEquals(1, floatValue, 0);
        
        Short shortValue = fromString(Short.class, value);
        assertEquals(Short.valueOf(value), shortValue);
        
        Byte byteValue = fromString(Byte.class, value);
        assertEquals(Byte.valueOf(value), byteValue);
        
        value = "true";
        assertTrue(fromString(Boolean.class, value));
        assertTrue(fromString(Boolean.TYPE, value));
        
        char charValue = fromString(Character.TYPE, "a");
        assertEquals('a', charValue);
        Character characterValue = fromString(Character.class, "a");
        assertEquals(Character.valueOf('a'), characterValue);
        
        BigDecimal bigDecimal = fromString(BigDecimal.class, "2");
        assertEquals(new BigDecimal("2"), bigDecimal);
     
        BigInteger bigInteger = fromString(BigInteger.class, "2");
        assertEquals(new BigInteger("2"), bigInteger);
        
        String stringValue = fromString(String.class, value);
        assertEquals(stringValue, value);
        
    }
}
