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
package org.apache.aries.blueprint.spring;

import javax.xml.validation.Schema;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.parser.NamespaceHandlerSet;
import org.apache.aries.blueprint.spring.support.AbstractBlueprintTest;
import org.apache.aries.blueprint.spring.support.TestBlueprintContainer;
import org.apache.aries.blueprint.testbundles.BeanA;
import org.apache.aries.blueprint.testbundles.BeanB;
import org.springframework.beans.factory.xml.UtilNamespaceHandler;
import org.springframework.transaction.config.TxNamespaceHandler;
import org.xml.sax.SAXException;

public class SpringTest extends AbstractBlueprintTest {

    public void testSpring() throws Exception {
        NamespaceHandlerSet handlers = new SpringNamespaceHandlerSet();
        TestBlueprintContainer container = new TestBlueprintContainer();
        container.parse("/OSGI-INF/blueprint/config.xml", handlers);

        List list = (List) container.getComponentInstance("springList");
        assertEquals("foo", list.get(0));
        assertTrue(list.get(1) instanceof BeanA);
        assertTrue(list.get(2) instanceof BeanB);
        assertEquals(Connection.TRANSACTION_SERIALIZABLE, list.get(3));
        assertNotNull(((BeanB) list.get(2)).getApplicationContext());
    }

    private static class SpringNamespaceHandlerSet implements NamespaceHandlerSet {
        @Override
        public Set<URI> getNamespaces() {
            return null;
        }

        @Override
        public boolean isComplete() {
            return true;
        }

        @Override
        public NamespaceHandler getNamespaceHandler(URI namespace) {
            if ("http://www.springframework.org/schema/beans".equals(namespace.toString())) {
                return new BlueprintNamespaceHandler(null, null, new BeansNamespaceHandler());
            } else if ("http://www.springframework.org/schema/util".equals(namespace.toString())) {
                return new BlueprintNamespaceHandler(null, null, new UtilNamespaceHandler());
            } else if ("http://www.springframework.org/schema/tx".equals(namespace.toString())) {
                return new BlueprintNamespaceHandler(null, null, new TxNamespaceHandler());
            }
            return null;
        }

        @Override
        public Schema getSchema() throws SAXException, IOException {
            return null;
        }

        @Override
        public Schema getSchema(Map<String, String> locations) throws SAXException, IOException {
            return null;
        }

        @Override
        public void addListener(Listener listener) {
        }

        @Override
        public void removeListener(Listener listener) {
        }

        @Override
        public void destroy() {
        }
    }
}
