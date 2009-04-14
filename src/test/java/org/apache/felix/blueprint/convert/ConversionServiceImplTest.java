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

import java.net.URI;
import java.net.URL;
import java.math.BigInteger;
import java.util.Locale;
import java.util.Properties;
import java.io.ByteArrayOutputStream;

import junit.framework.TestCase;
import org.osgi.service.blueprint.convert.ConversionService;
import org.osgi.service.blueprint.convert.Converter;

public class ConversionServiceImplTest extends TestCase {

    private ConversionService service;

    protected void setUp() {
        service = new ConversionServiceImpl();
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
        // TODO: check the following tests
//        assertEquals('\n', service.convert("\\n", char.class));
//        assertEquals('\n', service.convert("\\n", Character.class));
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
    
    public void testCustom() throws Exception {
        ConversionServiceImpl s = new ConversionServiceImpl();
        s.registerConverter(new RegionConverter());
        s.registerConverter(new EuRegionConverter());
        
        // lookup on a specific registered converter type
        Object result;
        result = s.convert(null, Region.class);
        assertTrue(result instanceof Region);
        assertFalse(result instanceof EuRegion);
                
        result = s.convert(null, EuRegion.class);
        assertTrue(result instanceof EuRegion);
        
        // find first converter that matches the type
        s = new ConversionServiceImpl();
        s.registerConverter(new AsianRegionConverter());
        s.registerConverter(new EuRegionConverter());
        
        result = s.convert(null, Region.class);
        assertTrue(result instanceof AsianRegion || result instanceof EuRegion);
    }
    
    private interface Region {} 
    
    private interface EuRegion extends Region {}
    
    private interface AsianRegion extends Region {}
    
    private static class RegionConverter implements Converter {
        public Object convert(Object source) throws Exception {
            return new Region() {} ;
        }
        public Class getTargetClass() {
            return Region.class;
        }       
    }
    
    private static class EuRegionConverter implements Converter {
        public Object convert(Object source) throws Exception {
            return new EuRegion() {} ;
        }
        public Class getTargetClass() {
            return EuRegion.class;
        }       
    }
    
    private static class AsianRegionConverter implements Converter {
        public Object convert(Object source) throws Exception {
            return new AsianRegion() {} ;
        }
        public Class getTargetClass() {
            return AsianRegion.class;
        }       
    }

}
