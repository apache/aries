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

import java.net.URL;
import java.util.Collection;
import java.util.Map;

import org.apache.aries.cdi.container.internal.component.ComponentModel;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.xml.BeansXmlParser;

public class BeansModel {

	public BeansModel(
		Map<String, ComponentModel> components,
		Collection<URL> beanDescriptorURLs) {

		_components = components;

		BeansXml beansXml = BeansXml.EMPTY_BEANS_XML;

		if (!beanDescriptorURLs.isEmpty()) {
			BeansXmlParser beansXmlParser = new BeansXmlParser();
			beansXml = beansXmlParser.parse(beanDescriptorURLs);
		}

		_beansXml = beansXml;
	}

	public void addComponentModel(String componentClass, ComponentModel componentModel) {
		_components.put(componentClass, componentModel);
	}

	public Collection<String> getBeanClassNames() {
		return _components.keySet();
	}

	public BeansXml getBeansXml() {
		return _beansXml;
	}

	public ComponentModel getComponentModel(String componentClass) {
		return _components.get(componentClass);
	}

	public Collection<ComponentModel> getComponentModels() {
		return _components.values();
	}

	public void removeComponentModel(String beanClassName) {
		_components.remove(beanClassName);
	}

	private final BeansXml _beansXml;
	private final Map<String, ComponentModel> _components;

}