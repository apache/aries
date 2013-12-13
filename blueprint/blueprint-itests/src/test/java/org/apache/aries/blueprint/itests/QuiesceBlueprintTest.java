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
package org.apache.aries.blueprint.itests;

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;

import junit.framework.Assert;

import org.apache.aries.blueprint.testquiescebundle.TestBean;
import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.quiesce.manager.QuiesceCallback;
import org.apache.aries.quiesce.participant.QuiesceParticipant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.BootDelegationOption;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import static org.apache.aries.itest.ExtraOptions.*;

@RunWith(JUnit4TestRunner.class)
public class QuiesceBlueprintTest extends AbstractIntegrationTest {

  private static class TestQuiesceCallback implements QuiesceCallback
  {
    private int calls = 0;

  	public synchronized void bundleQuiesced(Bundle... bundlesQuiesced) {
  		System.out.println("bundleQuiesced "+ Arrays.toString(bundlesQuiesced));
  	  calls++;
  	}
  	
  	public synchronized int getCalls() {
  		return calls;
  	}
  }
  
  private QuiesceParticipant getParticipant(String bundleName) throws InvalidSyntaxException {
    ServiceReference[] refs = bundleContext.getServiceReferences(QuiesceParticipant.class.getName(), null);
    
    if(refs != null) {
      for(ServiceReference ref : refs) {
        if(ref.getBundle().getSymbolicName().equals(bundleName))
          return (QuiesceParticipant) bundleContext.getService(ref);
        else System.out.println(ref.getBundle().getSymbolicName());
      }
    }
    
    
    return null;
  }

