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
package org.apache.aries.blueprint;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.IOException;

import javax.xml.validation.Schema;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.aries.blueprint.parser.NamespaceHandlerSet;
import org.apache.aries.blueprint.reflect.BeanMetadataImpl;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.NullMetadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.xml.sax.SAXException;

/**
 * TODO: constructor injection
 * TODO: Dependency#setMethod 
 */
public class ParserTest extends AbstractBlueprintTest {

    public void test() {
        Integer[] oo = new Integer[1];
        Object[] ii = oo;
    }

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
        assertEquals("org.apache.aries.blueprint.pojos.PojoA", local.getClassName());
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
        assertNull(((ValueMetadata) param.getValue()).getType());
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
        assertNull(((ValueMetadata) param.getValue()).getType());
        param = params.get(4);
        assertNotNull(param);
        assertEquals(-1, param.getIndex());
        assertNull(param.getValueType());
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof CollectionMetadata);
        CollectionMetadata array = (CollectionMetadata) param.getValue();
        assertNull(array.getValueType());
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
        
        assertEquals(null, local.getInitMethod());
        assertEquals(null, local.getDestroyMethod());
                
        // test pojoB
        ComponentMetadata pojoB = registry.getComponentDefinition("pojoB");
        assertNotNull(pojoB);
        assertEquals("pojoB", pojoB.getId());
        assertTrue(pojoB instanceof BeanMetadata);
        BeanMetadata pojoBLocal = (BeanMetadata) pojoB;
        assertEquals("initPojo", pojoBLocal.getInitMethod());
//        assertEquals("", pojoBLocal.getDestroyMethod());
        
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
        ComponentDefinitionRegistry registry = parse("/test-custom-nodes.xml", new TestNamespaceHandlerSet());
        
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
        assertEquals("org.apache.aries.CacheProperty", innerComp.getClassName()); 
        
        metadata = registry.getComponentDefinition("myCache");
        assertNotNull(metadata);
        assertTrue(metadata instanceof BeanMetadata);
        BeanMetadata comp3 = (BeanMetadata) metadata;
        assertEquals("org.apache.aries.Cache", comp3.getClassName());         
    }
    
    public void testScopes() throws Exception {
        ComponentDefinitionRegistry registry = parse("/test-scopes.xml", new TestNamespaceHandlerSet());

        ComponentMetadata metadata = registry.getComponentDefinition("fooService");
        assertNotNull(metadata);
        assertTrue(metadata instanceof BeanMetadata);
        BeanMetadata bm = (BeanMetadata) metadata;
        assertNull(bm.getScope());
        
        metadata = registry.getComponentDefinition("barService");
        assertNotNull(metadata);
        assertTrue(metadata instanceof BeanMetadata);
        bm = (BeanMetadata) metadata;
        assertEquals("prototype", bm.getScope());
        
        metadata = registry.getComponentDefinition("bazService");
        assertNotNull(metadata);
        assertTrue(metadata instanceof BeanMetadata);
        bm = (BeanMetadata) metadata;
        assertEquals("singleton", bm.getScope());
        
        metadata = registry.getComponentDefinition("booService");
        assertNotNull(metadata);
        assertTrue(metadata instanceof BeanMetadata);
        bm = (BeanMetadata) metadata;
        assertEquals("{http://test.org}boo", bm.getScope());
    }

    private static class TestNamespaceHandlerSet implements NamespaceHandlerSet {
        private static final URI CACHE = URI.create("http://cache.org");

        private static final URI TEST = URI.create("http://test.org");

        private TestNamespaceHandlerSet() {
        }

        public Set<URI> getNamespaces() {
            Set<URI> namespaces = new HashSet<URI>();
            namespaces.add(CACHE);
            namespaces.add(TEST);
            
            return namespaces;
        }

        public boolean isComplete() {
            return true;
        }

        public NamespaceHandler getNamespaceHandler(URI namespace) {
            if (CACHE.equals(namespace)) {
                return new TestNamespaceHandler();
            } else if (TEST.equals(namespace)) {
                return new ScopeNamespaceHandler();
            } else {
                return null;
            }
        }

        public Schema getSchema() throws SAXException, IOException {
            return null;
        }

        public Schema getSchema(Map<String, String> locations) throws SAXException, IOException {
            return null;
        }

        public void addListener(NamespaceHandlerSet.Listener listener) {
        }

        public void removeListener(NamespaceHandlerSet.Listener listener) {
        }

        public void destroy() {
        }
    }
    
    private static class ScopeNamespaceHandler implements NamespaceHandler {

        public URL getSchemaLocation(String namespace) {
            // TODO Auto-generated method stub
            return null;
        }

        public Set<Class> getManagedClasses() {
            // TODO Auto-generated method stub
            return null;
        }

        public Metadata parse(Element element, ParserContext context) {
            // TODO Auto-generated method stub
            return null;
        }

        public ComponentMetadata decorate(Node node,
                ComponentMetadata component, ParserContext context) {
            return component;
        }
    }

    private static class TestNamespaceHandler implements NamespaceHandler {

        public URL getSchemaLocation(String namespace) {
            return getClass().getResource("/cache.xsd");
        }

        public Set<Class> getManagedClasses() {
            return new HashSet<Class>();
        }

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

        public Metadata parse(Element element, ParserContext context) {
            String comp = (context.getEnclosingComponent() == null) ? null : context.getEnclosingComponent().getId();
            //System.out.println("parse: " + element.getLocalName() + " " + comp);
            
            String className;
            if (context.getEnclosingComponent() == null) {
                className = "org.apache.aries.Cache";
            } else {
                className = "org.apache.aries.CacheProperty";
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
