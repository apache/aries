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

import static org.assertj.core.api.Assertions.assertThat;

import javax.servlet.ServletContextListener;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.util.tracker.ServiceTracker;

public class BeanPropertyTypeTest extends AbstractTestCase {

	@BeforeClass
	public static void beforeClass() throws Exception {
	}

	@AfterClass
	public static void afterClass() throws Exception {
	}

	@Override
	public void setUp() throws Exception {
	}

	@After
	@Override
	public void tearDown() throws Exception {
	}

	@Test
	public void usesMarkerAnnotation() throws Exception {
		Bundle tbBundle = installBundle("tb13.jar");

		try {
			getBeanManager(tbBundle);

			ServiceTracker<ServletContextListener, ServletContextListener> oneTracker = track("(objectClass=%s)", ServletContextListener.class.getName());
			oneTracker.open();
			Object service = oneTracker.waitForService(timeout);

			assertThat(service).isNotNull();
		}
		finally {
			tbBundle.uninstall();
		}
	}

}
