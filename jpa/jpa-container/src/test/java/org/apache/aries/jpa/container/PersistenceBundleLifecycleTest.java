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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import org.apache.aries.jpa.container.impl.EntityManagerFactoryManager;
import org.apache.aries.jpa.container.impl.PersistenceBundleManager;
import org.apache.aries.jpa.container.util.FakeManagedPersistenceUnitFactory;
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
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;

public class PersistenceBundleLifecycleTest
{
  private static final String FRAGMENT_SYM_NAME = "scooby.doo.jpa.fragment";
  
  private Bundle persistenceBundle;
  private BundleContext persistenceBundleContext;
  
  private Bundle providerBundleP100;
  private Bundle providerBundleP101;
  private Bundle providerBundleP110;
  private Bundle providerBundleP111;

  private PersistenceProvider providerP100;
  private PersistenceProvider providerP101;
  private PersistenceProvider providerP110;
  private PersistenceProvider providerP111;
  
  private Bundle extenderBundle;
  
  private PersistenceBundleManager mgr;
  private PersistenceProvider pp;
  
  @Before
  public void setUp() throws Exception
  {
    persistenceBundle = Skeleton.newMock(new BundleMock("scooby.doo", new Hashtable<String, Object>()), Bundle.class);
    persistenceBundleContext = persistenceBundle.getBundleContext();
    
    pp = Skeleton.newMock(PersistenceProvider.class);
    
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
//    Skeleton.getSkeleton(extenderBundle).setReturnValue(new MethodCall(Bundle.class, "getResource", "schemas/persistence_1_0.xsd"), new File("unittest/resources/persistence_1_0.xsd").toURI().toURL());
  }

  @After
  public void destroy() throws Exception
  {
    mgr = null;
    BundleContextMock.clear();
  }
  
  private BundleContext preExistingBundleSetup() {
    
    BundleContext extenderContext = extenderBundle.getBundleContext();

    Skeleton.getSkeleton(extenderContext).setReturnValue(
        new MethodCall(BundleContext.class, "getBundles"),
        new Bundle[] {persistenceBundle});
    
    mgr = new PersistenceBundleManager(extenderContext);
    mgr.setConfig(new Properties());
    return extenderContext;
  }
  
  @Test
  public void testManager_NonPersistenceBundle() throws Exception
  {
    preExistingBundleSetup();
    mgr.open();

    //Check the persistence.xml was not looked for
    Skeleton.getSkeleton(persistenceBundle).assertNotCalled(new MethodCall(Bundle.class, "getEntry", String.class));
    //Check we didn't use getResource()
    Skeleton.getSkeleton(persistenceBundle).assertNotCalled(new MethodCall(Bundle.class, "getResource", String.class));
    //Check we don't have an EMF
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
    
    assertNull("We should not have an EntityManagerFactoryManager", getTrackedObject());
    
  }
  
  @Test
  public void testManager_OnePreExistingPersistenceBundle_NoProvider() throws Exception
  {
    preExistingBundleSetup();
    
    //Set the persistence.xml etc
    setupPersistenceBundle("file4", "");
    
    mgr.open();
    
    //Check the persistence.xml was looked for
    Skeleton.getSkeleton(persistenceBundle).assertCalled(new MethodCall(Bundle.class, "getEntry", "META-INF/persistence.xml"));
    //Check we didn't use getResource()
    Skeleton.getSkeleton(persistenceBundle).assertNotCalled(new MethodCall(Bundle.class, "getResource", String.class));
    //Check we don't have an EMF
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
    
    assertNull("We should not have an EntityManagerFactoryManager", getTrackedObject());
  }

