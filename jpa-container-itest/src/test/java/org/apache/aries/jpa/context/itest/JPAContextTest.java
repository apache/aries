package org.apache.aries.jpa.context.itest;
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



import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackages;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContextType;
import javax.persistence.Query;
import javax.persistence.TransactionRequiredException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.transaction.UserTransaction;

import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.context.PersistenceContextProvider;
import org.apache.aries.jpa.container.itest.entities.Car;
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
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.util.tracker.ServiceTracker;

@RunWith(JUnit4TestRunner.class)
public class JPAContextTest {
  public static final long DEFAULT_TIMEOUT = 60000;

  @Inject
  protected BundleContext bundleContext;
 
  @Test
  public void findEntityManagerFactory() throws Exception {
    EntityManagerFactory emf = getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))", DEFAULT_TIMEOUT);
  }
  
  @Test
  public void findManagedContextFactory() throws Exception {
    EntityManagerFactory emf = null;
    try{
      getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))", 5000);
      fail("No context should exist");
    } catch (RuntimeException re) {
      //Expected
    }
    
    registerClient("test-unit");
    
    emf = getProxyEMF("test-unit");
    
  }

  private EntityManagerFactory getProxyEMF(String name) {
    
    return getOsgiService(EntityManagerFactory.class, "(&(osgi.unit.name=" + name + ")(" 
          + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
        "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))", DEFAULT_TIMEOUT);
  }
  
  @Test
  public void testTranRequired() throws Exception {
    registerClient("bp-test-unit");
    
    EntityManagerFactory emf = getProxyEMF("bp-test-unit");
    
    final EntityManager managedEm = emf.createEntityManager();
    
    ensureTREBehaviour(false, managedEm, "contains", new Object());
    ensureTREBehaviour(false, managedEm, "createNamedQuery", "hi");
    ensureTREBehaviour(false, managedEm, "createNativeQuery", "hi");
    ensureTREBehaviour(false, managedEm, "createNativeQuery", "hi", Object.class);
    ensureTREBehaviour(false, managedEm, "createNativeQuery", "hi", "hi");
    ensureTREBehaviour(false, managedEm, "createQuery", "hi");
    ensureTREBehaviour(false, managedEm, "find", Object.class, new Object());
    ensureTREBehaviour(true, managedEm, "flush");
    ensureTREBehaviour(false, managedEm, "getDelegate");
    ensureTREBehaviour(false, managedEm, "getFlushMode");
    ensureTREBehaviour(false, managedEm, "getReference", Object.class, new Object());
    ensureTREBehaviour(true, managedEm, "lock", new Object(), LockModeType.NONE);
    ensureTREBehaviour(true, managedEm, "merge", new Object());
    ensureTREBehaviour(true, managedEm, "persist", new Object());
    ensureTREBehaviour(true, managedEm, "refresh", new Object());
    ensureTREBehaviour(true, managedEm, "remove", new Object());
    ensureTREBehaviour(false, managedEm, "setFlushMode", FlushModeType.AUTO);
    ensureTREBehaviour(false, managedEm, "createNamedQuery", "hi", Object.class);
    ensureTREBehaviour(false, managedEm, "createQuery", Proxy.newProxyInstance(this.getClass().getClassLoader(),
       new Class[] {CriteriaQuery.class}, new InvocationHandler() {
      
      public Object invoke(Object proxy, Method method, Object[] args)
          throws Throwable {
        return null;
      }
    }));
    ensureTREBehaviour(false, managedEm, "createQuery", "hi", Object.class);
    ensureTREBehaviour(false, managedEm, "detach", new Object());
    ensureTREBehaviour(false, managedEm, "find", Object.class, new Object(), new HashMap());
    ensureTREBehaviour(false, managedEm, "find", Object.class, new Object(), LockModeType.NONE);
    ensureTREBehaviour(true, managedEm, "find", Object.class, new Object(), LockModeType.OPTIMISTIC);
    ensureTREBehaviour(false, managedEm, "find", Object.class, new Object(), LockModeType.NONE, 
        new HashMap());
    ensureTREBehaviour(true, managedEm, "find", Object.class, new Object(), LockModeType.OPTIMISTIC, 
        new HashMap());
    ensureTREBehaviour(false, managedEm, "getCriteriaBuilder");
    ensureTREBehaviour(true, managedEm, "getLockMode", new Object());
    ensureTREBehaviour(false, managedEm, "getMetamodel");
    ensureTREBehaviour(false, managedEm, "getProperties");
    ensureTREBehaviour(true, managedEm, "lock", new Object(), LockModeType.NONE, new HashMap());
    ensureTREBehaviour(true, managedEm, "refresh", new Object(), new HashMap());
    ensureTREBehaviour(true, managedEm, "refresh", new Object(), LockModeType.NONE);
    ensureTREBehaviour(true, managedEm, "refresh", new Object(), LockModeType.NONE, new HashMap());
    ensureTREBehaviour(false, managedEm, "setProperty", "hi", new Object());
    ensureTREBehaviour(false, managedEm, "unwrap", Object.class);

    UserTransaction ut = getOsgiService(UserTransaction.class);
    
    ut.begin();
    try{
      ensureTREBehaviour(false, managedEm, "contains", new Object());
      ensureTREBehaviour(false, managedEm, "createNamedQuery", "hi");
      ensureTREBehaviour(false, managedEm, "createNativeQuery", "hi");
      ensureTREBehaviour(false, managedEm, "createNativeQuery", "hi", Object.class);
      ensureTREBehaviour(false, managedEm, "createNativeQuery", "hi", "hi");
      ensureTREBehaviour(false, managedEm, "createQuery", "hi");
      ensureTREBehaviour(false, managedEm, "find", Object.class, new Object());
      ensureTREBehaviour(false, managedEm, "flush");
      ensureTREBehaviour(false, managedEm, "getDelegate");
      ensureTREBehaviour(false, managedEm, "getFlushMode");
      ensureTREBehaviour(false, managedEm, "getReference", Object.class, new Object());
      ensureTREBehaviour(false, managedEm, "lock", new Object(), LockModeType.NONE);
      ensureTREBehaviour(false, managedEm, "merge", new Object());
      ensureTREBehaviour(false, managedEm, "persist", new Object());
      ensureTREBehaviour(false, managedEm, "refresh", new Object());
      ensureTREBehaviour(false, managedEm, "remove", new Object());
      ensureTREBehaviour(false, managedEm, "setFlushMode", FlushModeType.AUTO);
      ensureTREBehaviour(false, managedEm, "createNamedQuery", "hi", Object.class);
      ensureTREBehaviour(false, managedEm, "createQuery", Proxy.newProxyInstance(this.getClass().getClassLoader(),
         new Class[] {CriteriaQuery.class}, new InvocationHandler() {
          
          public Object invoke(Object proxy, Method method, Object[] args)
              throws Throwable {
            return null;
          }
        }));
      ensureTREBehaviour(false, managedEm, "createQuery", "hi", Object.class);
      ensureTREBehaviour(false, managedEm, "detach", new Object());
      ensureTREBehaviour(false, managedEm, "find", Object.class, new Object(), new HashMap());
      ensureTREBehaviour(false, managedEm, "find", Object.class, new Object(), LockModeType.NONE);
      ensureTREBehaviour(false, managedEm, "find", Object.class, new Object(), LockModeType.OPTIMISTIC);
      ensureTREBehaviour(false, managedEm, "find", Object.class, new Object(), LockModeType.NONE, 
          new HashMap());
      ensureTREBehaviour(false, managedEm, "find", Object.class, new Object(), LockModeType.OPTIMISTIC, 
          new HashMap());
      ensureTREBehaviour(false, managedEm, "getCriteriaBuilder");
      ensureTREBehaviour(false, managedEm, "getLockMode", new Object());
      ensureTREBehaviour(false, managedEm, "getMetamodel");
      ensureTREBehaviour(false, managedEm, "getProperties");
      ensureTREBehaviour(false, managedEm, "lock", new Object(), LockModeType.NONE, new HashMap());
      ensureTREBehaviour(false, managedEm, "refresh", new Object(), new HashMap());
      ensureTREBehaviour(false, managedEm, "refresh", new Object(), LockModeType.NONE);
      ensureTREBehaviour(false, managedEm, "refresh", new Object(), LockModeType.NONE, new HashMap());
      ensureTREBehaviour(false, managedEm, "setProperty", "hi", new Object());
      ensureTREBehaviour(false, managedEm, "unwrap", Object.class);
    } finally {
      ut.rollback();
    }
  }
  
  @Test
  public void testNonTxEmIsCleared() throws Exception {
    
    registerClient("bp-test-unit");
    
    EntityManagerFactory emf = getProxyEMF("bp-test-unit");
    
    final EntityManager managedEm = emf.createEntityManager();
    
    UserTransaction ut = getOsgiService(UserTransaction.class);
    
    ut.begin();
    try {
      
      Query q = managedEm.createQuery("DELETE from Car c");
      q.executeUpdate();
      
      q = managedEm.createQuery("SELECT Count(c) from Car c");
      assertEquals(0l, q.getSingleResult());
      
      Car car = new Car();
      car.setNumberOfSeats(5);
      car.setEngineSize(1200);
      car.setColour("blue");
      car.setNumberPlate("A1AAA");
      managedEm.persist(car);
    } catch (Exception e) {
      e.printStackTrace();
    }finally {
      ut.commit();
    }
    
    Car c = managedEm.find(Car.class, "A1AAA");
    
    assertEquals(5, c.getNumberOfSeats());
    assertEquals(1200, c.getEngineSize());
    assertEquals("blue", c.getColour());
    
    ut.begin();
    try {
      Car car = managedEm.find(Car.class, "A1AAA");
      car.setNumberOfSeats(2);
      car.setEngineSize(2000);
      car.setColour("red");
    } finally {
      ut.commit();
    }
    
    c = managedEm.find(Car.class, "A1AAA");
    
    assertEquals(2, c.getNumberOfSeats());
    assertEquals(2000, c.getEngineSize());
    assertEquals("red", c.getColour());
    
  }

  @Test
  public void testNonTxQueries() throws Exception {
    
    registerClient("bp-test-unit");
    
    EntityManagerFactory emf = getProxyEMF("bp-test-unit");
    
    final EntityManager managedEm = emf.createEntityManager();
    
    UserTransaction ut = getOsgiService(UserTransaction.class);
    
    ut.begin();
    try {
      
      Query q = managedEm.createQuery("DELETE from Car c");
      q.executeUpdate();
      
      q = managedEm.createQuery("SELECT Count(c) from Car c");
      assertEquals(0l, q.getSingleResult());
    } finally {
      ut.commit();
    }
    
    Query countQuery = managedEm.createQuery("SELECT Count(c) from Car c");
    assertEquals(0l, countQuery.getSingleResult());
    
    ut.begin();
    try {
      Car car = new Car();
      car.setNumberOfSeats(5);
      car.setEngineSize(1200);
      car.setColour("blue");
      car.setNumberPlate("A1AAA");
      managedEm.persist(car);
      
      car = new Car();
      car.setNumberOfSeats(7);
      car.setEngineSize(1800);
      car.setColour("green");
      car.setNumberPlate("B2BBB");
      managedEm.persist(car);
    } finally {
      ut.commit();
    }
    
    assertEquals(2l, countQuery.getSingleResult());
    
    TypedQuery<Car> carQuery = managedEm.
             createQuery("Select c from Car c ORDER by c.engineSize", Car.class);
    
    List<Car> list = carQuery.getResultList();
    assertEquals(2l, list.size());
    
    assertEquals(5, list.get(0).getNumberOfSeats());
    assertEquals(1200, list.get(0).getEngineSize());
    assertEquals("blue", list.get(0).getColour());
    assertEquals("A1AAA", list.get(0).getNumberPlate());
    
    assertEquals(7, list.get(1).getNumberOfSeats());
    assertEquals(1800, list.get(1).getEngineSize());
    assertEquals("green", list.get(1).getColour());
    assertEquals("B2BBB", list.get(1).getNumberPlate());
    
    ut.begin();
    try {
      Car car = managedEm.find(Car.class, "A1AAA");
      car.setNumberOfSeats(2);
      car.setEngineSize(2000);
      car.setColour("red");
      
      car = managedEm.find(Car.class, "B2BBB");
      managedEm.remove(car);
      
      car = new Car();
      car.setNumberOfSeats(2);
      car.setEngineSize(800);
      car.setColour("black");
      car.setNumberPlate("C3CCC");
      managedEm.persist(car);
      
    } finally {
      ut.commit();
    }
    
    assertEquals(2l, countQuery.getSingleResult());
    
    list = carQuery.getResultList();
    assertEquals(2l, list.size());
    
    assertEquals(2, list.get(0).getNumberOfSeats());
    assertEquals(800, list.get(0).getEngineSize());
    assertEquals("black", list.get(0).getColour());
    assertEquals("C3CCC", list.get(0).getNumberPlate());
    
    assertEquals(5, list.get(1).getNumberOfSeats());
    assertEquals(1200, list.get(1).getEngineSize());
    assertEquals("blue", list.get(1).getColour());
    assertEquals("A1AAA", list.get(1).getNumberPlate());
  }
  
  private void registerClient(String name) {
    PersistenceContextProvider provider = getOsgiService(PersistenceContextProvider.class);
    
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.TRANSACTION);
    provider.registerContext(name, bundleContext.getBundle(), props);
  }

  private void ensureTREBehaviour(boolean expectedToFail, EntityManager em, String methodName, Object... args) throws Exception {
    
    List<Class> argTypes = new ArrayList<Class>();
    for(Object o : args) {
      if(o instanceof Map)
        argTypes.add(Map.class);
      else if (o instanceof CriteriaQuery)
        argTypes.add(CriteriaQuery.class);
      else
        argTypes.add(o.getClass());
    }
    
    Method m = EntityManager.class.getMethod(methodName, 
        argTypes.toArray(new Class[args.length]));
    
    try {
      m.invoke(em, args);
      if(expectedToFail)
        fail("A transaction is required");
    } catch (InvocationTargetException ite) {
      if(expectedToFail && 
          !!!(ite.getCause() instanceof TransactionRequiredException))
        fail("We got the wrong failure. Expected a TransactionRequiredException" +
        		", got a " + ite.toString());
      else if (!!!expectedToFail && 
          ite.getCause() instanceof TransactionRequiredException)
        fail("We got the wrong failure. Expected not to get a TransactionRequiredException" +
            ", but we got one anyway!");
    }
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
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        // Adding blueprint to the runtime is a hack to placate the maven bundle plugin. 
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jpa_2.0_spec"),
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.api"),
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.core"),
        mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.url"),
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.api"),
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container"),
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.context"),
        mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.manager" ),
        mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.wrappers" ),
        mavenBundle("org.apache.derby", "derby"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec"),
        mavenBundle("commons-lang", "commons-lang"),
        mavenBundle("commons-collections", "commons-collections"),
        mavenBundle("commons-pool", "commons-pool"),
        mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.serp"),
        mavenBundle("org.apache.openjpa", "openjpa"),

//        mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.jpa"),
//        mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.core"),
//        mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.asm"),
        
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.itest.bundle"),
        
//        vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006"),
//        waitForFrameworkStartup(),
        
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
