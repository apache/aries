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
package org.apache.aries.jpa.container.weaving;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.persistence.spi.ClassTransformer;

import org.apache.aries.jpa.container.weaving.impl.JPAWeavingHook;
import org.apache.aries.jpa.container.weaving.impl.TransformerRegistryFactory;
import org.apache.aries.mocks.BundleMock;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;

public class JPAWeavingHookTest {

  private Bundle b1;
  
  private ClassTransformer ct1a;
  
  
  @Before
  public void setup() {
    b1 = Skeleton.newMock(new BundleMock("bundle1", new Hashtable()), Bundle.class);
    ct1a = Skeleton.newMock(ClassTransformer.class);
  }
  
  @After
  public void teardown() {
    Skeleton.getSkeleton(b1).clearMethodCalls();
    Skeleton.getSkeleton(ct1a).clearMethodCalls();
  }
  
  @Test
  public void testFactory() {
    assertSame(JPAWeavingHook.class, 
        TransformerRegistryFactory.getTransformerRegistry().getClass());
    
    assertSame(TransformerRegistryFactory.getTransformerRegistry().getClass(), 
        TransformerRegistryFactory.getTransformerRegistry().getClass());
  }
  
  
  private ServiceReference basicProviderSetup(String[] weavingPackages) {
    ServiceReference provider1 = Skeleton.newMock(ServiceReference.class);
	     
    Skeleton.getSkeleton(provider1).setReturnValue(new MethodCall(ServiceReference.class, 
        "getProperty", "org.apache.aries.jpa.container.weaving.packages"), weavingPackages);
    
    Skeleton.getSkeleton(provider1).setReturnValue(new MethodCall(Bundle.class,
        "adapt", BundleWiring.class), Skeleton.getSkeleton(provider1).createMock(BundleWiring.class));
	     
    Skeleton.getSkeleton(provider1).setReturnValue(new MethodCall(Bundle.class,
        "getSymbolicName"), b1.getSymbolicName());
	     
    Skeleton.getSkeleton(provider1).setReturnValue(new MethodCall(Bundle.class,
        "getVersion"), b1.getVersion());
	     
    Skeleton.getSkeleton(provider1).setReturnValue(new MethodCall(BundleRevision.class, 
        "getDeclaredCapabilities", BundleRevision.PACKAGE_NAMESPACE), 
        Collections.singletonList(Skeleton.getSkeleton(provider1).createMock(BundleCapability.class)));
    Map m = new HashMap();
    m.put(BundleRevision.PACKAGE_NAMESPACE, "foundPackage");
    
    Skeleton.getSkeleton(provider1).setReturnValue(new MethodCall(BundleCapability.class,
        "getAttributes"), m);
    return provider1;
  }
  
  @Test
  public void testNotWoven() {
    ServiceReference provider1 = basicProviderSetup(null);
    JPAWeavingHook tr = (JPAWeavingHook) TransformerRegistryFactory.getTransformerRegistry();
     
    //Add a transforme and check it gets called to weave
    tr.addTransformer(b1, ct1a, provider1);
     
    ProtectionDomain pd = new ProtectionDomain(null, null);
    byte[] bytes = new byte[] {(byte) 0xAB, (byte)0xCD};

    WovenClass wc = getWovenClass(b1, "test1", bytes, this.getClass().getClassLoader(),
        this.getClass(), pd);
	     
    tr.weave(wc);
	     
    Skeleton.getSkeleton(ct1a).assertCalled(new MethodCall(ClassTransformer.class, "transform",
        this.getClass().getClassLoader(), "test1", this.getClass(), pd, bytes));
	     
     assertNotCalled(wc);
  }
  
  
  @Test
  public void testWeavingWrongBundle() {
    Bundle b2 = Skeleton.newMock(new BundleMock("bundle2", null), Bundle.class);
    ServiceReference provider1 = basicProviderSetup(null);
    JPAWeavingHook tr = (JPAWeavingHook) TransformerRegistryFactory.getTransformerRegistry();
	     
    //Add a transforme and check it gets called to weave
    tr.addTransformer(b1, ct1a, provider1);
	     
    ProtectionDomain pd = new ProtectionDomain(null, null);
    byte[] bytes = new byte[] {(byte) 0xAB, (byte)0xCD};

    WovenClass wc = getWovenClass(b2, "test2", bytes, this.getClass().getClassLoader(),
        this.getClass(), pd);
	     
    tr.weave(wc);
	     
    assertNotCalled(wc);
	     
    Skeleton.getSkeleton(ct1a).assertNotCalled(new MethodCall(ClassTransformer.class, "transform",
        ClassLoader.class, String.class, Class.class, ProtectionDomain.class, byte[].class));
  }
  
