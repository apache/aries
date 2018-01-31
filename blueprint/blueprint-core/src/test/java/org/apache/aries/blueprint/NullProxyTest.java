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

import org.apache.aries.blueprint.container.SatisfiableRecipe;
import org.apache.aries.blueprint.di.Repository;
import org.apache.aries.blueprint.parser.ComponentDefinitionRegistryImpl;
import org.apache.aries.blueprint.pojos.PojoC;
import org.apache.aries.blueprint.pojos.Service;
import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.proxy.impl.JdkProxyManager;

public class NullProxyTest extends AbstractBlueprintTest {

    public void testNullProxy() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-null-proxy.xml");
        ProxyManager proxyManager = new JdkProxyManager();
        Repository repository = new TestBlueprintContainer(registry, proxyManager).getRepository();
        ((SatisfiableRecipe) repository.getRecipe("refDefNullProxy")).start(new SatisfiableRecipe.SatisfactionListener() {
            @Override
            public void notifySatisfaction(SatisfiableRecipe satisfiable) {

            }
        });
        PojoC pojoC = (PojoC) repository.create("pojoC");
        assertTrue(pojoC.getService() instanceof Service);
        assertEquals(0, pojoC.getService().getInt());
        assertNull(pojoC.getService().getPojoA());

    }

    static class ProxyGenerationException extends RuntimeException {
    }
    
}
