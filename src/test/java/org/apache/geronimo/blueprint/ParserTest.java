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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import junit.framework.TestCase;
import org.apache.geronimo.blueprint.context.Parser;
import org.apache.geronimo.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.apache.geronimo.blueprint.reflect.LocalComponentMetadataImpl;
import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;
import org.osgi.service.blueprint.namespace.NamespaceHandler;
import org.osgi.service.blueprint.namespace.ParserContext;
import org.osgi.service.blueprint.reflect.ArrayValue;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.ComponentValue;
import org.osgi.service.blueprint.reflect.ConstructorInjectionMetadata;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;
import org.osgi.service.blueprint.reflect.NullValue;
import org.osgi.service.blueprint.reflect.ParameterSpecification;
import org.osgi.service.blueprint.reflect.PropertyInjectionMetadata;
import org.osgi.service.blueprint.reflect.ReferenceValue;
import org.osgi.service.blueprint.reflect.TypedStringValue;
import org.osgi.service.blueprint.reflect.Value;

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
        assertEquals("pojoA", component.getName());
        Set<String> deps = component.getExplicitDependencies();
        assertNotNull(deps);
        assertEquals(2, deps.size());
        assertTrue(deps.contains("pojoB"));
        assertTrue(deps.contains("pojoC"));
        assertTrue(component instanceof LocalComponentMetadata);
        LocalComponentMetadata local = (LocalComponentMetadata) component;
        assertEquals("org.apache.geronimo.blueprint.pojos.PojoA", local.getClassName());
        ConstructorInjectionMetadata cns = local.getConstructorInjectionMetadata();
        assertNotNull(cns);
        List<ParameterSpecification> params = cns.getParameterSpecifications();
        assertNotNull(params);
        assertEquals(6, params.size());
        ParameterSpecification param = params.get(0);
        assertNotNull(param);
        assertEquals(0, param.getIndex());
        assertNull(param.getTypeName());
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof TypedStringValue);
        assertEquals("val0", ((TypedStringValue) param.getValue()).getStringValue());
        assertNull(((TypedStringValue) param.getValue()).getTypeName());
        param = params.get(1);
        assertNotNull(param);
        assertEquals(2, param.getIndex());
        assertNull(param.getTypeName());
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof ReferenceValue);
        assertEquals("val1", ((ReferenceValue) param.getValue()).getComponentName());
        param = params.get(2);
        assertNotNull(param);
        assertEquals(1, param.getIndex());
        assertNull(param.getTypeName());
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof NullValue);
        param = params.get(3);
        assertNotNull(param);
        assertEquals(3, param.getIndex());
        assertEquals("java.lang.String", param.getTypeName());
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof TypedStringValue);
        assertEquals("val3", ((TypedStringValue) param.getValue()).getStringValue());
        assertNull(((TypedStringValue) param.getValue()).getTypeName());
        param = params.get(4);
        assertNotNull(param);
        assertEquals(4, param.getIndex());
        assertNull(param.getTypeName());
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof ArrayValue);
        ArrayValue array = (ArrayValue) param.getValue();
        assertNull(array.getValueType());
        assertNotNull(array.getArray());
        assertEquals(3, array.getArray().length);
        assertTrue(array.getArray()[0] instanceof TypedStringValue);
        assertTrue(array.getArray()[1] instanceof ComponentValue);
        assertTrue(array.getArray()[2] instanceof NullValue);
        param = params.get(5);
        assertNotNull(param);
        assertEquals(5, param.getIndex());
        assertNull(param.getTypeName());
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof ReferenceValue);
        assertEquals("pojoB", ((ReferenceValue) param.getValue()).getComponentName());
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
        assertTrue(metadata instanceof LocalComponentMetadata);
        LocalComponentMetadata comp2 = (LocalComponentMetadata) metadata;
        assertEquals(1, comp2.getPropertyInjectionMetadata().size());
        PropertyInjectionMetadata propertyMetadata = (PropertyInjectionMetadata)comp2.getPropertyInjectionMetadata().iterator().next();
        assertEquals("localCache", propertyMetadata.getName());
        Value propertyValue = propertyMetadata.getValue();
        assertTrue(propertyValue instanceof ComponentValue);
        ComponentValue componentValue = (ComponentValue) propertyValue;
        assertTrue(componentValue.getComponentMetadata() instanceof LocalComponentMetadata);
        LocalComponentMetadata innerComp = (LocalComponentMetadata) componentValue.getComponentMetadata();
        assertEquals("org.apache.geronimo.CacheProperty", innerComp.getClassName()); 
        
        metadata = registry.getComponentDefinition("myCache");
        assertNotNull(metadata);
        assertTrue(metadata instanceof LocalComponentMetadata);
        LocalComponentMetadata comp3 = (LocalComponentMetadata) metadata;
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
        
    }
    
    private static class TestNamespaceHandler implements NamespaceHandler {

        public ComponentMetadata decorate(Node node,
                                          ComponentMetadata component,
                                          ParserContext context) {
            //System.out.println("decorate: " + node + " " + component + " " + context.getEnclosingComponent().getName());
            
            if (node instanceof Attr) {
                Attr attr = (Attr) node;
                MyLocalComponentMetadata decoratedComp = new MyLocalComponentMetadata((LocalComponentMetadata)component);                
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
            return null;
        }

        public ComponentMetadata parse(Element element, ParserContext context) {
            String comp = (context.getEnclosingComponent() == null) ? null : context.getEnclosingComponent().getName();
            //System.out.println("parse: " + element.getLocalName() + " " + comp);
            
            String className;
            if (context.getEnclosingComponent() == null) {
                className = "org.apache.geronimo.Cache";
            } else {
                className = "org.apache.geronimo.CacheProperty";
            }
                        
            LocalComponentMetadataImpl p = new LocalComponentMetadataImpl();
            p.setName(element.getAttribute("id"));
            p.setClassName(className);
            
            return p;
        }
        
    }
    
    private static class MyLocalComponentMetadata extends LocalComponentMetadataImpl {
        
        private boolean cacheReturnValues;
        private String operation;
        
        public MyLocalComponentMetadata(LocalComponentMetadata impl) {
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
