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

import java.util.Hashtable;
import java.util.List;

import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.service.cdi.PortableExtensionNamespace;
import org.osgi.service.jndi.JNDIConstants;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

public class JndiExtensionTests extends AbstractTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		containerDTO = getContainerDTO(cdiBundle);
	}

	public void testGetBeanManagerThroughJNDI() throws Exception {
		Hashtable<String, Object> env = new Hashtable<>();
		env.put(JNDIConstants.BUNDLE_CONTEXT, cdiBundle.getBundleContext());
		InitialContext context = new InitialContext(env);

		BeanManager beanManager = (BeanManager)context.lookup("java:comp/BeanManager");

		assertNotNull(beanManager);
		assertBeanExists(Pojo.class, beanManager);
	}

	public void testDisableExtensionAndCDIContainerWaits() throws Exception {
		BundleTracker<Bundle> bt = new BundleTracker<>(
			bundle.getBundleContext(), Bundle.RESOLVED | Bundle.ACTIVE, new BundleTrackerCustomizer<Bundle>() {

				@Override
				public Bundle addingBundle(Bundle bundle, BundleEvent arg1) {
					List<BundleCapability> capabilities = bundle.adapt(
						BundleWiring.class).getCapabilities(PortableExtensionNamespace.CDI_EXTENSION_NAMESPACE);

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

		// TODO Check that everything is ok...

		extensionBundle.stop();

		// TODO check that CDI bundles dependent on the extension are not not OK

		extensionBundle.start();

		// TODO check that they are ok again!
	}

}