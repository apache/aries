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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.Test;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@ExamReactorStrategy(PerClass.class)
public class QuiesceJPATest extends AbstractQuiesceJPATest {
	@Test
	public void testSimpleContextQuiesce() throws Exception {
		registerClient(TEST_UNIT);
		getProxyEMF(TEST_UNIT);

		// Quiesce should work
		TestQuiesceCallback callback = quiesce(JPA_CONTEXT, TEST_BUNDLE);
		Thread.sleep(WAIT_TIME);
		assertFinished(callback);
		assertNoProxyEMFForTestUnit();

		// After restart emf should be there again
		restartTestBundle();
		getProxyEMF(TEST_UNIT);
	}

	@Test
	public void testComplexContextQuiesce() throws Exception {
		registerClient(TEST_UNIT);

        testQuiesceContext();

		restartTestBundle();

		//Test again to make sure we don't hold state over
		testQuiesceContext();
	}

    private void testQuiesceContext() throws Exception {
        EntityManagerFactory emf = getProxyEMF(TEST_UNIT);
		tm.begin();
		emf.createEntityManager().getProperties();

		TestQuiesceCallback callback = quiesce(JPA_CONTEXT, TEST_BUNDLE);
		assertNotFinished(callback);

		getProxyEMF(TEST_UNIT);
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

		TestQuiesceCallback callback = quiesce(JPA_CONTEXT, JPA_CONTEXT);
		assertNotFinished(callback);

		emf = getProxyEMF(TEST_UNIT);
		tm.commit();

		assertFinished(callback);
		assertNoProxyEMFForTestUnit();
	}

	@Test
	public void testSimpleUnitQuiesce() throws Exception {
		getEMF(TEST_UNIT);

		TestQuiesceCallback callback = quiesce(JPA_CONTAINER, TEST_BUNDLE);
		Thread.sleep(WAIT_TIME);
		assertFinished(callback);
		assertNoEMFForTestUnit();

		restartTestBundle();

		getEMF(TEST_UNIT);
	}

	
	@Test
	public void testComplexUnitQuiesce() throws Exception {
		testQuiesceUnit();
		restartTestBundle();
		//Test a second time to make sure state isn't held
		testQuiesceUnit();
	}
	
	private void testQuiesceUnit() throws Exception {
		EntityManager em = getEMF(TEST_UNIT).createEntityManager();

		TestQuiesceCallback callback = quiesce(JPA_CONTAINER, TEST_BUNDLE);
		assertNotFinished(callback);

		getEMF(TEST_UNIT);
		em.close();
		assertFinished(callback);
		assertNoEMFForTestUnit();
	}

	@Test
	public void testContainerRuntimeQuiesce() throws Exception {
		EntityManagerFactory emf = getEMF(TEST_UNIT);
		EntityManager em = emf.createEntityManager();

		TestQuiesceCallback callback = quiesce(JPA_CONTAINER, JPA_CONTAINER);
		assertNotFinished(callback);

		getEMF(TEST_UNIT);
		em.close();
		assertFinished(callback);
		assertNoEMFForTestUnit();
	}

}
