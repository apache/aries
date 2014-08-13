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
package org.apache.aries.jpa.container.context.impl;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.aries.jpa.container.context.JTAPersistenceContextManager;
import org.apache.aries.jpa.container.context.PersistenceContextProvider;
import org.apache.aries.jpa.container.context.transaction.impl.JTAPersistenceContextRegistry;
import org.apache.aries.mocks.BundleContextMock;
import org.apache.aries.mocks.BundleMock;
import org.apache.aries.quiesce.participant.QuiesceParticipant;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;

import static org.junit.Assert.*;

public class GlobalPersistenceManagerTest {
    private GlobalPersistenceManager sut;
    private Bundle client;
    private Bundle otherClient;
    private Bundle framework;
    
    @Before
    public void setup() throws Exception
    {
        framework = Skeleton.newMock(new BundleMock("framework", new Hashtable<Object, Object>()), Bundle.class);

        BundleContext ctx = Skeleton.newMock(BundleContext.class);
        Skeleton.getSkeleton(ctx).setReturnValue(new MethodCall(BundleContext.class, "getBundle", long.class), framework);
        
        client = Skeleton.newMock(Bundle.class);
        Skeleton.getSkeleton(client).setReturnValue(new MethodCall(Bundle.class, "getBundleContext"), ctx);

        otherClient = Skeleton.newMock(Bundle.class);
        Skeleton.getSkeleton(otherClient).setReturnValue(new MethodCall(Bundle.class, "getBundleContext"), ctx);        
        
        sut = new GlobalPersistenceManager();
        sut.start(ctx);
        Skeleton.getSkeleton(framework.getBundleContext()).setReturnValue(
                new MethodCall(BundleContext.class, "getBundles"), new Bundle[] {framework, client, otherClient});
    }
    
    @Test
    public void testServices() throws Exception {
      BundleContext bc = Skeleton.newMock(new BundleContextMock(), BundleContext.class);
      
      GlobalPersistenceManager gpm = new GlobalPersistenceManager();
      
      BundleContextMock.assertNoServiceExists(PersistenceContextProvider.class.getName());
      BundleContextMock.assertNoServiceExists(JTAPersistenceContextManager.class.getName());
      BundleContextMock.assertNoServiceExists(QuiesceParticipant.class.getName());
      
      gpm.start(bc);
      
      BundleContextMock.assertServiceExists(PersistenceContextProvider.class.getName());
      BundleContextMock.assertServiceExists(JTAPersistenceContextManager.class.getName());
      BundleContextMock.assertServiceExists(QuiesceParticipant.class.getName());
      
      gpm.stop(bc);
      
      BundleContextMock.assertNoServiceExists(PersistenceContextProvider.class.getName());
      BundleContextMock.assertNoServiceExists(JTAPersistenceContextManager.class.getName());
      BundleContextMock.assertNoServiceExists(QuiesceParticipant.class.getName());
    }
    
    @Test
    public void testRegister() throws Exception {
        sut.registerContext("name", client, new HashMap<String, Object>());
        sut.registerContext("otherName", otherClient, new HashMap<String, Object>());
        
        assertEquals(1, getManagers().size());
        assertEquals(new HashSet<String>(Arrays.asList("name")), getContexts().get(client));
        assertEquals(new HashSet<String>(Arrays.asList("otherName")), getContexts().get(otherClient));
        
        sut.registerContext("name2", client, new HashMap<String, Object>());
        assertEquals(new HashSet<String>(Arrays.asList("name", "name2")), getContexts().get(client));
    }
    
    @Test
    public void testStopFramework() throws Exception {
        sut.registerContext("name", client, new HashMap<String, Object>());
        sut.registerContext("otherName", otherClient, new HashMap<String, Object>());

        sut.bundleChanged(new BundleEvent(BundleEvent.STOPPING, framework));
        
        assertTrue(getManagers().isEmpty());
        assertTrue(getContexts().isEmpty());
    }
    
    @Test
    public void testIndividual() throws Exception {
        sut.registerContext("name", client, new HashMap<String, Object>());
        sut.registerContext("otherName", otherClient, new HashMap<String, Object>());
        
        sut.bundleChanged(new BundleEvent(BundleEvent.STOPPING, client));
        assertEquals(1, getManagers().size());
        assertNull(getContexts().get(client));
        assertNotNull(getContexts().get(otherClient));
        
        sut.bundleChanged(new BundleEvent(BundleEvent.STOPPING, otherClient));
        assertEquals(1, getManagers().size());
        assertNull(getContexts().get(client));
        assertNull(getContexts().get(otherClient));
        
        sut.bundleChanged(new BundleEvent(BundleEvent.STOPPING, framework));
        assertTrue(getManagers().isEmpty());
    }
    
    private Map<Bundle,PersistenceContextManager> getManagers() throws Exception {
        Field f = GlobalPersistenceManager.class.getDeclaredField("managers");
        f.setAccessible(true);
        return (Map<Bundle,PersistenceContextManager>) f.get(sut);
    }
    
    private Map<Bundle, Set<String>> getContexts() throws Exception {
        Field f = GlobalPersistenceManager.class.getDeclaredField("persistenceContexts");
        f.setAccessible(true);
        return (Map<Bundle,Set<String>>) f.get(sut);        
    }
}
