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
import org.apache.aries.transaction.parsing.TxBlueprintListener;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.reflect.BeanMetadata;

public class NameSpaceHandlerTest extends BaseNameSpaceHandlerSetup {
    
    @Test
    public void testMultipleElements_100() throws Exception
    {
      ComponentDefinitionRegistry cdr = parseCDR("aries.xml");
      
      BeanMetadata comp = (BeanMetadata) cdr.getComponentDefinition("top");
      
      BeanMetadata anon = (BeanMetadata) (comp.getProperties().get(0)).getValue();
      BeanMetadata anonToo = (BeanMetadata) (comp.getProperties().get(1)).getValue();

      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(anon, "doSomething"));
      assertEquals("Never", txenhancer.getComponentMethodTxAttribute(anonToo, "doSomething"));        
    }
    
    @Test
    public void testMultipleElements_110() throws Exception
    {
      ComponentDefinitionRegistry cdr = parseCDR("aries4.xml");
        
      BeanMetadata comp = (BeanMetadata) cdr.getComponentDefinition("top");
      
      BeanMetadata anon = (BeanMetadata) (comp.getProperties().get(0)).getValue();
      BeanMetadata anonToo = (BeanMetadata) (comp.getProperties().get(1)).getValue();

      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(anon, "doSomething"));
      assertEquals("Never", txenhancer.getComponentMethodTxAttribute(anonToo, "doSomething"));
        
    }
    
    @Test
    public void testOptionalMethodAttribute_100() throws Exception
    {
      ComponentDefinitionRegistry cdr = parseCDR("aries2.xml");
      
      BeanMetadata comp = (BeanMetadata) cdr.getComponentDefinition("top");
      
      BeanMetadata anon = (BeanMetadata) (comp.getProperties().get(0)).getValue();
      BeanMetadata anonToo = (BeanMetadata) (comp.getProperties().get(1)).getValue();

      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(anon, "doSomething"));
      assertEquals("Never", txenhancer.getComponentMethodTxAttribute(anonToo, "doSomething"));
        
    }
    
    @Test
    public void testOptionalMethodAttribute_110() throws Exception
    {
      ComponentDefinitionRegistry cdr = parseCDR("aries5.xml");
      
      BeanMetadata comp = (BeanMetadata) cdr.getComponentDefinition("top");
      
      BeanMetadata anon = (BeanMetadata) (comp.getProperties().get(0)).getValue();
      BeanMetadata anonToo = (BeanMetadata) (comp.getProperties().get(1)).getValue();

      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(anon, "doSomething"));
      assertEquals("Never", txenhancer.getComponentMethodTxAttribute(anonToo, "doSomething"));
        
    }
    
    @Test
    public void testOptionalValueAttribute_100() throws Exception
    {
      ComponentDefinitionRegistry cdr = parseCDR("aries3.xml");
      
      BeanMetadata comp = (BeanMetadata) cdr.getComponentDefinition("top");
      
      BeanMetadata anon = (BeanMetadata) (comp.getProperties().get(0)).getValue();
      BeanMetadata anonToo = (BeanMetadata) (comp.getProperties().get(1)).getValue();

      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(anon, "doSomething"));
      assertEquals("Never", txenhancer.getComponentMethodTxAttribute(anonToo, "doSomething"));
      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(anonToo, "require")); 
    }
    
    @Test
    public void testOptionalValueAttribute_110() throws Exception
    {
      ComponentDefinitionRegistry cdr = parseCDR("aries6.xml");
      
      BeanMetadata comp = (BeanMetadata) cdr.getComponentDefinition("top");
      
      BeanMetadata anon = (BeanMetadata) (comp.getProperties().get(0)).getValue();
      BeanMetadata anonToo = (BeanMetadata) (comp.getProperties().get(1)).getValue();

      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(anon, "doSomething"));
      assertEquals("Never", txenhancer.getComponentMethodTxAttribute(anonToo, "doSomething"));
      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(anonToo, "require")); 
    }
    
    @Test
    public void testBundleWideAndBeanLevelTx() throws Exception
    {
      ComponentDefinitionRegistry cdr = parseCDR("mixed-aries.xml");
      
      BeanMetadata compRequiresNew = (BeanMetadata) cdr.getComponentDefinition("requiresNew");
      BeanMetadata compNoTx = (BeanMetadata) cdr.getComponentDefinition("noTx");
      BeanMetadata compSomeTx = (BeanMetadata) cdr.getComponentDefinition("someTx");
      BeanMetadata compAnotherBean = (BeanMetadata) cdr.getComponentDefinition("anotherBean");

      assertEquals("RequiresNew", txenhancer.getComponentMethodTxAttribute(compRequiresNew, "doSomething"));
      assertEquals("Never", txenhancer.getComponentMethodTxAttribute(compNoTx, "doSomething"));
      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(compSomeTx, "doSomething"));
      assertEquals("Mandatory", txenhancer.getComponentMethodTxAttribute(compSomeTx, "getRows"));
      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(compAnotherBean, "doSomething"));
      assertEquals("Supports", txenhancer.getComponentMethodTxAttribute(compAnotherBean, "getWhatever"));       
    }
    
    @Test
    public void testLifecycleOld() throws Exception
    {
        ComponentDefinitionRegistry cdr = parseCDR("aries.xml");

        BeanMetadata comp = (BeanMetadata) cdr.getComponentDefinition("top");

        BeanMetadata anon = (BeanMetadata) (comp.getProperties().get(0)).getValue();
        BeanMetadata anonToo = (BeanMetadata) (comp.getProperties().get(1)).getValue();

        assertEquals("Required", txenhancer.getComponentMethodTxAttribute(anon, "doSomething"));
        assertEquals("Never", txenhancer.getComponentMethodTxAttribute(anonToo, "doSomething"));
        
        assertTrue(namespaceHandler.isRegistered(cdr));
        
        new TxBlueprintListener(namespaceHandler).blueprintEvent(
                new BlueprintEvent(BlueprintEvent.DESTROYED, b, Skeleton.newMock(Bundle.class)));

        assertNull(txenhancer.getComponentMethodTxAttribute(anon, "doSomething"));
        assertNull(txenhancer.getComponentMethodTxAttribute(anonToo, "doSomething"));
    }
    
    @Test
    public void testLifecycleMixed() throws Exception
    {
        ComponentDefinitionRegistry cdr = parseCDR("mixed-aries.xml");
        
        BeanMetadata compRequiresNew = (BeanMetadata) cdr.getComponentDefinition("requiresNew");
        BeanMetadata compNoTx = (BeanMetadata) cdr.getComponentDefinition("noTx");
        BeanMetadata compSomeTx = (BeanMetadata) cdr.getComponentDefinition("someTx");
        BeanMetadata compAnotherBean = (BeanMetadata) cdr.getComponentDefinition("anotherBean");

        assertEquals("RequiresNew", txenhancer.getComponentMethodTxAttribute(compRequiresNew, "doSomething"));
        assertEquals("Never", txenhancer.getComponentMethodTxAttribute(compNoTx, "doSomething"));
        assertEquals("Required", txenhancer.getComponentMethodTxAttribute(compSomeTx, "doSomething"));
        assertEquals("Mandatory", txenhancer.getComponentMethodTxAttribute(compSomeTx, "getRows"));
        assertEquals("Required", txenhancer.getComponentMethodTxAttribute(compAnotherBean, "doSomething"));
        assertEquals("Supports", txenhancer.getComponentMethodTxAttribute(compAnotherBean, "getWhatever"));   
        
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
}
