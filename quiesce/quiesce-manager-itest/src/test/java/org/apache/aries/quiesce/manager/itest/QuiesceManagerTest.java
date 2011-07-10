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
package org.apache.aries.quiesce.manager.itest;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.apache.aries.itest.ExtraOptions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.quiesce.manager.QuiesceManager;
import org.apache.aries.quiesce.participant.QuiesceParticipant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;

@RunWith(JUnit4TestRunner.class)
public class QuiesceManagerTest extends AbstractIntegrationTest {
    private QuiesceManager manager;
    private Bundle b1;
    private Bundle b2;
    private Bundle b3;
    private long timeoutTime;
    private List<Bundle> bundleList;
    private MockQuiesceParticipant participant1;
    private MockQuiesceParticipant participant2;
    private MockQuiesceParticipant participant3;


    @Before
    public void setup() {
        manager = context().getService(QuiesceManager.class);
        b1 = bundleContext.getBundle(5);
        b2 = bundleContext.getBundle(6);
        b3 = bundleContext.getBundle(10);
        participant1 = new MockQuiesceParticipant(MockQuiesceParticipant.RETURNIMMEDIATELY);
        participant2 = new MockQuiesceParticipant(MockQuiesceParticipant.NEVERRETURN);
        participant3 = new MockQuiesceParticipant(MockQuiesceParticipant.WAIT);

    }

    @After
    public void after() {
        participant1.reset();
        participant2.reset();
        participant3.reset();
    }

    @Test
    public void testNullSafe() throws Exception {
        //Check we're null safe
        manager.quiesce(null);  
    }

    @Test
    public void testNoParticipants() throws Exception {
        bundleList = new ArrayList<Bundle>();
        bundleList.add(b1);
        assertEquals("Bundle "+b1.getSymbolicName()+" should be in active state", Bundle.ACTIVE, b1.getState());

        //Try quiescing one bundle with no participants
        manager.quiesceWithFuture(2000, bundleList).get(5000, TimeUnit.MILLISECONDS);

        assertTrue("Bundle "+b1.getSymbolicName()+" should not be in active state", b1.getState() != Bundle.ACTIVE);
    }

    @Test
    public void testImmediateReturn() throws Exception {
        bundleList = new ArrayList<Bundle>();
        bundleList.add(b1);
        //Register a mock participant which will report back quiesced immediately
        bundleContext.registerService(QuiesceParticipant.class.getName(), participant1, null);
        //Try quiescing the bundle with immediate return
        assertEquals("Bundle "+b1.getSymbolicName()+" should be in active state", Bundle.ACTIVE, b1.getState());
        
        manager.quiesceWithFuture(1000,bundleList).get(5000, TimeUnit.MILLISECONDS);
        
        assertEquals("Participant should have finished once", 1, participant1.getFinishedCount());
        assertTrue("Bundle "+b1.getSymbolicName()+" should not be in active state", b1.getState() != Bundle.ACTIVE);
    }

    @Test
    public void testNoReturn() throws Exception {
        //Register a mock participant which won't respond
        bundleContext.registerService(QuiesceParticipant.class.getName(), participant2, null);
        //recreate the list as it may have been emptied?
        bundleList = new ArrayList<Bundle>();
        bundleList.add(b1);

        //Try quiescing the bundle with no return
        assertEquals("Bundle "+b1.getSymbolicName()+" should be in active state", Bundle.ACTIVE, b1.getState());
        manager.quiesce(1000,bundleList);
        timeoutTime = System.currentTimeMillis()+5000;
        while (System.currentTimeMillis() < timeoutTime && b1.getState() == Bundle.ACTIVE){
            Thread.sleep(500);
        }
        
        assertEquals("Participant should have started once", 1, participant2.getStartedCount());
        assertEquals("Participant should not have finished", 0, participant2.getFinishedCount());
        assertTrue("Bundle "+b1.getSymbolicName()+" should not be in active state", b1.getState() != Bundle.ACTIVE);
    }

    @Test
    public void testWaitAShortTime() throws Exception {
        //Try quiescing where participant takes 5s to do the work. We should get InterruptedException
        bundleContext.registerService(QuiesceParticipant.class.getName(), participant3, null);
        //recreate the list as it may have been emptied?
        bundleList = new ArrayList<Bundle>();
        bundleList.add(b1);

        assertEquals("Bundle "+b1.getSymbolicName()+" should be in active state", Bundle.ACTIVE, b1.getState());
        
        // we should be finishing in about 5000 millis not 10000
        manager.quiesceWithFuture(10000,bundleList).get(7000, TimeUnit.MILLISECONDS);

        assertEquals("Participant should have started once", 1, participant3.getStartedCount());
        assertEquals("Participant should finished once", 1, participant3.getFinishedCount());
        assertTrue("Bundle "+b1.getSymbolicName()+" should not be in active state", b1.getState() != Bundle.ACTIVE);
    }

