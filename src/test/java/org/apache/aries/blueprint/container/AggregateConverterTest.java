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
package org.apache.aries.blueprint.container;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import junit.framework.TestCase;
import org.apache.aries.blueprint.TestBlueprintContainer;
import org.osgi.service.blueprint.container.ReifiedType;
import org.osgi.service.blueprint.container.Converter;

public class AggregateConverterTest extends TestCase {

    private AggregateConverter service;

    protected void setUp() throws Exception {
        service = new AggregateConverter(new TestBlueprintContainer(null));
    }

    public void testConvertNumbers() throws Exception {
        assertEquals(1, service.convert(1.46f, int.class));
        assertEquals(1.0d, service.convert(1, double.class));
    }

    public void testConvertSimpleTypes() throws Exception {
        assertEquals(123, service.convert("123", int.class));
        assertEquals(123, service.convert("123", Integer.class));
        assertEquals(123l, service.convert("123", long.class));
        assertEquals(123l, service.convert("123", Long.class));
        assertEquals((short) 123, service.convert("123", short.class));
        assertEquals((short) 123, service.convert("123", Short.class));
        assertEquals(1.5f, service.convert("1.5", float.class));
        assertEquals(1.5f, service.convert("1.5", Float.class));
        assertEquals(1.5, service.convert("1.5", double.class));
        assertEquals(1.5, service.convert("1.5", Double.class));
    }

    public void testConvertCharacter() throws Exception {
        assertEquals('c', service.convert("c", char.class));
        assertEquals('c', service.convert("c", Character.class));
        assertEquals('\u00F6', service.convert("\\u00F6", char.class));
        assertEquals('\u00F6', service.convert("\\u00F6", Character.class));
    }

    public void testConvertBoolean() throws Exception {
        assertEquals(Boolean.TRUE, service.convert("true", Boolean.class));
        assertEquals(Boolean.TRUE, service.convert("yes", Boolean.class));
        assertEquals(Boolean.TRUE, service.convert("on", Boolean.class));
        assertEquals(Boolean.TRUE, service.convert("TRUE", Boolean.class));
        assertEquals(Boolean.TRUE, service.convert("YES", Boolean.class));
        assertEquals(Boolean.TRUE, service.convert("ON", Boolean.class));
        assertEquals(Boolean.TRUE, service.convert("true", boolean.class));
        assertEquals(Boolean.TRUE, service.convert("yes", boolean.class));
        assertEquals(Boolean.TRUE, service.convert("on", boolean.class));
        assertEquals(Boolean.TRUE, service.convert("TRUE", boolean.class));
        assertEquals(Boolean.TRUE, service.convert("YES", boolean.class));
        assertEquals(Boolean.TRUE, service.convert("ON", boolean.class));
        assertEquals(Boolean.FALSE, service.convert("false", Boolean.class));
        assertEquals(Boolean.FALSE, service.convert("no", Boolean.class));
        assertEquals(Boolean.FALSE, service.convert("off", Boolean.class));
        assertEquals(Boolean.FALSE, service.convert("FALSE", Boolean.class));
        assertEquals(Boolean.FALSE, service.convert("NO", Boolean.class));
        assertEquals(Boolean.FALSE, service.convert("OFF", Boolean.class));
        assertEquals(Boolean.FALSE, service.convert("false", boolean.class));
        assertEquals(Boolean.FALSE, service.convert("no", boolean.class));
        assertEquals(Boolean.FALSE, service.convert("off", boolean.class));
        assertEquals(Boolean.FALSE, service.convert("FALSE", boolean.class));
        assertEquals(Boolean.FALSE, service.convert("NO", boolean.class));
        assertEquals(Boolean.FALSE, service.convert("OFF", boolean.class));
        
        assertEquals(Boolean.FALSE, service.convert(false, boolean.class));
        assertEquals(Boolean.TRUE, service.convert(true, boolean.class));        
        assertEquals(Boolean.FALSE, service.convert(false, Boolean.class));
        assertEquals(Boolean.TRUE, service.convert(true, Boolean.class));
    }

    public void testConvertOther() throws Exception {
        assertEquals(URI.create("urn:test"), service.convert("urn:test", URI.class));
        assertEquals(new URL("file:/test"), service.convert("file:/test", URL.class));
        assertEquals(new BigInteger("12345"), service.convert("12345", BigInteger.class));
    }

    public void testConvertProperties() throws Exception {
        Properties props = new Properties();
        props.setProperty("key", "value");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.store(baos, null);
        props = (Properties) service.convert(baos.toString(), Properties.class);
        assertEquals(1, props.size());
        assertEquals("value", props.getProperty("key"));
    }

    public void testConvertLocale() throws Exception {
        Object result;
        result = service.convert("en", Locale.class);
        assertTrue(result instanceof Locale);
        assertEquals(new Locale("en"), result);
        
        result = service.convert("de_DE", Locale.class);
        assertTrue(result instanceof Locale);
        assertEquals(new Locale("de", "DE"), result);
        
        result = service.convert("_GB", Locale.class);
        assertTrue(result instanceof Locale);
        assertEquals(new Locale("", "GB"), result);
        
        result = service.convert("en_US_WIN", Locale.class);
        assertTrue(result instanceof Locale);
        assertEquals(new Locale("en", "US", "WIN"), result);
        
        result = service.convert("de__POSIX", Locale.class);
        assertTrue(result instanceof Locale);
        assertEquals(new Locale("de", "", "POSIX"), result);
    }
    
