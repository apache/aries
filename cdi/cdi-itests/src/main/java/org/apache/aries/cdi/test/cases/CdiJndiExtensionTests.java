package org.apache.aries.cdi.test.cases;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.service.cdi.CdiContainer;
import org.osgi.service.cdi.CdiEvent;
import org.osgi.service.cdi.CdiExtenderConstants;
import org.osgi.service.jndi.JNDIConstants;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

public class CdiJndiExtensionTests extends AbstractTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		cdiContainer = waitForCdiContainer(cdiBundle.getBundleId());
	}

	public void testGetBeanManagerThroughJNDI() throws Exception {
		Hashtable<String, Object> env = new Hashtable<>();
		env.put(JNDIConstants.BUNDLE_CONTEXT, cdiBundle.getBundleContext());
		InitialContext context = new InitialContext(env);

		BeanManager beanManager = (BeanManager)context.lookup("java:comp/BeanManager");

		assertNotNull(beanManager);
		assertPojoExists(beanManager);
	}

	public void testDisableExtensionAndCDIContainerWaits() throws Exception {
		BundleTracker<Bundle> bt = new BundleTracker<>(
			bundle.getBundleContext(), Bundle.RESOLVED | Bundle.ACTIVE, new BundleTrackerCustomizer<Bundle>() {

				@Override
				public Bundle addingBundle(Bundle bundle, BundleEvent arg1) {
					List<BundleCapability> capabilities = bundle.adapt(
						BundleWiring.class).getCapabilities(CdiExtenderConstants.CDI_EXTENSION);

					if (capabilities.isEmpty()) {
						return null;
					}

					for (Capability capability : capabilities) {
						if (capability.getAttributes().containsValue("jndi")) {
							return bundle;
						}
					}

					return null;
				}

				@Override
				public void modifiedBundle(Bundle bundle, BundleEvent arg1, Bundle arg2) {
				}

				@Override
				public void removedBundle(Bundle bundle, BundleEvent arg1, Bundle arg2) {
				}
			}
		);

		bt.open();

		assertFalse(bt.isEmpty());

		Bundle extensionBundle = bt.getBundles()[0];

		Collection<ServiceReference<CdiContainer>> serviceReferences = bundleContext.getServiceReferences(
			CdiContainer.class, "(&(objectClass=" + CdiContainer.class.getName() + ")(service.bundleid=" +
				cdiBundle.getBundleId() + "))");

		assertNotNull(serviceReferences);
		assertFalse(serviceReferences.isEmpty());

		ServiceReference<CdiContainer> serviceReference = serviceReferences.iterator().next();

		CdiEvent.State state = (CdiEvent.State)serviceReference.getProperty(
			CdiExtenderConstants.CDI_EXTENDER_CONTAINER_STATE);

		assertEquals(CdiEvent.State.CREATED, state);

		extensionBundle.stop();

		state = (CdiEvent.State)serviceReference.getProperty(
			CdiExtenderConstants.CDI_EXTENDER_CONTAINER_STATE);

		assertEquals(CdiEvent.State.WAITING_FOR_EXTENSIONS, state);

		extensionBundle.start();

		state = (CdiEvent.State)serviceReference.getProperty(
			CdiExtenderConstants.CDI_EXTENDER_CONTAINER_STATE);

		assertEquals(CdiEvent.State.CREATED, state);
	}

}