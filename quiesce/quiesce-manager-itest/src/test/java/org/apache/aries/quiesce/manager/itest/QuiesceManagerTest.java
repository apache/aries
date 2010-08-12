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

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.util.ArrayList;
import java.util.List;

import org.apache.aries.quiesce.manager.QuiesceManager;
import org.apache.aries.quiesce.participant.QuiesceParticipant;
import org.junit.Before;
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
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.util.tracker.ServiceTracker;

@RunWith(JUnit4TestRunner.class)
public class QuiesceManagerTest {
  public static final long DEFAULT_TIMEOUT = 30000;
  private QuiesceManager manager;
  private Bundle b1;
  private Bundle b2;
  private Bundle b3;
  private long timeoutTime;
  private List<Bundle> bundleList;
  private MockQuiesceParticipant participant1;
  private MockQuiesceParticipant participant2;
  private MockQuiesceParticipant participant3;


  @Inject
  protected BundleContext bundleContext;
  
  @Before
  public void setup() {
	  manager = getOsgiService(QuiesceManager.class);
	  b1 = bundleContext.getBundle(5);
	  b2 = bundleContext.getBundle(6);
	  b3 = bundleContext.getBundle(10);
	  participant1 = new MockQuiesceParticipant(MockQuiesceParticipant.RETURNIMMEDIATELY);
	  participant2 = new MockQuiesceParticipant(MockQuiesceParticipant.NEVERRETURN);
	  participant3 = new MockQuiesceParticipant(MockQuiesceParticipant.WAIT);

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
	  assertTrue("Bundle "+b1.getSymbolicName()+" should be in active state", b1.getState() == Bundle.ACTIVE);
	  //Try quiescing one bundle with no participants
	  manager.quiesce(bundleList);
	  //quiesce is non-blocking so what do we do? 
	  //verify bundle is no longer active
	  timeoutTime = System.currentTimeMillis()+5000;
	  while (System.currentTimeMillis() < timeoutTime && b1.getState() == Bundle.ACTIVE){
		  Thread.sleep(500);
	  }
	  assertTrue("Bundle "+b1.getSymbolicName()+" should not be in active state", b1.getState() != Bundle.ACTIVE);
	  b1.start();
  }
  
  @Test
  public void testImmediateReturn() throws Exception {
	  bundleList = new ArrayList<Bundle>();
	  bundleList.add(b1);
	  //Register a mock participant which will report back quiesced immediately
	  ServiceRegistration sr = bundleContext.registerService(QuiesceParticipant.class.getName(), participant1, null);
	  //Try quiescing the bundle with immediate return
	  assertTrue("Bundle "+b1.getSymbolicName()+" should be in active state", b1.getState() == Bundle.ACTIVE);
	  manager.quiesce(1000,bundleList);
	  timeoutTime = System.currentTimeMillis()+5000;
	  while (System.currentTimeMillis() < timeoutTime && b1.getState() == Bundle.ACTIVE){
		  Thread.sleep(500);
	  }
	  assertTrue("Participant should have finished once", participant1.getFinishedCount() == 1);
	  assertTrue("Bundle "+b1.getSymbolicName()+" should not be in active state", b1.getState() != Bundle.ACTIVE);
	  b1.start();
	  sr.unregister();
	  participant1.reset();
  }
    
  @Test
  public void testNoReturn() throws Exception {
	  //Register a mock participant which won't respond
	  ServiceRegistration sr = bundleContext.registerService(QuiesceParticipant.class.getName(), participant2, null);
	  //recreate the list as it may have been emptied?
	  bundleList = new ArrayList<Bundle>();
	  bundleList.add(b1);
	  
	  //Try quiescing the bundle with no return
	  assertTrue("Bundle "+b1.getSymbolicName()+" should be in active state", b1.getState() == Bundle.ACTIVE);
	  manager.quiesce(1000,bundleList);
	  timeoutTime = System.currentTimeMillis()+5000;
	  while (System.currentTimeMillis() < timeoutTime && b1.getState() == Bundle.ACTIVE){
		  Thread.sleep(500);
	  }
	  assertTrue("Participant should have started once", participant2.getStartedCount() == 1);
	  assertTrue("Participant should not have finished", participant2.getFinishedCount() == 0);
	  assertTrue("Bundle "+b1.getSymbolicName()+" should not be in active state", b1.getState() != Bundle.ACTIVE);
	  b1.start();
	  sr.unregister();
	  participant2.reset();
  }
	  
