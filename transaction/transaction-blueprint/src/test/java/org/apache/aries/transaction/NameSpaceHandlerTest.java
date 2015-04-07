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

import static org.junit.Assert.*;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.transaction.annotations.TransactionPropagationType;
import org.apache.aries.transaction.parsing.TxBlueprintListener;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.reflect.BeanMetadata;

public class NameSpaceHandlerTest extends BaseNameSpaceHandlerSetup {
	
    private void testMultipleElements(String xml) throws Exception
    {
      ComponentDefinitionRegistry cdr = parseCDR(xml);
      
      BeanMetadata comp = (BeanMetadata) cdr.getComponentDefinition("top");
      
      BeanMetadata anon = (BeanMetadata) (comp.getProperties().get(0)).getValue();
      BeanMetadata anonToo = (BeanMetadata) (comp.getProperties().get(1)).getValue();

      assertEquals(TransactionPropagationType.Required, txenhancer.getComponentMethodTxAttribute(anon, "doSomething"));
      assertEquals(TransactionPropagationType.Never, txenhancer.getComponentMethodTxAttribute(anonToo, "doSomething"));        
    }
	
    @Test
    public void testMultipleElements_100() throws Exception
    {
      testMultipleElements("aries.xml");
    }
    
    @Test
    public void testMultipleElements_110() throws Exception
    {
      testMultipleElements("aries4.xml");
    }
    
    @Test
    public void testMultipleElements_120() throws Exception
    {
      testMultipleElements("aries7.xml");
    }
    
    private void testOptionalMethodAttribute(String xml) throws Exception
    {
      ComponentDefinitionRegistry cdr = parseCDR(xml);
      
      BeanMetadata comp = (BeanMetadata) cdr.getComponentDefinition("top");
      
      BeanMetadata anon = (BeanMetadata) (comp.getProperties().get(0)).getValue();
      BeanMetadata anonToo = (BeanMetadata) (comp.getProperties().get(1)).getValue();

      assertEquals(TransactionPropagationType.Required, txenhancer.getComponentMethodTxAttribute(anon, "doSomething"));
      assertEquals(TransactionPropagationType.Never, txenhancer.getComponentMethodTxAttribute(anonToo, "doSomething"));
        
    }
    
    @Test
    public void testOptionalMethodAttribute_100() throws Exception
    {
      testOptionalMethodAttribute("aries2.xml");
    }
    
    @Test
    public void testOptionalMethodAttribute_110() throws Exception
    {
      testOptionalMethodAttribute("aries5.xml");
    }
    
    @Test
    public void testOptionalMethodAttribute_120() throws Exception
    {
      testOptionalMethodAttribute("aries8.xml");
    }
    
    private void testOptionalValueAttribute(String xml) throws Exception
    {
      ComponentDefinitionRegistry cdr = parseCDR(xml);
      
      BeanMetadata comp = (BeanMetadata) cdr.getComponentDefinition("top");
      
      BeanMetadata anon = (BeanMetadata) (comp.getProperties().get(0)).getValue();
      BeanMetadata anonToo = (BeanMetadata) (comp.getProperties().get(1)).getValue();

      assertEquals(TransactionPropagationType.Required, txenhancer.getComponentMethodTxAttribute(anon, "doSomething"));
      assertEquals(TransactionPropagationType.Never, txenhancer.getComponentMethodTxAttribute(anonToo, "doSomething"));
      assertEquals(TransactionPropagationType.Required, txenhancer.getComponentMethodTxAttribute(anonToo, "require")); 
    }
    
    @Test
    public void testOptionalValueAttribute_100() throws Exception
    {
      testOptionalValueAttribute("aries3.xml");
    }
    
    @Test
    public void testOptionalValueAttribute_110() throws Exception
    {
      testOptionalValueAttribute("aries6.xml");
    }
    
    @Test
    public void testOptionalValueAttribute_120() throws Exception
    {
      testOptionalValueAttribute("aries9.xml");
    }
    
    private void testBundleWideAndBeanLevelTx(String xml) throws Exception
    {
      ComponentDefinitionRegistry cdr = parseCDR(xml);
      
      BeanMetadata compRequiresNew = (BeanMetadata) cdr.getComponentDefinition("requiresNew");
      BeanMetadata compNoTx = (BeanMetadata) cdr.getComponentDefinition("noTx");
      BeanMetadata compSomeTx = (BeanMetadata) cdr.getComponentDefinition("someTx");
      BeanMetadata compAnotherBean = (BeanMetadata) cdr.getComponentDefinition("anotherBean");

      assertEquals(TransactionPropagationType.RequiresNew, txenhancer.getComponentMethodTxAttribute(compRequiresNew, "doSomething"));
      assertEquals(TransactionPropagationType.Never, txenhancer.getComponentMethodTxAttribute(compNoTx, "doSomething"));
      assertEquals(TransactionPropagationType.Required, txenhancer.getComponentMethodTxAttribute(compSomeTx, "doSomething"));
      assertEquals(TransactionPropagationType.Mandatory, txenhancer.getComponentMethodTxAttribute(compSomeTx, "getRows"));
      assertEquals(TransactionPropagationType.Required, txenhancer.getComponentMethodTxAttribute(compAnotherBean, "doSomething"));
      assertEquals(TransactionPropagationType.Supports, txenhancer.getComponentMethodTxAttribute(compAnotherBean, "getWhatever"));       
    }
    
