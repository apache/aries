/*  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.jpa.quiesce.itest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackages;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import java.util.Collections;
import java.util.HashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;
import javax.transaction.UserTransaction;

import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.context.PersistenceContextProvider;
import org.apache.aries.quiesce.manager.QuiesceCallback;
import org.apache.aries.quiesce.participant.QuiesceParticipant;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.BootDelegationOption;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.util.tracker.ServiceTracker;

import com.sun.xml.internal.messaging.saaj.util.TeeInputStream;

@RunWith(JUnit4TestRunner.class)
public class QuiesceJPATest {
  
  private static class TestQuiesceCallback implements QuiesceCallback{

    private int calls = 0;
    
    public void bundleQuiesced(Bundle... arg0) {
      calls++;
    }

    public boolean bundleClearedUp()
    {
      return calls == 1;
    }
  }
  
  private class MultiQuiesceCallback implements QuiesceCallback{

    private int calls = 0;
    
    private boolean contextFirst = true;
    
    public void bundleQuiesced(Bundle... arg0) {
      if(++calls == 1)
        try {
          getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
              + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))", DEFAULT_TIMEOUT);
        } catch (Throwable t){
          contextFirst = false;
          if(t instanceof RuntimeException)
            throw (RuntimeException) t;
          else if (t instanceof Error)
            throw (Error) t;
          else
            throw new RuntimeException(t);
        }
            
    }

    public boolean bundleClearedUp()
    {
      return calls == 2 && contextFirst;
    }
  }
  
  
  public static final long DEFAULT_TIMEOUT = 30000;

  @Inject
  protected BundleContext bundleContext;
 
  
  @After
  public void restartTestBundles() throws BundleException {
    Bundle b = getBundle("org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle");
    b.stop();
    b.start();
    
    b = getBundle("org.apache.aries.jpa.container");
    b.stop();
    b.start();
    
    b = getBundle("org.apache.aries.jpa.container.context");
    b.stop();
    b.start();
  }
  
  @Test
  public void testSimpleContextQuiesce() throws Exception {

    //Get a managed context registered
    PersistenceContextProvider provider = getOsgiService(PersistenceContextProvider.class);
    
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.TRANSACTION);
    provider.registerContext("test-unit", bundleContext.getBundle(), props);
    
    EntityManagerFactory emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))", DEFAULT_TIMEOUT);
    
    
    //Quiesce it
    QuiesceParticipant participant = getParticipant("org.apache.aries.jpa.container.context");
    
    TestQuiesceCallback callback = new TestQuiesceCallback();
    
    participant.quiesce(callback, Collections.singletonList(getBundle(
        "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle")));
    
    Thread.sleep(1000);
    
    assertTrue("Quiesce not finished", callback.bundleClearedUp());
    
    ServiceReference[] refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    assertNull("No context should exist",refs);
    
    //Restart the bundle to check the context gets re-registered
    Bundle b = getBundle("org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle");
    b.stop();
    b.start();
    
    emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
      "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))", DEFAULT_TIMEOUT);
  }

  @Test
  public void testComplexContextQuiesce() throws Exception {

   // Get a managed context registered
    PersistenceContextProvider provider = getOsgiService(PersistenceContextProvider.class);
    
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.TRANSACTION);
    provider.registerContext("test-unit", bundleContext.getBundle(), props);
    
    EntityManagerFactory emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))", DEFAULT_TIMEOUT);
    
    
    //Set up a transaction so we can check the Quiesce waits properly
    UserTransaction tm = getOsgiService(UserTransaction.class);
    
    tm.begin();
    
    emf.createEntityManager().getProperties();
    
    QuiesceParticipant participant = getParticipant("org.apache.aries.jpa.container.context");
    
    TestQuiesceCallback callback = new TestQuiesceCallback();
    
    participant.quiesce(callback, Collections.singletonList(getBundle(
        "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle")));
    
    Thread.sleep(1000);
    
    assertFalse("Quiesce finished", callback.bundleClearedUp());
    
    emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
      "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))", DEFAULT_TIMEOUT);
    
    tm.commit();
    
    assertTrue("Quiesce not finished", callback.bundleClearedUp());
    
    ServiceReference[] refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    assertNull("No context should exist",refs);
    
    
    //Restart the bundle to check the context gets re-registered, then ensure it isn't
    //tidied up immediately again!
    Bundle b = getBundle("org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle");
    b.stop();
    b.start();
    
    emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
      "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))", DEFAULT_TIMEOUT);
    
    tm.begin();
    
    emf.createEntityManager().getProperties();
    
    tm.commit();
    
    Thread.sleep(1000);
    
    emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
      "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))", 100);
  }
  
  @Test
  public void testContextRuntimeQuiesce() throws Exception {

    PersistenceContextProvider provider = getOsgiService(PersistenceContextProvider.class);
    
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.TRANSACTION);
    provider.registerContext("test-unit", bundleContext.getBundle(), props);
    
    EntityManagerFactory emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))", DEFAULT_TIMEOUT);
    
    
    UserTransaction tm = getOsgiService(UserTransaction.class);
    
    tm.begin();
    
    emf.createEntityManager().getProperties();
    
    QuiesceParticipant participant = getParticipant("org.apache.aries.jpa.container.context");
    
    TestQuiesceCallback callback = new TestQuiesceCallback();
    
    participant.quiesce(callback, Collections.singletonList(getBundle(
        "org.apache.aries.jpa.container.context")));
    
    Thread.sleep(1000);
    
    assertFalse("Quiesce not finished", callback.bundleClearedUp());
    
    emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
      "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))", DEFAULT_TIMEOUT);
    
    tm.commit();
    
    assertTrue("Quiesce not finished", callback.bundleClearedUp());
    
    ServiceReference[] refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    assertNull("No context should exist",refs);
  }
  
  @Test
  public void testSimpleUnitQuiesce() throws Exception {

    
    EntityManagerFactory emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))", DEFAULT_TIMEOUT);
    
    QuiesceParticipant participant = getParticipant("org.apache.aries.jpa.container");
    
    TestQuiesceCallback callback = new TestQuiesceCallback();
    
    participant.quiesce(callback, Collections.singletonList(getBundle(
        "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle")));
    
    Thread.sleep(1000);
    
    assertTrue("Quiesce not finished", callback.bundleClearedUp());
    
    ServiceReference[] refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    assertNull("No unit should exist",refs);
    
    //Restart the bundle to check the unit gets re-registered
    Bundle b = getBundle("org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle");
    b.stop();
    b.start();
    
    emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))", DEFAULT_TIMEOUT);
  }

  @Test
  public void testComplexUnitQuiesce() throws Exception {

    
    EntityManagerFactory emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))", DEFAULT_TIMEOUT);
    
    EntityManager em = emf.createEntityManager();
    
    QuiesceParticipant participant = getParticipant("org.apache.aries.jpa.container");
    
    TestQuiesceCallback callback = new TestQuiesceCallback();
    
    participant.quiesce(callback, Collections.singletonList(getBundle(
        "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle")));
    
    Thread.sleep(1000);
    
    assertFalse("Quiesce finished", callback.bundleClearedUp());
    
    emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))", DEFAULT_TIMEOUT);
    
    em.close();
    
    assertTrue("Quiesce not finished", callback.bundleClearedUp());
    
    ServiceReference[] refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    assertNull("No context should exist",refs);
    
    //Restart the bundle to check the unit gets re-registered and is not immediately unregistered
    Bundle b = getBundle("org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle");
    b.stop();
    b.start();
    
    emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))", DEFAULT_TIMEOUT);
    
    em = emf.createEntityManager();
    em.close();
    
    emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))", 100);
  }
  
  @Test
  public void testContainerRuntimeQuiesce() throws Exception {
    
    EntityManagerFactory emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))", DEFAULT_TIMEOUT);
    
    
    EntityManager em = emf.createEntityManager();
    
    QuiesceParticipant participant = getParticipant("org.apache.aries.jpa.container");
    
    TestQuiesceCallback callback = new TestQuiesceCallback();
    
    participant.quiesce(callback, Collections.singletonList(getBundle(
        "org.apache.aries.jpa.container")));
    
    Thread.sleep(1000);
    
    assertFalse("Quiesce finished early", callback.bundleClearedUp());
    
    emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))", DEFAULT_TIMEOUT);

    em.close();
    
    assertTrue("Quiesce not finished", callback.bundleClearedUp());
    
    ServiceReference[] refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    assertNull("No context should exist",refs);
  }
  
  @Test
  public void testComplexQuiesceInteraction() throws Exception {

   // Get a managed context registered
    PersistenceContextProvider provider = getOsgiService(PersistenceContextProvider.class);
    
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.TRANSACTION);
    provider.registerContext("test-unit", bundleContext.getBundle(), props);
    
    EntityManagerFactory emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))", DEFAULT_TIMEOUT);
    
    
    //Set up a transaction so we can check the Quiesce waits properly
    UserTransaction tm = getOsgiService(UserTransaction.class);
    
    tm.begin();
    
    emf.createEntityManager().getProperties();
    
    //Quiesce the Unit, nothing should happen
    QuiesceParticipant participant = getParticipant("org.apache.aries.jpa.container");
    
    TestQuiesceCallback unitCallback = new TestQuiesceCallback();
    
    participant.quiesce(unitCallback, Collections.singletonList(getBundle(
        "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle")));
    
    Thread.sleep(1000);
    
    assertFalse("Quiesce finished", unitCallback.bundleClearedUp());
    
    emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
      "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))", DEFAULT_TIMEOUT);
    
    
    //Quiesce the context, still nothing
    
    participant = getParticipant("org.apache.aries.jpa.container.context");
    
    TestQuiesceCallback contextCallback = new TestQuiesceCallback();
    
    participant.quiesce(contextCallback, Collections.singletonList(getBundle(
    "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle")));
    
    Thread.sleep(1000);
    
    assertFalse("Quiesce finished", unitCallback.bundleClearedUp());
    assertFalse("Quiesce finished", contextCallback.bundleClearedUp());
    
    emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
      "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))", DEFAULT_TIMEOUT);
    
    //Keep the unit alive
    
    emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))", DEFAULT_TIMEOUT);
  
  
    EntityManager em = emf.createEntityManager();
    
    tm.commit();
    
    assertTrue("Quiesce not finished", contextCallback.bundleClearedUp());
    
    ServiceReference[] refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))");
    
    assertNull("No context should exist",refs);
    
    //Still a unit
    emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))", DEFAULT_TIMEOUT);
    
    em.close();
    
    assertTrue("Quiesce not finished", unitCallback.bundleClearedUp());
    
    refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
  
    assertNull("No unit should exist",refs);
  }
  
  @Test
  public void testComplexQuiesceInteraction2() throws Exception {

   // Get a managed context registered
    PersistenceContextProvider provider = getOsgiService(PersistenceContextProvider.class);
    
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.TRANSACTION);
    provider.registerContext("test-unit", bundleContext.getBundle(), props);
    
    EntityManagerFactory emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))", DEFAULT_TIMEOUT);
    
    
    //Set up a transaction so we can check the Quiesce waits properly
    UserTransaction tm = getOsgiService(UserTransaction.class);
    
    tm.begin();
    
    emf.createEntityManager().getProperties();
    
    //Quiesce the Unit, nothing should happen
    QuiesceParticipant participant = getParticipant("org.apache.aries.jpa.container");
    
    MultiQuiesceCallback callback = new MultiQuiesceCallback();
    
    participant.quiesce(callback, Collections.singletonList(getBundle(
        "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle")));
    
    //Quiesce the context, still nothing
    participant = getParticipant("org.apache.aries.jpa.container.context");
    
    participant.quiesce(callback, Collections.singletonList(getBundle(
       "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle")));
    
    Thread.sleep(1000);
    
    assertFalse("Quiesce finished", callback.bundleClearedUp());
    
    emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
      "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))", DEFAULT_TIMEOUT);
    
    emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
        + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))", DEFAULT_TIMEOUT);
    
    
    tm.commit();
    
    assertTrue("Quiesce not finished", callback.bundleClearedUp());
    
    ServiceReference[] refs = bundleContext.getAllServiceReferences(EntityManagerFactory.class.getName(), "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
    
    assertNull("No context should exist",refs);
    
  }
  
  
  
  private QuiesceParticipant getParticipant(String bundleName) throws InvalidSyntaxException {
    ServiceReference[] refs = bundleContext.getServiceReferences(QuiesceParticipant.class.getName(), null);
    
    if(refs != null) {
      for(ServiceReference ref : refs) {
        if(ref.getBundle().getSymbolicName().equals(bundleName))
          return (QuiesceParticipant) bundleContext.getService(ref);
      }
    }
    
    
    return null;
  }

  @org.ops4j.pax.exam.junit.Configuration
  public static Option[] configuration() {
    Option[] options = options(
        bootDelegationPackages("javax.transaction", "javax.transaction.*"),
        vmOption("-Dorg.osgi.framework.system.packages=javax.accessibility,javax.activation,javax.activity,javax.annotation,javax.annotation.processing,javax.crypto,javax.crypto.interfaces,javax.crypto.spec,javax.imageio,javax.imageio.event,javax.imageio.metadata,javax.imageio.plugins.bmp,javax.imageio.plugins.jpeg,javax.imageio.spi,javax.imageio.stream,javax.jws,javax.jws.soap,javax.lang.model,javax.lang.model.element,javax.lang.model.type,javax.lang.model.util,javax.management,javax.management.loading,javax.management.modelmbean,javax.management.monitor,javax.management.openmbean,javax.management.relation,javax.management.remote,javax.management.remote.rmi,javax.management.timer,javax.naming,javax.naming.directory,javax.naming.event,javax.naming.ldap,javax.naming.spi,javax.net,javax.net.ssl,javax.print,javax.print.attribute,javax.print.attribute.standard,javax.print.event,javax.rmi,javax.rmi.CORBA,javax.rmi.ssl,javax.script,javax.security.auth,javax.security.auth.callback,javax.security.auth.kerberos,javax.security.auth.login,javax.security.auth.spi,javax.security.auth.x500,javax.security.cert,javax.security.sasl,javax.sound.midi,javax.sound.midi.spi,javax.sound.sampled,javax.sound.sampled.spi,javax.sql,javax.sql.rowset,javax.sql.rowset.serial,javax.sql.rowset.spi,javax.swing,javax.swing.border,javax.swing.colorchooser,javax.swing.event,javax.swing.filechooser,javax.swing.plaf,javax.swing.plaf.basic,javax.swing.plaf.metal,javax.swing.plaf.multi,javax.swing.plaf.synth,javax.swing.table,javax.swing.text,javax.swing.text.html,javax.swing.text.html.parser,javax.swing.text.rtf,javax.swing.tree,javax.swing.undo,javax.tools,javax.xml,javax.xml.bind,javax.xml.bind.annotation,javax.xml.bind.annotation.adapters,javax.xml.bind.attachment,javax.xml.bind.helpers,javax.xml.bind.util,javax.xml.crypto,javax.xml.crypto.dom,javax.xml.crypto.dsig,javax.xml.crypto.dsig.dom,javax.xml.crypto.dsig.keyinfo,javax.xml.crypto.dsig.spec,javax.xml.datatype,javax.xml.namespace,javax.xml.parsers,javax.xml.soap,javax.xml.stream,javax.xml.stream.events,javax.xml.stream.util,javax.xml.transform,javax.xml.transform.dom,javax.xml.transform.sax,javax.xml.transform.stax,javax.xml.transform.stream,javax.xml.validation,javax.xml.ws,javax.xml.ws.handler,javax.xml.ws.handler.soap,javax.xml.ws.http,javax.xml.ws.soap,javax.xml.ws.spi,javax.xml.xpath,org.ietf.jgss,org.omg.CORBA,org.omg.CORBA.DynAnyPackage,org.omg.CORBA.ORBPackage,org.omg.CORBA.TypeCodePackage,org.omg.CORBA.portable,org.omg.CORBA_2_3,org.omg.CORBA_2_3.portable,org.omg.CosNaming,org.omg.CosNaming.NamingContextExtPackage,org.omg.CosNaming.NamingContextPackage,org.omg.Dynamic,org.omg.DynamicAny,org.omg.DynamicAny.DynAnyFactoryPackage,org.omg.DynamicAny.DynAnyPackage,org.omg.IOP,org.omg.IOP.CodecFactoryPackage,org.omg.IOP.CodecPackage,org.omg.Messaging,org.omg.PortableInterceptor,org.omg.PortableInterceptor.ORBInitInfoPackage,org.omg.PortableServer,org.omg.PortableServer.CurrentPackage,org.omg.PortableServer.POAManagerPackage,org.omg.PortableServer.POAPackage,org.omg.PortableServer.ServantLocatorPackage,org.omg.PortableServer.portable,org.omg.SendingContext,org.omg.stub.java.rmi,org.w3c.dom,org.w3c.dom.bootstrap,org.w3c.dom.css,org.w3c.dom.events,org.w3c.dom.html,org.w3c.dom.ls,org.w3c.dom.ranges,org.w3c.dom.stylesheets,org.w3c.dom.traversal,org.w3c.dom.views,org.xml.sax,org.xml.sax.ext,org.xml.sax.helpers,javax.transaction;partial=true;mandatory:=partial,javax.transaction.xa;partial=true;mandatory:=partial"),
        
        // Log
        mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
        mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),
        // Felix Config Admin
        mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
        // Felix mvn url handler
        mavenBundle("org.ops4j.pax.url", "pax-url-mvn"),

        // this is how you set the default log level when using pax
        // logging (logProfile)
        systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),

        // Bundles
        mavenBundle("org.osgi", "org.osgi.compendium"),
        mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.cglib"),
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.api"),
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.core"),
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.url"),
        mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.testds"),
        mavenBundle("org.apache.derby", "derby"),
        mavenBundle("org.apache.aries.quiesce", "org.apache.aries.quiesce.api"),
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"), 
        mavenBundle("org.apache.geronimo.specs", "geronimo-jpa_2.0_spec"),
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.api"),
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container"),
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.context"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec"),
        mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.manager"),
        mavenBundle("commons-lang", "commons-lang"),
        mavenBundle("commons-collections", "commons-collections"),
        mavenBundle("commons-pool", "commons-pool"),
        mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.serp"),
        mavenBundle("org.apache.openjpa", "openjpa"),

//        mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.jpa"),
//        mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.core"),
//        mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.asm"),
        
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.itest.bundle"),
        
        equinox().version("3.5.0"));
    options = updateOptions(options);
    return options;
  }
  
  
  protected Bundle getBundle(String symbolicName) {
    return getBundle(symbolicName, null);
  }

  protected Bundle getBundle(String bundleSymbolicName, String version) {
    Bundle result = null;
    for (Bundle b : bundleContext.getBundles()) {
      if (b.getSymbolicName().equals(bundleSymbolicName)) {
        if (version == null
            || b.getVersion().equals(Version.parseVersion(version))) {
          result = b;
          break;
        }
      }
    }
    return result;
  }

  public static BootDelegationOption bootDelegation() {
    return new BootDelegationOption("org.apache.aries.unittest.fixture");
  }
  
  public static MavenArtifactProvisionOption mavenBundle(String groupId,
      String artifactId) {
    return CoreOptions.mavenBundle().groupId(groupId).artifactId(artifactId)
        .versionAsInProject();
  }

  protected static Option[] updateOptions(Option[] options) {
    // We need to add pax-exam-junit here when running with the ibm
    // jdk to avoid the following exception during the test run:
    // ClassNotFoundException: org.ops4j.pax.exam.junit.Configuration
    if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
      Option[] ibmOptions = options(wrappedBundle(mavenBundle(
          "org.ops4j.pax.exam", "pax-exam-junit")));
      options = combine(ibmOptions, options);
    }

    return options;
  }

  protected <T> T getOsgiService(Class<T> type, long timeout) {
    return getOsgiService(type, null, timeout);
  }

  protected <T> T getOsgiService(Class<T> type) {
    return getOsgiService(type, null, DEFAULT_TIMEOUT);
  }
  
  protected <T> T getOsgiService(Class<T> type, String filter, long timeout) {
    return getOsgiService(null, type, filter, timeout);
  }

  protected <T> T getOsgiService(BundleContext bc, Class<T> type,
      String filter, long timeout) {
    ServiceTracker tracker = null;
    try {
      String flt;
      if (filter != null) {
        if (filter.startsWith("(")) {
          flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")"
              + filter + ")";
        } else {
          flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")("
              + filter + "))";
        }
      } else {
        flt = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
      }
      Filter osgiFilter = FrameworkUtil.createFilter(flt);
      tracker = new ServiceTracker(bc == null ? bundleContext : bc, osgiFilter,
          null);
      tracker.open();
      // Note that the tracker is not closed to keep the reference
      // This is buggy, has the service reference may change i think
      Object svc = type.cast(tracker.waitForService(timeout));
      if (svc == null) {
        throw new RuntimeException("Gave up waiting for service " + flt);
      }
      return type.cast(svc);
    } catch (InvalidSyntaxException e) {
      throw new IllegalArgumentException("Invalid filter", e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
