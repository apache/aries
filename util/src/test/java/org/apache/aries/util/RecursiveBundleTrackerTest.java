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

import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.apache.aries.util.tracker.BundleTrackerFactory;
import org.apache.aries.util.tracker.InternalRecursiveBundleTracker;
import org.apache.aries.util.tracker.RecursiveBundleTracker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.framework.CompositeBundle;
import org.osgi.service.framework.CompositeBundleFactory;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import static org.junit.Assert.*;

public class RecursiveBundleTrackerTest {
    BundleContext context;
    
    @Before
    public void setup() {
        context = Skeleton.newMock(BundleContext.class);
        Skeleton.getSkeleton(context).setReturnValue(
                new MethodCall(BundleContext.class, "getServiceReference", "org.osgi.service.framework.CompositeBundleFactory"), 
                Skeleton.newMock(ServiceReference.class));
    }
    
    @After
    public void closeTrackes() {
        BundleTrackerFactory.unregisterAndCloseBundleTracker("test");
    }
    
    @Test
    public void testCompositeLifeCycle() {
        BundleTrackerCustomizer customizer = Skeleton.newMock(BundleTrackerCustomizer.class);

        InternalRecursiveBundleTracker sut = new InternalRecursiveBundleTracker(context, 
                Bundle.INSTALLED | Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING, customizer);
        
        sut.open();
        
        CompositeBundle cb = Skeleton.newMock(CompositeBundle.class);
        Skeleton cbSkel = Skeleton.getSkeleton(cb);
        cbSkel.setReturnValue(new MethodCall(CompositeBundle.class, "getSymbolicName"), "test.composite");
        cbSkel.setReturnValue(new MethodCall(CompositeBundle.class, "getVersion"), new Version("1.0.0"));
        
        assertTrue(BundleTrackerFactory.getAllBundleTracker().isEmpty());
        
        sut.addingBundle(cb, new BundleEvent(BundleEvent.INSTALLED, cb));
        assertEquals(1, BundleTrackerFactory.getAllBundleTracker().size());
        assertEquals(1, BundleTrackerFactory.getBundleTrackerList("test.composite_1.0.0").size());
        
        sut.removedBundle(cb, new BundleEvent(BundleEvent.STOPPED, cb), cb);
        assertTrue(BundleTrackerFactory.getAllBundleTracker().isEmpty());        
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testMissingStopping() {
        new RecursiveBundleTracker(null, Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE, null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testMissingStarting() {
        new RecursiveBundleTracker(null, Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE | Bundle.STOPPING, null);        
    }

    @Test(expected=IllegalArgumentException.class)
    public void testMissingInstalled() {
        new RecursiveBundleTracker(null, Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING, null);        
    }
}
