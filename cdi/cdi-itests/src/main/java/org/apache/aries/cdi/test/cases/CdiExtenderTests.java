package org.apache.aries.cdi.test.cases;

import org.osgi.framework.Bundle;
import org.osgi.service.cdi.CdiContainer;
import org.osgi.util.tracker.ServiceTracker;

public class CdiExtenderTests extends AbstractTestCase {

	public void testStopExtender() throws Exception {
		Bundle cdiExtenderBundle = getCdiExtenderBundle();

		ServiceTracker<CdiContainer,CdiContainer> serviceTracker = getServiceTracker(cdiBundle.getBundleId());

		try {
			assertNotNull(serviceTracker.waitForService(timeout));

			cdiExtenderBundle.stop();

			assertTrue(serviceTracker.isEmpty());

			cdiExtenderBundle.start();

			assertNotNull(serviceTracker.waitForService(timeout));
		}
		finally {
			serviceTracker.close();
		}
	}

}