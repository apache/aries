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
public class QuiesceJPAInteractionTest extends AbstractQuiesceJPATest {
    
    /**
     * TODO This test does not work together with the QuiesceJPATest tests.
     * This might point to an error in the code
     */
	@Test
	public void testComplexQuiesceInteraction() throws Exception {
		registerClient(TEST_UNIT);

		EntityManagerFactory emf = getProxyEMF(TEST_UNIT);
		tm.begin();
		emf.createEntityManager().getProperties();

		//Quiesce the Unit, nothing should happen
		TestQuiesceCallback unitCallback = quiesce(JPA_CONTAINER, TEST_BUNDLE);
		assertNotFinished(unitCallback);
		getProxyEMF(TEST_UNIT);

		//Quiesce the context, still nothing
		TestQuiesceCallback contextCallback = quiesce(JPA_CONTEXT, TEST_BUNDLE);
		assertNotFinished(unitCallback, contextCallback);
		getProxyEMF(TEST_UNIT);

		//Keep the unit alive
		emf = getEMF(TEST_UNIT);

		EntityManager em = emf.createEntityManager();
		LOG.info("After commit quiesce should happen");
		tm.commit();
		
		assertFinished(contextCallback);
		assertNoProxyEMFForTestUnit();
		getEMF(TEST_UNIT);
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

		// Quiesce the Unit, nothing should happen
		getParticipant(JPA_CONTAINER).quiesce(callback, getListForTestBundle());
		assertNotFinished(callback);

		// Quiesce the context, still nothing
		getParticipant(JPA_CONTEXT).quiesce(callback, getListForTestBundle());
		assertNotFinished(callback);

		getProxyEMF(TEST_UNIT);
		getEMF(TEST_UNIT);

		tm.commit();

		assertFinished(callback);
		assertNoEMFForTestUnit();
	}

}