  @Test
  public void testBasicWeaving() {
    ServiceReference provider1 = basicProviderSetup(null);
    JPAWeavingHook tr = (JPAWeavingHook) TransformerRegistryFactory.getTransformerRegistry();
	     
    //Add a transforme and check it gets called to weave
    tr.addTransformer(b1, ct1a, provider1);

    ProtectionDomain pd = new ProtectionDomain(null, null);
    byte[] bytes = new byte[] {(byte) 0xBE, (byte)0xAD};
	     
    byte[] returnedBytes = new byte[] {(byte) 0xBA, (byte)0xDE};
	     
    Skeleton.getSkeleton(ct1a).setReturnValue(new MethodCall(ClassTransformer.class, "transform",
        this.getClass().getClassLoader(), "test3", this.getClass(), pd, bytes), returnedBytes);
	     
    WovenClass wc = getWovenClass(b1, "test3", bytes, this.getClass().getClassLoader(),
        this.getClass(), pd);
	     
    tr.weave(wc);
	     
    Skeleton.getSkeleton(ct1a).assertCalled(new MethodCall(ClassTransformer.class, "transform",
        this.getClass().getClassLoader(), "test3", this.getClass(), pd, bytes));
	     
    assertCalled(wc, returnedBytes, b1, "foundPackage;" + Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE + "=bundle1;" + 
        Constants.BUNDLE_VERSION_ATTRIBUTE + "=0.0.0");
  }
  
  @Test
  public void testMultipleTransformers() {
    ClassTransformer ct1b = Skeleton.newMock(ClassTransformer.class);
    ServiceReference provider1 = basicProviderSetup(null);
    JPAWeavingHook tr = (JPAWeavingHook) TransformerRegistryFactory.getTransformerRegistry();
	     
    //Add a transforme and check it gets called to weave
    tr.addTransformer(b1, ct1a, provider1);
    //Check a second weaver isn't called for a when the first gives a result
    tr.addTransformer(b1, ct1b, provider1);

    ProtectionDomain pd = new ProtectionDomain(null, null);
    byte[] bytes = new byte[] {(byte) 0xBE, (byte)0xAD};
	     
    byte[] returnedBytes = new byte[] {(byte) 0xBA, (byte)0xDE};
	     
    Skeleton.getSkeleton(ct1a).setReturnValue(new MethodCall(ClassTransformer.class, "transform",
        this.getClass().getClassLoader(), "test3", this.getClass(), pd, bytes), returnedBytes);
	     
    WovenClass wc = getWovenClass(b1, "test3", bytes, this.getClass().getClassLoader(),
        this.getClass(), pd);
	     
    tr.weave(wc);
	     
    Skeleton.getSkeleton(ct1a).assertCalled(new MethodCall(ClassTransformer.class, "transform",
        this.getClass().getClassLoader(), "test3", this.getClass(), pd, bytes));
	     
    Skeleton.getSkeleton(ct1b).assertNotCalled(new MethodCall(ClassTransformer.class, "transform",
        ClassLoader.class, String.class, Class.class, ProtectionDomain.class, byte[].class));
	     
    assertCalled(wc, returnedBytes, b1, "foundPackage;" + Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE + "=bundle1;" + 
        Constants.BUNDLE_VERSION_ATTRIBUTE + "=0.0.0");
  }
  
  @Test
  public void testMultipleTransformers2() {
    ClassTransformer ct1b = Skeleton.newMock(ClassTransformer.class);
    ServiceReference provider1 = basicProviderSetup(null);
    JPAWeavingHook tr = (JPAWeavingHook) TransformerRegistryFactory.getTransformerRegistry();
    
    //Add a two transformers that will return null
    tr.addTransformer(b1, ct1a, provider1);
    tr.addTransformer(b1, ct1b, provider1);
    
    ProtectionDomain pd = new ProtectionDomain(null, null);
    byte[] bytes = new byte[] {(byte) 0xBE, (byte)0xAD};
    
    WovenClass wc = getWovenClass(b1, "test4", bytes, this.getClass().getClassLoader(),
        this.getClass(), pd);
    
    tr.weave(wc);
    
    Skeleton.getSkeleton(ct1a).assertCalled(new MethodCall(ClassTransformer.class, "transform",
        this.getClass().getClassLoader(), "test4", this.getClass(), pd, bytes));
    
    Skeleton.getSkeleton(ct1b).assertCalled(new MethodCall(ClassTransformer.class, "transform",
        this.getClass().getClassLoader(), "test4", this.getClass(), pd, bytes));
    
    assertNotCalled(wc);
  }
  
  
  @Test
  public void testMultipleTransformers3() {
    ClassTransformer ct1b = Skeleton.newMock(ClassTransformer.class);
    ServiceReference provider1 = basicProviderSetup(null);
    JPAWeavingHook tr = (JPAWeavingHook) TransformerRegistryFactory.getTransformerRegistry();
	    
    //Add a two transformers that will return null
    tr.addTransformer(b1, ct1a, provider1);
    tr.addTransformer(b1, ct1b, provider1);
	    
    tr.removeTransformer(b1, ct1a);
	    
    ProtectionDomain pd = new ProtectionDomain(null, null);
    byte[] bytes = new byte[] {(byte) 0xBE, (byte)0xAD};
	    
    WovenClass wc = getWovenClass(b1, "test5", bytes, this.getClass().getClassLoader(),
           this.getClass(), pd);
	    
    tr.weave(wc);
	    
    Skeleton.getSkeleton(ct1a).assertNotCalled(new MethodCall(ClassTransformer.class, "transform",
            ClassLoader.class, String.class, Class.class, ProtectionDomain.class, byte[].class));
	         
    Skeleton.getSkeleton(ct1b).assertCalled(new MethodCall(ClassTransformer.class, "transform",
            this.getClass().getClassLoader(), "test5", this.getClass(), pd, bytes));
	         
    assertNotCalled(wc);
	    
  }
  
