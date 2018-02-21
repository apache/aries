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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.aries.mocks.BundleContextMock;
import org.apache.aries.mocks.BundleMock;
import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.NoSuchComponentException;

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
  
  static class AnotherComponent extends SimpleComponent { 
    public AnotherComponent (String i) { 
      super(i);
    }
    @Override
    public String getIdMessage () { 
      return "AnotherComponent with id " + id;
    }
  }
  
  static class BlueprintContainerStub 
  { 
    SimpleComponent comp1 = new SimpleComponent ("comp1");
    AnotherComponent comp2 = new AnotherComponent ("comp2");
    
    public Object getComponentInstance (String compId) throws NoSuchComponentException { 
      if (compId.equals("comp1")) { 
        return comp1;
      } else if (compId.equals("comp2")) { 
        return comp2;
      }
      throw new NoSuchComponentException("Component does not exist", compId);
    }
    
    public Set<String> getComponentIds() { 
      return new HashSet<String>(Arrays.asList("comp1", "comp2"));
    }
  }
  
  
  @BeforeClass
  public static void setup() throws Exception {
    System.setProperty("org.apache.aries.jndi.disable.builder", "false");
    bundle = Skeleton.newMock(new BundleMock("aBundle", new Hashtable<String, String>()), Bundle.class);
    BundleContext bc = bundle.getBundleContext();
    new org.apache.aries.jndi.startup.Activator().start(bc);
    Activator a = new Activator();
    a.start(bc);
    ProxyManager pm = (ProxyManager) Proxy.newProxyInstance(
            BlueprintURLContext.class.getClassLoader(),
            new Class[]{ProxyManager.class},
            (proxy, method, args) -> null);
    a.serviceChanged(null, pm);
    
    // Register a BlueprintContainer mock that will answer getComponentInstance(String id) calls
    BlueprintContainer bpc = Skeleton.newMock(new BlueprintContainerStub(), BlueprintContainer.class);
    bc.registerService("org.osgi.service.blueprint.container.BlueprintContainer", bpc, new Hashtable<String, String>());
    
  }
  
  @AfterClass
  public static void teardown() { 
    BundleContextMock.clear();
  }
  
  @Before
  public void setupClassLoader() { 
    BundleMock mock = new BundleMock("bundle.for.new.initial.context", new Properties());
    Thread.currentThread().setContextClassLoader(mock.getClassLoader());
  }
  
  @After
  public void restoreClassLoader() { 
    Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
  }
  

  /**
   * Check that we can directly address a blueprint component
   */
  @Test
  public void testSimpleComponentLookup() throws Exception { 
    BlueprintURLContext bpURLc = new BlueprintURLContext (bundle, new Hashtable<String, String>());
    SimpleComponent sc = (SimpleComponent) bpURLc.lookup("blueprint:comp/comp1");
    assertNotNull (sc);
    String msg = sc.getIdMessage();
    assertEquals ("comp1 message wrong", "comp1_message", msg);
  }
  
  /**
   * Validate that we can create an InitialContext at blueprint:comp scope, and then 
   * look components up within it
   */
  @Test
  public void testTwoLevelComponentLookup() throws Exception { 
    InitialContext ctx = new InitialContext();
    Context ctx2 = (Context) ctx.lookup("blueprint:comp");
    SimpleComponent sc = (SimpleComponent) ctx2.lookup("comp2"); 
    assertNotNull (sc);
    String msg = sc.getIdMessage();
    assertEquals ("comp2 message wrong", "AnotherComponent with id comp2", msg);
  }
  
  /**
   * Check that we get a NameNotFoundException if we lookup something not in the
   * registry.
   * 
   * @throws NamingException
   */
  @Test(expected=NameNotFoundException.class)
  public void testLookupForServiceWeNeverHad() throws NamingException
  {
    InitialContext ctx = new InitialContext();
    ctx.lookup("blueprint:comp/this.is.not.a.component");
  }
  

  /**
   * Validate that list() function works for BlueprintURLContext. 
   * This returns an enumeration of component id -> component class name pairs
   */
  @Test
  public void testList() throws Exception { 
    InitialContext ctx = new InitialContext();
    NamingEnumeration<NameClassPair> compList = ctx.list("blueprint:comp");
    
    Set<String> expectedCompIds = new BlueprintContainerStub().getComponentIds();
    while (compList.hasMore()) { 
      NameClassPair ncp = compList.next();
      String compId = ncp.getName();
      String compClass = ncp.getClassName();
      if (compId.equals("comp1")) { 
        assertEquals ("comp1 class wrong in list", SimpleComponent.class.getName(), compClass);
      } else if (compId.equals("comp2")) { 
        assertEquals ("comp2 class wrong in list", AnotherComponent.class.getName(), compClass);
      }
      expectedCompIds.remove(compId);
    }
    assertEquals ("Not all expected components were found", expectedCompIds.size(), 0);
  }
  
  /**
   * Test BlueprintURLContext.listBindings() 
   * This returns an enumeration of component id -> component pairs
   */
  @Test
  public void testListBindings() throws Exception { 
    InitialContext ctx = new InitialContext();
    NamingEnumeration<Binding> bindings = ctx.listBindings("blueprint:comp");
    
    Set<String> expectedCompIds = new BlueprintContainerStub().getComponentIds();
    while (bindings.hasMore()) { 
      Binding b = bindings.next();
      String compId = b.getName();
      Object component = b.getObject();
      if (compId.equals("comp1")) { 
        SimpleComponent sc = (SimpleComponent) component;
        assertEquals ("comp1 message wrong", "comp1_message", sc.getIdMessage());
      } else if (compId.equals("comp2")) { 
        AnotherComponent ac = (AnotherComponent) component;
        assertEquals ("comp2 message wrong", "AnotherComponent with id comp2", ac.getIdMessage());
      }
      expectedCompIds.remove(compId);
    }
    assertEquals ("Not all expected components were found", expectedCompIds.size(), 0);
  }
  
  @Test 
  public void testBlueprintTimeoutExtractionBothSpecified() { 
    Bundle b = bundleMock ("bundle.name;x=y;p:=q;blueprint.graceperiod:=true;blueprint.timeout:=10000;a=b;c:=d");
    int timeout = BlueprintURLContext.getGracePeriod(b);
    assertEquals ("graceperiod wrong", 10000, timeout);
  }
  
  @Test
  public void testGracePeriodFalseHandled() throws Exception  { 
    Bundle b = bundleMock ("bundle.name;x=y;p:=q;blueprint.graceperiod:=false;blueprint.timeout:=10000;a=b;c:=d");
    int timeout = BlueprintURLContext.getGracePeriod(b);
    assertEquals ("graceperiod wrong", -1, timeout);
    
    b = bundleMock ("bundle.name;x=y;p:=q;blueprint.graceperiod:=false;a=b;c:=d");
    timeout = BlueprintURLContext.getGracePeriod(b);
    assertEquals ("graceperiod wrong", -1, timeout);
  }
  
  @Test 
  public void testDefaultsReturnedByDefault() throws Exception { 
    Bundle b = bundleMock("bundle.name;x=y;p:=q;blueprint.graceperiod:=true;a=b;c:=d");
    int timeout = BlueprintURLContext.getGracePeriod(b);
    assertEquals ("graceperiod wrong", 300000, timeout);
    
    b = bundleMock ("bundle.name;x=y;p:=q;a=b;c:=d");
    timeout = BlueprintURLContext.getGracePeriod(b);
    assertEquals ("graceperiod wrong", 300000, timeout);
  }
  
  Bundle bundleMock (String bundleSymbolicNameHeader) { 
    Hashtable<String, String> props = new Hashtable<String, String>();
    props.put(Constants.BUNDLE_SYMBOLICNAME, bundleSymbolicNameHeader);
    Bundle result = Skeleton.newMock(new BundleMock("aBundle", props), Bundle.class);
    return result;
  }
}
