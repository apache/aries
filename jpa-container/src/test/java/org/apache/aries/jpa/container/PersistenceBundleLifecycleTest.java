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


package org.apache.aries.jpa.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import org.apache.aries.jpa.container.impl.PersistenceBundleManager;
import org.apache.aries.mocks.BundleContextMock;
import org.apache.aries.mocks.BundleMock;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

public class PersistenceBundleLifecycleTest
{
  private static final String FRAGMENT_SYM_NAME = "scooby.doo.jpa.fragment";
  
  private Bundle persistenceBundle;
  
  private Bundle providerBundleP100;
  private Bundle providerBundleP101;
  private Bundle providerBundleP110;
  private Bundle providerBundleP111;
  
  private Bundle extenderBundle;
  
  @Before
  public void setUp() throws Exception
  {
    persistenceBundle = Skeleton.newMock(new BundleMock("scooby.doo", new Hashtable<String, Object>()), Bundle.class);
    
    providerBundleP100 = Skeleton.newMock(new BundleMock("no.such.Provider", new Hashtable<String, Object>()), Bundle.class);
    Skeleton skel = Skeleton.getSkeleton(providerBundleP100);
    skel.setReturnValue(new MethodCall(Bundle.class, "getVersion"), new Version("1.0.0"));

    providerBundleP101 = Skeleton.newMock(new BundleMock("no.such.Provider", new Hashtable<String, Object>()), Bundle.class);
    skel = Skeleton.getSkeleton(providerBundleP101);
    skel.setReturnValue(new MethodCall(Bundle.class, "getVersion"), new Version("1.0.1"));

    providerBundleP110 = Skeleton.newMock(new BundleMock("no.such.Provider", new Hashtable<String, Object>()), Bundle.class);
    skel = Skeleton.getSkeleton(providerBundleP110);
    skel.setReturnValue(new MethodCall(Bundle.class, "getVersion"), new Version("1.1.0"));

    providerBundleP111 = Skeleton.newMock(new BundleMock("no.such.Provider", new Hashtable<String, Object>()), Bundle.class);
    skel = Skeleton.getSkeleton(providerBundleP111);
    skel.setReturnValue(new MethodCall(Bundle.class, "getVersion"), new Version("1.1.1"));

    extenderBundle = Skeleton.newMock(new BundleMock("extender", new Hashtable<String, Object>()), Bundle.class);
    Skeleton.getSkeleton(extenderBundle).setReturnValue(new MethodCall(Bundle.class, "getResource", "schemas/persistence_1_0.xsd"), new File("unittest/resources/persistence_1_0.xsd").toURI().toURL());
    persistenceBundle = Skeleton.newMock(new BundleMock("scooby.doo", new Hashtable<String, Object>()), Bundle.class);
  }

  @After
  public void destroy() throws Exception
  {
    BundleContextMock.clear();
  }
  
