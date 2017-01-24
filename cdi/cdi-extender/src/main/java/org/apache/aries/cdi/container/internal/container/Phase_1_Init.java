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

import java.util.Collection;

import org.apache.aries.cdi.container.internal.locate.ClassLocater;
import org.apache.aries.cdi.container.internal.locate.ClassLocaterResult;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.xml.BeansXmlParser;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

public class Phase_1_Init {

	public Phase_1_Init(Bundle bundle, CdiContainerState cdiContainerState) {
		BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);

		ClassLocaterResult locatorResult = ClassLocater.locate(bundleWiring);
		Collection<String> beanClassNames = locatorResult.getBeanClassNames();
		BeansXmlParser beansXmlParser = new BeansXmlParser();
		BeansXml beansXml = beansXmlParser.parse(locatorResult.getBeanDescriptorURLs());

		_extensionPhase = new Phase_2_Extension(bundle, cdiContainerState, beanClassNames, beansXml);
	}

	public void close() {
		_extensionPhase.close();
	}

	public void open() {
		_extensionPhase.open();
	}

	private final Phase_2_Extension _extensionPhase;

}