    @Test
    public void testThreeParticipants() throws Exception {
        //Register three participants. One returns immediately, one waits 5s then returns, one never returns
        bundleContext.registerService(QuiesceParticipant.class.getName(), participant1, null);
        bundleContext.registerService(QuiesceParticipant.class.getName(), participant2, null);
        bundleContext.registerService(QuiesceParticipant.class.getName(), participant3, null);
        //recreate the list as it may have been emptied
        bundleList = new ArrayList<Bundle>();
        bundleList.add(b1);
        assertEquals("Bundle "+b1.getSymbolicName()+" should be in active state", Bundle.ACTIVE, b1.getState());
        
        manager.quiesceWithFuture(10000,bundleList).get(15000, TimeUnit.MILLISECONDS);
        
        assertEquals("Participant 1 should have started once", 1, participant1.getStartedCount());
        assertEquals("Participant 1 should finished once", 1, participant1.getFinishedCount());
        assertEquals("Participant 2 should have started once", 1, participant2.getStartedCount());
        assertEquals("Participant 2 should not have finished", 0, participant2.getFinishedCount());
        assertEquals("Participant 3 should have started once", 1, participant3.getStartedCount());
        assertEquals("Participant 3 should finished once", 1, participant3.getFinishedCount());
        assertTrue("Bundle "+b1.getSymbolicName()+" should not be in active state", b1.getState() != Bundle.ACTIVE);
    }

    @Test
    public void testFuture() throws Exception {
        bundleContext.registerService(QuiesceParticipant.class.getName(), participant2, null);
        bundleContext.registerService(QuiesceParticipant.class.getName(), participant3, null);
        bundleList = new ArrayList<Bundle>();
        bundleList.add(b1);
        assertEquals("Bundle "+b1.getSymbolicName()+" should be in active state", Bundle.ACTIVE, b1.getState());

        Future<?> future = manager.quiesceWithFuture(2000, Arrays.asList(b1));

        // causes us to wait
        future.get();

        assertEquals("Participant 2 has started", 1, participant2.getStartedCount());
        assertEquals("Participant 2 has finished", 0, participant2.getFinishedCount());
        assertEquals("Participant 3 has started", 1, participant3.getStartedCount());
        assertEquals("Participant 3 has finished", 1, participant3.getFinishedCount());
    }
    
    @Test
    public void testFutureWithWait() throws Exception {
        bundleContext.registerService(QuiesceParticipant.class.getName(), participant2, null);
        bundleContext.registerService(QuiesceParticipant.class.getName(), participant3, null);
        bundleList = new ArrayList<Bundle>();
        bundleList.add(b1);
        assertEquals("Bundle "+b1.getSymbolicName()+" should be in active state", Bundle.ACTIVE, b1.getState());

        Future<?> future = manager.quiesceWithFuture(2000, Arrays.asList(b1));

        try {
            // causes us to wait, but too short
            future.get(500, TimeUnit.MILLISECONDS);
            fail("Too short wait, should have thrown TimeoutException");
        } catch (TimeoutException te) {
            // expected
        }

        assertEquals("Participant 2 has started", 1, participant2.getStartedCount());
        assertEquals("Participant 2 has finished", 0, participant2.getFinishedCount());
        assertEquals("Participant 3 has started", 1, participant3.getStartedCount());
        assertEquals("Participant 3 has finished", 0, participant3.getFinishedCount());
        assertEquals("Bundle "+b1.getSymbolicName()+" should still be active, because we did not wait long enough", Bundle.ACTIVE, b1.getState());
    }

