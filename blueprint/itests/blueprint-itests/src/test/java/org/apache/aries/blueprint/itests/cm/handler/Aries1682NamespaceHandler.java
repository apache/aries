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
package org.apache.aries.blueprint.itests.cm.handler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.PassThroughMetadata;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class Aries1682NamespaceHandler implements NamespaceHandler {

    @Override
    public URL getSchemaLocation(String namespace) {
        if ("http://aries.apache.org/blueprint/xmlns/blueprint-aries-1682/v1.0.0".equals(namespace)) {
            try {
                return new URL("jar:" + FrameworkUtil.getBundle(this.getClass()).getLocation() + "!/blueprint-aries-1682.xsd");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public Set<Class> getManagedClasses() {
        return new HashSet<Class>(Collections.<Class>singletonList(String.class));
    }

    @Override
    public Metadata parse(Element element, ParserContext context) {
        MutableBeanMetadata metadata = context.createMetadata(MutableBeanMetadata.class);
        metadata.setProcessor(true);
        metadata.setId("aries-1682");
        metadata.setScope(BeanMetadata.SCOPE_SINGLETON);
        metadata.addArgument(new PassThroughMetadata() {
            @Override
            public Object getObject() {
                return "ARIES-1682";
            }

            @Override
            public String getId() {
                return "aries-1682-arg";
            }

            @Override
            public int getActivation() {
                return 0;
            }

            @Override
            public List<String> getDependsOn() {
                return null;
            }
        }, null, 0);
        metadata.setRuntimeClass(String.class);
        return metadata;
    }

    @Override
    public ComponentMetadata decorate(Node node, ComponentMetadata component, ParserContext context) {
        return null;
    }

}
