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

package org.apache.aries.cdi.container.internal.model;

import static org.osgi.namespace.extender.ExtenderNamespace.EXTENDER_NAMESPACE;
import static org.osgi.service.cdi.CdiExtenderConstants.CDI_EXTENDER;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class BeansModelBuilder extends AbstractModelBuilder {

	public BeansModelBuilder(BundleWiring bundleWiring, Bundle extenderBundle) {
		_bundleWiring = bundleWiring;
		_extenderBundle = extenderBundle;
		_bundle = _bundleWiring.getBundle();

		List<BundleWire> wires = bundleWiring.getRequiredWires(EXTENDER_NAMESPACE);

		Map<String, Object> cdiAttributes = Collections.emptyMap();

		for (BundleWire wire : wires) {
			BundleCapability capability = wire.getCapability();
			Map<String, Object> attributes = capability.getAttributes();
			String extender = (String)attributes.get(EXTENDER_NAMESPACE);

			if (extender.equals(CDI_EXTENDER)) {
				BundleRequirement requirement = wire.getRequirement();
				cdiAttributes = requirement.getAttributes();
				break;
			}
		}

		_attributes = cdiAttributes;
	}

	@Override
	Map<String, Object> getAttributes() {
		return _attributes;
	}

	@Override
	ClassLoader getClassLoader() {
		return _extenderBundle.adapt(BundleWiring.class).getClassLoader();
	}

	@Override
	URL getResource(String resource) {
		return _bundle.getResource(resource);
	}

	@Override
	Collection<String> getResources(String descriptorString) {
		int pos = descriptorString.lastIndexOf('/');
		String path = descriptorString.substring(0, pos);
		String fileName = descriptorString.substring(pos, descriptorString.length());

		return _bundleWiring.listResources(path, fileName, BundleWiring.LISTRESOURCES_LOCAL);
	}

	private final Map<String, Object> _attributes;
	private final Bundle _bundle;
	private final BundleWiring _bundleWiring;
	private final Bundle _extenderBundle;

}
