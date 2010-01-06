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
package org.apache.aries.blueprint.testbundlea;

import java.net.URL;
import java.util.Set;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class NSHandlerThree implements NamespaceHandler{
    public static String NSURI = "http://ns.handler.three";
    
    private static String ELT_NAME = "nshandlerthree";
    private static String ATTRIB_ONE = "attribone";
    private static String ATTRIB_TWO = "attribtwo";
    
    public ComponentMetadata decorate(Node node, ComponentMetadata component,
            ParserContext context) {
        if(node.getLocalName().equals(ATTRIB_ONE)){
            if(component instanceof BeanMetadata){
                if(context.getComponentDefinitionRegistry().getComponentDefinition(NSURI+"/BeanProcessor")==null){
                    BeanMetadata bm = context.createMetadata(BeanMetadata.class);
                    MutableBeanMetadata mbm = (MutableBeanMetadata)bm;
                    mbm.setProcessor(true);
                    mbm.setRuntimeClass(BeanProcessorTest.class);
                    mbm.setScope(BeanMetadata.SCOPE_SINGLETON);
                    mbm.setId(NSURI+"/BeanProcessor");
                    context.getComponentDefinitionRegistry().registerComponentDefinition(mbm);
                }
            }
        }
        return component;
    }

    //process elements
    public Metadata parse(Element element, ParserContext context) {
        return null;
    }    

    //supply schema back to blueprint.
    public URL getSchemaLocation(String namespace) {
        return this.getClass().getResource("nshandlerthree.xsd");
    }

    public Set<Class> getManagedClasses() {
        return null;
    }

}
