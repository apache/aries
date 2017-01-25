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

package org.apache.aries.cdi.container.internal.locate;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.cdi.CdiExtenderConstants;

public class ClassLocater {

	private static final String BEANS_DIRECTIVE = CdiExtenderConstants.REQUIREMENT_BEANS_DIRECTIVE;
	private static final String[] BEAN_DESCRIPTOR_PATHS = new String[] {"META-INF/beans.xml", "WEB-INF/beans.xml"};

	@SuppressWarnings("unchecked")
	public static ClassLocaterResult locate(BundleWiring bundleWiring) {
		List<URL> beanDescriptorURLs = new ArrayList<URL>();

		for (String descriptorPath : BEAN_DESCRIPTOR_PATHS) {
			URL beanDescriptorURL = bundleWiring.getBundle().getResource(descriptorPath);

			if (beanDescriptorURL != null) {
				beanDescriptorURLs.add(beanDescriptorURL);
			}
		}

		List<String> beanClasses = new ArrayList<String>();

		List<BundleWire> wires = bundleWiring.getRequiredWires(ExtenderNamespace.EXTENDER_NAMESPACE);

		for (BundleWire wire : wires) {
			BundleCapability capability = wire.getCapability();
			Map<String, Object> attributes = capability.getAttributes();
			String extender = (String)attributes.get(ExtenderNamespace.EXTENDER_NAMESPACE);
			if (extender.equals(CdiExtenderConstants.CDI_EXTENDER)) {
				BundleRequirement requirement = wire.getRequirement();
				Map<String, String> directives = requirement.getDirectives();
				if (directives.containsKey(BEANS_DIRECTIVE)) {
					String string = directives.get(BEANS_DIRECTIVE);
					List<String> list = Arrays.asList(string.split("\\s*,\\s*"));
					beanClasses.addAll(list);
				}
			}
		}

		if (beanClasses.isEmpty()) {
			Collection<String> resources = bundleWiring.listResources(
				"/", "*.class", BundleWiring.LISTRESOURCES_LOCAL | BundleWiring.LISTRESOURCES_RECURSE);

			if (resources != null) {
				for (String resource : resources) {
					resource = resource.replace('/', '.');
					resource = resource.replace(".class", "");

					beanClasses.add(resource);
				}
			}
		}

		return new ClassLocaterResult(beanClasses, beanDescriptorURLs);
	}

}
