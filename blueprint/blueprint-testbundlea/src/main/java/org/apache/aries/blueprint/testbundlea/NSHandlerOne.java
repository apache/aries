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
import java.util.List;
import java.util.Set;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.PassThroughMetadata;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableRefMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * A simple example namespace handler, that understands an element, and 2 attributes
 * 
 * When the element is encountered in a top level blueprint element, the handler will add a 
 * passthroughmetadata with it's id as the contained attribone.
 * The passthroughmetadata will return a string with the value from the contained 
 * attrib two.
 * 
 * If the element is encountered during processing of a bean, it will add a property to the 
 * bean with the name of the attribone value, and a value of the passthroughmetadata with id
 * matching attribtwo
 * 
 * This handler is designed to exercise aspects of the NamespaceHandler capability set.
 *
 */
public class NSHandlerOne implements NamespaceHandler {
    
    public static String NSURI = "http://ns.handler.one";
    
    private static String ELT_NAME = "nshandlerone";
    private static String ATTRIB_ONE = "attribone";
    private static String ATTRIB_TWO = "attribtwo";

    //process attributes
    public ComponentMetadata decorate(Node node, ComponentMetadata component,
            ParserContext context) {
        
        //this test makes use of the 'Mutable' implementations
        //without which the code would need to implement our own BeanMetadata,
        //and RefMetadata.
        if(component !=null && component instanceof MutableBeanMetadata){
            MutableBeanMetadata mbm = (MutableBeanMetadata)component;
            
            Attr a = (Attr)node;
            Element bean = a.getOwnerElement();            
            
            String propname = bean.getAttributeNS(NSURI,ATTRIB_ONE);
            
            //if this were not a test, we might attempt to ensure this ref existed
            String passthruref = bean.getAttributeNS(NSURI,ATTRIB_TWO);
            
            MutableRefMetadata ref = (MutableRefMetadata)context.createMetadata(RefMetadata.class);
            ref.setComponentId(passthruref);
            
            mbm.addProperty(propname, ref);
        }
        return component;
    }
    
    //process elements
    public Metadata parse(Element element, ParserContext context) {
        Metadata retval = null;       
        if( element.getLocalName().equals(ELT_NAME) ) {
            
            final String id = element.getAttributeNS(NSURI,ATTRIB_ONE);
            final String value = element.getAttributeNS(NSURI,ATTRIB_TWO);
            
            PassThroughMetadata ptm = new PassThroughMetadata() {
                
                public String getId() {
                    return id;
                }
                
                //not used currently
                public List<String> getDependsOn() {
                    return null;
                }
                
                //also not used currently
                public int getActivation() {
                    return 0;
                }
                
                public Object getObject() {
                    return value;
                }
            };
            
            retval = ptm;
        }
        return retval;
    }    

    //supply schema back to blueprint.
    public URL getSchemaLocation(String namespace) {
        return this.getClass().getResource("nshandlerone.xsd");
    }

    public Set<Class> getManagedClasses() {
        return null;
    }
    
}
