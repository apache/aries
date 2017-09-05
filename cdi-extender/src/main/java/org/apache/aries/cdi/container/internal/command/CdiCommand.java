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

package org.apache.aries.cdi.container.internal.command;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.aries.cdi.container.internal.component.ComponentModel;
import org.apache.aries.cdi.container.internal.configuration.ConfigurationCallback;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.extension.ExtensionDependency;
import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.apache.aries.cdi.container.internal.reference.ReferenceCallback;
import org.apache.aries.cdi.container.internal.reference.ReferenceModel;
import org.apache.aries.cdi.container.internal.service.ServiceDeclaration;
import org.apache.aries.cdi.container.internal.util.Conversions;
import org.osgi.framework.Bundle;

public class CdiCommand {

	public String list() {
		try (Formatter f = new Formatter()) {
			for (ContainerState cdiContainerState : _states.values()) {
				f.format("[%s] %s%n", cdiContainerState.id(), cdiContainerState.lastState());
			}
			return f.toString();
		}
	}

	public String info(Bundle bundle) {
		try (Formatter f = new Formatter()) {
			ContainerState containerState = _states.get(bundle);

			if (containerState == null) {
				f.format("No CDI Bundle found matching {}", bundle);

				return f.toString();
			}

			f.format("[%s] %s%n", containerState.id(), containerState.lastState());

			List<ExtensionDependency> extensionDependencies = containerState.extensionDependencies();

			if (!extensionDependencies.isEmpty()) {
				f.format("  [EXTENSIONS]");

				for (ExtensionDependency extensionDependency : extensionDependencies) {
					f.format("    Extension: %s%s%n", extensionDependency.toString(), " ???is this resolved???");
				}
			}

			BeansModel beansModel = containerState.beansModel();
			Collection<ComponentModel> componentModels = beansModel.getComponentModels();

			if (!componentModels.isEmpty()) {
				for (ComponentModel componentModel : componentModels) {
					ServiceDeclaration serviceDeclaration = containerState.serviceComponents().get(componentModel);

					f.format("[COMPONENT]%n");
					f.format(
						"  Name: %s%n    BeanClass: %s%n    ServiceScope: %s%n    Provides: %s%n",
						componentModel.getName(),
						componentModel.getBeanClass().getName(),
						componentModel.getServiceScope(),
						serviceDeclaration != null ? Arrays.toString(serviceDeclaration.getClassNames()): "not yet ready!");

					f.format("  [CONFIGURATIONS]%n");

					Map<String, ConfigurationCallback> configurationCallbacks = containerState.configurationCallbacks().get(componentModel);

					for (Entry<String, ConfigurationCallback> entry : configurationCallbacks.entrySet()) {
						f.format(
							"    PID: %s%n      Policy: %s%n      Resolved: %s%n",
							entry.getKey(),
							entry.getValue().policy(),
							entry.getValue().resolved() ? "YES" : "NO");
					}

					if (serviceDeclaration != null) {
						Dictionary<String, ?> configuration = serviceDeclaration.getServiceProperties();

						if (!configuration.isEmpty()) {
							f.format("    Properties:%n");

							List<String> keys = Collections.list(configuration.keys());

							Collections.sort(keys);

							for (String key : keys) {
								String value = Conversions.toString(configuration.get(key));

								f.format("      %s = %s%n", key, value);
							}
						}
					}

					List<ReferenceModel> references = componentModel.getReferences();

					if (!references.isEmpty()) {
						f.format("  [REFERENCES]%n");

						Map<String, ReferenceCallback> referenceCallbacks = containerState.referenceCallbacks().get(componentModel);

						for (ReferenceModel referenceModel : references) {
							f.format(
								"    Name: %s%n      Service: %s%n      Target: %s%n      Cardinality: %s%n      Policy: %s%n      Policy Option: %s%n      Scope: %s%n      Resolved: %s%n",
								referenceModel.getName(),
								referenceModel.getServiceClass().getName(),
								referenceModel.getTarget(),
								referenceModel.getCardinality(),
								referenceModel.getPolicy(),
								referenceModel.getPolicyOption(),
								referenceModel.getScope(),
								referenceCallbacks.get(referenceModel.getName()).resolved() ? "YES" : "NO");
						}
					}
				}
			}

			return f.toString();
		}
	}

	public void add(Bundle bundle, ContainerState cdiContainerState) {
		_states.put(bundle, cdiContainerState);
	}

	public void remove(Bundle bundle) {
		_states.remove(bundle);
	}

	private final Map<Bundle, ContainerState> _states = new ConcurrentHashMap<>();


}