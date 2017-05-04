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

import static org.junit.Assert.assertArrayEquals;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.TypeLiteral;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.CdiConstants;
import org.osgi.service.cdi.CdiContainer;
import org.osgi.service.cdi.CdiEvent;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class ConfigurationTests extends AbstractTestCase {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testConfiguration() throws Exception {
		Bundle tb3Bundle = installBundle("tb3.jar");

		Configuration configurationA = null, configurationB = null;

		try {
			Filter filter = bundleContext.createFilter(
				"(&(objectClass=" + CdiContainer.class.getName() + ")(service.bundleid=" + tb3Bundle.getBundleId() + "))");

			ServiceTracker<CdiContainer, CdiContainer> containerTracker = new ServiceTracker<>(
				bundleContext, filter, null);

			containerTracker.open();

			containerTracker.waitForService(timeout);

			ServiceReference<CdiContainer> serviceReference = containerTracker.getServiceReference();

			assertNotNull(serviceReference);

			assertEquals(
				CdiEvent.Type.WAITING_FOR_CONFIGURATIONS,
				serviceReference.getProperty(CdiConstants.CDI_CONTAINER_STATE));

			configurationA = configurationAdmin.getConfiguration("configA", "?");

			Dictionary<String, Object> properties = new Hashtable<>();
			properties.put("ports", new int[] {12, 4567});
			configurationA.update(properties);

			configurationB = configurationAdmin.getConfiguration("configB", "?");

			properties = new Hashtable<>();
			properties.put("color", "green");
			properties.put("ports", new int[] {80});
			configurationB.update(properties);

			containerTracker.close();

			filter = bundleContext.createFilter(
				"(&(objectClass=" + CdiContainer.class.getName() + ")(service.bundleid=" + tb3Bundle.getBundleId() +
				")(" + CdiConstants.CDI_CONTAINER_STATE + "=CREATED))");

			containerTracker = new ServiceTracker<>(bundleContext, filter, null);

			containerTracker.open();

			containerTracker.waitForService(timeout);

			ServiceTracker<BeanService, BeanService> stA = new ServiceTracker<BeanService, BeanService>(
				bundleContext, bundleContext.createFilter(
					"(&(objectClass=org.apache.aries.cdi.test.interfaces.BeanService)(bean=A))"), null);
			stA.open(true);

			BeanService<Callable<int[]>> beanService = stA.waitForService(timeout);

			assertNotNull(beanService);
			assertEquals("blue", beanService.doSomething());
			assertArrayEquals(new int[] {12, 4567}, beanService.get().call());

			ServiceTracker<BeanService, BeanService> stB = new ServiceTracker<BeanService, BeanService>(
				bundleContext, bundleContext.createFilter(
					"(&(objectClass=org.apache.aries.cdi.test.interfaces.BeanService)(bean=B))"), null);
			stB.open(true);

			beanService = stB.waitForService(timeout);

			assertNotNull(beanService);
			assertEquals("green", beanService.doSomething());
			assertArrayEquals(new int[] {80}, beanService.get().call());
		}
		finally {
			if (configurationA != null)
				configurationA.delete();
			if (configurationB != null)
				configurationB.delete();
			tb3Bundle.uninstall();
		}
	}

	@SuppressWarnings({ "unchecked", "serial" })
	public void testNamedConfiguration() throws Exception {
		Bundle tb3Bundle = installBundle("tb3.jar");

		Configuration configurationA = null, configurationB = null;

		try {
			configurationA = configurationAdmin.getConfiguration("configA", "?");

			Dictionary<String, Object> properties = new Hashtable<>();
			properties.put("ports", new int[] {12, 4567});
			configurationA.update(properties);

			configurationB = configurationAdmin.getConfiguration("configB", "?");

			properties = new Hashtable<>();
			properties.put("color", "green");
			properties.put("ports", new int[] {80});
			configurationB.update(properties);

			Filter filter = bundleContext.createFilter(
				"(&(objectClass=" + CdiContainer.class.getName() + ")(service.bundleid=" + tb3Bundle.getBundleId() +
				")(" + CdiConstants.CDI_CONTAINER_STATE + "=CREATED))");

			ServiceTracker<CdiContainer, CdiContainer> serviceTracker = new ServiceTracker<>(bundleContext, filter, null);

			serviceTracker.open();

			CdiContainer container = serviceTracker.waitForService(timeout);

			assertNotNull(container);

			int trackerCount = serviceTracker.getTrackingCount();

			BeanManager beanManager = container.getBeanManager();
			Set<Bean<?>> beans = beanManager.getBeans("configB");
			assertNotNull(beans);
			Bean<? extends Object> bean = beanManager.resolve(beans);
			CreationalContext<?> ctx = beanManager.createCreationalContext(bean);

			Map<String, Object> config = (Map<String, Object>)beanManager.getReference(
				bean, new TypeLiteral<Map<String, Object>>() {}.getType(), ctx);

			assertNotNull(config);
			assertEquals("green", config.get("color"));

			assertArrayEquals(new int[] {80}, (int[])config.get("ports"));

			configurationA.delete();

			while (trackerCount == serviceTracker.getTrackingCount()) {
				Thread.sleep(10);
			}

			assertTrue(serviceTracker.isEmpty());

			serviceTracker.close();

			filter = bundleContext.createFilter(
				"(&(objectClass=" + CdiContainer.class.getName() + ")(service.bundleid=" + tb3Bundle.getBundleId() +
				")(" + CdiConstants.CDI_CONTAINER_STATE + "=" + CdiEvent.Type.WAITING_FOR_CONFIGURATIONS + "))");

			serviceTracker = new ServiceTracker<>(bundleContext, filter, null);

			serviceTracker.open();

			assertFalse(serviceTracker.isEmpty());
		}
		finally {
			if (configurationB != null)
				configurationB.delete();
			tb3Bundle.uninstall();
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testOptionalConfiguration() throws Exception {
		Bundle tb5Bundle = installBundle("tb5.jar");

		Configuration configurationC = null;

		try {
			Filter filter = bundleContext.createFilter(
				"(&(objectClass=" + CdiContainer.class.getName() + ")(service.bundleid=" + tb5Bundle.getBundleId() +
				")(" + CdiConstants.CDI_CONTAINER_STATE + "=CREATED))");

			ServiceTracker<CdiContainer, CdiContainer> containerTracker = new ServiceTracker<>(bundleContext, filter, null);

			containerTracker.open();

			containerTracker.waitForService(timeout);

			ServiceTracker<BeanService, BeanService> stC = new ServiceTracker<BeanService, BeanService>(
				bundleContext, bundleContext.createFilter(
					"(&(objectClass=org.apache.aries.cdi.test.interfaces.BeanService)(bean=C))"), null);
			stC.open(true);

			BeanService<Callable<int[]>> beanService = stC.waitForService(timeout);

			int trackingCount = stC.getTrackingCount();

			assertNotNull(beanService);
			assertEquals("blue", beanService.doSomething());
			assertArrayEquals(new int[] {35777}, beanService.get().call());

			configurationC = configurationAdmin.getConfiguration("foo.bar", "?");

			Dictionary<String, Object> properties = new Hashtable<>();
			properties.put("ports", new int[] {12, 4567});
			configurationC.update(properties);

			do {Thread.sleep(100);}
			while (trackingCount == stC.getTrackingCount());

			beanService = stC.waitForService(timeout);

			trackingCount = stC.getTrackingCount();

			assertNotNull(beanService);
			assertEquals("blue", beanService.doSomething());
			assertArrayEquals(new int[] {12, 4567}, beanService.get().call());

			configurationC.delete();

			do {Thread.sleep(100);}
			while (trackingCount == stC.getTrackingCount());

			beanService = stC.waitForService(timeout);

			assertNotNull(beanService);
			assertEquals("blue", beanService.doSomething());
			assertArrayEquals(new int[] {35777}, beanService.get().call());
		}
		finally {
			tb5Bundle.uninstall();
		}
	}

	@Override
	protected void setUp() throws Exception {
		adminTracker = new ServiceTracker<>(bundleContext, ConfigurationAdmin.class, null);

		adminTracker.open();

		configurationAdmin = adminTracker.getService();
	}

	@Override
	protected void tearDown() throws Exception {
		adminTracker.close();
	}

	private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> adminTracker;
	private ConfigurationAdmin configurationAdmin;

}