  @Test
  public void testManager_OnePreExistingPersistenceBundle_OneExistingProvider() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    Hashtable<String,String> hash1 = new Hashtable<String, String>();
    hash1.put("javax.persistence.provider", "no.such.Provider");
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp, hash1 );
    ServiceReference ref = reg.getReference();
    
    mgr.addingProvider(ref);
    
    setupPersistenceBundle("file4", "");
    
    mgr.open();
    
    //Check the persistence.xml was looked for
    Skeleton.getSkeleton(persistenceBundle).assertCalled(new MethodCall(Bundle.class, "getEntry", "META-INF/persistence.xml"));
    //Check we didn't use getResource()
    Skeleton.getSkeleton(persistenceBundle).assertNotCalled(new MethodCall(Bundle.class, "getResource", String.class));
    
    testSuccessfulCreationEvent(ref, extenderContext, 1);
    testSuccessfulRegistrationEvent(ref, extenderContext, 1);
  }
  
  @Test
  public void testManagerStopUnregistersUnits() throws Exception
  {
    testManager_OnePreExistingPersistenceBundle_OneExistingProvider();
    
    mgr.close();
    
    Skeleton.getSkeleton(pp).assertCalled(new MethodCall(EntityManagerFactory.class, "close"));
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
  }

  @Test
  public void testManager_BundleInstalled_OnePreexistingProvider() throws Exception
  {
    //Check we correctly parse and register Persistence Units
    
    BundleContext extenderContext = extenderBundle.getBundleContext();
    
    mgr = new PersistenceBundleManager(extenderContext);
    mgr.setConfig(new Properties());
    mgr.open();
    
    Hashtable<String,String> hash1 = new Hashtable<String, String>();
    hash1.put("javax.persistence.provider", "no.such.Provider");
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
        pp, hash1 );
    ServiceReference ref = reg.getReference();
    mgr.addingProvider(ref);
    
    setupPersistenceBundle("file4", "");
    
    //INSTALL the bundle
    
    Skeleton.getSkeleton(persistenceBundle).setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.INSTALLED);
    
    Object o = mgr.addingBundle(persistenceBundle, new BundleEvent(BundleEvent.INSTALLED, persistenceBundle));

    //Check the persistence.xml was looked for
    Skeleton.getSkeleton(persistenceBundle).assertCalled(new MethodCall(Bundle.class, "getEntry", "META-INF/persistence.xml"));
    //Check we didn't use getResource()
    Skeleton.getSkeleton(persistenceBundle).assertNotCalled(new MethodCall(Bundle.class, "getResource", String.class));
    
    //Check we didn't get the Provider, register a service or create an EMF
    Skeleton.getSkeleton(extenderContext).assertNotCalled(new MethodCall(BundleContext.class, "getService", ServiceReference.class));
    Skeleton.getSkeleton(persistenceBundleContext).assertNotCalled(new MethodCall(BundleContext.class, "registerService", EntityManagerFactory.class.getName(), EntityManagerFactory.class, Dictionary.class));
    Skeleton.getSkeleton(pp).assertNotCalled(new MethodCall(PersistenceProvider.class, "createContainerEntityManagerFactory", PersistenceUnitInfo.class, Map.class));
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
    
    
    //Now try Resolving
    Skeleton.getSkeleton(persistenceBundle).setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.RESOLVED);
    mgr.modifiedBundle(persistenceBundle, new BundleEvent(BundleEvent.RESOLVED, persistenceBundle), o);
    
    testSuccessfulCreationEvent(ref, extenderContext, 1);
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
    
    
    //Now try starting (This should not create again, so check we only called 1 time)
    Skeleton.getSkeleton(persistenceBundle).setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.STARTING);
    mgr.modifiedBundle(persistenceBundle, new BundleEvent(BundleEvent.STARTING, persistenceBundle), o);
    
    testSuccessfulCreationEvent(ref, extenderContext, 1);
    testSuccessfulRegistrationEvent(ref, extenderContext, 1);
    
    //Now try active (This should not create again, so check we only called 1 time)
    Skeleton.getSkeleton(persistenceBundle).setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.ACTIVE);
    mgr.modifiedBundle(persistenceBundle, new BundleEvent(BundleEvent.STARTED, persistenceBundle), o);
    
    testSuccessfulCreationEvent(ref, extenderContext, 1);
    testSuccessfulRegistrationEvent(ref, extenderContext, 1);
    
    //Now stop the bundle, check no extra calls, and the EMFs are still open
    Skeleton.getSkeleton(persistenceBundle).setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.STOPPING);
    mgr.modifiedBundle(persistenceBundle, new BundleEvent(BundleEvent.STOPPING, persistenceBundle), o);
    
    testSuccessfulCreationEvent(ref, extenderContext, 1);
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
    Skeleton.getSkeleton(pp).assertNotCalled(new MethodCall(EntityManagerFactory.class, "close"));
    
    //Now Mark the bundle stopped, check no extra calls, and the EMFs are still open
    Skeleton.getSkeleton(persistenceBundle).setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.RESOLVED);
    mgr.modifiedBundle(persistenceBundle, new BundleEvent(BundleEvent.STOPPING, persistenceBundle), o);
    
    testSuccessfulCreationEvent(ref, extenderContext, 1);
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
    Skeleton.getSkeleton(pp).assertNotCalled(new MethodCall(EntityManagerFactory.class, "close"));
    
    //Now Uninstall, check no extra calls, and the EMFs are closed
    Skeleton.getSkeleton(persistenceBundle).setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.UNINSTALLED);
    mgr.removedBundle(persistenceBundle, new BundleEvent(BundleEvent.UNINSTALLED, persistenceBundle), o);
    
    testSuccessfulCreationEvent(ref, extenderContext, 1);
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
    Skeleton.getSkeleton(pp).assertCalled(new MethodCall(EntityManagerFactory.class, "close"));
  }
  
  @Test
  public void testBundleChangedUpdated() throws Exception
  {
    setupPersistenceBundle("file4", "");
    BundleContext extenderContext = preExistingBundleSetup();
    
    Hashtable<String,String> hash1 = new Hashtable<String, String>();
    hash1.put("javax.persistence.provider", "no.such.Provider");
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
        pp, hash1 );
    
    ServiceReference ref = reg.getReference();
    mgr.addingProvider(ref);
    mgr.open();

    testSuccessfulCreationEvent(ref, extenderContext, 1);
    testSuccessfulRegistrationEvent(ref, extenderContext, 1);
    
    //Clear the extender context to remove the previous get for the PersistenceProvider.
    Skeleton.getSkeleton(extenderContext).clearMethodCalls();
    
    System.out.println(getTrackedObject());
    //Update the bundle
    Skeleton.getSkeleton(persistenceBundle).setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.INSTALLED);
    mgr.modifiedBundle(persistenceBundle, new BundleEvent(BundleEvent.UPDATED, persistenceBundle), getTrackedObject());
    
    //Check the persistence.xml was looked for
    Skeleton.getSkeleton(persistenceBundle).assertCalled(new MethodCall(Bundle.class, "getEntry", "META-INF/persistence.xml"));
    //Check we didn't use getResource()
    Skeleton.getSkeleton(persistenceBundle).assertNotCalled(new MethodCall(Bundle.class, "getResource", String.class));
    
    //Check we didn't get the Provider, and there is no Service in the registry
    Skeleton.getSkeleton(extenderContext).assertNotCalled(new MethodCall(BundleContext.class, "getService", ServiceReference.class));
    Skeleton.getSkeleton(pp).assertCalled(new MethodCall(EntityManagerFactory.class, "close"));
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
    
    //Now resolve the bundle again and check we get another EMF created
    Skeleton.getSkeleton(persistenceBundle).setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.RESOLVED);
    mgr.modifiedBundle(persistenceBundle, new BundleEvent(BundleEvent.RESOLVED, persistenceBundle), getTrackedObject());
    
    //We will have created the EMF a total of 2 times
    testSuccessfulCreationEvent(ref, extenderContext, 2);
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
  }
  
  @Test
  public void testBundleChangedUnresolved() throws Exception
  {
    setupPersistenceBundle("file4", "");
    BundleContext extenderContext = preExistingBundleSetup();
    
    Hashtable<String,String> hash1 = new Hashtable<String, String>();
    hash1.put("javax.persistence.provider", "no.such.Provider");
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
        pp, hash1 );
    
    ServiceReference ref = reg.getReference();
    mgr.addingProvider(ref);
    mgr.open();

    testSuccessfulCreationEvent(ref, extenderContext, 1);
    testSuccessfulRegistrationEvent(ref, extenderContext, 1);
    
    Skeleton.getSkeleton(extenderContext).clearMethodCalls();
    Skeleton.getSkeleton(persistenceBundle).clearMethodCalls();
    
    Skeleton.getSkeleton(persistenceBundle).setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.INSTALLED);
    mgr.modifiedBundle(persistenceBundle, new BundleEvent(BundleEvent.UNRESOLVED, persistenceBundle), getTrackedObject());
    
    //Check we don't re-parse the xml
    Skeleton.getSkeleton(persistenceBundle).assertNotCalled(new MethodCall(Bundle.class, "getEntry", "META-INF/persistence.xml"));
    //Check we didn't use getResource()
    Skeleton.getSkeleton(persistenceBundle).assertNotCalled(new MethodCall(Bundle.class, "getResource", String.class));
    
    //Check we didn't get the Provider, and there is no Service in the registry
    Skeleton.getSkeleton(extenderContext).assertNotCalled(new MethodCall(BundleContext.class, "getService", ServiceReference.class));
    Skeleton.getSkeleton(pp).assertCalled(new MethodCall(EntityManagerFactory.class, "close"));
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
  }
  
  @Test
  public void testBundle_ProviderRemoved() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    Hashtable<String,String> hash1 = new Hashtable<String, String>();
    hash1.put("javax.persistence.provider", "no.such.Provider");
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp, hash1 );
    ServiceReference ref = reg.getReference();
    
    mgr.addingProvider(ref);
    
    setupPersistenceBundle("file4", "");
    
    mgr.open();
    testSuccessfulCreationEvent(ref, extenderContext, 1);
    testSuccessfulRegistrationEvent(ref, extenderContext, 1);
    
    mgr.removingProvider(ref);
    
    Skeleton.getSkeleton(pp).assertCalled(new MethodCall(EntityManagerFactory.class, "close"));
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());    
    
    mgr.modifiedBundle(persistenceBundle, null, getTrackedObject());
  }
  
  @Test
  public void testInstalledWithBadXML() throws Exception
  {
  
    BundleContext extenderContext = extenderBundle.getBundleContext();
    
    mgr = new PersistenceBundleManager(extenderContext);
    mgr.setConfig(new Properties());
    
    Hashtable<String,String> hash1 = new Hashtable<String, String>();
    hash1.put("javax.persistence.provider", "no.such.Provider");
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp, hash1 );
    ServiceReference ref = reg.getReference();
    
    mgr.addingProvider(ref);
    
    setupPersistenceBundle("file3", "");
    
    mgr.open();
    
    Object o = mgr.addingBundle(persistenceBundle, null);
    
    assertNull("We should not have received a manager", o);
    //Check we didn't get the Provider, and there is no Service in the registry
    Skeleton.getSkeleton(extenderContext).assertNotCalled(new MethodCall(BundleContext.class, "getService", ServiceReference.class));
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
  }

  @Test
  public void testdefaultProvider() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    Hashtable<String,Object> hash1 = new Hashtable<String, Object>();
    hash1.put("javax.persistence.provider", "use.this.Provider");
    hash1.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp, hash1 );
    ServiceReference ppRef = reg.getReference();
    
    PersistenceProvider pp2 = Skeleton.newMock(PersistenceProvider.class);
    Hashtable<String,Object> hash2 = new Hashtable<String, Object>();
    hash2.put("javax.persistence.provider", "do.not.use.this.Provider");
    hash2.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
    ServiceRegistration reg2 = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp2, hash2 );
    
    mgr.addingProvider(ppRef);
    mgr.addingProvider(reg2.getReference());
    
    setupPersistenceBundle("file5", "");
    
    mgr.open();
    testSuccessfulCreationEvent(ppRef, extenderContext, 1);
    testSuccessfulRegistrationEvent(ppRef, extenderContext, 1);

  }
  
  @Test
  public void testdefaultProviderFromManagedPersistenceUnitFactory() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    Hashtable<String,Object> hash1 = new Hashtable<String, Object>();
    hash1.put("javax.persistence.provider", "use.this.Provider");
    hash1.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp, hash1 );
    ServiceReference ppRef = reg.getReference();
    
    PersistenceProvider pp2 = Skeleton.newMock(PersistenceProvider.class);
    Hashtable<String,Object> hash2 = new Hashtable<String, Object>();
    hash2.put("javax.persistence.provider", "do.not.use.this.Provider");
    hash2.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
    ServiceRegistration reg2 = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp2, hash2 );
    ServiceReference pp2Ref = reg2.getReference();
    
    mgr.addingProvider(ppRef);
    mgr.addingProvider(reg2.getReference());
    
    setupPersistenceBundle("file5", "");
    
    Properties props = new Properties();
    props.put("org.apache.aries.jpa.container.ManagedPersistenceUnitInfoFactory", FakeManagedPersistenceUnitFactory.class.getName());
    
    mgr.setConfig(props);
    
    mgr.open();
    testSuccessfulCreationEvent(ppRef, extenderContext, 1);
    testSuccessfulRegistrationEvent(ppRef, extenderContext, 1);
  }

  @Test
  public void testSameProviders() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    Hashtable<String,String> hash1 = new Hashtable<String, String>();
    hash1.put("javax.persistence.provider", "no.such.Provider");
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp, hash1 );
    ServiceReference ref = reg.getReference();

    PersistenceProvider pp2 = Skeleton.newMock(PersistenceProvider.class);
    Hashtable<String,Object> hash2 = new Hashtable<String, Object>();
    hash2.put("javax.persistence.provider", "do.not.use.this.Provider");
    hash2.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
    ServiceRegistration reg2 = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp2, hash2 );
    
    mgr.addingProvider(ref);
    mgr.addingProvider(reg2.getReference());
    
    setupPersistenceBundle("file6", "");
    
    mgr.open();
    
    //Check the persistence.xml was looked for
    Skeleton.getSkeleton(persistenceBundle).assertCalled(new MethodCall(Bundle.class, "getEntry", "META-INF/persistence.xml"));
    //Check we didn't use getResource()
    Skeleton.getSkeleton(persistenceBundle).assertNotCalled(new MethodCall(Bundle.class, "getResource", String.class));
    
    testSuccessfulCreationEvent(ref, extenderContext, 2);
    testSuccessfulRegistrationEvent(ref, extenderContext, 2, "alpha", "bravo");
  }

  @Test
  public void testOneWithProviderOneWithout() throws Exception
  {
    //Check we behave correctly when one persistence unit defines a provder
    //and another doesn't 
    
    BundleContext extenderContext = preExistingBundleSetup();
    
    Hashtable<String,String> hash1 = new Hashtable<String, String>();
    hash1.put("javax.persistence.provider", "no.such.Provider");
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp, hash1 );
    ServiceReference ref = reg.getReference();

    PersistenceProvider pp2 = Skeleton.newMock(PersistenceProvider.class);
    Hashtable<String,Object> hash2 = new Hashtable<String, Object>();
    hash2.put("javax.persistence.provider", "do.not.use.this.Provider");
    hash2.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
    ServiceRegistration reg2 = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp2, hash2 );
    
    mgr.addingProvider(ref);
    mgr.addingProvider(reg2.getReference());
    
    setupPersistenceBundle("file7", "");
    
    mgr.open();
    
    //Check the persistence.xml was looked for
    Skeleton.getSkeleton(persistenceBundle).assertCalled(new MethodCall(Bundle.class, "getEntry", "META-INF/persistence.xml"));
    //Check we didn't use getResource()
    Skeleton.getSkeleton(persistenceBundle).assertNotCalled(new MethodCall(Bundle.class, "getResource", String.class));
    
    testSuccessfulCreationEvent(ref, extenderContext, 2);
    testSuccessfulRegistrationEvent(ref, extenderContext, 2, "alpha", "bravo");
  }

  @Test
  public void testTwoProviders() throws Exception
  {
    //Check we behave correctly when two persistence units define different providers
    
    BundleContext extenderContext = preExistingBundleSetup();
    
    Hashtable<String,String> hash1 = new Hashtable<String, String>();
    hash1.put("javax.persistence.provider", "no.such.Provider");
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp, hash1 );
    ServiceReference ref = reg.getReference();

    PersistenceProvider pp2 = Skeleton.newMock(PersistenceProvider.class);
    Hashtable<String,Object> hash2 = new Hashtable<String, Object>();
    hash2.put("javax.persistence.provider", "do.not.use.this.Provider");
    hash2.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
    ServiceRegistration reg2 = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp2, hash2 );
    ServiceReference ref2 = reg2.getReference();
    
    mgr.addingProvider(ref);
    mgr.addingProvider(ref2);
    
    setupPersistenceBundle("file8", "");
    
    mgr.open();
    
    //Check the persistence.xml was looked for
    Skeleton.getSkeleton(persistenceBundle).assertCalled(new MethodCall(Bundle.class, "getEntry", "META-INF/persistence.xml"));
    //Check we didn't use getResource()
    Skeleton.getSkeleton(persistenceBundle).assertNotCalled(new MethodCall(Bundle.class, "getResource", String.class));
    
    Skeleton.getSkeleton(extenderContext).assertNotCalled(new MethodCall(BundleContext.class, "getService", ServiceReference.class));
    Skeleton.getSkeleton(extenderContext).assertNotCalled(new MethodCall(BundleContext.class, "ungetService", ServiceReference.class));
    Skeleton.getSkeleton(pp).assertNotCalled(new MethodCall(PersistenceProvider.class, "createContainerEntityManagerFactory", PersistenceUnitInfo.class, Map.class));
    Skeleton.getSkeleton(pp2).assertNotCalled(new MethodCall(PersistenceProvider.class, "createContainerEntityManagerFactory", PersistenceUnitInfo.class, Map.class));

    
  
  }
  
  @Test
  public void testpp100() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file9", "");

    mgr.open();
    
    assertCorrectPersistenceProviderUsed(extenderContext, providerP100);
  }

  @Test
  public void testpp101() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file10", "");

    mgr.open();
    
    assertCorrectPersistenceProviderUsed(extenderContext, providerP101);
  }
  
  @Test
  public void testpp101b() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file11", "");

    mgr.open();
    
    assertCorrectPersistenceProviderUsed(extenderContext, providerP101);
  }

  @Test
  public void testpp111() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file12", "");

    mgr.open();
    
    assertCorrectPersistenceProviderUsed(extenderContext, providerP111);
  }

  @Test
  public void testppNoMatch() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file13", "");

    mgr.open();

    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());

    //A provider was instantiated
    Skeleton.getSkeleton(extenderContext).assertNotCalled(new MethodCall(BundleContext.class, "getService", ServiceReference.class));
  }

  @Test
  public void testTwoProvidersMatch() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file14", "");

    mgr.open();
    
    assertCorrectPersistenceProviderUsed(extenderContext, providerP101, 2);
  }

  @Test
  public void testTwoProvidersNoVersionMatch() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file15", "");

    mgr.open();

    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());

    //A provider was instantiated
    Skeleton.getSkeleton(extenderContext).assertNotCalled(new MethodCall(BundleContext.class, "getService", ServiceReference.class));
  }

  @Test
  public void testThreeProvidersNoVersionMatch() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file16", "");

    mgr.open();

    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());

    //A provider was instantiated
    Skeleton.getSkeleton(extenderContext).assertNotCalled(new MethodCall(BundleContext.class, "getService", ServiceReference.class));
  }
  
  @Test
  public void testTwoProvidersExistNoCommonVersion() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file17", "");

    mgr.open();

    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());

    //A provider was instantiated
    Skeleton.getSkeleton(extenderContext).assertNotCalled(new MethodCall(BundleContext.class, "getService", ServiceReference.class));
  }
  

  @Test
  public void testTwoProvidersNoVersionMatchOneExists() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file18", "");

    mgr.open();

    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());

    //A provider was instantiated
    Skeleton.getSkeleton(extenderContext).assertNotCalled(new MethodCall(BundleContext.class, "getService", ServiceReference.class));    
  }
  
  @Test
  public void testThreeProvidersNoVersionMatchOneExists() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file19", "");

    mgr.open();

    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());

    //A provider was instantiated
    Skeleton.getSkeleton(extenderContext).assertNotCalled(new MethodCall(BundleContext.class, "getService", ServiceReference.class));
  }

  @Test
  public void testThreeProvidersNoVersionMatchTwoExist() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file20", "");

    mgr.open();

    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());

    //A provider was instantiated
    Skeleton.getSkeleton(extenderContext).assertNotCalled(new MethodCall(BundleContext.class, "getService", ServiceReference.class));
  }
  
  @Test
  public void testMultipleLocations() throws Exception
  {
    //Check we correctly parse and register EMFactories according to the Meta-Persistence Header
    BundleContext extenderContext = preExistingBundleSetup();
    
    Hashtable<String,String> hash1 = new Hashtable<String, String>();
    hash1.put("javax.persistence.provider", "no.such.Provider");
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp, hash1 );
    
    ServiceReference ref = reg.getReference();
    setupPersistenceBundle21();
    
    mgr.addingProvider(ref);
    mgr.open();
    
    testSuccessfulCreationEvent(ref, extenderContext, 4);
    testSuccessfulRegistrationEvent(ref, extenderContext, 4, "persistence", "found", "jar", "another");
    
  }

  private void setupPersistenceBundle21() throws Exception {
    persistenceBundle.getHeaders().put("Meta-Persistence", "OSGI-INF/found.xml, jarfile.jar!/jar.xml,persistence/another.xml, does-not-exist.xml");
    
    Skeleton skel = Skeleton.getSkeleton(persistenceBundle);
    skel.setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.ACTIVE);

    URL xml = getClass().getClassLoader().getResource("file21/META-INF/persistence.xml");
    skel.setReturnValue(new MethodCall(Bundle.class, "getEntry", "META-INF/persistence.xml"), xml);
    
    xml = getClass().getClassLoader().getResource("file21/OSGI-INF/found.xml");
    skel.setReturnValue(new MethodCall(Bundle.class, "getEntry", "OSGI-INF/found.xml"), xml);
    
    URL root = getClass().getClassLoader().getResource("file21");
    
    File f = new File(new File(root.toURI()), "jarfile.jar");
    
    JarOutputStream jos = new JarOutputStream(new FileOutputStream(f));
    
    jos.putNextEntry(new ZipEntry("jar.xml"));
    
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(jos));
    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    writer.newLine();
    writer.write("<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\"");
    writer.newLine();
    writer.write("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
    writer.newLine();    
    writer.write("xsi:schemaLocation=\"http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_1_0.xsd\"");
    writer.newLine();
    writer.write("version=\"1.0\">");
    writer.newLine();
    writer.write("<persistence-unit name=\"jar\"/>");
    writer.newLine();
    writer.write("</persistence>");
    
    writer.close();
    
    xml = getClass().getClassLoader().getResource("file21/jarfile.jar");
    skel.setReturnValue(new MethodCall(Bundle.class, "getEntry", "jarfile.jar"), xml);

    xml = getClass().getClassLoader().getResource("file21/persistence/another.xml");
    skel.setReturnValue(new MethodCall(Bundle.class, "getEntry", "persistence/another.xml"), xml);

    xml = getClass().getClassLoader().getResource("file21/OSGI-INF/not-found.xml");
    skel.setReturnValue(new MethodCall(Bundle.class, "getEntry", "OSGI-INF/not-found.xml"), xml);
    
    skel.setReturnValue(new MethodCall(Bundle.class, "getEntry", "does-not-exist.xml"), null);
    
    skel.setReturnValue(new MethodCall(Bundle.class, "getVersion"), new Version("0.0.0"));
    
  }

  private void setupPersistenceBundle(String root, String header) throws MalformedURLException
  {
    persistenceBundle.getHeaders().put("Meta-Persistence", header);
    
    Skeleton skel = Skeleton.getSkeleton(persistenceBundle);
    
    skel.setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.ACTIVE);
    
    URL rootURL = getClass().getClassLoader().getResource(root);
    URL xml = getClass().getClassLoader().getResource(root + "/META-INF/persistence.xml");
    
    skel.setReturnValue(new MethodCall(Bundle.class, "getResource", "/"), rootURL);
    skel.setReturnValue(new MethodCall(Bundle.class, "getEntry", "META-INF/persistence.xml"), xml);
    skel.setReturnValue(new MethodCall(Bundle.class, "getVersion"), new Version("0.0.0"));
  }
  
  private void registerVersionedPersistenceProviders() {
    
    providerP100 = Skeleton.newMock(PersistenceProvider.class);
    providerP101 = Skeleton.newMock(PersistenceProvider.class);
    providerP110 = Skeleton.newMock(PersistenceProvider.class);
    providerP111 = Skeleton.newMock(PersistenceProvider.class);
    
    ServiceRegistration reg;
    
    Hashtable<String,String> hash1 = new Hashtable<String, String>();
    hash1.put("javax.persistence.provider", "no.such.Provider");
    reg = providerBundleP100.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()},
            providerP100, hash1 );
    mgr.addingProvider(reg.getReference());    
    
    reg = providerBundleP101.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()},
            providerP101, hash1 );
    mgr.addingProvider(reg.getReference());
    
    reg = providerBundleP110.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()},
            providerP110, hash1 );
    mgr.addingProvider(reg.getReference());
    
    reg = providerBundleP111.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()},
            providerP111, hash1 );
    mgr.addingProvider(reg.getReference());
  }
  

  private void testSuccessfulCreationEvent(ServiceReference providerRef, BundleContext extenderContext, int numberOfPersistenceUnits)
  {
  //Check we loaded the Provider service
    Skeleton.getSkeleton(extenderContext).assertCalledExactNumberOfTimes(new MethodCall(BundleContext.class, "getService", providerRef), 1);
    Skeleton.getSkeleton(extenderContext).assertCalledExactNumberOfTimes(new MethodCall(BundleContext.class, "ungetService", providerRef), 1);
    Skeleton.getSkeleton(pp).assertCalledExactNumberOfTimes(new MethodCall(PersistenceProvider.class, "createContainerEntityManagerFactory", PersistenceUnitInfo.class, Map.class), numberOfPersistenceUnits);
  }
  
  private void testSuccessfulRegistrationEvent(ServiceReference providerRef, BundleContext extenderContext, int numberOfPersistenceUnits, String... names) throws InvalidSyntaxException
  {
    Skeleton.getSkeleton(persistenceBundleContext).assertCalledExactNumberOfTimes(new MethodCall(BundleContext.class, "registerService", EntityManagerFactory.class.getName(), EntityManagerFactory.class, Dictionary.class), numberOfPersistenceUnits);
    
    BundleContextMock.assertServiceExists(EntityManagerFactory.class.getName());
    
    ServiceReference[] emfs = extenderContext.getServiceReferences(EntityManagerFactory.class.getName(), null);
    
    assertEquals("Too many services registered", numberOfPersistenceUnits, emfs.length);
    
    if(names.length == 0)
      names = new String[]{"alpha"};
    
    for(int i = 0; i < numberOfPersistenceUnits; i++) {
      ServiceReference emf = emfs[i]; 
      
      boolean found = false;
      for(int j = 0; j < names.length; j++) {
        found = emf.getProperty("osgi.unit.name").equals(names[j]);
        if(found) {
          names[j] = null;
          break;
        }
      }
      assertTrue("No emf expected with the name " + emf.getProperty("osgi.unit.name"), found);
      assertEquals("".equals(emf.getProperty("osgi.unit.name")),
          emf.getProperty(PersistenceUnitConstants.EMPTY_PERSISTENCE_UNIT_NAME));
      
      assertEquals("Wrong unit provider name registered", providerRef.getProperty("javax.persistence.provider"), emf.getProperty("osgi.unit.provider"));
      
      assertEquals("Wrong unit name registered", Boolean.TRUE, emf.getProperty("org.apache.aries.jpa.container.managed"));
    }
  }
  
  private void assertCorrectPersistenceProviderUsed (BundleContext extenderContext, PersistenceProvider provider, int numEMFs) throws InvalidSyntaxException
  {
      BundleContextMock.assertServiceExists(EntityManagerFactory.class.getName());

      ServiceReference[] refs = persistenceBundleContext.getServiceReferences(EntityManagerFactory.class.getCanonicalName(), null);
      
      assertEquals("Too many EMFs", numEMFs, refs.length);
      
      Skeleton.getSkeleton(provider).assertCalledExactNumberOfTimes(new MethodCall(PersistenceProvider.class, "createContainerEntityManagerFactory", PersistenceUnitInfo.class, Map.class), numEMFs);
      
      for(ServiceReference emf : refs)
        assertSame("The EMF came from the wrong provider", Skeleton.getSkeleton(provider), Skeleton.getSkeleton(persistenceBundleContext.getService(emf)));
      
      //More than one provider was instantiated
      Skeleton.getSkeleton(extenderContext).assertCalledExactNumberOfTimes(new MethodCall(BundleContext.class, "getService", ServiceReference.class), 1);
  }

  private void assertCorrectPersistenceProviderUsed (BundleContext extenderContext, PersistenceProvider provider) throws InvalidSyntaxException
  {
    assertCorrectPersistenceProviderUsed(extenderContext, provider, 1); 
  }
  
  private EntityManagerFactoryManager getTrackedObject() throws Exception {
    Field f = mgr.getClass().getDeclaredField("bundleToManagerMap");
    f.setAccessible(true);
    Map<Bundle, EntityManagerFactoryManager> map = (Map<Bundle, EntityManagerFactoryManager>) f.get(mgr);
    
    return map.get(persistenceBundle);
  }
}

