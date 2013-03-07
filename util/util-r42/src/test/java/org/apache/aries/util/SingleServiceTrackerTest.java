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
package org.apache.aries.util;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.aries.mocks.BundleContextMock;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.apache.aries.util.tracker.SingleServiceTracker;
import org.apache.aries.util.tracker.SingleServiceTracker.SingleServiceListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

import static org.junit.Assert.*;

public class SingleServiceTrackerTest {
  private BundleContext ctx;
  private SingleServiceTracker<String> sut;
  private SingleServiceTracker.SingleServiceListener listener;
  
  @Before
  public void setup() {
    ctx = Skeleton.newMock(new BundleContextMock(), BundleContext.class);
  }
  
  @After
  public void teardown() {
    BundleContextMock.clear();
  }
  
  private void createSut() {
	  createSut(null);
  }
  
  private void createSut(String filter) {
    listener = Skeleton.newMock(SingleServiceListener.class);
    try {
		sut = new SingleServiceTracker<String>(ctx, String.class, filter, listener);
	} catch (InvalidSyntaxException e) {
		throw new RuntimeException(e);
	}
    sut.open();
  }
  
  @Test
  public void testBeforeTheFactService() {
	  ctx.registerService("java.lang.String", "uno", null);
	  createSut();
	  Skeleton.getSkeleton(listener).assertCalled(Arrays.asList(new MethodCall(SingleServiceListener.class, "serviceFound")), true);
	  assertEquals("uno", sut.getService());
  }
  
  @Test
  public void testBeforeTheFactServiceDoubleRegistration() {
	  testBeforeTheFactService();
	  
	  ctx.registerService("java.lang.String", "due", null);
	  Skeleton.getSkeleton(listener).assertCalled(Arrays.asList(new MethodCall(SingleServiceListener.class, "serviceFound")), true);
	  assertEquals("uno", sut.getService());
  }
  
  @Test
  public void testBeforeTheFactChoice() {
	  ctx.registerService("java.lang.String", "uno", null);
	  ctx.registerService("java.lang.String", "due", null);
	  createSut();
	  Skeleton.getSkeleton(listener).assertCalled(Arrays.asList(new MethodCall(SingleServiceListener.class, "serviceFound")), true);
	  assertEquals("uno", sut.getService());
  }
  
  @Test
  public void testBeforeTheFactChoiceWithPropertiesAndFilterWithFirstMatch() {
	  Dictionary<String, String> props = new Hashtable<String, String>();
	  props.put("foo", "bar");
	  ctx.registerService("java.lang.String", "uno", props);
	  ctx.registerService("java.lang.String", "due", null);
	  createSut("(foo=bar)");
	  Skeleton.getSkeleton(listener).assertCalled(Arrays.asList(new MethodCall(SingleServiceListener.class, "serviceFound")), true);
	  assertEquals("uno", sut.getService());
  }
  
  @Test
  public void testBeforeTheFactChoiceWithPropertiesAndFilterWithSecondMatch() {
	  Dictionary<String, String> props = new Hashtable<String, String>();
	  props.put("foo", "bar");
	  ctx.registerService("java.lang.String", "uno", null);
	  ctx.registerService("java.lang.String", "due", props);
	  createSut("(foo=bar)");
	  Skeleton.getSkeleton(listener).assertCalled(Arrays.asList(new MethodCall(SingleServiceListener.class, "serviceFound")), true);
	  assertEquals("due", sut.getService());
  }
  
  @Test
  public void testAfterTheFactService() 
  {
    createSut();
    Skeleton.getSkeleton(listener).assertSkeletonNotCalled();
    
    ctx.registerService("java.lang.String", "uno", null);
    Skeleton.getSkeleton(listener).assertCalled(new MethodCall(SingleServiceListener.class, "serviceFound"));
    
    assertEquals("uno", sut.getService());
  }
  
  @Test
  public void testDoubleRegistration() {
    testAfterTheFactService();
    
    Skeleton.getSkeleton(listener).clearMethodCalls();
    ctx.registerService("java.lang.String", "due", null);
    
    Skeleton.getSkeleton(listener).assertSkeletonNotCalled();
    assertEquals("uno", sut.getService());
  }
  
  @Test
  public void testRegistrationWhileClosed() {
    createSut();
    sut.close();
    
    ctx.registerService("java.lang.String", "uno", null);
    Skeleton.getSkeleton(listener).assertSkeletonNotCalled();
    
    assertNull(sut.getService());
  }
  
}
