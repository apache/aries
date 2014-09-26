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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import org.apache.aries.jpa.container.impl.EntityManagerFactoryManager;
import org.apache.aries.jpa.container.impl.PersistenceBundleManager;
import org.apache.aries.mocks.BundleContextMock;
import org.apache.aries.mocks.BundleMock;
import org.apache.aries.quiesce.manager.QuiesceCallback;
import org.apache.aries.quiesce.participant.QuiesceParticipant;
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
import org.osgi.service.jdbc.DataSourceFactory;

public class PersistenceBundleLifecycleTest
{
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
  
  private BundleContext preExistingBundleSetup() throws Exception {
    
    BundleContext extenderContext = extenderBundle.getBundleContext();

    Skeleton.getSkeleton(extenderContext).setReturnValue(
        new MethodCall(BundleContext.class, "getBundles"),
        new Bundle[] {persistenceBundle});
    
    mgr = new PersistenceBundleManager();

    return extenderContext;
  }
  
  @Test
  public void testManager_NonPersistenceBundle() throws Exception
  {
    BundleContext ctx = preExistingBundleSetup();
    mgr.start(ctx);

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
    BundleContext ctx = preExistingBundleSetup();
    
    //Set the persistence.xml etc
    setupPersistenceBundle("file4", "");
    
    mgr.start(ctx);
    
    //Check the persistence.xml was looked for
    Skeleton.getSkeleton(persistenceBundle).assertCalled(new MethodCall(Bundle.class, "getEntry", "META-INF/persistence.xml"));
    //Check we didn't use getResource()
    Skeleton.getSkeleton(persistenceBundle).assertNotCalled(new MethodCall(Bundle.class, "getResource", String.class));
    //Check we don't have an EMF
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
    
    assertNotNull("We should have an EntityManagerFactoryManager", getTrackedObject());
  }
  
  @Test
  public void testManager_WABandJPABundle() throws Exception 
  {
    BundleContext ctx = preExistingBundleSetup();
    setupPersistenceBundle("file4", "");
    persistenceBundle.getHeaders().put("Web-ContextPath", "/test");

    // make sure we don't succeed because of not having a provider
    Hashtable<String,Object> hash1 = new Hashtable<String, Object>();
    hash1.put("javax.persistence.provider", "no.such.Provider");
    hash1.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp, hash1 );
    ServiceReference ref = reg.getReference();
    
    mgr.start(ctx);
    
    //Check the persistence.xml was looked for
    Skeleton.getSkeleton(persistenceBundle).assertCalled(new MethodCall(Bundle.class, "getEntry", "META-INF/persistence.xml"));
    //Check we didn't use getResource()
    Skeleton.getSkeleton(persistenceBundle).assertNotCalled(new MethodCall(Bundle.class, "getResource", String.class));
    
    testSuccessfulCreationEvent(ref, ctx, 1);
    testSuccessfulRegistrationEvent(ref, ctx, 1);
    
