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
package org.apache.aries.blueprint;

import java.net.URL;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;

public interface NamespaceHandler  {
    
    URL getSchemaLocation(String namespace);

    Set<Class> getManagedClasses();
    
    Metadata parse(Element element, ParserContext context);
    
    /**
     * Process a child node of an enclosing blueprint component. 
     * 
     * If the decorator returns a new ComponentMetadata instance, then this namespace handler must 
     * ensure that existing interceptors are registered against the new instance if appropriate.
     * 
     * @param node The node associated with this NamespaceHandler that should be used to decorate the enclosing 
     * component
     * @param component The enclosing blueprint component
     * @param context The parser context
     * @return The decorated component to be used instead of the original enclosing component. This can of course be
     * the original component.
     */
    ComponentMetadata decorate(Node node, ComponentMetadata component, ParserContext context);
             
}