  @Test
  public void testManager_OnePreExistingPersistenceBundle_NoProvider() throws Exception
  {
    BundleContext ctx = extenderBundle.getBundleContext();

    PersistenceBundleManager mgr = new PersistenceBundleManager(ctx);
    
    setupPersistenceBundle("unittest/resources/file4/");
    
    mgr.addingBundle(persistenceBundle, null);
    
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
    
  }
//
//  @Test
//  public void testManagerStartOneExistingPersistenceBundleOneExistingProvider() throws Exception
//  {
//    //Check we don't register anything (the bundle was installed before we started)
//
//    PersistenceProvider pp = Skeleton.newMock(PersistenceProvider.class);
//    
//    Hashtable<String,String> hash1 = new Hashtable<String, String>();
//    persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
//        pp, hash1 );
//    
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    Skeleton.getSkeleton(ctx).setReturnValue(
//        new MethodCall(BundleContext.class, "getBundles"),
//        new Bundle[] {persistenceBundle});
//    
//    setupPersistenceBundle("unittest/resources/file4/");
//    
//    mgr.start(ctx);
//    
//    BundleContextMock.assertNoServiceExists(PersistenceUnitInfoService.class.getName());
//  }
//  
//  @Test
//  public void testManagerStopUnregistersUnits() throws Exception
//  {
//    PersistenceProvider pp = Skeleton.newMock(PersistenceProvider.class);
//    
//    Hashtable<String,String> hash1 = new Hashtable<String, String>();
//    persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
//        pp, hash1 );
//    
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file4/");
//    mgr.start(ctx);
//
//    testSuccessfulInstalledEvent(mgr, ctx, 1);
//    
//    mgr.stop(ctx);
//    
//    BundleContextMock.assertNoServiceExists(PersistenceUnitInfoService.class.getName());
//  }
//
//  @Test
//  public void testBundleChangedInstalledOnePreexistingProvider() throws Exception
//  {
//    //Check we correctly parse and register Persistence Units
//
//    PersistenceProvider pp = Skeleton.newMock(PersistenceProvider.class);
//    
//    Hashtable<String,String> hash1 = new Hashtable<String, String>();
//    persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
//        pp, hash1 );
//    
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file4/");
//    mgr.start(ctx);
//
//    testSuccessfulInstalledEvent(mgr, ctx, 1);
//  }
//  
//  @Test
//  public void testBundleChangedUninstalled() throws Exception
//  {
//    PersistenceProvider pp = Skeleton.newMock(PersistenceProvider.class);
//    
//    Hashtable<String,String> hash1 = new Hashtable<String, String>();
//    persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
//        pp, hash1 );
//    
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    Bundle fragment = Skeleton.newMock(Bundle.class);
//    
//    Skeleton.getSkeleton(ctx).setReturnValue(new MethodCall(BundleContext.class, "installBundle",
//        FRAGMENT_SYM_NAME, InputStream.class), fragment);
//    
//    setupPersistenceBundle("unittest/resources/file4/");
//    mgr.start(ctx);
//
//    testSuccessfulInstalledEvent(mgr, ctx, 1);
//    
//    mgr.bundleChanged(new BundleEvent(BundleEvent.UNINSTALLED, persistenceBundle));
//    
//    BundleContextMock.assertNoServiceExists(PersistenceUnitInfoService.class.getName());
//    
//    Skeleton.getSkeleton(fragment).assertCalled(new MethodCall(Bundle.class, "uninstall"));
//  }
//  
//  @Test
//  public void testBundleChangedUpdated() throws Exception
//  {
//    PersistenceProvider pp = Skeleton.newMock(PersistenceProvider.class);
//    
//    Hashtable<String,String> hash1 = new Hashtable<String, String>();
//    persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
//        pp, hash1 );
//    
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    Bundle fragment = Skeleton.newMock(Bundle.class);
//    
//    Skeleton.getSkeleton(ctx).setReturnValue(new MethodCall(BundleContext.class, "installBundle",
//        FRAGMENT_SYM_NAME, InputStream.class), fragment);
//    
//    setupPersistenceBundle("unittest/resources/file4/");
//    mgr.start(ctx);
//
//    testSuccessfulInstalledEvent(mgr, ctx, 1);
//    
//    Skeleton.getSkeleton(ctx).clearMethodCalls();
//    
//    mgr.bundleChanged(new BundleEvent(BundleEvent.UPDATED, persistenceBundle));
//    
//    BundleContextMock.assertServiceExists(PersistenceUnitInfoService.class.getName());
//    
//    ServiceReference[] refs = ctx.getServiceReferences(PersistenceUnitInfoService.class.getName(), null);
//    
//    assertEquals("Too many persistence units registered", 1, refs.length);
//    
//    Skeleton.getSkeleton(fragment).assertCalled(new MethodCall(Bundle.class, "uninstall"));
//    Skeleton.getSkeleton(ctx).assertCalled(new MethodCall(BundleContext.class, "installBundle",
//        FRAGMENT_SYM_NAME, InputStream.class));
//  }
//  
//  @Test
//  public void testBundleChangedUnresolved() throws Exception
//  {
//    PersistenceProvider pp = Skeleton.newMock(PersistenceProvider.class);
//    
//    Hashtable<String,String> hash1 = new Hashtable<String, String>();
//    persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
//        pp, hash1 );
//    
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    Bundle fragment = Skeleton.newMock(Bundle.class);
//    
//    Skeleton.getSkeleton(ctx).setReturnValue(new MethodCall(BundleContext.class, "installBundle",
//        FRAGMENT_SYM_NAME, InputStream.class), fragment);
//    
//    setupPersistenceBundle("unittest/resources/file4/");
//    mgr.start(ctx);
//
//    testSuccessfulInstalledEvent(mgr, ctx, 1);
//    
//    Skeleton.getSkeleton(ctx).clearMethodCalls();
//    
//    mgr.bundleChanged(new BundleEvent(BundleEvent.UNRESOLVED, persistenceBundle));
//    
//    BundleContextMock.assertServiceExists(PersistenceUnitInfoService.class.getName());
//    
//    ServiceReference[] refs = ctx.getServiceReferences(PersistenceUnitInfoService.class.getName(), null);
//    
//    assertEquals("Too many persistence units registered", 1, refs.length);
//    
//    Skeleton.getSkeleton(fragment).assertCalled(new MethodCall(Bundle.class, "uninstall"));
//    Skeleton.getSkeleton(ctx).assertCalled(new MethodCall(BundleContext.class, "installBundle",
//        FRAGMENT_SYM_NAME, InputStream.class));
//  }
//  
//  @Test
//  public void testBundleChangedInstalledOnePostRegisteredProvider() throws Exception
//  {
//    //Check we correctly parse and register Persistence Units
//    
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file4/");
//    mgr.start(ctx);
//
//    BundleContextMock.assertNoServiceExists(PersistenceUnitInfoService.class.getName());
//
//    PersistenceProvider pp = Skeleton.newMock(PersistenceProvider.class);
//    
//    Hashtable<String,String> hash1 = new Hashtable<String, String>();
//    persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
//        pp, hash1 );
//    
//    testSuccessfulInstalledEvent(mgr, ctx, 1);
//  }
//  
//  @Test
//  public void testBundleChangedInstalledNoProvider() throws Exception
//  {
//    //Check we correctly parse and register Persistence Units
//    
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file4/");
//    mgr.start(ctx);
//
//    testUnsuccessfulInstalledEvent(mgr, ctx);
//  }
//  
//  @Test
//  public void testInstalledWithBadXML() throws Exception
//  {
//    //Check we correctly parse and register EMFactories
//
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file3/");
//    
//    mgr.start(ctx);
//    
//    testUnsuccessfulInstalledEvent(mgr, ctx);
//  }
//
//  
//  @Test
//  public void testBundleChangedStarting() throws Exception
//  {
//    //Check we correctly don't do anything on the started event
//
//    PersistenceProvider pp = Skeleton.newMock(PersistenceProvider.class);
//    
//    Hashtable<String,String> hash1 = new Hashtable<String, String>();
//    persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
//        pp, hash1 );
//    
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file4/");
//
//    mgr.start(ctx);
//    
//    testSuccessfulInstalledEvent(mgr, ctx, 1);
//    
//    mgr.bundleChanged(new BundleEvent(BundleEvent.STARTING, persistenceBundle));
//
//    BundleContextMock.assertServiceExists(PersistenceUnitInfoService.class.getName());
//    
//    ServiceReference[] refs = persistenceBundle.getBundleContext().getServiceReferences(PersistenceUnitInfoService.class.getName(), null);
//    
//    assertEquals("The wrong number of persistence units were registered", 1, refs.length);
// 
//  }
//
//  @Test
//  public void testBundleChangedLazyActivation() throws Exception
//  {
//  //Check we correctly don't do anything on the LAZY event
//
//    PersistenceProvider pp = Skeleton.newMock(PersistenceProvider.class);
//    
//    Hashtable<String,String> hash1 = new Hashtable<String, String>();
//    persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
//        pp, hash1 );
//    
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file4/");
//
//    mgr.start(ctx);
//    
//    testSuccessfulInstalledEvent(mgr, ctx, 1);
//    
//    mgr.bundleChanged(new BundleEvent(BundleEvent.LAZY_ACTIVATION, persistenceBundle));
//
//    BundleContextMock.assertServiceExists(PersistenceUnitInfoService.class.getName());
//    
//    ServiceReference[] refs = persistenceBundle.getBundleContext().getServiceReferences(PersistenceUnitInfoService.class.getName(), null);
//    
//    assertEquals("The wrong number of persistence units were registered", 1, refs.length);
//   }
//    
//  @Test
//  public void testNoProviderInstalled() throws Exception
//  {
//    //Check we do not register a service when there is no Provider
//    
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file5/");
//    
//    mgr.start(ctx);
//    
//    testUnsuccessfulInstalledEvent(mgr, ctx);
//  }
//
//  @Test
//  public void testdefaultProvider() throws Exception
//  {
//    //Check we correctly parse and register EMFactories
//
//    PersistenceProvider pp = Skeleton.newMock(PersistenceProvider.class);
//    
//    Hashtable<String,String> hash1 = new Hashtable<String, String>();
//    persistenceBundle.getBundleContext().registerService(new String[] {PersistenceBundleManager.DEFAULT_JPA_PROVIDER} ,
//        pp, hash1 );
//    
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file5/");
//    
//    mgr.start(ctx);
//    
//    testSuccessfulInstalledEvent(mgr, ctx, 1);
//
//  }
//  
//  @Test
//  public void testdefaultProviderWithWAR() throws Exception
//  {
//    //Check we correctly parse and register EMFactories
//
//    PersistenceProvider pp = Skeleton.newMock(PersistenceProvider.class);
//    
//    Hashtable<String,String> hash1 = new Hashtable<String, String>();
//    persistenceBundle.getBundleContext().registerService(new String[] {PersistenceBundleManager.DEFAULT_JPA_PROVIDER} ,
//        pp, hash1 );
//    
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    Skeleton skel = Skeleton.getSkeleton(persistenceBundle);
//    
//    skel.setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.ACTIVE);
//    
//    URL root = new File("unittest/resources/file5/").toURI().toURL();
//    URL xml = new File("unittest/resources/file5/META-INF/persistence.xml").toURI().toURL();
//    
//    skel.setReturnValue(new MethodCall(Bundle.class, "getEntry", "WEB-INF/classes/"), root);
//    skel.setReturnValue(new MethodCall(Bundle.class, "getEntry", "WEB-INF/classes/META-INF/persistence.xml"), xml);
//    skel.setReturnValue(new MethodCall(Bundle.class, "getVersion"), new Version("0.0.0"));
//
//    mgr.start(ctx);
//    
//    testSuccessfulInstalledEvent(mgr, ctx, 1);
//  }
//
//  @Test
//  public void testdefaultProviderWithWARLib() throws Exception
//  {
//    //Check we correctly parse and register EMFactories
//
//    PersistenceProvider pp = Skeleton.newMock(PersistenceProvider.class);
//    
//    Hashtable<String,String> hash1 = new Hashtable<String, String>();
//    persistenceBundle.getBundleContext().registerService(new String[] {PersistenceBundleManager.DEFAULT_JPA_PROVIDER} ,
//        pp, hash1 );
//    
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    Skeleton skel = Skeleton.getSkeleton(persistenceBundle);
//    
//    skel.setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.ACTIVE);
//    
//    Vector<String> v = new Vector<String>();
//      v.add("WEB-INF/lib/jarfile.jar");
//    
//    skel.setReturnValue(new MethodCall(Bundle.class, "getEntryPaths", "WEB-INF/lib"), v.elements());
//    skel.setReturnValue(new MethodCall(Bundle.class, "getVersion"), new Version("0.0.0"));
//    skel.setReturnValue(new MethodCall(Bundle.class, "getEntry", "WEB-INF/lib/jarfile.jar"), new File("unittest/resources/jarfile.jar").toURI().toURL());
//
//    mgr.start(ctx);
//    
//    testSuccessfulInstalledEvent(mgr, ctx, 1);
//  }
//  
//  @Test
//  public void testdefaultProviderWithBundledJar() throws Exception
//  {
//    //Check we correctly parse and register EMFactories
//
//    PersistenceProvider pp = Skeleton.newMock(PersistenceProvider.class);
//    
//    Hashtable<String,String> hash1 = new Hashtable<String, String>();
//    persistenceBundle.getBundleContext().registerService(new String[] {PersistenceBundleManager.DEFAULT_JPA_PROVIDER} ,
//        pp, hash1 );
//    
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    Skeleton skel = Skeleton.getSkeleton(persistenceBundle);
//    
//    skel.setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.ACTIVE);
//    
//    URL u = new File("unittest/resources/jarfile.jar").toURI().toURL();
//    
//    skel.setReturnValue(new MethodCall(Bundle.class, "getEntry", "unittest/resources/jarfile.jar"), u);
//    skel.setReturnValue(new MethodCall(Bundle.class, "getVersion"), new Version("0.0.0"));
//    persistenceBundle.getHeaders().put(Constants.BUNDLE_CLASSPATH, "., unittest/resources/jarfile.jar");
//
//    mgr.start(ctx);
//    
//    testSuccessfulInstalledEvent(mgr, ctx, 1);
//
//  }
//  
//  @Test
//  public void testSameProviders() throws Exception
//  {
//    //Check we behave correctly when two persistence units define the same
//    //provder name
//
//    PersistenceProvider pp = Skeleton.newMock(PersistenceProvider.class);
//    
//    Hashtable<String,String> hash1 = new Hashtable<String, String>();
//    persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
//        pp, hash1 );
//    
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file6/");
//    
//    mgr.start(ctx);
//    
//    testSuccessfulInstalledEvent(mgr, ctx, 2);
//  }
//
//  @Test
//  public void testOneWithProviderOneWithout() throws Exception
//  {
//    //Check we behave correctly when two persistence units define different
//    //provder names
//
//    PersistenceProvider pp = Skeleton.newMock(PersistenceProvider.class);
//    
//    Hashtable<String,String> hash1 = new Hashtable<String, String>();
//    persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
//        pp, hash1 );
//    
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file7/");
//    
//    mgr.start(ctx);
//    
//    BundleContextMock.assertNoServiceExists(PersistenceUnitInfoService.class.getName());
//    
//    mgr.bundleChanged(new BundleEvent(BundleEvent.INSTALLED, persistenceBundle));
//    
//    BundleContextMock.assertServiceExists(PersistenceUnitInfoService.class.getName());
//    
//    ServiceReference[] refs = persistenceBundle.getBundleContext().getServiceReferences(
//        PersistenceUnitInfoService.class.getName(), null);
//       
//    assertEquals("The wrong number of EMFs were registered", 2, refs.length);
//    
//    Skeleton.getSkeleton(ctx).assertCalled(new MethodCall(BundleContext.class, "installBundle",
//        FRAGMENT_SYM_NAME, InputStream.class));
//  }
//
//  @Test
//  public void testTwoProviders() throws Exception
//  {
//    //Check we correctly parse and register EMFactories
//
//    PersistenceProvider pp = Skeleton.newMock(PersistenceProvider.class);
//    
//    Hashtable<String,String> hash1 = new Hashtable<String, String>();
//    persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
//        pp, hash1 );
//    
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    Skeleton skel = Skeleton.getSkeleton(persistenceBundle);
//    
//    skel.setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.ACTIVE);
//    
//    Vector<URL> v = new Vector<URL>();
//    v.add(new File("unittest/resources/file8/META-INF/persistence.xml").toURI().toURL());
//    
//    skel.setReturnValue(new MethodCall(Bundle.class, "findEntries", "/", "persistence.xml", true), v.elements());
//    skel.setReturnValue(new MethodCall(Bundle.class, "getVersion"), new Version("0.0.0"));
//    
//    mgr.start(ctx);
//    
//    BundleContextMock.assertNoServiceExists(PersistenceUnitInfoService.class.getName());
//    
//    mgr.bundleChanged(new BundleEvent(BundleEvent.INSTALLED, persistenceBundle));
//    
//    BundleContextMock.assertNoServiceExists(PersistenceUnitInfoService.class.getName());
//    
//    Skeleton.getSkeleton(ctx).assertNotCalled(new MethodCall(BundleContext.class, "installBundle",
//        FRAGMENT_SYM_NAME, InputStream.class));
//  }
//  
//  @Test
//  public void testpp100() throws Exception
//  {
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//     
//    PersistenceProvider pp100 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp101 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp110 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp111 = Skeleton.newMock(PersistenceProvider.class);
//    
//    registerVersionedPersistenceProviders(pp100, pp101, pp110, pp111);
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file9/");
//
//    assertCorrectPersistenceProviderUsed(mgr, ctx, pp100);
//  }
//
//  @Test
//  public void testpp101() throws Exception
//  {
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    PersistenceProvider pp100 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp101 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp110 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp111 = Skeleton.newMock(PersistenceProvider.class);
//    
//    registerVersionedPersistenceProviders(pp100, pp101, pp110, pp111);
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file10/");
//    
//    mgr.start(ctx);
//    
//    assertCorrectPersistenceProviderUsed(mgr, ctx, pp101);
//  }
//  
//  @Test
//  public void testpp101b() throws Exception
//  {
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    PersistenceProvider pp100 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp101 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp110 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp111 = Skeleton.newMock(PersistenceProvider.class);
//    
//    registerVersionedPersistenceProviders(pp100, pp101, pp110, pp111);
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file11/");
//
//    assertCorrectPersistenceProviderUsed(mgr, ctx, pp101);
//
//  }
//
//  @Test
//  public void testpp111() throws Exception
//  {
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    PersistenceProvider pp100 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp101 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp110 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp111 = Skeleton.newMock(PersistenceProvider.class);
//    
//    registerVersionedPersistenceProviders(pp100, pp101, pp110, pp111);
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file12/");
//
//    assertCorrectPersistenceProviderUsed(mgr, ctx, pp111);
//  }
//
//  @Test
//  public void testppNoMatch() throws Exception
//  {
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    PersistenceProvider pp100 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp101 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp110 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp111 = Skeleton.newMock(PersistenceProvider.class);
//    
//    registerVersionedPersistenceProviders(pp100, pp101, pp110, pp111);
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file13/");
//    
//    mgr.start(ctx);
//    
//    testUnsuccessfulInstalledEvent(mgr,ctx);
//    
//  }
//
//  @Test
//  public void testTwoProvidersMatch() throws Exception
//  {
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    PersistenceProvider pp100 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp101 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp110 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp111 = Skeleton.newMock(PersistenceProvider.class);
//    
//    registerVersionedPersistenceProviders(pp100, pp101, pp110, pp111);
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file14/");
//    
//    mgr.start(ctx);
//    assertCorrectPersistenceProviderUsed(mgr, ctx, pp101);
//  }
//
//  @Test
//  public void testTwoProvidersNoVersionMatch() throws Exception
//  {
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    PersistenceProvider pp100 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp101 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp110 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp111 = Skeleton.newMock(PersistenceProvider.class);
//    
//    registerVersionedPersistenceProviders(pp100, pp101, pp110, pp111);
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file15/");
//    
//    mgr.start(ctx);
//    
//    testUnsuccessfulInstalledEvent(mgr,ctx);
//    
//  }
//
//  @Test
//  public void testTwoProvidersExistNoCommonVersion() throws Exception
//  {
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    PersistenceProvider pp100 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp101 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp110 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp111 = Skeleton.newMock(PersistenceProvider.class);
//    
//    registerVersionedPersistenceProviders(pp100, pp101, pp110, pp111);
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file17/");
//    
//    mgr.start(ctx);
//    
//    testUnsuccessfulInstalledEvent(mgr,ctx);
//    
//  }
//  
//  @Test
//  public void testThreeProvidersNoVersionMatch() throws Exception
//  {
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    PersistenceProvider pp100 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp101 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp110 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp111 = Skeleton.newMock(PersistenceProvider.class);
//    
//    registerVersionedPersistenceProviders(pp100, pp101, pp110, pp111);
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file16/");
//   
//    mgr.start(ctx);
//    
//    testUnsuccessfulInstalledEvent(mgr,ctx);
//    
//  }
//
//  @Test
//  public void testTwoProvidersNoVersionMatchOneExists() throws Exception
//  {
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    PersistenceProvider pp100 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp101 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp110 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp111 = Skeleton.newMock(PersistenceProvider.class);
//    
//    registerVersionedPersistenceProviders(pp100, pp101, pp110, pp111);
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    setupPersistenceBundle("unittest/resources/file18/");
//
//    mgr.start(ctx);
//    
//    testUnsuccessfulInstalledEvent(mgr,ctx);
//  }
//
//  @Test
//  public void testThreeProvidersNoVersionMatchOneExists() throws Exception
//  {
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    PersistenceProvider pp100 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp101 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp110 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp111 = Skeleton.newMock(PersistenceProvider.class);
//    
//    registerVersionedPersistenceProviders(pp100, pp101, pp110, pp111);
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    Skeleton skel = Skeleton.getSkeleton(persistenceBundle);
//    
//    skel.setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.ACTIVE);
//    
//    setupPersistenceBundle("unittest/resources/file19/");
//
//    mgr.start(ctx);
//    
//    testUnsuccessfulInstalledEvent(mgr,ctx);
//    
//  }
//
//  @Test
//  public void testThreeProvidersNoVersionMatchTwoExist() throws Exception
//  {
//    PersistenceBundleManager mgr = new PersistenceBundleManager();
//    
//    PersistenceProvider pp100 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp101 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp110 = Skeleton.newMock(PersistenceProvider.class);
//    PersistenceProvider pp111 = Skeleton.newMock(PersistenceProvider.class);
//    
//    registerVersionedPersistenceProviders(pp100, pp101, pp110, pp111);
//    
//    BundleContext ctx = extenderBundle.getBundleContext();
//    
//    Skeleton skel = Skeleton.getSkeleton(persistenceBundle);
//    
//    skel.setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.ACTIVE);
//    
//    setupPersistenceBundle("unittest/resources/file20/");
//
//    mgr.start(ctx);
//
//    testUnsuccessfulInstalledEvent(mgr, ctx);
//  }
// 
  private void setupPersistenceBundle(String s) throws MalformedURLException
  {
    Skeleton skel = Skeleton.getSkeleton(persistenceBundle);
    
    skel.setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.ACTIVE);
    
    URL root = new File(s).toURI().toURL();
    
    URL xml = new File(s + "META-INF/persistence.xml").toURI().toURL();
    
    skel.setReturnValue(new MethodCall(Bundle.class, "getEntry", "/"), root);
    skel.setReturnValue(new MethodCall(Bundle.class, "getEntry", "/META-INF/persistence.xml"), xml);
    skel.setReturnValue(new MethodCall(Bundle.class, "getVersion"), new Version("0.0.0"));

  }
//  
//  private void registerVersionedPersistenceProviders(PersistenceProvider pp100,
//      PersistenceProvider pp101, PersistenceProvider pp110,
//      PersistenceProvider pp111) {
//    Hashtable<String,String> hash1 = new Hashtable<String, String>();
//    hash1.put("osgi.jpa.provider.version", "1.0.0");
//    providerBundleP100.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
//        pp100, hash1 );
//    
//    hash1 = new Hashtable<String, String>();
//    hash1.put("osgi.jpa.provider.version", "1.0.1");
//    providerBundleP101.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
//        pp101, hash1 );
//    
//    hash1 = new Hashtable<String, String>();
//    hash1.put("osgi.jpa.provider.version", "1.1.0");
//    providerBundleP110.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
//        pp110, hash1 );
//    
//    hash1 = new Hashtable<String, String>();
//    hash1.put("osgi.jpa.provider.version", "1.1.1");
//    providerBundleP111.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
//        pp111, hash1 );
//  }
//  
//
//  private void testSuccessfulInstalledEvent(PersistenceBundleManager mgr, BundleContext ctx, int numberOfPersistenceUnits) throws InvalidSyntaxException
//  {
//    BundleContextMock.assertNoServiceExists(PersistenceUnitInfoService.class.getName());
//    
//    mgr.bundleChanged(new BundleEvent(BundleEvent.INSTALLED, persistenceBundle));
//    
//    BundleContextMock.assertServiceExists(PersistenceUnitInfoService.class.getName());
//    
//    ServiceReference[] refs = persistenceBundle.getBundleContext().getServiceReferences(PersistenceUnitInfoService.class.getName(), null);
//    
//    assertEquals("The wrong number of persistence units were registered", numberOfPersistenceUnits, refs.length);
//    
//    for(ServiceReference ref : refs) {
//      assertEquals("Incorrect properties registerered", "scooby.doo", ref.getProperty(PersistenceUnitInfoService.PERSISTENCE_BUNDLE_SYMBOLIC_NAME));
//      assertEquals("Incorrect properties registerered", Version.emptyVersion, ref.getProperty(PersistenceUnitInfoService.PERSISTENCE_BUNDLE_VERSION));
//      assertNotNull("Incorrect properties registerered", ref.getProperty(PersistenceUnitInfoService.PERSISTENCE_UNIT_NAME));
//    }
//    
//    Skeleton.getSkeleton(ctx).assertCalled(new MethodCall(BundleContext.class, "installBundle",
//        FRAGMENT_SYM_NAME, InputStream.class));
//  }
//  
//  private void testUnsuccessfulInstalledEvent(PersistenceBundleManager mgr, BundleContext ctx)
//  {
//    BundleContextMock.assertNoServiceExists(PersistenceUnitInfoService.class.getName());
//    
//    mgr.bundleChanged(new BundleEvent(BundleEvent.INSTALLED, persistenceBundle));
//    
//    BundleContextMock.assertNoServiceExists(PersistenceUnitInfoService.class.getName());
//    
//    Skeleton.getSkeleton(ctx).assertNotCalled(new MethodCall(BundleContext.class, "installBundle",
//        String.class, InputStream.class));
//  }
//  
//  private void assertCorrectPersistenceProviderUsed (PersistenceBundleManager mgr, BundleContext ctx, PersistenceProvider provider)
//  {
//    try {
//      mgr.start(ctx);
//      
//      BundleContextMock.assertNoServiceExists(PersistenceUnitInfoService.class.getName());
//      
//      mgr.bundleChanged(new BundleEvent(BundleEvent.INSTALLED, persistenceBundle));
//      
//      BundleContextMock.assertServiceExists(PersistenceUnitInfoService.class.getName());
//      ServiceReference[] refs = persistenceBundle.getBundleContext().getServiceReferences(PersistenceUnitInfoService.class.getName(), null);
//    
//      for(ServiceReference ref : refs) {
//        PersistenceUnitInfoService pu = (PersistenceUnitInfoService) persistenceBundle.getBundleContext().getService(ref);
//    
//        assertNotNull("No PersistenceUnit was registered", pu);
//    
//        Object pp = persistenceBundle.getBundleContext().getService(pu.getProviderReference());
//    
//        assertSame("The perstistnce unit was associated with the wrong persistence provider",
//        pp, provider);
//      }
//      
//      Skeleton.getSkeleton(ctx).assertCalled(new MethodCall(BundleContext.class, "installBundle",
//          FRAGMENT_SYM_NAME, InputStream.class));
//      
//    } catch (Exception e) {
//      throw new RuntimeException(e);
//    }
//  }

}