    assertNotNull("We should not have an EntityManagerFactoryManager", getTrackedObject());
  }
  
  @Test
  public void testManager_WABNoMetaPersistence() throws Exception {
   
    
    BundleContext extenderContext = preExistingBundleSetup();
    
    Hashtable<String,String> hash1 = new Hashtable<String, String>();
    hash1.put("javax.persistence.provider", "no.such.Provider");
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp, hash1 );
    
    ServiceReference ref = reg.getReference();
    setupWABBundle();
    
    mgr.start(extenderContext);
    
    
    Skeleton.getSkeleton(persistenceBundle).assertCalledExactNumberOfTimes(new MethodCall(Bundle.class, "getEntry", String.class), 3);
    
    testSuccessfulCreationEvent(ref, extenderContext, 3);
    testSuccessfulRegistrationEvent(ref, extenderContext, 3, "webInfClassesOnClassPath", "jarOne", "jarTwo");
  }
  
  @Test
  public void testManager_EJBNoMetaPersistence() throws Exception {
   
    
    BundleContext extenderContext = preExistingBundleSetup();
    
    Hashtable<String,String> hash1 = new Hashtable<String, String>();
    hash1.put("javax.persistence.provider", "no.such.Provider");
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp, hash1 );
    
    ServiceReference ref = reg.getReference();
    setupEJBBundle();
    
    mgr.start(extenderContext);
    
    
    Skeleton.getSkeleton(persistenceBundle).assertCalledExactNumberOfTimes(new MethodCall(Bundle.class, "getEntry", String.class), 1);
    
    testSuccessfulCreationEvent(ref, extenderContext, 1);
    testSuccessfulRegistrationEvent(ref, extenderContext, 1, "root");
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
    
    setupPersistenceBundle("file4", "");
    
    mgr.start(extenderContext);
    
    //Check the persistence.xml was looked for
    Skeleton.getSkeleton(persistenceBundle).assertCalled(new MethodCall(Bundle.class, "getEntry", "META-INF/persistence.xml"));
    //Check we didn't use getResource()
    Skeleton.getSkeleton(persistenceBundle).assertNotCalled(new MethodCall(Bundle.class, "getResource", String.class));
    
    testSuccessfulCreationEvent(ref, extenderContext, 1);
    testSuccessfulRegistrationEvent(ref, extenderContext, 1);
  }
  
  @Test
  public void testManager_OnePreExistingPersistenceBundle_OneProviderLater() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    setupPersistenceBundle("file4", "");
    
    mgr.start(extenderContext);
    
    //Check the persistence.xml was looked for
    Skeleton.getSkeleton(persistenceBundle).assertCalled(new MethodCall(Bundle.class, "getEntry", "META-INF/persistence.xml"));
    //Check we didn't use getResource()
    Skeleton.getSkeleton(persistenceBundle).assertNotCalled(new MethodCall(Bundle.class, "getResource", String.class));
    
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
    assertNotNull("We should have an EntityManagerFactoryManager", getTrackedObject());
    
    Hashtable<String,String> hash1 = new Hashtable<String, String>();
    hash1.put("javax.persistence.provider", "no.such.Provider");
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp, hash1 );
    ServiceReference ref = reg.getReference();
    
    BundleContextMock.assertServiceExists(EntityManagerFactory.class.getName());
    
    testSuccessfulCreationEvent(ref, extenderContext, 1);
    testSuccessfulRegistrationEvent(ref, extenderContext, 1);
  }
  
  @Test
  public void testManager_OnePersistenceBundle_SwitchProviders() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    setupPersistenceBundle("file4", "");
    
    mgr.start(extenderContext);
    
    //Check the persistence.xml was looked for
    Skeleton.getSkeleton(persistenceBundle).assertCalled(new MethodCall(Bundle.class, "getEntry", "META-INF/persistence.xml"));
    //Check we didn't use getResource()
    Skeleton.getSkeleton(persistenceBundle).assertNotCalled(new MethodCall(Bundle.class, "getResource", String.class));
    
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
    assertNotNull("We should have an EntityManagerFactoryManager", getTrackedObject());
    
    Hashtable<String,String> hash1 = new Hashtable<String, String>();
    hash1.put("javax.persistence.provider", "no.such.Provider");
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp, hash1 );
    ServiceReference ref = reg.getReference();
    
    BundleContextMock.assertServiceExists(EntityManagerFactory.class.getName());
    
    testSuccessfulCreationEvent(ref, extenderContext, 1);
    testSuccessfulRegistrationEvent(ref, extenderContext, 1);
    
    Hashtable<String,String> hash2 = new Hashtable<String, String>();
    hash2.put("javax.persistence.provider", "no.such.Provider");
    hash2.put("key", "value");
    ServiceRegistration reg2 = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp, hash2 );
    ServiceReference ref2 = reg2.getReference();
    
    BundleContextMock.assertServiceExists(EntityManagerFactory.class.getName());
    
    testSuccessfulCreationEvent(ref, extenderContext, 1);
    testSuccessfulRegistrationEvent(ref, extenderContext, 1);
    //Clear the call to createContainerEntityManagerFactory so that we can check nothing
    //was done with the new reference
    Skeleton.getSkeleton(pp).clearMethodCalls();
    testSuccessfulCreationEvent(ref2, extenderContext, 0);
    
    //Clear the registration call
    Skeleton.getSkeleton(persistenceBundleContext).clearMethodCalls();
    reg.unregister();
    
    BundleContextMock.assertServiceExists(EntityManagerFactory.class.getName());
    testSuccessfulCreationEvent(ref2, extenderContext, 1);
    testSuccessfulRegistrationEvent(ref2, extenderContext, 1);
  }
  
  @Test
  public void testManagerStopUnregistersUnits() throws Exception
  {
    testManager_OnePreExistingPersistenceBundle_OneExistingProvider();
    
    mgr.stop(extenderBundle.getBundleContext());
    
    assertCloseCalled();
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
  }

  @Test
  public void testManager_BundleInstalled_OnePreexistingProvider() throws Exception
  {
    //Check we correctly parse and register Persistence Units
    
    BundleContext extenderContext = extenderBundle.getBundleContext();
    
    mgr = new PersistenceBundleManager();
    mgr.start(extenderContext);
    
    Hashtable<String,String> hash1 = new Hashtable<String, String>();
    hash1.put("javax.persistence.provider", "no.such.Provider");
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName(), "no.such.Provider"} ,
        pp, hash1 );
    ServiceReference ref = reg.getReference();
    
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
    assertCloseCalled();
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
    mgr.start(extenderContext);

    testSuccessfulCreationEvent(ref, extenderContext, 1);
    testSuccessfulRegistrationEvent(ref, extenderContext, 1);
    
    //Clear the extender context to remove the previous get for the PersistenceProvider.
    Skeleton.getSkeleton(extenderContext).clearMethodCalls();
    
    //Update the bundle
    Skeleton.getSkeleton(persistenceBundle).setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.INSTALLED);
    mgr.modifiedBundle(persistenceBundle, new BundleEvent(BundleEvent.UPDATED, persistenceBundle), getTrackedObject());
    
    //Check the persistence.xml was looked for
    Skeleton.getSkeleton(persistenceBundle).assertCalled(new MethodCall(Bundle.class, "getEntry", "META-INF/persistence.xml"));
    //Check we didn't use getResource()
    Skeleton.getSkeleton(persistenceBundle).assertNotCalled(new MethodCall(Bundle.class, "getResource", String.class));
    
    //Check we didn't get the Provider, and there is no Service in the registry
    Skeleton.getSkeleton(extenderContext).assertNotCalled(new MethodCall(BundleContext.class, "getService", ServiceReference.class));
    assertCloseCalled();
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
    mgr.start(extenderContext);

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
    assertCloseCalled();
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
    
    setupPersistenceBundle("file4", "");
    
    mgr.start(extenderContext);
    testSuccessfulCreationEvent(ref, extenderContext, 1);
    testSuccessfulRegistrationEvent(ref, extenderContext, 1);
    
    reg.unregister();
    
    assertCloseCalled();
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());    
    
    mgr.modifiedBundle(persistenceBundle, null, getTrackedObject());
  }

