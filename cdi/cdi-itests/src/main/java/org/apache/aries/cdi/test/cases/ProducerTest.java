/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.test.cases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.cdi.runtime.CDIComponentRuntime;
import org.osgi.util.tracker.ServiceTracker;

public class ProducerTest extends AbstractTestCase {

	@Test
	public void checkProducersAreProperlyHandled() throws Exception {
		Bundle bundle = installBundle("tb12.jar");
		bundle.start();

		try (CloseableTracker<Pojo, Pojo> track = track("(&(objectClass=%s)(component.name=integerManager))", Pojo.class.getName())) {
			Pojo pojo = track.waitForService(5000);

			assertNotNull(pojo);
			assertEquals(4, pojo.getCount());
			assertNotNull(pojo.getMap());
			assertTrue(pojo.getMap().containsKey(Constants.SERVICE_RANKING));
			assertEquals(100000, pojo.getMap().get(Constants.SERVICE_RANKING));
		}
		finally {
			bundle.uninstall();
		}
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		runtimeTracker = new ServiceTracker<>(
			bundleContext, CDIComponentRuntime.class, null);
		runtimeTracker.open();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		runtimeTracker.close();
	}

	@Override
	@Before
	public void setUp() throws Exception {
		cdiRuntime = runtimeTracker.waitForService(timeout);
	}

	@Override
	@After
	public void tearDown() throws Exception {
	}

}
