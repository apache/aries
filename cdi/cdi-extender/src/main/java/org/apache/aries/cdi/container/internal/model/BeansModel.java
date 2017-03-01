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

import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.xml.BeansXmlParser;

public class BeansModel {

	public BeansModel(
		Collection<String> beanClasses, Collection<ConfigurationModel> configurationModels,
		Collection<ReferenceModel> referenceModels, Collection<ServiceModel> serviceModels,
		Collection<URL> beanDescriptorURLs) {

		_beanClasses = beanClasses;
		_configurationModels = configurationModels;
		_referenceModels = referenceModels;
		_serviceModels = serviceModels;

		BeansXml beansXml = BeansXml.EMPTY_BEANS_XML;

		if (!beanDescriptorURLs.isEmpty()) {
			BeansXmlParser beansXmlParser = new BeansXmlParser();
			beansXml = beansXmlParser.parse(beanDescriptorURLs);
		}

		_beansXml = beansXml;
	}

	public Collection<String> getBeanClassNames() {
		return _beanClasses;
	}

	public BeansXml getBeansXml() {
		return _beansXml;
	}

	public Collection<ConfigurationModel> getConfigurationModels() {
		return _configurationModels;
	}

	public Collection<ReferenceModel> getReferenceModels() {
		return _referenceModels;
	}

	public Collection<ServiceModel> getServiceModels() {
		return _serviceModels;
	}

	private final Collection<String> _beanClasses;
	private final BeansXml _beansXml;
	private final Collection<ConfigurationModel> _configurationModels;
	private final Collection<ReferenceModel> _referenceModels;
	private final Collection<ServiceModel> _serviceModels;

}