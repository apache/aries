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
package org.apache.geronimo.blueprint;

import java.net.URI;
import java.net.URL;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.geronimo.blueprint.reflect.BeanMetadataImpl;
import org.apache.geronimo.blueprint.ComponentDefinitionRegistry;
import org.apache.geronimo.blueprint.NamespaceHandler;
import org.apache.geronimo.blueprint.ParserContext;
import org.apache.geronimo.blueprint.container.NamespaceHandlerRegistry;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.NullMetadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;

/**
 * TODO: constructor injection
 * TODO: Dependency#setMethod 
 */
public class ParserTest extends AbstractBlueprintTest {

    public void testParseComponent() throws Exception {
        ComponentDefinitionRegistry registry = parse("/test-simple-component.xml");
        assertNotNull(registry);
        ComponentMetadata component = registry.getComponentDefinition("pojoA");
        assertNotNull(component);
        assertEquals("pojoA", component.getId());
        assertTrue(component instanceof BeanMetadata);
        BeanMetadata local = (BeanMetadata) component;
        List<String> deps = local.getDependsOn();
        assertNotNull(deps);
        assertEquals(2, deps.size());
        assertTrue(deps.contains("pojoB"));
        assertTrue(deps.contains("pojoC"));
        assertEquals("org.apache.geronimo.blueprint.pojos.PojoA", local.getClassName());
        List<BeanArgument> params = local.getArguments();
        assertNotNull(params);
        assertEquals(6, params.size());
        BeanArgument param = params.get(0);
        assertNotNull(param);
        assertEquals(-1, param.getIndex());
        assertNull(param.getValueType());
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof ValueMetadata);
        assertEquals("val0", ((ValueMetadata) param.getValue()).getStringValue());
        assertNull(((ValueMetadata) param.getValue()).getTypeName());
        param = params.get(1);
        assertNotNull(param);
        assertEquals(-1, param.getIndex());
        assertNull(param.getValueType());
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof RefMetadata);
        assertEquals("val1", ((RefMetadata) param.getValue()).getComponentId());
        param = params.get(2);
        assertNotNull(param);
        assertEquals(-1, param.getIndex());
        assertNull(param.getValueType());
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof NullMetadata);
        param = params.get(3);
        assertNotNull(param);
        assertEquals(-1, param.getIndex());
        assertEquals("java.lang.String", param.getValueType());
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof ValueMetadata);
        assertEquals("val3", ((ValueMetadata) param.getValue()).getStringValue());
        assertNull(((ValueMetadata) param.getValue()).getTypeName());
        param = params.get(4);
        assertNotNull(param);
        assertEquals(-1, param.getIndex());
        assertNull(param.getValueType());
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof CollectionMetadata);
        CollectionMetadata array = (CollectionMetadata) param.getValue();
        assertNull(array.getValueTypeName());
        assertNotNull(array.getValues());
        assertEquals(3, array.getValues().size());
        assertTrue(array.getValues().get(0) instanceof ValueMetadata);
        assertTrue(array.getValues().get(1) instanceof ComponentMetadata);
        assertTrue(array.getValues().get(2) instanceof NullMetadata);
        param = params.get(5);
        assertNotNull(param);
        assertEquals(-1, param.getIndex());
        assertNull(param.getValueType());
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof RefMetadata);
        assertEquals("pojoB", ((RefMetadata) param.getValue()).getComponentId());
        
        assertEquals(null, local.getInitMethodName());
        assertEquals(null, local.getDestroyMethodName());
                
        // test pojoB
        ComponentMetadata pojoB = registry.getComponentDefinition("pojoB");
        assertNotNull(pojoB);
        assertEquals("pojoB", pojoB.getId());
        assertTrue(pojoB instanceof BeanMetadata);
        BeanMetadata pojoBLocal = (BeanMetadata) pojoB;
        assertEquals("initPojo", pojoBLocal.getInitMethodName());
