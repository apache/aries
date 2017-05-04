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

package org.apache.aries.cdi.container.internal.container;

import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.apache.aries.cdi.container.internal.model.BeansModelBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

public class Phase_Init implements Phase {

	public Phase_Init(Bundle bundle, CdiContainerState cdiContainerState) {
		BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);

		BeansModel beansModel = new BeansModelBuilder(bundleWiring, cdiContainerState.getExtenderBundle()).build();

		cdiContainerState.setBeansModel(beansModel);

		_extensionPhase = new Phase_Extension(cdiContainerState);
	}

	@Override
	public void close() {
		_extensionPhase.close();
	}

	@Override
	public void open() {
		_extensionPhase.open();
	}

	private final Phase _extensionPhase;

}