  @Test
  public void testWeavingPackages() {
    ClassTransformer ct1b = Skeleton.newMock(ClassTransformer.class);
    ServiceReference provider1 = basicProviderSetup(new String[] {"specifiedPackage1", "specifiedPackage2;attribute=value"});
    JPAWeavingHook tr = (JPAWeavingHook) TransformerRegistryFactory.getTransformerRegistry();
	     
    //Add a transforme and check it gets called to weave
    tr.addTransformer(b1, ct1a, provider1);
    //Check a second weaver isn't called for a when the first gives a result
    tr.addTransformer(b1, ct1b, provider1);

    ProtectionDomain pd = new ProtectionDomain(null, null);
    byte[] bytes = new byte[] {(byte) 0xBE, (byte)0xAD};
	     
    byte[] returnedBytes = new byte[] {(byte) 0xBA, (byte)0xDE};
	     
    Skeleton.getSkeleton(ct1a).setReturnValue(new MethodCall(ClassTransformer.class, "transform",
        this.getClass().getClassLoader(), "test3", this.getClass(), pd, bytes), returnedBytes);
	     
    WovenClass wc = getWovenClass(b1, "test3", bytes, this.getClass().getClassLoader(),
        this.getClass(), pd);
	     
    tr.weave(wc);
	     
    Skeleton.getSkeleton(ct1a).assertCalled(new MethodCall(ClassTransformer.class, "transform",
        this.getClass().getClassLoader(), "test3", this.getClass(), pd, bytes));
	     
    Skeleton.getSkeleton(ct1b).assertNotCalled(new MethodCall(ClassTransformer.class, "transform",
        ClassLoader.class, String.class, Class.class, ProtectionDomain.class, byte[].class));
	     
    assertCalled(wc, returnedBytes, b1, "specifiedPackage1", "specifiedPackage2;attribute=value");
  }
  
  private void assertCalled(WovenClass wc, byte[] returnedBytes, Bundle b,
      String... string) {
    Skeleton.getSkeleton(wc).assertCalledExactNumberOfTimes(new MethodCall(WovenClass.class, 
        "setBytes", byte[].class), 1);
    Skeleton.getSkeleton(wc).assertCalled(new MethodCall(WovenClass.class, 
        "setBytes", returnedBytes));
       
    assertSame(string.length, wc.getDynamicImports().size());
    for(String s : string) {
      assertTrue(wc.getDynamicImports().contains(s));
    }
  }

  private void assertNotCalled(WovenClass wc) {
    Skeleton.getSkeleton(wc).assertNotCalled(new MethodCall(WovenClass.class, 
        "setBytes", byte[].class));
    
    Skeleton.getSkeleton(wc).assertNotCalled(new MethodCall(List.class, 
        "add", String.class));
  }

  private WovenClass getWovenClass(Bundle b, String name, byte[] bytes, ClassLoader cl, Class c, 
      ProtectionDomain pd ) {
    
    WovenClass result = Skeleton.newMock(WovenClass.class);
    Skeleton.getSkeleton(result).setReturnValue(new MethodCall(BundleWiring.class,
        "getBundle"), b);
    Skeleton.getSkeleton(result).setReturnValue(new MethodCall(BundleWiring.class,
        "getClassLoader"), cl);
    Skeleton.getSkeleton(result).setReturnValue(new MethodCall(WovenClass.class,
        "getBytes"), bytes);
    Skeleton.getSkeleton(result).setReturnValue(new MethodCall(WovenClass.class,
        "getClassName"), name);
    Skeleton.getSkeleton(result).setReturnValue(new MethodCall(WovenClass.class,
         "getProtectionDomain"), pd);
    Skeleton.getSkeleton(result).setReturnValue(new MethodCall(WovenClass.class,
         "getDefinedClass"), c);
    
    Skeleton.getSkeleton(result).setReturnValue(new MethodCall(WovenClass.class,
        "getDynamicImports"), new ArrayList<String>());
    
    return result;
  }
  
}
