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
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.Callable;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;

import org.apache.aries.jndi.api.JNDIConstants;
import org.apache.aries.mocks.BundleContextMock;
import org.apache.aries.mocks.BundleMock;
import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.MethodCallHandler;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceException;
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
  public void registerService() throws Exception
  {
    bc =  Skeleton.newMock(new BundleContextMock(), BundleContext.class);
    registerProxyManager();
    new org.apache.aries.jndi.startup.Activator().start(bc);
    new Activator().start(bc);
        
    service = Skeleton.newMock(Runnable.class);
    
    registerService(service);
  }
  
  private void registerProxyManager() 
  {
    ProxyManager mgr = Skeleton.newMock(ProxyManager.class);
    
    //   public Object createDelegatingProxy(Bundle clientBundle, Collection<Class<?>> classes, Callable<Object> dispatcher, Object template) throws UnableToProxyException;

    Skeleton.getSkeleton(mgr).registerMethodCallHandler(new MethodCall(ProxyManager.class, "createDelegatingProxy", Bundle.class, Collection.class, Callable.class, Object.class),
        new MethodCallHandler() 
        {
          public Object handle(MethodCall methodCall, Skeleton skeleton) throws Exception 
          {
            @SuppressWarnings("unchecked")
            Collection<Class<?>> interfaceClasses = (Collection<Class<?>>) methodCall.getArguments()[1];
            Class<?>[] classes = new Class<?>[interfaceClasses.size()];
            
            Iterator<Class<?>> it = interfaceClasses.iterator(); 
            for (int i = 0; it.hasNext(); i++) {
              classes[i] = it.next();
            }
            
            @SuppressWarnings("unchecked")
            final Callable<Object> target = (Callable<Object>) methodCall.getArguments()[2];
            
            return Proxy.newProxyInstance(this.getClass().getClassLoader(), classes, new InvocationHandler() 
            {
              public Object invoke(Object mock, Method method, Object[] arguments)
                  throws Throwable 
              {
                return method.invoke(target.call(), arguments);
              }
            });
          }
        });
    
    bc.registerService(ProxyManager.class.getName(), mgr, null);
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
  
  @Test
  public void testBaseLookup() throws NamingException
  {
     BundleMock mock = new BundleMock("scooby.doo", new Properties());
        
     Thread.currentThread().setContextClassLoader(mock.getClassLoader());

     InitialContext ctx = new InitialContext();
     
     Context ctx2 = (Context) ctx.lookup("osgi:service");
     
     Runnable r1 = (Runnable) ctx2.lookup("java.lang.Runnable");   
     assertNotNull(r1);
     assertTrue("expected proxied service class", r1 != service);
     
     Runnable r2 = (Runnable) ctx.lookup("aries:services/java.lang.Runnable");
     assertNotNull(r2);
     assertTrue("expected non-proxied service class", r2 == service);
  }
  
  @Test
  public void testLookupWithPause() throws NamingException
  {
     BundleMock mock = new BundleMock("scooby.doo", new Properties());
        
     Thread.currentThread().setContextClassLoader(mock.getClassLoader());

     Hashtable<Object, Object> env = new Hashtable<Object, Object>();
     env.put(JNDIConstants.REBIND_TIMEOUT, 1000);
     
     InitialContext ctx = new InitialContext(env);
     
     Context ctx2 = (Context) ctx.lookup("osgi:service");
     
     Runnable r1 = (Runnable) ctx2.lookup("java.lang.Runnable");   
     
     reg.unregister();
     
     long startTime = System.currentTimeMillis();
     
     try {
       r1.run();
       fail("Should have received an exception");
     } catch (ServiceException e) {
       long endTime = System.currentTimeMillis();
       long diff = endTime - startTime;
       
       assertTrue("The run method did not fail in the expected time (1s): " + diff, diff >= 1000);
     }
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
  
  @Test
  public void jndiLookupServiceNameTest() throws NamingException, SQLException
  {
    InitialContext ctx = new InitialContext(new Hashtable<Object, Object>());
    
    BundleMock mock = new BundleMock("scooby.doo", new Properties());
    
    Thread.currentThread().setContextClassLoader(mock.getClassLoader());
    
    DataSource first = Skeleton.newMock(DataSource.class);
    DataSource second = Skeleton.newMock(DataSource.class);
    
    Hashtable<String, String> properties = new Hashtable<String, String>();
    properties.put("osgi.jndi.service.name", "jdbc/myDataSource");
    
    bc.registerService(DataSource.class.getName(), first, properties);

    properties = new Hashtable<String, String>();
    properties.put("osgi.jndi.service.name", "jdbc/myDataSource2");
    
    bc.registerService(DataSource.class.getName(), second, properties);
    
    DataSource s = (DataSource) ctx.lookup("osgi:service/jdbc/myDataSource");
    
    assertNotNull(s);
    
    s = (DataSource) ctx.lookup("osgi:service/javax.sql.DataSource/(osgi.jndi.service.name=jdbc/myDataSource2)");
    
    assertNotNull(s);
    
    s.isWrapperFor(DataSource.class); // don't care about the method, just need to call something.
    
    Skeleton.getSkeleton(second).assertCalled(new MethodCall(DataSource.class, "isWrapperFor", Class.class));
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
    
    BundleMock mock = new BundleMock("scooby.doo.1", new Properties());
    
    Thread.currentThread().setContextClassLoader(mock.getClassLoader());
    
    Runnable s = (Runnable) ctx.lookup("osgi:service/java.lang.Runnable");
    
    assertNotNull("We didn't get a service back from our lookup :(", s);
    
    s.run();
    
    Skeleton.getSkeleton(service).assertCalledExactNumberOfTimes(new MethodCall(Runnable.class, "run"), 1);
    
    Skeleton skel = Skeleton.getSkeleton(mock.getBundleContext());
    
    skel.assertCalled(new MethodCall(BundleContext.class, "getServiceReferences", "java.lang.Runnable", null));

    ctx = new InitialContext(new Hashtable<Object, Object>());
    
    mock = new BundleMock("scooby.doo.2", new Properties());
    
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
    
    Object s = ctx.lookup("osgi:service/java.lang.Runnable/(rubbish=smelly)");
    
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
    
    ctx.lookup("osgi:service/java.lang.Runnable");
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
    
    ctx.lookup("osgi:service/java.lang.Integer");
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
    
    NamingEnumeration<NameClassPair> serviceList = ctx.list("osgi:service/java.lang.Runnable/(rubbish=smelly)");
    
    checkThreadRetrievedViaListMethod(serviceList);
    
    assertFalse("The repository contained more objects than we expected", serviceList.hasMoreElements());
    
    //Now add a second service
    
    registerService(new Thread());
    
    serviceList = ctx.list("osgi:service/java.lang.Runnable/(rubbish=smelly)");
    
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
  public void checkServiceListLookup() throws NamingException
  {
    BundleMock mock = new BundleMock("scooby.doo", new Properties());
    
    Thread.currentThread().setContextClassLoader(mock.getClassLoader());

    InitialContext ctx = new InitialContext();
    
    String className = Runnable.class.getName();
    
    Runnable t = Skeleton.newMock(Runnable.class);
    
    // we don't want the default service
    reg.unregister();
    
    ServiceRegistration reg = bc.registerService(className, t, null);
    ServiceRegistration reg2 = bc.registerService("java.lang.Thread", new Thread(), null);
    
    Context ctx2 = (Context) ctx.lookup("osgi:servicelist/java.lang.Runnable");
    
    Runnable r = (Runnable) ctx2.lookup(String.valueOf(reg.getReference().getProperty(Constants.SERVICE_ID)));

    r.run();
    
    Skeleton.getSkeleton(t).assertCalled(new MethodCall(Runnable.class, "run"));
    
    reg.unregister();
    
    try {
      r.run();
      fail("Should have received a ServiceException");
    } catch (ServiceException e) {
      assertEquals("service exception has the wrong type", ServiceException.UNREGISTERED, e.getType());
    }
    
    try {
      ctx2.lookup(String.valueOf(reg2.getReference().getProperty(Constants.SERVICE_ID)));
      fail("Expected a NameNotFoundException");
    } catch (NameNotFoundException e) {
    }
  }
  
  @Test
  public void checkServiceListList() throws NamingException
  {
    BundleMock mock = new BundleMock("scooby.doo", new Properties());
    
    Thread.currentThread().setContextClassLoader(mock.getClassLoader());

    InitialContext ctx = new InitialContext();
    
    String className = Runnable.class.getName();
    
    Runnable t = Skeleton.newMock(Runnable.class);
    
    // we don't want the default service
    reg.unregister();
    
    ServiceRegistration reg = bc.registerService(className, t, null);
    ServiceRegistration reg2 = bc.registerService(className, new Thread(), null);
    
    NamingEnumeration<NameClassPair> ne = ctx.list("osgi:servicelist/" + className);
    
    assertTrue(ne.hasMoreElements());
    
    NameClassPair ncp = ne.nextElement();
    
    assertEquals(String.valueOf(reg.getReference().getProperty(Constants.SERVICE_ID)), ncp.getName());
    assertTrue("Class name not correct. Was: " + ncp.getClassName(), ncp.getClassName().contains("Proxy"));
    
    assertTrue(ne.hasMoreElements());
    
    ncp = ne.nextElement();
    
    assertEquals(String.valueOf(reg2.getReference().getProperty(Constants.SERVICE_ID)), ncp.getName());
    assertEquals("Class name not correct.", Thread.class.getName(), ncp.getClassName());
    
    assertFalse(ne.hasMoreElements());
  }

  @Test
  public void checkServiceListListBindings() throws NamingException
  {
    BundleMock mock = new BundleMock("scooby.doo", new Properties());
    
    Thread.currentThread().setContextClassLoader(mock.getClassLoader());

    InitialContext ctx = new InitialContext();
    
    String className = Runnable.class.getName();
    
    MethodCall run = new MethodCall(Runnable.class, "run");
    
    Runnable t = Skeleton.newMock(Runnable.class);
    Runnable t2 = Skeleton.newMock(Runnable.class);
    
    // we don't want the default service
    reg.unregister();
    
    ServiceRegistration reg = bc.registerService(className, t, null);
    ServiceRegistration reg2 = bc.registerService(className, t2, null);
    
    NamingEnumeration<Binding> ne = ctx.listBindings("osgi:servicelist/" + className);
    
    assertTrue(ne.hasMoreElements());
    
    Binding bnd = ne.nextElement();
    
    assertEquals(String.valueOf(reg.getReference().getProperty(Constants.SERVICE_ID)), bnd.getName());
    assertTrue("Class name not correct. Was: " + bnd.getClassName(), bnd.getClassName().contains("Proxy") || bnd.getClassName().contains("EnhancerByCGLIB"));
    
    Runnable r = (Runnable) bnd.getObject();
    
    assertNotNull(r);
    
    r.run();
    
    Skeleton.getSkeleton(t).assertCalledExactNumberOfTimes(run, 1);
    Skeleton.getSkeleton(t2).assertNotCalled(run);
    
    assertTrue(ne.hasMoreElements());
    
    bnd = ne.nextElement();
    
    assertEquals(String.valueOf(reg2.getReference().getProperty(Constants.SERVICE_ID)), bnd.getName());
    assertTrue("Class name not correct. Was: " + bnd.getClassName(), bnd.getClassName().contains("Proxy") || bnd.getClassName().contains("EnhancerByCGLIB"));
    
    r = (Runnable) bnd.getObject();
    
    assertNotNull(r);
    
    r.run();
    
    Skeleton.getSkeleton(t).assertCalledExactNumberOfTimes(run, 1);
    Skeleton.getSkeleton(t2).assertCalledExactNumberOfTimes(run, 1);
    
    assertFalse(ne.hasMoreElements());
  }

  @Test(expected=ServiceException.class)
  public void checkProxyWhenServiceGoes() throws ServiceException, NamingException
  {
    BundleMock mock = new BundleMock("scooby.doo", new Properties());
    
    Thread.currentThread().setContextClassLoader(mock.getClassLoader());

    InitialContext ctx = new InitialContext();
    
    Runnable r = (Runnable) ctx.lookup("osgi:service/java.lang.Runnable");
    
    r.run();
    
    Skeleton.getSkeleton(service).assertCalled(new MethodCall(Runnable.class, "run"));
    
    reg.unregister();
    
    r.run();
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

    bc.registerService(className, t, null);
    bc.registerService(className, t2, props);
    
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
    
    assertEquals("osgi:service/java.lang.Runnable/(rubbish=smelly)", ncp.getName().toString());
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
    
    NamingEnumeration<Binding> serviceList = ctx.listBindings("osgi:service/java.lang.Runnable/(rubbish=smelly)");
    
    Object returnedService = checkThreadRetrievedViaListBindingsMethod(serviceList);
    
    assertFalse("The repository contained more objects than we expected", serviceList.hasMoreElements());
    
    assertTrue("The returned service was not the service we expected", returnedService == service);
    
    //Now add a second service
    Thread secondService = new Thread();
    registerService(secondService);
    
    serviceList = ctx.listBindings("osgi:service/java.lang.Runnable/(rubbish=smelly)");
    
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
    
    assertEquals("osgi:service/java.lang.Runnable/(rubbish=smelly)", binding.getName().toString());
    
    return binding.getObject();
  }
  
}