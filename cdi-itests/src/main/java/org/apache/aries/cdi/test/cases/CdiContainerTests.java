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

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

import org.osgi.framework.wiring.BundleWiring;

public class CdiContainerTests extends AbstractTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		cdiContainer = waitForCdiContainer(cdiBundle.getBundleId());
	}

	public void testGetBeanFromCdiContainerService() throws Exception {
		BeanManager beanManager = cdiContainer.getBeanManager();

		assertNotNull(beanManager);
		assertPojoExists(beanManager);
	}

	public void testGetBeanManagerFromCDI() throws Exception {
		Thread currentThread = Thread.currentThread();
		ClassLoader contextClassLoader = currentThread.getContextClassLoader();

		try {
			BundleWiring bundleWiring = cdiBundle.adapt(BundleWiring.class);

			currentThread.setContextClassLoader(bundleWiring.getClassLoader());

			BeanManager beanManager = CDI.current().getBeanManager();

			assertNotNull(beanManager);
			assertPojoExists(beanManager);
		}
		finally {
			currentThread.setContextClassLoader(contextClassLoader);
		}
	}

}