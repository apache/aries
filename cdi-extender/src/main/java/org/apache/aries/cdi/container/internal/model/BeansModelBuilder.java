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
import static org.osgi.service.cdi.CdiConstants.CDI_CAPABILITY_NAME;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class BeansModelBuilder extends AbstractModelBuilder {

	public BeansModelBuilder(ClassLoader classLoader, BundleWiring bundleWiring) {
		_classLoader = classLoader;
		_bundleWiring = bundleWiring;
		_bundle = _bundleWiring.getBundle();

		List<BundleWire> wires = bundleWiring.getRequiredWires(EXTENDER_NAMESPACE);

		Map<String, Object> cdiAttributes = Collections.emptyMap();

		for (BundleWire wire : wires) {
			BundleCapability capability = wire.getCapability();
			Map<String, Object> attributes = capability.getAttributes();
			String extender = (String)attributes.get(EXTENDER_NAMESPACE);

			if (extender.equals(CDI_CAPABILITY_NAME)) {
				BundleRequirement requirement = wire.getRequirement();
				cdiAttributes = requirement.getAttributes();
				break;
			}
		}

		_attributes = cdiAttributes;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return _attributes;
	}

	@Override
	public ClassLoader getClassLoader() {
		return _classLoader;
	}

	@Override
	public URL getResource(String resource) {
		return _bundle.getResource(resource);
	}

	@Override
	public List<String> getDefaultResources() {
		return new ArrayList<>(_bundleWiring.listResources("OSGI-INF/cdi", "*.xml", BundleWiring.LISTRESOURCES_LOCAL));
	}

	private final Map<String, Object> _attributes;
	private final Bundle _bundle;
	private final ClassLoader _classLoader;
	private final BundleWiring _bundleWiring;

}