//        assertEquals("", pojoBLocal.getDestroyMethodName());
        
        params = pojoBLocal.getArguments();
        assertNotNull(params);
        assertEquals(2, params.size());
        param = params.get(0);
        assertNotNull(param);
        assertEquals(1, param.getIndex());
        param = params.get(1);
        assertNotNull(param);
        assertEquals(0, param.getIndex());
    }

    public void testParse() throws Exception {
        parse("/test.xml");
    }


    public void testCustomNodes() throws Exception {
        ComponentDefinitionRegistry registry = parse("/test-custom-nodes.xml", new TestNamespaceHandlerRegistry());
        
        ComponentMetadata metadata;
        
        metadata = registry.getComponentDefinition("fooService");
        assertNotNull(metadata);
        assertTrue(metadata instanceof MyLocalComponentMetadata);
        MyLocalComponentMetadata comp1 = (MyLocalComponentMetadata) metadata;
        assertEquals(true, comp1.getCacheReturnValues());
        assertEquals("getVolatile", comp1.getOperation());
        
        metadata = registry.getComponentDefinition("barService");
        assertNotNull(metadata);
        assertTrue(metadata instanceof BeanMetadata);
        BeanMetadata comp2 = (BeanMetadata) metadata;
        assertEquals(1, comp2.getProperties().size());
        BeanProperty propertyMetadata = comp2.getProperties().get(0);
        assertEquals("localCache", propertyMetadata.getName());
        Metadata propertyValue = propertyMetadata.getValue();
        assertTrue(propertyValue instanceof BeanMetadata);
        BeanMetadata innerComp = (BeanMetadata) propertyValue;
        assertEquals("org.apache.geronimo.CacheProperty", innerComp.getClassName()); 
        
        metadata = registry.getComponentDefinition("myCache");
        assertNotNull(metadata);
        assertTrue(metadata instanceof BeanMetadata);
        BeanMetadata comp3 = (BeanMetadata) metadata;
        assertEquals("org.apache.geronimo.Cache", comp3.getClassName());         
    }

    private static class TestNamespaceHandlerRegistry implements NamespaceHandlerRegistry {
        
        public void destroy() {
        }
        
        public NamespaceHandler getNamespaceHandler(URI uri) {
            URI u = URI.create("http://cache.org");
            if (u.equals(uri)) {
                return new TestNamespaceHandler();
            } else {
                return null;
            }        
        }

        public void addListener(Listener listener) {
        }

        public void removeListener(Listener listener) {
        }
    }
    
    private static class TestNamespaceHandler implements NamespaceHandler {

        public ComponentMetadata decorate(Node node,
                                          ComponentMetadata component,
                                          ParserContext context) {
            //System.out.println("decorate: " + node + " " + component + " " + container.getEnclosingComponent().getId());
            
            if (node instanceof Attr) {
                Attr attr = (Attr) node;
                MyLocalComponentMetadata decoratedComp = new MyLocalComponentMetadata((BeanMetadata)component);
                decoratedComp.setCacheReturnValues(Boolean.parseBoolean(attr.getValue()));
                return decoratedComp;
            } else if (node instanceof Element) {
                Element element = (Element) node;                
                MyLocalComponentMetadata decoratedComp = (MyLocalComponentMetadata) component;
                decoratedComp.setOperation(element.getAttribute("name"));
                return decoratedComp;
            } else {
                throw new RuntimeException("Unhandled node: " + node);
            }
        }

        public URL getSchemaLocation(String namespace) {
            return getClass().getResource("/cache.xsd");
        }

        public Metadata parse(Element element, ParserContext context) {
            String comp = (context.getEnclosingComponent() == null) ? null : context.getEnclosingComponent().getId();
            //System.out.println("parse: " + element.getLocalName() + " " + comp);
            
            String className;
            if (context.getEnclosingComponent() == null) {
                className = "org.apache.geronimo.Cache";
            } else {
                className = "org.apache.geronimo.CacheProperty";
            }
                        
            BeanMetadataImpl p = new BeanMetadataImpl();
            p.setId(element.getAttribute("id"));
            p.setClassName(className);
            
            return p;
        }
        
    }
    
    private static class MyLocalComponentMetadata extends BeanMetadataImpl {
        
        private boolean cacheReturnValues;
        private String operation;
        
        public MyLocalComponentMetadata(BeanMetadata impl) {
            super(impl);
        }
        
        public boolean getCacheReturnValues() {
            return cacheReturnValues;
        }
        
        public void setCacheReturnValues(boolean value) {
            cacheReturnValues = value;
        }
        
        public void setOperation(String operation) {
            this.operation = operation;
        }
        
        public String getOperation() {
            return this.operation;
        }
    }

}