    @Test
    public void testBundleWideAndBeanLevelTx_110() throws Exception {
      testBundleWideAndBeanLevelTx("mixed-aries.xml");
    }
    
    @Test
    public void testBundleWideAndBeanLevelTx_120() throws Exception {
      testBundleWideAndBeanLevelTx("mixed-aries2.xml");
    }
    
    
    private void testLifecycleOld(String xml) throws Exception
    {
        ComponentDefinitionRegistry cdr = parseCDR(xml);

        BeanMetadata comp = (BeanMetadata) cdr.getComponentDefinition("top");

        BeanMetadata anon = (BeanMetadata) (comp.getProperties().get(0)).getValue();
        BeanMetadata anonToo = (BeanMetadata) (comp.getProperties().get(1)).getValue();

        assertEquals(TransactionPropagationType.Required, txenhancer.getComponentMethodTxAttribute(anon, "doSomething"));
        assertEquals(TransactionPropagationType.Never, txenhancer.getComponentMethodTxAttribute(anonToo, "doSomething"));
        
        assertTrue(namespaceHandler.isRegistered(cdr));
        
        new TxBlueprintListener(namespaceHandler).blueprintEvent(
                new BlueprintEvent(BlueprintEvent.DESTROYED, b, Skeleton.newMock(Bundle.class)));

        assertNull(txenhancer.getComponentMethodTxAttribute(anon, "doSomething"));
        assertNull(txenhancer.getComponentMethodTxAttribute(anonToo, "doSomething"));
    }
    
    @Test
    public void testLifecycleOld() throws Exception
    {
    	testLifecycleOld("aries.xml");
    }
    
    @Test
    public void testLifecycleOld_110() throws Exception
    {
    	testLifecycleOld("aries4.xml");
    }
    
    @Test
    public void testLifecycleOld_120() throws Exception
    {
    	testLifecycleOld("aries7.xml");
    }
    
    private void testLifecycleMixed(String xml) throws Exception
    {
        ComponentDefinitionRegistry cdr = parseCDR(xml);
        
        BeanMetadata compRequiresNew = (BeanMetadata) cdr.getComponentDefinition("requiresNew");
        BeanMetadata compNoTx = (BeanMetadata) cdr.getComponentDefinition("noTx");
        BeanMetadata compSomeTx = (BeanMetadata) cdr.getComponentDefinition("someTx");
        BeanMetadata compAnotherBean = (BeanMetadata) cdr.getComponentDefinition("anotherBean");

        assertEquals(TransactionPropagationType.RequiresNew, txenhancer.getComponentMethodTxAttribute(compRequiresNew, "doSomething"));
        assertEquals(TransactionPropagationType.Never, txenhancer.getComponentMethodTxAttribute(compNoTx, "doSomething"));
        assertEquals(TransactionPropagationType.Required, txenhancer.getComponentMethodTxAttribute(compSomeTx, "doSomething"));
        assertEquals(TransactionPropagationType.Mandatory, txenhancer.getComponentMethodTxAttribute(compSomeTx, "getRows"));
        assertEquals(TransactionPropagationType.Required, txenhancer.getComponentMethodTxAttribute(compAnotherBean, "doSomething"));
        assertEquals(TransactionPropagationType.Supports, txenhancer.getComponentMethodTxAttribute(compAnotherBean, "getWhatever"));   
        
        // cleanup
        
        assertTrue(namespaceHandler.isRegistered(cdr));
        
        new TxBlueprintListener(namespaceHandler).blueprintEvent(
                new BlueprintEvent(BlueprintEvent.DESTROYED, b, Skeleton.newMock(Bundle.class)));
        
        assertFalse(namespaceHandler.isRegistered(cdr));
        assertNull(txenhancer.getComponentMethodTxAttribute(compRequiresNew, "doSomething"));
        assertNull(txenhancer.getComponentMethodTxAttribute(compSomeTx, "doSomething"));
        assertNull(txenhancer.getComponentMethodTxAttribute(compSomeTx, "getRows"));
        assertNull(txenhancer.getComponentMethodTxAttribute(compAnotherBean, "doSomething"));
        assertNull(txenhancer.getComponentMethodTxAttribute(compAnotherBean, "getWhatever"));   
    }
    
    @Test
    public void testLifecycleMixed_110() throws Exception
    {
    	testLifecycleMixed("mixed-aries.xml");
    }
    
    @Test
    public void testLifecycleMixed_120() throws Exception
    {
    	testLifecycleMixed("mixed-aries2.xml");
    }
}
