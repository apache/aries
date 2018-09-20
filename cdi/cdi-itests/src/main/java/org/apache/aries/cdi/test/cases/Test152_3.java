package org.apache.aries.cdi.test.cases;

import static org.assertj.core.api.Assertions.*;

import javax.enterprise.context.spi.Context;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.ServiceTracker;

public class Test152_3 extends AbstractTestCase {

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
	public void componentScopeContext() throws Exception {
		Bundle tbBundle = installBundle("tb152_3.jar");

		try {
			getBeanManager(tbBundle);

			ServiceTracker<Object, Object> oneTracker = track("(&(objectClass=%s)(%s=%s))", BeanService.class.getName(), Constants.SERVICE_DESCRIPTION, "one");
			oneTracker.open();
			Object service = oneTracker.waitForService(timeout);

			ServiceTracker<Object, Object> twoTracker = track("(&(objectClass=%s)(%s=%s))", BeanService.class.getName(), Constants.SERVICE_DESCRIPTION, "two");
			twoTracker.open();
			twoTracker.waitForService(timeout);

			assertThat(service).isNotNull();
			BeanService<Context> bs = (BeanService<Context>)service;
			Context context = bs.get();
			assertThat(context).isNotNull();
		}
		finally {
			tbBundle.uninstall();
		}
	}

}
