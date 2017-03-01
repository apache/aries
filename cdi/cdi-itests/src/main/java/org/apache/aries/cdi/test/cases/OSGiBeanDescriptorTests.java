package org.apache.aries.cdi.test.cases;

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.TypeLiteral;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.framework.Bundle;
import org.osgi.service.cdi.CdiContainer;
import org.osgi.util.tracker.ServiceTracker;

public class OSGiBeanDescriptorTests extends AbstractTestCase {

	public void testServices() throws Exception {
		Bundle tb2Bundle = installBundle("tb2.jar");

		ServiceTracker<Pojo, Pojo> st = new ServiceTracker<Pojo, Pojo>(
			bundleContext, Pojo.class, null);
		st.open(true);

		try {
			Pojo pojo = st.waitForService(timeout);
			assertNotNull(pojo);
		}
		finally {
			tb2Bundle.uninstall();
		}
	}

	@SuppressWarnings("serial")
	public void testReferences() throws Exception {
		Bundle tb1Bundle = installBundle("tb1.jar");
		Bundle tb2Bundle = installBundle("tb2.jar");

		CdiContainer cdiContainer = waitForCdiContainer(tb1Bundle.getBundleId());

		try {
			BeanManager beanManager = cdiContainer.getBeanManager();
			Set<Bean<?>> beans = beanManager.getBeans("beanimpl");
			Bean<?> bean = beanManager.resolve(beans);
			CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
			BeanService<?> beanService = (BeanService<?>)beanManager.getReference(
				bean, new TypeLiteral<BeanService<?>>() {}.getType(), ctx);

			assertNotNull(beanService);
			assertEquals("POJO-IMPLBEAN-IMPL", beanService.doSomething());
		}
		finally {
			tb2Bundle.uninstall();
			tb1Bundle.uninstall();
		}
	}

	@Override
	protected void setUp() throws Exception {
	}

	@Override
	protected void tearDown() throws Exception {
	}

}