    @Test
    public void testTwoBundles() throws Exception {
        //Register three participants. One returns immediately, one waits 5s then returns, one never returns
        bundleContext.registerService(QuiesceParticipant.class.getName(), participant1, null);
        bundleContext.registerService(QuiesceParticipant.class.getName(), participant2, null);
        bundleContext.registerService(QuiesceParticipant.class.getName(), participant3, null);
        //recreate the list as it may have been emptied
        bundleList = new ArrayList<Bundle>();
        bundleList.add(b1);
        bundleList.add(b2);
        assertEquals("Bundle "+b1.getSymbolicName()+" should be in active state", Bundle.ACTIVE, b1.getState());
        assertEquals("Bundle "+b2.getSymbolicName()+" should be in active state", Bundle.ACTIVE, b2.getState());

        manager.quiesceWithFuture(10000,bundleList).get(15000, TimeUnit.MILLISECONDS);

        assertEquals("Participant 1 should have started once", 1, participant1.getStartedCount());
        assertEquals("Participant 1 should finished once", 1, participant1.getFinishedCount());
        assertEquals("Participant 2 should have started once", 1, participant2.getStartedCount());
        assertEquals("Participant 2 should not have finished", 0, participant2.getFinishedCount());
        assertEquals("Participant 3 should have started once", 1, participant3.getStartedCount());
        assertEquals("Participant 3 should finished once", 1, participant3.getFinishedCount());
        assertTrue("Bundle "+b1.getSymbolicName()+" should not be in active state", b1.getState() != Bundle.ACTIVE);
        assertTrue("Bundle "+b2.getSymbolicName()+" should not be in active state", b2.getState() != Bundle.ACTIVE);
    }

    @Test
    public void testOverlappedQuiesces() throws Exception {

        //Register three participants. One returns immediately, one waits 5s then returns, one never returns
        bundleContext.registerService(QuiesceParticipant.class.getName(), participant1, null);
        bundleContext.registerService(QuiesceParticipant.class.getName(), participant2, null);
        bundleContext.registerService(QuiesceParticipant.class.getName(), participant3, null);
        //recreate the list as it may have been emptied
        bundleList = new ArrayList<Bundle>();
        bundleList.add(b1);
        bundleList.add(b2);
        assertEquals("Bundle "+b1.getSymbolicName()+" should be in active state", Bundle.ACTIVE, b1.getState());
        assertEquals("Bundle "+b2.getSymbolicName()+" should be in active state", Bundle.ACTIVE, b2.getState());
        assertEquals("Bundle "+b3.getSymbolicName()+" should be in active state", Bundle.ACTIVE, b3.getState());
        manager.quiesce(2000,bundleList);
        bundleList = new ArrayList<Bundle>();
        bundleList.add(b2);
        bundleList.add(b3);
        manager.quiesce(2000,bundleList);
        timeoutTime = System.currentTimeMillis()+10000;
        while (System.currentTimeMillis() < timeoutTime && (b1.getState() == Bundle.ACTIVE || b2.getState() == Bundle.ACTIVE || b3.getState() == Bundle.ACTIVE)) {
            Thread.sleep(500);
        }
        assertEquals("Participant 1 should have started twice as it has been asked to quiesce twice", 2, participant1.getStartedCount());
        assertEquals("Participant 1 should finished twice as it should have returned from two quiesce requests immediately", 2, participant1.getFinishedCount());
        assertEquals("Participant 2 should have started twice as it has been asked to quiesce twice", 2, participant2.getStartedCount());
        assertEquals("Participant 2 should not have finished as it should never return from it's two quiesce requests", 0, participant2.getFinishedCount());
        assertEquals("Participant 3 should have started twice as it has been asked to quiesce twice", 2, participant3.getStartedCount());
        assertEquals("Participant 3 should finished twice as it should have waited a short time before returning from it's two quiesce requests", 2, participant3.getFinishedCount());
        assertTrue("Bundle "+b1.getSymbolicName()+" should not be in active state", b1.getState() != Bundle.ACTIVE);
        assertTrue("Bundle "+b2.getSymbolicName()+" should not be in active state", b2.getState() != Bundle.ACTIVE);
        assertTrue("Bundle "+b3.getSymbolicName()+" should not be in active state", b3.getState() != Bundle.ACTIVE);
    }

    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
        return testOptions(
                paxLogging("DEBUG"),

                // Bundles
                mavenBundle("org.osgi", "org.osgi.compendium"),
                mavenBundle("org.apache.aries", "org.apache.aries.util"),
                mavenBundle("commons-lang", "commons-lang"),
                mavenBundle("commons-collections", "commons-collections"),
                mavenBundle("commons-pool", "commons-pool"),
                mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.serp"),
                mavenBundle("org.apache.aries.quiesce", "org.apache.aries.quiesce.api"),
                mavenBundle("org.apache.aries.quiesce", "org.apache.aries.quiesce.manager"),
                
                //new VMOption( "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000" ),
                //new TimeoutOption( 0 ),

                equinox().version("3.5.0"));
    }
}
