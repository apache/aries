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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.apache.aries.jndi.services.ServiceHelper;
import org.apache.aries.jndi.url.Activator;
import org.apache.aries.mocks.BundleContextMock;
import org.apache.aries.mocks.BundleMock;


/**
 * This class contains tests for the ServiceHelper
 */
public class ServiceHelperTest
{
  /** The service we register by default */
  private Thread service;
  /** The bundle context for the test */
  private BundleContext bc;
  
  /**
   * This method does the setup to ensure we always have a service.
   * @throws NoSuchFieldException 
   * @throws SecurityException 
   * @throws IllegalAccessException 
   * @throws IllegalArgumentException 
   */
  @Before
  public void registerService() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
  {
    
    bc =  Skeleton.newMock(new BundleContextMock(), BundleContext.class);
    new Activator().start(bc);
    
    Field f = ServiceHelper.class.getDeclaredField("context");
    f.setAccessible(true);
    f.set(null, bc);

    service = new Thread();
    
    registerService(service);
  }

  /**
   * Register a service in our map.
   * 
   * @param service2 The service to register.
   */
  private void registerService(Thread service2)
  {
    ServiceFactory factory = Skeleton.newMock(ServiceFactory.class);
    Skeleton skel = Skeleton.getSkeleton(factory);
    
    skel.setReturnValue(new MethodCall(ServiceFactory.class, "getService", Bundle.class, ServiceRegistration.class), service2);
    
    bc.registerService(new String[] {"java.lang.Runnable"}, factory, new Hashtable<String, String>());
  }
  
  /**
   * Make sure we clear the caches out before the next test.
   */
  @After
  public void teardown()
  {
    BundleContextMock.clear();
  }
  
  /**
   * Check that the basic behaviour is correct. Do we call ungetService, do
   * we call getService the right number of times.
   */
  @Test
  public void getAService()
  {
    Bundle b = Skeleton.newMock(new BundleMock("scooby.doo", new Properties()), Bundle.class);
    
    Skeleton skel = Skeleton.getSkeleton(b);
    
    Thread.currentThread().setContextClassLoader(((BundleMock)skel.getTemplateObject()).getClassLoader());
    
    skel = Skeleton.getSkeleton(b.getBundleContext());

    Object retrievedService = ServiceHelper.getService("java.lang.Runnable", null);
    
    assertNotNull("We could not locate the service in the registry", retrievedService);
    
    assertTrue("We didn't get back the service we expected", service == retrievedService);
    
    MethodCall getService = new MethodCall(BundleContext.class, "getService", ServiceReference.class);
    MethodCall ungetService = new MethodCall(BundleContext.class, "ungetService", ServiceReference.class);
    
    skel.assertNotCalled(ungetService);
    skel.assertCalledExactNumberOfTimes(getService, 1);

    Object retrievedService2 = ServiceHelper.getService("java.lang.Runnable", null);

    assertTrue("We got different objects, which we did not want", retrievedService == retrievedService2);
    skel.assertCalledExactNumberOfTimes(getService, 2);
    skel.assertCalledExactNumberOfTimes(ungetService, 1);
  }
  
  /**
   * This method checks that we get two different services from different
   * bundles if the thread context classloader is different.
   */
  @Test
  public void getAServiceFromTwoDifferentApplications()
  {
    Bundle b = Skeleton.newMock(new BundleMock("scooby.doo", new Properties()), Bundle.class);
    
    Skeleton skel = Skeleton.getSkeleton(b);
    
    Thread.currentThread().setContextClassLoader(((BundleMock)skel.getTemplateObject()).getClassLoader());
    
    Object retrievedService = ServiceHelper.getService("java.lang.Runnable", null);
    Bundle b2 = Skeleton.newMock(new BundleMock("scooby.doo", new Properties()), Bundle.class);
    
    skel = Skeleton.getSkeleton(b2);
    
    Thread.currentThread().setContextClassLoader(((BundleMock)skel.getTemplateObject()).getClassLoader());
    Object retrievedService2 = ServiceHelper.getService("java.lang.Runnable", null);
    
    assertNotNull("We could not locate the service in the registry", retrievedService);
    assertNotNull("We could not locate the service in the registry", retrievedService2);
    
    assertTrue("We got different objects, which we did not want", retrievedService == retrievedService2);
    
    assertFalse("We expected different bundles from our calls to the BundleMaker, but we got the same one", b == b2);
    
    MethodCall getService = new MethodCall(BundleContext.class, "getService", ServiceReference.class);
    Skeleton.getSkeleton(b.getBundleContext()).assertCalledExactNumberOfTimes(getService, 1);
    Skeleton.getSkeleton(b2.getBundleContext()).assertCalledExactNumberOfTimes(getService, 1);
  }
  
  /**
   * This test checks that the getServices method returns the expected number of
   * services, and that it changes when new services are registered.
   */
  @Test
  public void getMultipleServices()
  {
    Bundle b = Skeleton.newMock(new BundleMock("scooby.doo", new Properties()), Bundle.class);
    
    Skeleton skel = Skeleton.getSkeleton(b);
    
    Thread.currentThread().setContextClassLoader(((BundleMock)skel.getTemplateObject()).getClassLoader());
    
    MethodCall getService = new MethodCall(BundleContext.class, "getService", ServiceReference.class);
    MethodCall ungetService = new MethodCall(BundleContext.class, "ungetService", ServiceReference.class);
    
    List<?> services = ServiceHelper.getServices("java.lang.Runnable", null);

    skel = Skeleton.getSkeleton(b.getBundleContext());

    skel.assertNotCalled(ungetService);
    skel.assertCalledExactNumberOfTimes(getService, 1);

    assertEquals("At this point we really should only have one service.", 1, services.size());
    Thread anotherService = new Thread();
    registerService(anotherService);
    
    services = ServiceHelper.getServices("java.lang.Runnable", null);
    
    assertEquals("At this point we really should have two services.", 2, services.size());
    assertTrue("The master service was not there, odd.", services.contains(service));
    assertTrue("The service we added just for this test was not there, odd.", services.contains(anotherService));
    
    // this should be 3 times, once for the first call to getServices, and twice for the second
    skel.assertCalledExactNumberOfTimes(getService, 3);
    skel.assertCalledExactNumberOfTimes(ungetService, 1);
  }
}