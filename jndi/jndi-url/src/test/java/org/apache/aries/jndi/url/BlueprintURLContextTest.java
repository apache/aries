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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.aries.jndi.url;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.aries.mocks.BundleMock;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;

public class BlueprintURLContextTest {

  private static Bundle bundle;
  
  static class SimpleComponent { 
    String id;
    public SimpleComponent (String i) { 
      id = i;
    }
    public String getIdMessage () { 
      return id + "_message";
    }
  }
  
  
  @BeforeClass
  public static void setup() { 
    bundle = Skeleton.newMock(new BundleMock("aBundle", new Hashtable<String, String>()), Bundle.class);
    BundleContext bc = bundle.getBundleContext();
    new org.apache.aries.jndi.startup.Activator().start(bc);
    new Activator().start(bc);
    
    // Register a BlueprintContainer mock that will answer getComponentInstance(String id) calls
    SimpleComponent comp1 = new SimpleComponent ("comp1");
    BlueprintContainer bpc = Skeleton.newMock(BlueprintContainer.class);
    Skeleton.getSkeleton(bpc).setReturnValue(new MethodCall(BlueprintContainer.class, "getComponentInstance", String.class), comp1);
    bc.registerService("org.osgi.service.blueprint.container.BlueprintContainer", bpc, new Hashtable<String, String>());
    
  }
  
  @Test
  public void simpleComponentLookupTest () throws Exception { 
    BlueprintURLContext bpURLc = new BlueprintURLContext (bundle, new Hashtable<String, String>());
    SimpleComponent sc = (SimpleComponent) bpURLc.lookup("blueprint:comp/comp1");
    assertNotNull (sc);
    String msg = sc.getIdMessage();
    assertEquals ("comp1 message wrong", "comp1_message", msg);
  }
  
  @Test
  public void twoLevelComponentLookupTest() throws Exception { 
    BundleMock mock = new BundleMock("bundle.for.new.initial.context", new Properties());
    Thread.currentThread().setContextClassLoader(mock.getClassLoader());
    
    InitialContext ctx = new InitialContext();
    Context ctx2 = (Context) ctx.lookup("blueprint:comp");
    SimpleComponent sc = (SimpleComponent) ctx2.lookup("comp1"); 
    assertNotNull (sc);
    String msg = sc.getIdMessage();
    assertEquals ("comp1 message wrong", "comp1_message", msg);
  }
  
}