private void assertCloseCalled() {
    Skeleton.getSkeleton(pp).assertCalled(new MethodCall(EntityManagerFactory.class, "close"));
}
  
  @Test
  public void testInstalledWithBadXML() throws Exception
  {
  
    BundleContext extenderContext = extenderBundle.getBundleContext();
    
    mgr = new PersistenceBundleManager();
    
    Hashtable<String,String> hash1 = new Hashtable<String, String>();
    hash1.put("javax.persistence.provider", "no.such.Provider");
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp, hash1 );
    ServiceReference ref = reg.getReference();
    
    setupPersistenceBundle("file3", "");
    
    mgr.start(extenderContext);
    
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
    
    
    setupPersistenceBundle("file5", "");
    
    mgr.start(extenderContext);
    testSuccessfulCreationEvent(ppRef, extenderContext, 1);
    testSuccessfulRegistrationEvent(ppRef, extenderContext, 1);
    Skeleton.getSkeleton(pp).clearMethodCalls();
    testSuccessfulCreationEvent(reg2.getReference(), extenderContext, 0);
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
    
    
    setupPersistenceBundle("file5", "");
    
    Skeleton.getSkeleton(extenderBundle).setReturnValue(new MethodCall(Bundle.class, "getResource", ManagedPersistenceUnitInfoFactory.ARIES_JPA_CONTAINER_PROPERTIES),
        getClass().getClassLoader().getResource("testProps.props"));
    
    mgr.start(extenderContext);
    testSuccessfulCreationEvent(ppRef, extenderContext, 1);
    testSuccessfulRegistrationEvent(ppRef, extenderContext, 1);
    //Clear the call to createContainerEntityManagerFactory so that we can check nothing
    //was done with the new reference
    Skeleton.getSkeleton(pp).clearMethodCalls();
    testSuccessfulCreationEvent(pp2Ref, extenderContext, 0);
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
    
    setupPersistenceBundle("file6", "");
    
    mgr.start(extenderContext);
    
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
    
    setupPersistenceBundle("file7", "");
    
    mgr.start(extenderContext);
    
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

    setupPersistenceBundle("file8", "");
    
    mgr.start(extenderContext);
    
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

    mgr.start(extenderContext);
    
    assertCorrectPersistenceProviderUsed(extenderContext, providerP100);
  }

  @Test
  public void testpp101() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file10", "");

    mgr.start(extenderContext);
    
    assertCorrectPersistenceProviderUsed(extenderContext, providerP101);
  }
  
  @Test
  public void testpp101b() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file11", "");

    mgr.start(extenderContext);
    
    assertCorrectPersistenceProviderUsed(extenderContext, providerP101);
  }

  @Test
  public void testpp111() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file12", "");

    mgr.start(extenderContext);
    
    assertCorrectPersistenceProviderUsed(extenderContext, providerP111);
  }

  @Test
  public void testppNoMatch() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file13", "");

    mgr.start(extenderContext);

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

    mgr.start(extenderContext);
    
    assertCorrectPersistenceProviderUsed(extenderContext, providerP101, 2);
  }

  @Test
  public void testTwoProvidersNoVersionMatch() throws Exception
  {
    BundleContext extenderContext = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file15", "");

    mgr.start(extenderContext);

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

    mgr.start(extenderContext);

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

    mgr.start(extenderContext);

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

    mgr.start(extenderContext);

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

    mgr.start(extenderContext);

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

    mgr.start(extenderContext);

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
    
    mgr.start(extenderContext);
    
    testSuccessfulCreationEvent(ref, extenderContext, 4);
    testSuccessfulRegistrationEvent(ref, extenderContext, 4, "persistence", "found", "jar", "another");
    
  }

  /**
   * Quiesce a bundle that has no JPA content
   * @throws Exception
   */
  @Test
  public void testQuiesceNoOp() throws Exception
  {
    BundleContext ctx = preExistingBundleSetup();
    mgr.start(ctx);
    
    QuiesceCallback cbk = Skeleton.newMock(QuiesceCallback.class);
    
    QuiesceParticipant p = (QuiesceParticipant) ctx.getService(ctx.getServiceReference(QuiesceParticipant.class.getName()));
  
    p.quiesce(cbk, Collections.singletonList(persistenceBundle));
    
    Thread.sleep(100);
    
    Skeleton.getSkeleton(cbk).assertCalledExactNumberOfTimes(new MethodCall(QuiesceCallback.class,
        "bundleQuiesced", Bundle[].class), 1);
  }
  
  /**
   * Quiesce a JPA bundle that is not active
   * @throws Exception
   */
  @Test
  public void testQuiesceBasic() throws Exception
  {
    BundleContext ctx = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file14", "");

    mgr.start(ctx);
    
    assertCorrectPersistenceProviderUsed(ctx, providerP101, 2);
    
    QuiesceCallback cbk = Skeleton.newMock(QuiesceCallback.class);
    
    QuiesceParticipant p = (QuiesceParticipant) ctx.getService(ctx.getServiceReference(QuiesceParticipant.class.getName()));
  
    p.quiesce(cbk, Collections.singletonList(persistenceBundle));
    
    Thread.sleep(100);
    
    Skeleton.getSkeleton(cbk).assertCalledExactNumberOfTimes(new MethodCall(QuiesceCallback.class,
        "bundleQuiesced", Bundle[].class), 1);
    
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
  }
  
  /**
   * Quiesce multiple JPA units that are in use
   * @throws Exception
   */
  @Test
  public void testQuiesceComplex() throws Exception
  {
    BundleContext ctx = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file14", "");

    mgr.start(ctx);
    
    assertCorrectPersistenceProviderUsed(ctx, providerP101, 2);
    
    EntityManager alpha = ((EntityManagerFactory) ctx.getService(ctx.getServiceReferences(
        EntityManagerFactory.class.getName(), "(osgi.unit.name=alpha)")[0])).createEntityManager();
    
    EntityManager bravo = ((EntityManagerFactory) ctx.getService(ctx.getServiceReferences(
        EntityManagerFactory.class.getName(), "(osgi.unit.name=bravo)")[0])).createEntityManager();
    
    QuiesceCallback cbk = Skeleton.newMock(QuiesceCallback.class);
    
    QuiesceParticipant p = (QuiesceParticipant) ctx.getService(ctx.getServiceReference(QuiesceParticipant.class.getName()));
  
    p.quiesce(cbk, Collections.singletonList(persistenceBundle));
    
    Thread.sleep(100);
    
    Skeleton.getSkeleton(cbk).assertNotCalled(new MethodCall(QuiesceCallback.class,
        "bundleQuiesced", Bundle[].class));
    
    assertEquals("Should still be registered", 1, ctx.getServiceReferences(
        EntityManagerFactory.class.getName(), "(osgi.unit.name=alpha)").length);
    
    assertEquals("Should still be registered", 1, ctx.getServiceReferences(
        EntityManagerFactory.class.getName(), "(osgi.unit.name=bravo)").length);
    
    alpha.close();
    
    Skeleton.getSkeleton(cbk).assertNotCalled(new MethodCall(QuiesceCallback.class,
        "bundleQuiesced", Bundle[].class));
    
    assertNull("Should be unregistered", ctx.getServiceReferences(
        EntityManagerFactory.class.getName(), "(osgi.unit.name=alpha)"));
    
    assertEquals("Should still be registered", 1, ctx.getServiceReferences(
        EntityManagerFactory.class.getName(), "(osgi.unit.name=bravo)").length);
    
    bravo.close();
    
    Skeleton.getSkeleton(cbk).assertCalledExactNumberOfTimes(new MethodCall(QuiesceCallback.class,
        "bundleQuiesced", Bundle[].class), 1);
    
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
  }
  
  /**
   * Quiesce the JPA extender when there is nothing to do
   * @throws Exception
   */
  @Test
  public void testQuiesceAllNoOp() throws Exception
  {
    BundleContext ctx = preExistingBundleSetup();
    mgr.start(ctx);
    
    QuiesceCallback cbk = Skeleton.newMock(QuiesceCallback.class);
    
    QuiesceParticipant p = (QuiesceParticipant) ctx.getService(ctx.getServiceReference(QuiesceParticipant.class.getName()));
  
    p.quiesce(cbk, Collections.singletonList(extenderBundle));
    
    Thread.sleep(100);
    
    Skeleton.getSkeleton(cbk).assertCalledExactNumberOfTimes(new MethodCall(QuiesceCallback.class,
        "bundleQuiesced", Bundle[].class), 1);
  }
  
  /**
   * Quiesce the JPA container when it has some work to do
   * @throws Exception
   */
  public void testQuiesceAllBasic() throws Exception
  {
    BundleContext ctx = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file14", "");

    mgr.start(ctx);
    
    assertCorrectPersistenceProviderUsed(ctx, providerP101, 2);
    
    QuiesceCallback cbk = Skeleton.newMock(QuiesceCallback.class);
    
    QuiesceParticipant p = (QuiesceParticipant) ctx.getService(ctx.getServiceReference(QuiesceParticipant.class.getName()));
  
    p.quiesce(cbk, Collections.singletonList(extenderBundle));
    
    Thread.sleep(100);
    
    Skeleton.getSkeleton(cbk).assertCalledExactNumberOfTimes(new MethodCall(QuiesceCallback.class,
        "bundleQuiesced", Bundle[].class), 1);
    
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
  }
  
  /**
   * Quiesce the container when there are multiple active persistence bundles
   * @throws Exception
   */
  @Test
  public void testQuiesceAllComplex() throws Exception
  {
    BundleContext ctx = preExistingBundleSetup();
    
    registerVersionedPersistenceProviders();
    
    setupPersistenceBundle("file14", "");
    
    Bundle persistenceBundle2 = Skeleton.newMock(new BundleMock("scrappy.doo", new Hashtable<String, Object>()), Bundle.class);

    persistenceBundle2.getHeaders().put("Meta-Persistence", "");
    
    Skeleton skel = Skeleton.getSkeleton(persistenceBundle2);
    
    skel.setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.ACTIVE);
    
    URL rootURL = getClass().getClassLoader().getResource("file12");
    URL xml = getClass().getClassLoader().getResource("file12" + "/META-INF/persistence.xml");
    
    skel.setReturnValue(new MethodCall(Bundle.class, "getResource", "/"), rootURL);
    skel.setReturnValue(new MethodCall(Bundle.class, "getEntry", "META-INF/persistence.xml"), xml);
    skel.setReturnValue(new MethodCall(Bundle.class, "getVersion"), new Version("0.0.0"));
    

    mgr.start(ctx);
    
    mgr.addingBundle(persistenceBundle2, new BundleEvent(BundleEvent.STARTING, persistenceBundle2));
    
    ServiceReference[] alphas = ctx.getServiceReferences(
        EntityManagerFactory.class.getName(), "(osgi.unit.name=alpha)");
    
    EntityManager alpha1 = ((EntityManagerFactory) ctx.getService(alphas[0])).createEntityManager();
    
    EntityManager alpha2 = ((EntityManagerFactory) ctx.getService(alphas[1])).createEntityManager();
    
    EntityManager bravo = ((EntityManagerFactory) ctx.getService(ctx.getServiceReferences(
        EntityManagerFactory.class.getName(), "(osgi.unit.name=bravo)")[0])).createEntityManager();
    
    QuiesceCallback cbk = Skeleton.newMock(QuiesceCallback.class);
    
    QuiesceParticipant p = (QuiesceParticipant) ctx.getService(ctx.getServiceReference(QuiesceParticipant.class.getName()));
  
    p.quiesce(cbk, Collections.singletonList(extenderBundle));
    
    Thread.sleep(100);
    
    Skeleton.getSkeleton(cbk).assertNotCalled(new MethodCall(QuiesceCallback.class,
        "bundleQuiesced", Bundle[].class));
    
    assertEquals("Two should still be registered", 2, ctx.getServiceReferences(
        EntityManagerFactory.class.getName(), "(osgi.unit.name=alpha)").length);
    
    assertEquals("Should still be registered", 1, ctx.getServiceReferences(
        EntityManagerFactory.class.getName(), "(osgi.unit.name=bravo)").length);
    
    alpha1.close();
    
    Skeleton.getSkeleton(cbk).assertNotCalled(new MethodCall(QuiesceCallback.class,
        "bundleQuiesced", Bundle[].class));
    
    assertEquals("One should still be registered", 1, ctx.getServiceReferences(
        EntityManagerFactory.class.getName(), "(osgi.unit.name=alpha)").length);
    
    assertEquals("Should still be registered", 1, ctx.getServiceReferences(
        EntityManagerFactory.class.getName(), "(osgi.unit.name=bravo)").length);
    
    alpha2.close();
    
    Skeleton.getSkeleton(cbk).assertNotCalled(new MethodCall(QuiesceCallback.class,
        "bundleQuiesced", Bundle[].class));
    
    assertNull("Should be unregistered", ctx.getServiceReferences(
        EntityManagerFactory.class.getName(), "(osgi.unit.name=alpha)"));
    
    assertEquals("Should still be registered", 1, ctx.getServiceReferences(
        EntityManagerFactory.class.getName(), "(osgi.unit.name=bravo)").length);
    
    bravo.close();
    
    Skeleton.getSkeleton(cbk).assertCalledExactNumberOfTimes(new MethodCall(QuiesceCallback.class,
        "bundleQuiesced", Bundle[].class), 1);
    
    BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
  }
  
  @Test
  public void testDataSourceFactoryLifecycle() throws Exception
  {
    //Basic startup
    BundleContext extenderContext = preExistingBundleSetup();
    
    Hashtable<String,String> hash1 = new Hashtable<String, String>();
    hash1.put("javax.persistence.provider", "no.such.Provider");
    ServiceRegistration reg = persistenceBundle.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()} ,
        pp, hash1 );
    ServiceReference ref = reg.getReference();

    setupPersistenceBundle("file25", "");
    
    mgr.start(extenderContext);
    
    //Check the persistence.xml was looked for
    Skeleton.getSkeleton(persistenceBundle).assertCalled(new MethodCall(Bundle.class, "getEntry", "META-INF/persistence.xml"));
    //Check we didn't use getResource()
    Skeleton.getSkeleton(persistenceBundle).assertNotCalled(new MethodCall(Bundle.class, "getResource", String.class));
    
    //We should create them all, but then wait for the DataSourceFactory services
    testSuccessfulCreationEvent(ref, extenderContext, 3);
    testSuccessfulRegistrationEvent(ref, extenderContext, 0);
    
    //Register the DSF for alpha and it should appear
    hash1 = new Hashtable<String, String>();
    hash1.put(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS, "alpha.db.class");
    reg = persistenceBundle.getBundleContext().registerService(new String[] {DataSourceFactory.class.getName()} ,
        Skeleton.newMock(DataSourceFactory.class), hash1 );
    
    testSuccessfulRegistrationEvent(ref, extenderContext, 1, "alpha");
    
    //Register the other DSF
    hash1 = new Hashtable<String, String>();
    hash1.put(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS, "shared.db.class");
    persistenceBundle.getBundleContext().registerService(new String[] {DataSourceFactory.class.getName()} ,
        Skeleton.newMock(DataSourceFactory.class), hash1 );
    
    testSuccessfulRegistrationEvent(ref, extenderContext, 3, "alpha", "bravo", "charlie");
    
    
    //Unregister the service for alpha and it should go away again!
    reg.unregister();
    
    ServiceReference[] emfs = extenderContext.getServiceReferences(EntityManagerFactory.class.getName(), null);
    assertEquals("Too many services registered", 2, emfs.length);
    
    assertNotNull(extenderContext.getServiceReferences(
        EntityManagerFactory.class.getName(), "(osgi.unit.name=bravo)"));
    assertNotNull(extenderContext.getServiceReferences(
        EntityManagerFactory.class.getName(), "(osgi.unit.name=charlie)"));
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

  private void setupWABBundle() throws Exception {
    
    persistenceBundle.getHeaders().put("Web-ContextPath", "/test2");
    persistenceBundle.getHeaders().put("Bundle-ClassPath", "WEB-INF/classes/onClasspath, WEB-INF/lib/onClasspath.jar;" +
    		" WEB-INF/lib/alsoOnClasspath.jar; prop=\"value\"; complexProp:=\"a,b\";complexProp2:= \"c;d\";anotherProp=anotherValue; yetMoreProp=something");
    
    Skeleton skel = Skeleton.getSkeleton(persistenceBundle);
    skel.setReturnValue(new MethodCall(Bundle.class, "getState"), Bundle.ACTIVE);

    URL xml = getClass().getClassLoader().getResource("file23/META-INF/persistence.xml");
    skel.setReturnValue(new MethodCall(Bundle.class, "getEntry", "META-INF/persistence.xml"), xml);
    
    xml = getClass().getClassLoader().getResource("file23/WEB-INF/classes/onClasspath/META-INF/persistence.xml");
    skel.setReturnValue(new MethodCall(Bundle.class, "getEntry", "WEB-INF/classes/onClasspath/META-INF/persistence.xml"), xml);
    
    xml = getClass().getClassLoader().getResource("file23/WEB-INF/classes/notOnClasspath/META-INF/persistence.xml");
    skel.setReturnValue(new MethodCall(Bundle.class, "getEntry", "WEB-INF/classes/notOnClasspath/META-INF/persistence.xml"), xml);
    
    URL root = getClass().getClassLoader().getResource("file23");
    
    buildJarFile(skel, root, "WEB-INF/lib/onClasspath.jar", "jarOne");
    buildJarFile(skel, root, "WEB-INF/lib/alsoOnClasspath.jar", "jarTwo");
    
    buildJarFile(skel, root, "WEB-INF/lib/notOnClasspath.jar", "jarNotOnClassPath");
    
    skel.setReturnValue(new MethodCall(Bundle.class, "getVersion"), new Version("0.0.0"));
    
  }
  
  private void setupEJBBundle() throws Exception {
    
    setupWABBundle();
    persistenceBundle.getHeaders().remove("Web-ContextPath");
    persistenceBundle.getHeaders().put("Export-EJB", "");
  }

  private void buildJarFile(Skeleton skel, URL root, String filePath, String pUnitName) throws URISyntaxException,
      IOException, FileNotFoundException {
    
    File f = new File(new File(root.toURI()), filePath);
    
    f.getParentFile().mkdirs();
    
    JarOutputStream jos = new JarOutputStream(new FileOutputStream(f));
    
    jos.putNextEntry(new ZipEntry("META-INF/persistence.xml"));
    
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
    writer.write("<persistence-unit name=\"" + pUnitName + "\"/>");
    writer.newLine();
    writer.write("</persistence>");
    
    writer.close();
    
    skel.setReturnValue(new MethodCall(Bundle.class, "getEntry", filePath), f.toURI().toURL());
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
    
    reg = providerBundleP101.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()},
            providerP101, hash1 );
    
    reg = providerBundleP110.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()},
            providerP110, hash1 );
    
    reg = providerBundleP111.getBundleContext().registerService(new String[] {PersistenceProvider.class.getName()},
            providerP111, hash1 );
  }
  

  private void testSuccessfulCreationEvent(ServiceReference providerRef, BundleContext extenderContext, int numberOfPersistenceUnits)
  {
  //Check we loaded the Provider service
    Skeleton.getSkeleton(extenderContext).assertCalledExactNumberOfTimes(new MethodCall(BundleContext.class, "getService", providerRef), (numberOfPersistenceUnits == 0) ? 0 : 1);
    Skeleton.getSkeleton(extenderContext).assertCalledExactNumberOfTimes(new MethodCall(BundleContext.class, "ungetService", providerRef), (numberOfPersistenceUnits == 0) ? 0 : 1);
    Skeleton.getSkeleton(pp).assertCalledExactNumberOfTimes(new MethodCall(PersistenceProvider.class, "createContainerEntityManagerFactory", PersistenceUnitInfo.class, Map.class), numberOfPersistenceUnits);
  }
  
  private void testSuccessfulRegistrationEvent(ServiceReference providerRef, BundleContext extenderContext, int numberOfPersistenceUnits, String... names) throws InvalidSyntaxException
  {
    Skeleton.getSkeleton(persistenceBundleContext).assertCalledExactNumberOfTimes(new MethodCall(BundleContext.class, "registerService", EntityManagerFactory.class.getName(), EntityManagerFactory.class, Dictionary.class), numberOfPersistenceUnits);
    
    if(numberOfPersistenceUnits == 0) {
      BundleContextMock.assertNoServiceExists(EntityManagerFactory.class.getName());
      return;
    }
      
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
  
  private void assertCorrectPersistenceProviderUsed (BundleContext extenderContext, PersistenceProvider provider, int numEMFs) throws Exception
  {
      BundleContextMock.assertServiceExists(EntityManagerFactory.class.getName());

      ServiceReference[] refs = persistenceBundleContext.getServiceReferences(EntityManagerFactory.class.getCanonicalName(), null);
      
      assertEquals("Too many EMFs", numEMFs, refs.length);
      
      Skeleton.getSkeleton(provider).assertCalledExactNumberOfTimes(new MethodCall(PersistenceProvider.class, "createContainerEntityManagerFactory", PersistenceUnitInfo.class, Map.class), numEMFs);
      
      //for(ServiceReference emf : refs)
      //  assertSame("The EMF came from the wrong provider", Skeleton.getSkeleton(provider), Skeleton.getSkeleton(unwrap(persistenceBundleContext.getService(emf))));
      
      //More than one provider was instantiated
      Skeleton.getSkeleton(extenderContext).assertCalledExactNumberOfTimes(new MethodCall(BundleContext.class, "getService", ServiceReference.class), 1);
  }

  private void assertCorrectPersistenceProviderUsed (BundleContext extenderContext, PersistenceProvider provider) throws Exception
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

