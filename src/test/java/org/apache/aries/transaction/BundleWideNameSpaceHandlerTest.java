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
package org.apache.aries.transaction;

import static org.apache.aries.transaction.annotations.TransactionPropagationType.NotSupported;
import static org.apache.aries.transaction.annotations.TransactionPropagationType.Required;
import static org.apache.aries.transaction.annotations.TransactionPropagationType.RequiresNew;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.transaction.annotations.TransactionPropagationType;
import org.junit.Test;
import org.osgi.service.blueprint.reflect.BeanMetadata;

public class BundleWideNameSpaceHandlerTest extends BaseNameSpaceHandlerSetup {


    private void testMultipleElements(String xml) throws Exception
    {
        ComponentDefinitionRegistry cdr = parseCDR(xml);

        BeanMetadata compTop = (BeanMetadata) cdr.getComponentDefinition("top1");
        BeanMetadata compDown = (BeanMetadata) cdr.getComponentDefinition("down1");

        assertNotNull(compTop);
        assertNotNull(compDown);

        assertEquals(1, cdr.getInterceptors(compTop).size());
        assertEquals(1, cdr.getInterceptors(compDown).size());

        assertEquals(TransactionPropagationType.Required, txenhancer.getComponentMethodTxAttribute(compTop, "doSomething"));
        assertEquals(TransactionPropagationType.Never, txenhancer.getComponentMethodTxAttribute(compDown, "doSomething"));

    }

    @Test
    public void testMultipleElements_110() throws Exception {
        testMultipleElements("bundlewide-aries.xml");
    }

    @Test
    public void testMultipleElements_120() throws Exception {
        testMultipleElements("bundlewide-aries4.xml");
    }

    private void testMultipleElements2(String xml) throws Exception
    {
        ComponentDefinitionRegistry cdr = parseCDR(xml);

        BeanMetadata compTop = (BeanMetadata) cdr.getComponentDefinition("top2");
        BeanMetadata compDown = (BeanMetadata) cdr.getComponentDefinition("down2");
        BeanMetadata compMiddle = (BeanMetadata) cdr.getComponentDefinition("middle2");

        assertNotNull(compTop);
        assertNotNull(compDown);
        assertNotNull(compMiddle);

        assertEquals(1, cdr.getInterceptors(compTop).size());
        assertEquals(1, cdr.getInterceptors(compDown).size());
        assertEquals(0, cdr.getInterceptors(compMiddle).size());

        assertEquals(RequiresNew, txenhancer.getComponentMethodTxAttribute(compTop, "update1234"));
        assertEquals(Required, txenhancer.getComponentMethodTxAttribute(compTop, "update"));
        assertEquals(NotSupported, txenhancer.getComponentMethodTxAttribute(compTop, "doSomething"));

        assertEquals(Required, txenhancer.getComponentMethodTxAttribute(compDown, "doSomething"));
        assertEquals(NotSupported, txenhancer.getComponentMethodTxAttribute(compDown, "update1234"));

        assertEquals(null, txenhancer.getComponentMethodTxAttribute(compMiddle, "doSomething"));

    }

    @Test
    public void testMultipleElements2_110() throws Exception {
        testMultipleElements2("bundlewide-aries2.xml");
    }

    @Test
    public void testMultipleElements2_120() throws Exception {
        testMultipleElements2("bundlewide-aries5.xml");
    }

    private void testMultipleElements3(String xml) throws Exception
    {
        ComponentDefinitionRegistry cdr = parseCDR(xml);

        BeanMetadata compTop = (BeanMetadata) cdr.getComponentDefinition("top3");
        BeanMetadata compDown = (BeanMetadata) cdr.getComponentDefinition("down3");
        BeanMetadata compMiddle = (BeanMetadata) cdr.getComponentDefinition("middle3");

        assertNotNull(compTop);
        assertNotNull(compDown);
        assertNotNull(compMiddle);

        assertEquals(1, cdr.getInterceptors(compTop).size());
        assertEquals(1, cdr.getInterceptors(compDown).size());
        assertEquals(1, cdr.getInterceptors(compMiddle).size());

        assertEquals(TransactionPropagationType.RequiresNew, txenhancer.getComponentMethodTxAttribute(compTop, "update1234"));
        assertEquals(TransactionPropagationType.Required, txenhancer.getComponentMethodTxAttribute(compTop, "update"));
        assertEquals(TransactionPropagationType.NotSupported, txenhancer.getComponentMethodTxAttribute(compTop, "doSomething"));

        assertEquals(TransactionPropagationType.Required, txenhancer.getComponentMethodTxAttribute(compDown, "doSomething"));
        assertEquals(TransactionPropagationType.NotSupported, txenhancer.getComponentMethodTxAttribute(compDown, "update1234"));

        assertEquals(TransactionPropagationType.Required, txenhancer.getComponentMethodTxAttribute(compMiddle, "doSomething"));

    }

    @Test
    public void testMultipleElements3_110() throws Exception {
        testMultipleElements3("bundlewide-aries3.xml");
    }

    @Test
    public void testMultipleElements3_120() throws Exception {
        testMultipleElements3("bundlewide-aries6.xml");
    }
}
