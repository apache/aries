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
package org.apache.geronimo.blueprint.namespace;

import org.w3c.dom.Node;

import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;
import org.osgi.service.blueprint.namespace.ParserContext;
import org.osgi.service.blueprint.reflect.ComponentMetadata;

/**
 * A simple ParserContext implementation.
 * 
 * This class is supposed to be short lived and only used for calling a given namespace handler.
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class ParserContextImpl implements ParserContext {

    private final ComponentDefinitionRegistry componentDefinitionRegistry;
    private final ComponentMetadata enclosingComponent;
    private final Node sourceNode;

    public ParserContextImpl(ComponentDefinitionRegistry componentDefinitionRegistry,
                             ComponentMetadata enclosingComponent,
                             Node sourceNode) {
        this.componentDefinitionRegistry = componentDefinitionRegistry;
        this.enclosingComponent = enclosingComponent;
        this.sourceNode = sourceNode;
    }

    public ComponentDefinitionRegistry getComponentDefinitionRegistry() {
        return componentDefinitionRegistry;
    }

    public ComponentMetadata getEnclosingComponent() {
        return enclosingComponent;
    }

    public Node getSourceNode() {
        return sourceNode;
    }
}