    public void testConvertClass() throws Exception {
        assertEquals(this, service.convert(this, AggregateConverterTest.class));
        assertEquals(AggregateConverterTest.class, service.convert(this.getClass().getName(), Class.class));
        assertEquals(int[].class, service.convert("int[]", Class.class));
    }

    public void testConvertArray() throws Exception {
        Object obj = service.convert(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)),
                                     GenericType.parse("java.util.List<java.lang.Integer>[]", getClass().getClassLoader()));
        assertNotNull(obj);
        assertTrue(obj.getClass().isArray());
        Object[] o = (Object[]) obj;
        assertEquals(2, o.length);
        assertNotNull(o[0]);
        assertTrue(o[0] instanceof List);
        assertEquals(2, ((List) o[0]).size());
        assertEquals(1, ((List) o[0]).get(0));
        assertEquals(2, ((List) o[0]).get(1));
        assertNotNull(o[0]);
        assertTrue(o[1] instanceof List);
        assertEquals(2, ((List) o[1]).size());
        assertEquals(3, ((List) o[1]).get(0));
        assertEquals(4, ((List) o[1]).get(1));
        //assertEquals((Object) new int[] { 1, 2 }, (Object) service.convert(Arrays.asList(1, 2), int[].class));
    }
    
    public void testCustom() throws Exception {
        AggregateConverter s = new AggregateConverter(new TestBlueprintContainer(null));
        s.registerConverter(new RegionConverter());
        s.registerConverter(new EuRegionConverter());
        
        // lookup on a specific registered converter type
        Object result;
        result = s.convert(new Object(), Region.class);
        assertTrue(result instanceof Region);
        assertFalse(result instanceof EuRegion);
                
        result = s.convert(new Object(), EuRegion.class);
        assertTrue(result instanceof EuRegion);
        
        // find first converter that matches the type
        s = new AggregateConverter(new TestBlueprintContainer(null));
        s.registerConverter(new AsianRegionConverter());
        s.registerConverter(new EuRegionConverter());
        s.registerConverter(new NullMarkerConverter());
        
        result = s.convert(new Object(), Region.class);
        // TODO: check with the spec about the result
        //assertTrue(result instanceof AsianRegion || result instanceof EuRegion);
        result = s.convert(new Object(), NullMarker.class);
        assertNull(result);
    }

    public void testGenericAssignable() throws Exception {
        AggregateConverter s = new AggregateConverter(new TestBlueprintContainer(null));

        assertNotNull(s.convert(new RegionIterable(), new GenericType(Iterable.class, new GenericType(Region.class))));

        try {
            s.convert(new ArrayList<Region>(), new GenericType(Iterable.class, new GenericType(Region.class)));
            fail("Conversion should have thrown an exception");
        } catch (Exception e) {
            // Ignore
        }

    }

    public void testGenericCollection() throws Exception {
        AggregateConverter s = new AggregateConverter(new TestBlueprintContainer(null));

        try {
            s.convert(new ArrayList(), new GenericType(Iterable.class, new GenericType(Region.class)));
            fail("Conversion should have thrown an exception");
        } catch (Exception e) {
            // Ignore
        }

        try {
            s.convert(Arrays.asList(0l), new GenericType(Iterable.class, new GenericType(Region.class)));
            fail("Conversion should have thrown an exception");
        } catch (Exception e) {
            // Ignore
        }

        assertNotNull(s.convert(Arrays.asList(new EuRegion() {}), new GenericType(List.class, new GenericType(Region.class))));
    }
    
    private interface Region {} 
    
    private interface EuRegion extends Region {}
    
    private interface AsianRegion extends Region {}

    private interface NullMarker {}
    
    private static class RegionConverter implements Converter {
        public boolean canConvert(Object fromValue, ReifiedType toType) {
            return Region.class == toType.getRawClass();
        }
        public Object convert(Object source, ReifiedType toType) throws Exception {
            return new Region() {} ;
        }
    }
    
    private static class EuRegionConverter implements Converter {
        public boolean canConvert(Object fromValue, ReifiedType toType) {
            return toType.getRawClass().isAssignableFrom(EuRegion.class);
        }
        public Object convert(Object source, ReifiedType toType) throws Exception {
            return new EuRegion() {} ;
        }
    }
    
    private static class AsianRegionConverter implements Converter {
        public boolean canConvert(Object fromValue, ReifiedType toType) {
            return toType.getRawClass().isAssignableFrom(AsianRegion.class);
        }
        public Object convert(Object source, ReifiedType toType) throws Exception {
            return new AsianRegion() {} ;
        }
    }

    private static class NullMarkerConverter implements Converter {
        public boolean canConvert(Object fromValue, ReifiedType toType) {
            return toType.getRawClass().isAssignableFrom(NullMarker.class);
        }
        public Object convert(Object source, ReifiedType toType) throws Exception {
            return null;
        }
    }

    private static class RegionIterable implements Iterable<Region> {
        public Iterator<Region> iterator() {
            return null;
        }
    }

}
