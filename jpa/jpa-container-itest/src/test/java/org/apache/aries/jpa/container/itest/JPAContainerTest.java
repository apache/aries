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
package org.apache.aries.jpa.container.itest;

import static org.ops4j.pax.exam.CoreOptions.options;

import org.apache.aries.jpa.itest.AbstractJPAItest;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

public class JPAContainerTest extends AbstractJPAItest {

	@Test
	public void findEntityManagerFactory() throws Exception {
		getEMF(TEST_UNIT);
	}

	@Test
	public void findEntityManagerFactory2() throws Exception {
		getEMF(BP_TEST_UNIT);
	}

	@Test
	public void findEntityManager() throws Exception {
		getEMF(TEST_UNIT).createEntityManager();
	}

	@Test
	public void findEntityManager2() throws Exception {
		getEMF(BP_TEST_UNIT).createEntityManager();
	}

	@Configuration
	public Option[] configuration() {
		return options(
				baseOptions(),
				ariesJpa20(),
				// Needed for the BP_TEST_UNIT
				transactionWrapper(),
				openJpa(),
				testDs(),
				testBundle());

	}

}
