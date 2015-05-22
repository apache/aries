/**
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
package org.apache.aries.blueprint.plugin.model;

import java.lang.reflect.Field;

import javax.xml.stream.XMLStreamException;

import org.apache.aries.blueprint.plugin.model.Bean;
import org.apache.aries.blueprint.plugin.model.Matcher;
import org.apache.aries.blueprint.plugin.model.Property;
import org.apache.aries.blueprint.plugin.test.ServiceAImpl1;
import org.junit.Assert;
import org.junit.Test;

public class PropertyTest {
    @Test
    public void testRefInject() throws XMLStreamException {
        Field field = TestBeanForRef.class.getDeclaredFields()[0];
        Matcher matcher = new Matcher() {
            public Bean getMatching(BeanRef template) {
                return new Bean(ServiceAImpl1.class);
            }
        };
        Property property = Property.create(matcher, field);
        Assert.assertEquals("serviceA", property.name);
        Assert.assertNull("Value should be null", property.value);
        Assert.assertEquals("my1", property.ref);
    }
    
    @Test
    public void testRefAutowired() throws XMLStreamException {
        Field field = TestBeanForRef.class.getDeclaredFields()[1];
        Matcher matcher = new Matcher() {
            public Bean getMatching(BeanRef template) {
                return null;
            }
        };
        Property property = Property.create(matcher, field);
        Assert.assertEquals("serviceB", property.name);
        Assert.assertNull("Value should be null", property.value);
        Assert.assertEquals("Should be default name as no match is found", "serviceB", property.ref);
    }

    @Test
    public void testValue() throws XMLStreamException {
        Field field = TestBeanForRef.class.getDeclaredFields()[2];
        Property property = Property.create(null, field);
        Assert.assertEquals("name", property.name);
        Assert.assertEquals("${name}", property.value);
        Assert.assertNull("Ref should be null", property.ref);
    }
    
    @Test
    public void testNoProperty() throws XMLStreamException {
        Field field = TestBeanForRef.class.getDeclaredFields()[3];
        Property property = Property.create(null, field);
        Assert.assertNull("Should not be a property", property);
    }
}
