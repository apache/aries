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
package org.apache.aries.jndi.url;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;

import org.apache.aries.jndi.ContextHelper;
import org.apache.aries.jndi.OSGiObjectFactoryBuilder;
import org.apache.aries.mocks.BundleContextMock;
import org.apache.aries.mocks.BundleMock;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Tests for our JNDI implementation for the service registry.
 */
public class ServiceRegistryContextTest
{
  /** The service we register by default */
  private Runnable service;
  /** The bundle context for the test */
  private BundleContext bc;
  /** The service registration for the service */
  private ServiceRegistration reg;
  
  /**
   * This method does the setup to ensure we always have a service.
   * @throws NamingException 
   * @throws NoSuchFieldException 
   * @throws SecurityException 
   * @throws IllegalAccessException 
   * @throws IllegalArgumentException 
   */
  @Before
  public void registerService() throws NamingException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException 
  {
    bc =  Skeleton.newMock(new BundleContextMock(), BundleContext.class);
    new Activator().start(bc);
    new org.apache.aries.jndi.startup.Activator().start(bc);
    
    Field f = ContextHelper.class.getDeclaredField("context");
    f.setAccessible(true);
    f.set(null, bc);
    f = OSGiObjectFactoryBuilder.class.getDeclaredField("context");
    f.setAccessible(true);
    f.set(null, bc);


    service = Skeleton.newMock(Runnable.class);
    
    registerService(service);
  }
  
  /**
   * Register a service in our map.
   * 
   * @param service2 The service to register.
   */
  private void registerService(Runnable service2)
  {
    ServiceFactory factory = Skeleton.newMock(ServiceFactory.class);
    Skeleton skel = Skeleton.getSkeleton(factory);
    
    skel.setReturnValue(new MethodCall(ServiceFactory.class, "getService", Bundle.class, ServiceRegistration.class), service2);
    
    Hashtable<String, String> props = new Hashtable<String, String>();
    props.put("rubbish", "smelly");
    
    reg = bc.registerService(new String[] {"java.lang.Runnable"}, factory, props);
  }
  
  /**
   * Make sure we clear the caches out before the next test.
   */
  @After
  public void teardown()
  {
    BundleContextMock.clear();
    
    Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
  }
  
  /**
   * This test checks that we correctly register and deregister the url context
   * object factory in the service registry.
   */
  @Test
  public void testJNDIRegistration()
  {
    ServiceReference ref = bc.getServiceReference(ObjectFactory.class.getName());
    
    assertNotNull("The aries url context object factory was not registered", ref);
    
    ObjectFactory factory = (ObjectFactory) bc.getService(ref);
    
    assertNotNull("The aries url context object factory was null", factory);
  }
  
  /**
   * This test does a simple JNDI lookup to prove that works.
   * @throws NamingException
   */
  @Test
  public void simpleJNDILookup() throws NamingException
  {
    System.setProperty(Context.URL_PKG_PREFIXES, "helloMatey");
        
    InitialContext ctx = new InitialContext(new Hashtable<Object, Object>());
    
    BundleMock mock = new BundleMock("scooby.doo", new Properties());
    
    Thread.currentThread().setContextClassLoader(mock.getClassLoader());
    
    Runnable s = (Runnable) ctx.lookup("aries:services/java.lang.Runnable");
    
    assertNotNull("We didn't get a service back from our lookup :(", s);
    
    s.run();
    
    Skeleton.getSkeleton(service).assertCalledExactNumberOfTimes(new MethodCall(Runnable.class, "run"), 1);
    
    Skeleton skel = Skeleton.getSkeleton(mock.getBundleContext());
    
    skel.assertCalled(new MethodCall(BundleContext.class, "getServiceReferences", "java.lang.Runnable", null));

    mock = new BundleMock("scooby.doo", new Properties());
    
    Thread.currentThread().setContextClassLoader(mock.getClassLoader());

    s = (Runnable) ctx.lookup("osgi:service/java.lang.Runnable");
    
    // Check we have the packages set correctly
    
    String packages = System.getProperty(Context.URL_PKG_PREFIXES, null);
    
    assertTrue(ctx.getEnvironment().containsValue(packages));

    assertNotNull("We didn't get a service back from our lookup :(", s);

    s.run();
    
    Skeleton.getSkeleton(service).assertCalledExactNumberOfTimes(new MethodCall(Runnable.class, "run"), 2);

    skel = Skeleton.getSkeleton(mock.getBundleContext());
    skel.assertCalled(new MethodCall(BundleContext.class, "getServiceReferences", "java.lang.Runnable", null));
  }