	@Test
	public void testWaitAShortTime() throws Exception {
	  //Try quiescing where participant takes 5s to do the work. We should get InterruptedException
	  ServiceRegistration sr = bundleContext.registerService(QuiesceParticipant.class.getName(), participant3, null);
	  //recreate the list as it may have been emptied?
	  bundleList = new ArrayList<Bundle>();
	  bundleList.add(b1);
	  
	  assertTrue("Bundle "+b1.getSymbolicName()+" should be in active state", b1.getState() == Bundle.ACTIVE);
	  manager.quiesce(10000,bundleList);
	  //timeout is > how long participant takes, and < the quiesce timeout
	  timeoutTime = System.currentTimeMillis()+7000;
	  while (System.currentTimeMillis() < timeoutTime && b1.getState() == Bundle.ACTIVE){
		  Thread.sleep(500);
	  }
	  assertTrue("Participant should have started once", participant3.getStartedCount() == 1);
	  assertTrue("Participant should finished once", participant3.getFinishedCount() == 1);
	  assertTrue("Bundle "+b1.getSymbolicName()+" should not be in active state", b1.getState() != Bundle.ACTIVE);
	  b1.start();
	  participant3.reset();
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
	  assertTrue("Bundle "+b1.getSymbolicName()+" should be in active state", b1.getState() == Bundle.ACTIVE);
	  manager.quiesce(10000,bundleList);
	  timeoutTime = System.currentTimeMillis()+15000;
	  while (System.currentTimeMillis() < timeoutTime && b1.getState() == Bundle.ACTIVE){
		  Thread.sleep(500);
	  }
	  assertTrue("Participant 1 should have started once", participant1.getStartedCount() == 1);
	  assertTrue("Participant 1 should finished once", participant1.getFinishedCount() == 1);
	  assertTrue("Participant 2 should have started once", participant2.getStartedCount() == 1);
	  assertTrue("Participant 2 should not have finished", participant2.getFinishedCount() == 0);
	  assertTrue("Participant 3 should have started once", participant3.getStartedCount() == 1);
	  assertTrue("Participant 3 should finished once", participant3.getFinishedCount() == 1);
	  assertTrue("Bundle "+b1.getSymbolicName()+" should not be in active state", b1.getState() != Bundle.ACTIVE);
	  
	  b1.start();
	  participant1.reset();
	  participant2.reset();
	  participant3.reset();
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
	  assertTrue("Bundle "+b1.getSymbolicName()+" should be in active state", b1.getState() == Bundle.ACTIVE);
	  assertTrue("Bundle "+b2.getSymbolicName()+" should be in active state", b2.getState() == Bundle.ACTIVE);
	  manager.quiesce(10000,bundleList);
	  timeoutTime = System.currentTimeMillis()+15000;
	  while (System.currentTimeMillis() < timeoutTime && b1.getState() == Bundle.ACTIVE){
		  Thread.sleep(500);
	  }
	  assertTrue("Participant 1 should have started once", participant1.getStartedCount() == 1);
	  assertTrue("Participant 1 should finished once", participant1.getFinishedCount() == 1);
	  assertTrue("Participant 2 should have started once", participant2.getStartedCount() == 1);
	  assertTrue("Participant 2 should not have finished", participant2.getFinishedCount() == 0);
	  assertTrue("Participant 3 should have started once", participant3.getStartedCount() == 1);
	  assertTrue("Participant 3 should finished once", participant3.getFinishedCount() == 1);
	  assertTrue("Bundle "+b1.getSymbolicName()+" should not be in active state", b1.getState() != Bundle.ACTIVE);
	  assertTrue("Bundle "+b2.getSymbolicName()+" should not be in active state", b2.getState() != Bundle.ACTIVE);
	  b1.start();
	  b2.start();	
	  participant1.reset();
	  participant2.reset();
	  participant3.reset();
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
	  assertTrue("Bundle "+b1.getSymbolicName()+" should be in active state", b1.getState() == Bundle.ACTIVE);
	  assertTrue("Bundle "+b2.getSymbolicName()+" should be in active state", b2.getState() == Bundle.ACTIVE);
	  assertTrue("Bundle "+b3.getSymbolicName()+" should be in active state", b3.getState() == Bundle.ACTIVE);
	  manager.quiesce(10000,bundleList);
	  bundleList = new ArrayList<Bundle>();
	  bundleList.add(b2);
	  bundleList.add(b3);
	  manager.quiesce(bundleList);
	  timeoutTime = System.currentTimeMillis()+15000;
	  while (System.currentTimeMillis() < timeoutTime && b1.getState() == Bundle.ACTIVE){
		  Thread.sleep(500);
	  }
	  assertTrue("Participant 1 should have started twice", participant1.getStartedCount() == 2);
	  assertTrue("Participant 1 should finished twice", participant1.getFinishedCount() == 2);
	  assertTrue("Participant 2 should have started twice", participant2.getStartedCount() == 2);
	  assertTrue("Participant 2 should not have finished", participant2.getFinishedCount() == 0);
	  assertTrue("Participant 3 should have started twice", participant3.getStartedCount() == 2);
	  assertTrue("Participant 3 should finished twice", participant3.getFinishedCount() == 2);
	  assertTrue("Bundle "+b1.getSymbolicName()+" should not be in active state", b1.getState() != Bundle.ACTIVE);
	  assertTrue("Bundle "+b2.getSymbolicName()+" should not be in active state", b2.getState() != Bundle.ACTIVE);
	  assertTrue("Bundle "+b3.getSymbolicName()+" should not be in active state", b3.getState() != Bundle.ACTIVE);
	  participant1.reset();
	  participant2.reset();
	  participant3.reset();
	  
	}
 
  @org.ops4j.pax.exam.junit.Configuration
  public static Option[] configuration() {
    Option[] options = options(
        bootDelegation(),
        
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
        mavenBundle("commons-lang", "commons-lang"),
        mavenBundle("commons-collections", "commons-collections"),
        mavenBundle("commons-pool", "commons-pool"),
        mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.serp"),
        mavenBundle("org.apache.aries.quiesce", "org.apache.aries.quiesce.api"),
        mavenBundle("org.apache.aries.quiesce", "org.apache.aries.quiesce.manager"),
        
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
