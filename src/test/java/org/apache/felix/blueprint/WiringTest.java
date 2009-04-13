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
package org.apache.felix.blueprint;

import java.net.URL;

import junit.framework.TestCase;
import org.apache.felix.blueprint.context.Instanciator;
import org.apache.felix.blueprint.context.Parser;
import org.apache.felix.blueprint.pojos.PojoA;
import org.apache.xbean.recipe.ObjectGraph;
import org.apache.xbean.recipe.Repository;

public class WiringTest extends TestCase {

    public void testWiring() throws Exception {
        Parser parser = parse("/test-wiring.xml");

        Repository repository = Instanciator.createRepository(parser.getRegistry());
        ObjectGraph graph = new ObjectGraph(repository);
        Object obj = graph.create("pojoA");
        assertNotNull(obj);
        assertTrue(obj instanceof PojoA);
        PojoA pojoa = (PojoA) obj;
        assertNotNull(pojoa.getPojob());
        assertNotNull(pojoa.getPojob().getUri());
    }

    protected Parser parse(String name) throws Exception {
        Parser parser = new Parser();
        parser.parse(new URL[] { getClass().getResource(name) });
        return parser;
    }

}