  /**
   * This test checks that we can pass a filter in without things blowing up.
   * Right now our mock service registry does not implement filtering, so the
   * effect is the same as simpleJNDILookup, but we at least know it is not
   * blowing up.
   * 
   * @throws NamingException
   */
  @Test
  public void jndiLookupWithFilter() throws NamingException
  {
    BundleMock mock = new BundleMock("scooby.doo", new Properties());
    
    Thread.currentThread().setContextClassLoader(mock.getClassLoader());

    InitialContext ctx = new InitialContext();
    
    Object s = ctx.lookup("aries:services/java.lang.Runnable/(rubbish=smelly)");
    
    assertNotNull("We didn't get a service back from our lookup :(", s);
    
    service.run();
    
    Skeleton.getSkeleton(service).assertCalledExactNumberOfTimes(new MethodCall(Runnable.class, "run"), 1);

    Skeleton.getSkeleton(mock.getBundleContext()).assertCalled(new MethodCall(BundleContext.class, "getServiceReferences", "java.lang.Runnable", "(rubbish=smelly)"));
  }
  
  /**
   * Check that we get a NameNotFoundException if we lookup after the service
   * has been unregistered.
   * 
   * @throws NamingException
   */
  @Test(expected=NameNotFoundException.class)
  public void testLookupWhenServiceHasBeenRemoved() throws NamingException
  {
    reg.unregister();

    BundleMock mock = new BundleMock("scooby.doo", new Properties());
    
    Thread.currentThread().setContextClassLoader(mock.getClassLoader());

    InitialContext ctx = new InitialContext();
    
    ctx.lookup("aries:services/java.lang.Runnable");
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
    BundleMock mock = new BundleMock("scooby.doo", new Properties());
    
    Thread.currentThread().setContextClassLoader(mock.getClassLoader());

    InitialContext ctx = new InitialContext();
    
    ctx.lookup("aries:services/java.lang.Integer");
  }
  
  /**
   * This test checks that we can list the contents of the repository using the
   * list method
   * 
   * @throws NamingException
   */
  public void listRepositoryContents() throws NamingException
  {
    InitialContext ctx = new InitialContext();
    
    NamingEnumeration<NameClassPair> serviceList = ctx.list("aries:services/java.lang.Runnable/(rubbish=smelly)");
    
    checkThreadRetrievedViaListMethod(serviceList);
    
    assertFalse("The repository contained more objects than we expected", serviceList.hasMoreElements());
    
    //Now add a second service
    
    registerService(new Thread());
    
    serviceList = ctx.list("aries:services/java.lang.Runnable/(rubbish=smelly)");
    
    checkThreadRetrievedViaListMethod(serviceList);
    
    checkThreadRetrievedViaListMethod(serviceList);
    
    assertFalse("The repository contained more objects than we expected", serviceList.hasMoreElements());
  }

  @Test
  public void checkProxyDynamism() throws NamingException
  {
    BundleMock mock = new BundleMock("scooby.doo", new Properties());
    
    Thread.currentThread().setContextClassLoader(mock.getClassLoader());

    InitialContext ctx = new InitialContext();
    
    String className = Runnable.class.getName();
    
    Runnable t = Skeleton.newMock(Runnable.class);
    Runnable t2 = Skeleton.newMock(Runnable.class);
    
    // we don't want the default service
    reg.unregister();
    
    ServiceRegistration reg = bc.registerService(className, t, null);
    bc.registerService(className, t2, null);
    
    Runnable r = (Runnable) ctx.lookup("osgi:service/java.lang.Runnable");
    
    r.run();
    
    Skeleton.getSkeleton(t).assertCalledExactNumberOfTimes(new MethodCall(Runnable.class, "run"), 1);
    Skeleton.getSkeleton(t2).assertNotCalled(new MethodCall(Runnable.class, "run"));
    
    reg.unregister();
    
    r.run();
    
    Skeleton.getSkeleton(t).assertCalledExactNumberOfTimes(new MethodCall(Runnable.class, "run"), 1);
    Skeleton.getSkeleton(t2).assertCalledExactNumberOfTimes(new MethodCall(Runnable.class, "run"), 1);
  }  

