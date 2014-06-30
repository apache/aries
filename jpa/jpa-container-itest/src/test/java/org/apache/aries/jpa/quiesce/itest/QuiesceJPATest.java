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
import static org.ops4j.pax.exam.CoreOptions.options;

import java.util.Collections;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.apache.aries.jpa.itest.AbstractJPAItest;
import org.apache.aries.quiesce.manager.QuiesceCallback;
import org.apache.aries.quiesce.participant.QuiesceParticipant;
import org.junit.After;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@ExamReactorStrategy(PerMethod.class)
public class QuiesceJPATest extends AbstractJPAItest {
	private static final int WAIT_TIME = 200;
	private static final String JPA_CONTAINER = "org.apache.aries.jpa.container";
	private static final String JPA_CONTEXT = "org.apache.aries.jpa.container.context";
	private final String TEST_BUNDLE = "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle";

	@Inject
	UserTransaction tm;

	//This is load bearing. we have to wait to create the EntityManager until the DataSource is available
	@Inject
	DataSource ds;

	private class TestQuiesceCallback implements QuiesceCallback{

		protected int calls = 0;

		public void bundleQuiesced(Bundle... arg0) {
			calls++;
		}

		public boolean bundleClearedUp()
		{
			return calls == 1;
		}
	}

	private class MultiQuiesceCallback extends TestQuiesceCallback implements QuiesceCallback{

		private boolean contextFirst = true;

