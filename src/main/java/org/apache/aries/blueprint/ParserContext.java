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

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public interface ParserContext  {
    /**
     * Returns the DOM Node that was passed to the NamespaceHandler call for which
     * this ParserContext instance was created.
     */
    Node getSourceNode();

    ComponentDefinitionRegistry getComponentDefinitionRegistry();
    
    /**
     * Retrieve the <code>ComponentMetadata</code> of the component that
     * encloses the current <code>Node</code> that is to be parsed by a 
     * namespace handler.
     * 
     * In case of top-level components this method will return <code>null</code>.
     * @return the enclosing component's metadata or null if there is no enclosing component
     */
    ComponentMetadata getEnclosingComponent();
    
    /**
     * Create a new metadata instance of the given type. The returned
     * object will also implement the appropriate <code>MutableComponentMetadata</code>
     * interface, so as to allow the caller to set the properties of the 
     * metadata.
     *
     * Note that the returned object may not be initialised, so callers
     * should take care to assure every property needed by the blueprint
     * extender is set.
     *
     * @param type the class of the Metadata object to create
     * @param <T> The expected Metadata type to be created
     * @return a new instance
     */
    <T extends Metadata> T createMetadata(Class<T> type);

    /**
     * Invoke the blueprint parser to parse a DOM element.
     * @param type the class of the Metadata type to be parsed
     * @param enclosingComponent The component metadata that contains the Element
     * to be parsed
     * @param element The DOM element that is to be parsed
     * @param <T> The expected metadata type to be parsed
     */
    <T> T parseElement(Class<T> type, ComponentMetadata enclosingComponent, Element element);

    /** 
     * Generate a unique id following the same scheme that the blueprint container
     * uses internally
     */
    String generateId();
    
    /**
     * Get the default activation setting for the current blueprint file
     */
    String getDefaultActivation();
    
    /**
     * Get the default availability setting for the current blueprint file
     */
    String getDefaultAvailability();
    
    /**
     * Get the default timeout setting for the current blueprint file
     */
    String getDefaultTimeout();
}