  @Test
  public void checkServiceOrderObserved() throws NamingException
  {
    BundleMock mock = new BundleMock("scooby.doo", new Properties());
    
    Thread.currentThread().setContextClassLoader(mock.getClassLoader());

    InitialContext ctx = new InitialContext();
    
    String className = Runnable.class.getName();
    
    Runnable t = Skeleton.newMock(Runnable.class);
    Runnable t2 = Skeleton.newMock(Runnable.class);
    
    // we don't want the default service
    reg.unregister();
    
    ServiceRegistration reg = bc.registerService(className, t, null);
    ServiceRegistration reg2 = bc.registerService(className, t2, null);
    
    Runnable r = (Runnable) ctx.lookup("osgi:service/java.lang.Runnable");
    
    r.run();
    
    Skeleton.getSkeleton(t).assertCalledExactNumberOfTimes(new MethodCall(Runnable.class, "run"), 1);
    Skeleton.getSkeleton(t2).assertNotCalled(new MethodCall(Runnable.class, "run"));
    
    reg.unregister();
    reg2.unregister();
    
    Hashtable<String, Object> props = new Hashtable<String, Object>();
    props.put(Constants.SERVICE_RANKING, 55);
    
    t = Skeleton.newMock(Runnable.class);
    t2 = Skeleton.newMock(Runnable.class);

    reg = bc.registerService(className, t, null);
    reg2 = bc.registerService(className, t2, props);
    
    r = (Runnable) ctx.lookup("osgi:service/java.lang.Runnable");
    
    r.run();
    
    Skeleton.getSkeleton(t).assertNotCalled(new MethodCall(Runnable.class, "run"));
    Skeleton.getSkeleton(t2).assertCalledExactNumberOfTimes(new MethodCall(Runnable.class, "run"), 1);
  }
  
  /**
   * Check that the NamingEnumeration passed in has another element, which represents a java.lang.Thread
   * @param serviceList
   * @throws NamingException
   */
  private void checkThreadRetrievedViaListMethod(NamingEnumeration<NameClassPair> serviceList)
      throws NamingException
  {
    assertTrue("The repository was empty", serviceList.hasMoreElements());
    
    NameClassPair ncp = serviceList.next();
    
    assertNotNull("We didn't get a service back from our lookup :(", ncp);
    
    assertNotNull("The object from the SR was null", ncp.getClassName());
    
    assertEquals("The service retrieved was not of the correct type", "java.lang.Thread", ncp.getClassName());
    
    assertEquals("aries:services/java.lang.Runnable/(rubbish=smelly)", ncp.getName().toString());
  }
  
  /**
   * This test checks that we can list the contents of the repository using the
   * list method
   * 
   * @throws NamingException
   */
  public void listRepositoryBindings() throws NamingException
  {
    InitialContext ctx = new InitialContext();
    
    NamingEnumeration<Binding> serviceList = ctx.listBindings("aries:services/java.lang.Runnable/(rubbish=smelly)");
    
    Object returnedService = checkThreadRetrievedViaListBindingsMethod(serviceList);
    
    assertFalse("The repository contained more objects than we expected", serviceList.hasMoreElements());
    
    assertTrue("The returned service was not the service we expected", returnedService == service);
    
    //Now add a second service
    Thread secondService = new Thread();
    registerService(secondService);
    
    serviceList = ctx.listBindings("aries:services/java.lang.Runnable/(rubbish=smelly)");
    
    Object returnedService1 = checkThreadRetrievedViaListBindingsMethod(serviceList);
    
    Object returnedService2 = checkThreadRetrievedViaListBindingsMethod(serviceList);
    
    assertFalse("The repository contained more objects than we expected", serviceList.hasMoreElements());
    
    assertTrue("The services were not the ones we expected!",(returnedService1 == service || returnedService2 == service) && (returnedService1 == secondService || returnedService2 == secondService) && (returnedService1 != returnedService2));
    
  }

  /**
   * Check that the NamingEnumeration passed in has another element, which represents a java.lang.Thread
   * @param serviceList
   * @return the object in the registry
   * @throws NamingException
   */
  private Object checkThreadRetrievedViaListBindingsMethod(NamingEnumeration<Binding> serviceList)
      throws NamingException
  {
    assertTrue("The repository was empty", serviceList.hasMoreElements());
    
    Binding binding = serviceList.nextElement();
    
    assertNotNull("We didn't get a service back from our lookup :(", binding);
    
    assertNotNull("The object from the SR was null", binding.getObject());
    
    assertTrue("The service retrieved was not of the correct type", binding.getObject() instanceof Thread);
    
    assertEquals("aries:services/java.lang.Runnable/(rubbish=smelly)", binding.getName().toString());
    
    return binding.getObject();
  }
  
}