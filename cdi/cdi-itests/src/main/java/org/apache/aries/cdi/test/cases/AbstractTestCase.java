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

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.cdi.CdiConstants;
import org.osgi.service.cdi.runtime.CdiRuntime;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.osgi.util.tracker.ServiceTracker;

import junit.framework.TestCase;

public class AbstractTestCase extends TestCase {

	@Override
	protected void setUp() throws Exception {
		servicesBundle = bundleContext.installBundle("services-one.jar" , getBundle("services-one.jar"));
		servicesBundle.start();
		cdiBundle = bundleContext.installBundle("basic-beans.jar" , getBundle("basic-beans.jar"));
		cdiBundle.start();

		runtimeTracker = new ServiceTracker<>(bundleContext, CdiRuntime.class, null);
		runtimeTracker.open();
		cdiRuntime = runtimeTracker.waitForService(timeout);
	}

	@Override
	protected void tearDown() throws Exception {
		runtimeTracker.close();
		cdiBundle.uninstall();
		servicesBundle.uninstall();
	}

	void assertBeanExists(Class<?> clazz, BeanManager beanManager) {
		Set<Bean<?>> beans = beanManager.getBeans(clazz, Any.Literal.INSTANCE);

		assertFalse(beans.isEmpty());
		Iterator<Bean<?>> iterator = beans.iterator();
		Bean<?> bean = iterator.next();
		assertTrue(clazz.isAssignableFrom(bean.getBeanClass()));
		assertFalse(iterator.hasNext());

		bean = beanManager.resolve(beans);
		CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
		Object pojo = clazz.cast(beanManager.getReference(bean, clazz, ctx));
		assertNotNull(pojo);
	}

	InputStream getBundle(String name) {
		Class<?> clazz = this.getClass();

		ClassLoader classLoader = clazz.getClassLoader();

		return classLoader.getResourceAsStream(name);
	}

	Bundle getCdiExtenderBundle() {
		BundleWiring bundleWiring = cdiBundle.adapt(BundleWiring.class);

		List<BundleWire> requiredWires = bundleWiring.getRequiredWires(ExtenderNamespace.EXTENDER_NAMESPACE);

		for (BundleWire wire : requiredWires) {
			Map<String, Object> attributes = wire.getCapability().getAttributes();
			String extender = (String)attributes.get(ExtenderNamespace.EXTENDER_NAMESPACE);

			if (CdiConstants.CDI_CAPABILITY_NAME.equals(extender)) {
				return wire.getProvider().getBundle();
			}
		}

		return null;
	}

	public Bundle installBundle(String url) throws Exception {
		return installBundle(url, true);
	}

	public Bundle installBundle(String bundleName, boolean start) throws Exception {
		Bundle b = bundleContext.installBundle(bundleName, getBundle(bundleName));

		if (start) {
			b.start();
		}

		return b;
	}

	Filter filter(String pattern, Object... objects) {
		try {
			return FrameworkUtil.createFilter(String.format(pattern, objects));
		}
		catch (InvalidSyntaxException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	BeanManager getBeanManager(Bundle bundle) throws Exception {
		return getServiceTracker(bundle).waitForService(timeout);
	}

	ServiceTracker<BeanManager, BeanManager> getServiceTracker(Bundle bundle) throws Exception {
		ServiceTracker<BeanManager, BeanManager> serviceTracker = new ServiceTracker<>(
			bundleContext,
			filter(
				"(&(objectclass=%s)(bundle.id=%d))",
				BeanManager.class.getName(),
				bundle.getBundleId()),
			null);
		serviceTracker.open();
		return serviceTracker;
	}

	Promise<ContainerDTO> getContainerDTO() throws Exception {
		return getContainerDTO(bundle);
	}

	Promise<ContainerDTO> getContainerDTO(Bundle bundle) throws Exception {
		final PromiseFactory factory = new PromiseFactory(
				PromiseFactory.inlineExecutor());

		Promise<ContainerDTO> promise = factory.submit(new Callable<ContainerDTO>() {
			@Override
			public ContainerDTO call() throws Exception {
				while (!Thread.interrupted()) {
					ContainerDTO containerDTO = cdiRuntime.getContainerDTO(bundle);
					if (containerDTO == null) {
						Thread.sleep(10);
						continue;
					}
					return containerDTO;
				}
				return null;
			}
		});

		return promise.timeout(timeout);
	}

	static final Bundle bundle = FrameworkUtil.getBundle(CdiBeanTests.class);
	static final BundleContext bundleContext = bundle.getBundleContext();
	static final long timeout = 5000;

	Bundle cdiBundle;
	Bundle servicesBundle;
	CdiRuntime cdiRuntime;
	Promise<ContainerDTO> containerDTO;
	ServiceTracker<CdiRuntime, CdiRuntime> runtimeTracker;

}