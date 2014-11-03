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
package org.apache.aries.blueprint.plugin.model;

import java.lang.reflect.Field;

import org.apache.aries.blueprint.plugin.model.Bean;
import org.apache.aries.blueprint.plugin.model.Context;
import org.apache.aries.blueprint.plugin.model.OsgiServiceBean;
import org.apache.aries.blueprint.plugin.test.MyBean3;
import org.apache.aries.blueprint.plugin.test.ServiceB;
import org.apache.aries.blueprint.plugin.test.ServiceReferences;
import org.junit.Assert;
import org.junit.Test;

public class ContextTest {

    @Test
    public void testLists()  {
        Context context = new Context(MyBean3.class);
        Assert.assertEquals(1, context.getBeans().size());
        Assert.assertEquals(0, context.getServiceRefs().size());
    }
    
    @Test
    public void testLists2()  {
        Context context = new Context(ServiceReferences.class);
        Assert.assertEquals(1, context.getBeans().size());
        Assert.assertEquals(1, context.getServiceRefs().size());
    }
    
    @Test
    public void testMatching() throws NoSuchFieldException, SecurityException  {
        Context context = new Context(ServiceReferences.class);
        Field field = ServiceReferences.class.getDeclaredFields()[0];
        Bean matching = context.getMatching(field);
        Assert.assertEquals(OsgiServiceBean.class, matching.getClass());
        Assert.assertEquals(ServiceB.class, matching.clazz);
        Assert.assertEquals("serviceB", matching.id);
    }
}
