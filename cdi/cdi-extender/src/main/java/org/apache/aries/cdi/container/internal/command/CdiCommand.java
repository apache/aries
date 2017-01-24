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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.aries.cdi.container.internal.container.CdiContainerState;
import org.apache.aries.cdi.container.internal.container.ExtensionDependency;
import org.apache.aries.cdi.container.internal.container.ReferenceDependency;

public class CdiCommand {

	public CdiCommand() {

	}

	public void list() {
		for (CdiContainerState cdiContainerState : _states.values()) {
			System.out.printf("[%s] %s\n", cdiContainerState.getId(), cdiContainerState.getLastState());
		}
	}

	public void info(long bundleId) {
		CdiContainerState cdiContainerState = _states.get(bundleId);

		if (cdiContainerState == null) {
			System.out.println("No matching CDI bundle found.");

			return;
		}

		System.out.printf("[%s] %s\n", cdiContainerState.getId(), cdiContainerState.getLastState());
		List<ExtensionDependency> extensionDependencies = cdiContainerState.getExtensionDependencies();
		if (!extensionDependencies.isEmpty()) {
			System.out.println("  [EXTENSIONS]");
			for (ExtensionDependency extensionDependency : extensionDependencies) {
				System.out.printf("    %s%s\n", extensionDependency.toString(), " ???is this resolved???");
			}
		}
		List<ReferenceDependency> referenceDependencies = cdiContainerState.getReferenceDependencies();
		if (!referenceDependencies.isEmpty()) {
			System.out.println("  [REFERENCES]");
			for (ReferenceDependency referenceDependency : referenceDependencies) {
				System.out.printf(
					"    %s\n      Status: %s\n      Min Cardinality: %s\n",
					referenceDependency.toString(),
					!referenceDependency.isResolved() ? " UNRESOLVED" : "resolved",
					referenceDependency.getMinCardinality());
			}
		}
	}

	public void add(Long bundleId, CdiContainerState cdiContainerState) {
		_states.put(bundleId, cdiContainerState);
	}

	public void remove(Long bundleId) {
		_states.remove(bundleId);
	}

	private final Map<Long, CdiContainerState> _states = new ConcurrentHashMap<>();

}