  @org.ops4j.pax.exam.junit.Configuration
  public static Option[] configuration() {
    return testOptions(
            paxLogging("DEBUG"),
            transactionBootDelegation(),
            Helper.blueprintBundles(),
        
            mavenBundle("org.apache.aries.quiesce", "org.apache.aries.quiesce.api"),
            mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.testbundlea").noStart(),
            mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.testbundleb").noStart(),
            mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.testquiescebundle"),

            equinox().version("3.5.0"));
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

  @Test
  public void testBasicQuieseEmptyCounter() throws Exception 
  {
	  //This test checks that a single bundle when called will not quiesce while 
	  //there is an active request (method sleeps), but will quiesce after the 
	  //request is completed. 
	  
	System.out.println("In testBasicQuieseEmptyCounter");
	Object obj = context().getService(TestBean.class);
	
	if (obj != null)
	{
	  QuiesceParticipant participant = getParticipant("org.apache.aries.blueprint.core");
	  
	  if (participant != null)
	  {
	    System.out.println(obj.getClass().getName());

	    TestQuiesceCallback callback = new TestQuiesceCallback();
	    
	    Bundle bundle = getBundle("org.apache.aries.blueprint.testquiescebundle");
	    
	    System.out.println("Got the bundle");
	    
	    List<Bundle> bundles = new ArrayList<Bundle>();
	    bundles.add(bundle);
	    
	    Thread t = new Thread(new TestBeanClient((TestBean)obj, 2000));
	    t.start();

	    System.out.println("Thread Started");
	    
	    participant.quiesce(callback, bundles);
	    
	    System.out.println("Called Quiesce");
	    
	    Thread.sleep(1000);
	    
	    Assert.assertTrue("Quiesce callback should not have occurred yet; calls should be 0, but it is "+callback.getCalls(), callback.getCalls()==0);
	    
	    t.join();
	    
	    System.out.println("After join");
	    
	    Assert.assertTrue("Quiesce callback should have occurred once; calls should be 1, but it is "+callback.getCalls(), callback.getCalls()==1);
	    
 	  }
  	  else
	  {
		throw new Exception("No Quiesce Participant found for the blueprint service");
	  }
  
	  System.out.println("done");
	}
	else
	{
		throw new Exception("No Service returned for " + TestBean.class);
	}
  }
  
  @Test
  public void testNoServicesQuiesce() throws Exception {
   
   //This test covers the case where one of the bundles being asked to quiesce has no 
   //services. It should be quiesced immediately.
	  
   System.out.println("In testNoServicesQuiesce");
	Object obj = context().getService(TestBean.class);
	
	if (obj != null)
	{    
		QuiesceParticipant participant = getParticipant("org.apache.aries.blueprint.core");
		
		if (participant != null)
		{
			TestQuiesceCallback callbackA = new TestQuiesceCallback();
			TestQuiesceCallback callbackB = new TestQuiesceCallback();
		    
			//bundlea provides the ns handlers, bean processors, interceptors etc for this test.
	        Bundle bundlea = getBundle("org.apache.aries.blueprint.testbundlea");
	        assertNotNull(bundlea);
	        bundlea.start();
	        
	        //bundleb has no services and makes use of the extensions provided by bundlea
	        Bundle bundleb = getBundle("org.apache.aries.blueprint.testbundleb");
	        assertNotNull(bundleb);
	        bundleb.start();
	        
	        Helper.getBlueprintContainerForBundle(context(), "org.apache.aries.blueprint.testbundleb");
	        
			participant.quiesce(callbackB, Collections.singletonList(getBundle(
				"org.apache.aries.blueprint.testbundleb")));
			
		    System.out.println("Called Quiesce");
		    
		    Thread.sleep(200);
		    
		    Assert.assertTrue("Quiesce callback B should have occurred; calls should be 1, but it is "+callbackB.getCalls(), callbackB.getCalls()==1);
		    Assert.assertTrue("Quiesce callback A should not have occurred yet; calls should be 0, but it is "+callbackA.getCalls(), callbackA.getCalls()==0);
		    
		    bundleb.stop();
		    
		    participant.quiesce(callbackA, Collections.singletonList(getBundle(
			"org.apache.aries.blueprint.testbundlea")));
				    
		    Thread.sleep(1000);
		    
		    System.out.println("After second sleep");
		    
		    Assert.assertTrue("Quiesce callback A should have occurred once; calls should be 1, but it is "+callbackA.getCalls(), callbackA.getCalls()==1);
		    Assert.assertTrue("Quiesce callback B should have occurred once; calls should be 1, but it is "+callbackB.getCalls(), callbackB.getCalls()==1);
		    
		}else{
			throw new Exception("No Quiesce Participant found for the blueprint service");
		}
	}else{
		throw new Exception("No Service returned for " + TestBean.class);
	}
  }

  @Test
  public void testMultiBundleQuiesce() throws Exception {
   
   //This test covers the case where two bundles are quiesced at the same time. 
   //Bundle A should quiesce immediately, quiesce bundle should quiesce after the 
   //request has completed.
	  
   System.out.println("In testMultiBundleQuiesce");
	Object obj = context().getService(TestBean.class);
	
	if (obj != null)
	{    
		QuiesceParticipant participant = getParticipant("org.apache.aries.blueprint.core");
		
		if (participant != null)
		{
			TestQuiesceCallback callback = new TestQuiesceCallback();
		    
			//bundlea provides the ns handlers, bean processors, interceptors etc for this test.
	        Bundle bundlea = getBundle("org.apache.aries.blueprint.testbundlea");
	        assertNotNull(bundlea);
	        bundlea.start();
	        
	        //quiesce bundle will sleep for a second so will quiesce after that
		    Bundle bundleq = getBundle("org.apache.aries.blueprint.testquiescebundle");
		    
		    System.out.println("Got the bundle");
		    
		    List<Bundle> bundles = new ArrayList<Bundle>();
		    bundles.add(bundlea);
		    bundles.add(bundleq);
		    
		    Thread t = new Thread(new TestBeanClient((TestBean)obj, 1500));
		    t.start();
		    Thread.sleep(200);
	        
			  participant.quiesce(callback, bundles);
			
		    System.out.println("Called Quiesce");
		    
		    Thread.sleep(500);
		    
		    Assert.assertTrue("Quiesce callback should have occurred once for bundle a but not for bundle q; calls should be 1, but it is "+callback.getCalls(), callback.getCalls()==1);
		    
		    Thread.sleep(1500);
		    
		    System.out.println("After second sleep");
		    
		    Assert.assertTrue("Quiesce callback should have occurred twice, once for bundle a and q respectively; calls should be 2, but it is "+callback.getCalls(), callback.getCalls()==2);
		    
		}else{
			throw new Exception("No Quiesce Participant found for the blueprint service");
		}
	}else{
		throw new Exception("No Service returned for " + TestBean.class);
	}
  }
  
  @Test
  public void testMultiRequestQuiesce() throws Exception {
   
   //This test covers the case where we have two active requests when 
   //the bundle is being quiesced.
	  
   System.out.println("In testMultiRequestQuiesce");
	Object obj = context().getService(TestBean.class);
	
	if (obj != null)
	{    
		QuiesceParticipant participant = getParticipant("org.apache.aries.blueprint.core");
		
		if (participant != null)
		{
			TestQuiesceCallback callback = new TestQuiesceCallback();
			TestBeanClient client =  new TestBeanClient((TestBean)obj, 1500);
		    

	        //quiesce bundle will sleep for a second so will quiesce after that
		    Bundle bundle = getBundle("org.apache.aries.blueprint.testquiescebundle");
		    
		    System.out.println("Got the bundle");
		    
		    List<Bundle> bundles = new ArrayList<Bundle>();
		    bundles.add(bundle);
		    
		    Thread t = new Thread(client);
		    t.start();
	        
			participant.quiesce(callback, bundles);
			
		    System.out.println("Called Quiesce, putting in a new request");
		    
		    Thread t2 = new Thread(client);
		    t2.start();
		    
		    Thread.sleep(5000);
		    
		    Assert.assertTrue("Quiesce callback should have occurred once; calls should be 1, but it is "+callback.getCalls(), callback.getCalls()==1);
		    
   
		}else{
			throw new Exception("No Quiesce Participant found for the blueprint service");
		}
	}else{
		throw new Exception("No Service returned for " + TestBean.class);
	}
  }
  
  
  private class TestBeanClient implements Runnable
  {
    private final TestBean myService;
    private final int time;
    
    public TestBeanClient(TestBean myService, int time)
    {
      this.myService = myService;
      this.time = time;
    }
	  
	public void run() 
	{
	  try
	  {
		System.out.println("In Test Bean Client - Sleeping zzzzzzz");
		myService.sleep(time);
		System.out.println("Woken up");
	  }
	  catch (InterruptedException ie)
	  {
		  ie.printStackTrace();
	  }
	}
	  
  }
}