		public void bundleQuiesced(Bundle... arg0) {
			if(++calls == 1)
				try {
					getEMF(TEST_UNIT);
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


	@After
	public void restartTestBundles() throws BundleException {
		restartTestBundle();
		restartBundle(JPA_CONTAINER);
		restartBundle(JPA_CONTEXT);
		try {
			tm.rollback();
		} catch (Exception e) {
			// Ignore
		}
	}

	@Test
	public void testSimpleContextQuiesce() throws Exception {
		registerClient(TEST_UNIT);
		getProxyEMF(TEST_UNIT);

		//Quiesce it
		TestQuiesceCallback callback = getQuiesceCallback(JPA_CONTEXT, TEST_BUNDLE);
		Thread.sleep(WAIT_TIME);
		assertFinished(callback);

		assertNoProxyEMFForTestUnit();

		restartTestBundle();

		getProxyEMF(TEST_UNIT);
	}

	@Test
	public void testComplexContextQuiesce() throws Exception {
		registerClient(TEST_UNIT);

		EntityManagerFactory emf = getProxyEMF(TEST_UNIT);
		tm.begin();
		emf.createEntityManager().getProperties();

		TestQuiesceCallback callback = getQuiesceCallback(JPA_CONTEXT, TEST_BUNDLE);
		assertNotFinished(callback);

		emf = getProxyEMF(TEST_UNIT);
		tm.commit();
		assertTrue("Quiesce not finished", callback.bundleClearedUp());
		assertNoProxyEMFForTestUnit();

		restartTestBundle();

		emf = getProxyEMF(TEST_UNIT);
		tm.begin();
		emf.createEntityManager().getProperties();
		tm.commit();

		Thread.sleep(WAIT_TIME);

		//Test again to make sure we don't hold state over
		emf = getProxyEMF(TEST_UNIT);
		tm.begin();
		emf.createEntityManager().getProperties();

		callback = getQuiesceCallback(JPA_CONTEXT, TEST_BUNDLE);
		assertNotFinished(callback);

		emf = getProxyEMF(TEST_UNIT);
		tm.commit();

		assertFinished(callback);
		assertNoProxyEMFForTestUnit();
	}

	@Test
	public void testContextRuntimeQuiesce() throws Exception {
		registerClient(TEST_UNIT);

		EntityManagerFactory emf = getProxyEMF(TEST_UNIT);
		tm.begin();
		emf.createEntityManager().getProperties();

		TestQuiesceCallback callback = getQuiesceCallback(JPA_CONTEXT, JPA_CONTEXT);
		assertNotFinished(callback);

		emf = getProxyEMF(TEST_UNIT);
		tm.commit();

		assertFinished(callback);
		assertNoProxyEMFForTestUnit();
	}

	@Test
	public void testSimpleUnitQuiesce() throws Exception {
		assertEMFForTestUnit();

		TestQuiesceCallback callback = getQuiesceCallback(JPA_CONTAINER, TEST_BUNDLE);
		Thread.sleep(WAIT_TIME);
		assertFinished(callback);
		assertNoEMFForTestUnit();

		restartTestBundle();

		assertEMFForTestUnit();
	}

	
	@Test
	public void testComplexUnitQuiesce() throws Exception {
		quiesceUnit();
		restartTestBundle();
	    getEMF(TEST_UNIT).createEntityManager().close();
		//Test a second time to make sure state isn't held
		quiesceUnit();
	}
	
	private void quiesceUnit() throws Exception {
		EntityManager em = getEMF(TEST_UNIT).createEntityManager();

		TestQuiesceCallback callback = getQuiesceCallback(JPA_CONTAINER, TEST_BUNDLE);
		assertNotFinished(callback);

		assertEMFForTestUnit();
		em.close();
		assertFinished(callback);
		assertNoEMFForTestUnit();
	}

	@Test
	public void testContainerRuntimeQuiesce() throws Exception {
		EntityManagerFactory emf = getEMF(TEST_UNIT);
		EntityManager em = emf.createEntityManager();

		TestQuiesceCallback callback = getQuiesceCallback(JPA_CONTAINER, JPA_CONTAINER);
		assertNotFinished(callback);

		assertEMFForTestUnit();
		em.close();
		assertFinished(callback);
		assertNoEMFForTestUnit();
	}

	@Test
	public void testComplexQuiesceInteraction() throws Exception {
		registerClient(TEST_UNIT);

		EntityManagerFactory emf = getProxyEMF(TEST_UNIT);
		tm.begin();
		emf.createEntityManager().getProperties();

		//Quiesce the Unit, nothing should happen
		TestQuiesceCallback unitCallback = getQuiesceCallback(JPA_CONTAINER, TEST_BUNDLE);
		assertNotFinished(unitCallback);

		emf = getProxyEMF(TEST_UNIT);

		//Quiesce the context, still nothing
		TestQuiesceCallback contextCallback = getQuiesceCallback(JPA_CONTEXT, TEST_BUNDLE);
		assertNotFinished(unitCallback, contextCallback);

		emf = getProxyEMF(TEST_UNIT);

		//Keep the unit alive
		emf = getEMF(TEST_UNIT);

		EntityManager em = emf.createEntityManager();
		tm.commit();
		assertFinished(contextCallback);
		assertNoProxyEMFForTestUnit();
		assertEMFForTestUnit();
		em.close();

		assertFinished(unitCallback);
		assertNoEMFForTestUnit();
	}

	@Test
	public void testComplexQuiesceInteraction2() throws Exception {
		registerClient(TEST_UNIT);

		EntityManagerFactory emf = getProxyEMF(TEST_UNIT);

		tm.begin();

		emf.createEntityManager().getProperties();

		MultiQuiesceCallback callback = new MultiQuiesceCallback();

		//Quiesce the Unit, nothing should happen
		QuiesceParticipant participant = getParticipant(JPA_CONTAINER);
		participant.quiesce(callback, Collections.singletonList(context().getBundleByName(
				TEST_BUNDLE)));

		//Quiesce the context, still nothing
		participant = getParticipant(JPA_CONTEXT);
		participant.quiesce(callback, Collections.singletonList(
				context().getBundleByName(TEST_BUNDLE)));
		assertNotFinished(callback);

		emf = getProxyEMF(TEST_UNIT);
		assertEMFForTestUnit();

		tm.commit();

		assertFinished(callback);
		assertNoEMFForTestUnit();
	}

	private void assertFinished(TestQuiesceCallback callback) {
		assertTrue("Quiesce not finished", callback.bundleClearedUp());
	}

	private void assertNotFinished(TestQuiesceCallback... callbacks)
			throws InterruptedException {
		Thread.sleep(WAIT_TIME);
		for (TestQuiesceCallback callback : callbacks) {
			assertFalse("Quiesce finished", callback.bundleClearedUp());
		}
	}

	private void assertNoEMFForTestUnit() throws InvalidSyntaxException {
		assertNull("No unit should exist", getEMFRefs(TEST_UNIT));
	}

	private void assertEMFForTestUnit() {
		getEMF(TEST_UNIT);
	}

	private void assertNoProxyEMFForTestUnit() throws InvalidSyntaxException {
		assertNull("No context should exist", getProxyEMFRefs(TEST_UNIT));
	}

	private TestQuiesceCallback getQuiesceCallback(String participantName, String bundleName) throws InvalidSyntaxException {
		QuiesceParticipant participant = getParticipant(participantName);
		TestQuiesceCallback callback = new TestQuiesceCallback();
		participant.quiesce(callback, Collections.singletonList(context().getBundleByName(bundleName)));
		return callback;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
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

	private void restartTestBundle() throws BundleException {
		restartBundle(TEST_BUNDLE_NAME);
	}

	private void restartBundle(String bundleName) throws BundleException {
		Bundle b = context().getBundleByName(bundleName);
		b.stop();
		b.start();
	}

	@Configuration
	public Option[] configuration() {
		return options(
				baseOptions(),
				ariesJpa(),
				openJpa(),
				testDs(),
				testBundle()
				);
	}

}
