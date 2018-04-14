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
import java.util.List;
import java.util.Map;

import org.apache.aries.cdi.container.internal.component.OSGiBean;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.xml.BeansXmlParser;

public class BeansModel {

	public BeansModel(
		Map<String, OSGiBean> beans,
		List<Class<?>> qualifierBlackList,
		List<Throwable> errors,
		Collection<URL> beanDescriptorURLs) {

		_beans = beans;
		_qualifierBlackList = qualifierBlackList;
		_errors = errors;

		BeansXml beansXml = BeansXml.EMPTY_BEANS_XML;

		if (!beanDescriptorURLs.isEmpty()) {
			BeansXmlParser beansXmlParser = new BeansXmlParser();
			beansXml = beansXmlParser.parse(beanDescriptorURLs);
		}

		_beansXml = beansXml;
	}

	public Collection<String> getBeanClassNames() {
		return _beans.keySet();
	}

	public BeansXml getBeansXml() {
		return _beansXml;
	}

	public List<Class<?>> getQualifierBlackList() {
		return _qualifierBlackList;
	}

	public List<Throwable> getErrors() {
		return _errors;
	}

	public OSGiBean getOSGiBean(String beanClass) {
		return _beans.get(beanClass);
	}

	public Collection<OSGiBean> getOSGiBeans() {
		return _beans.values();
	}

	public void putOSGiBean(String beanClass, OSGiBean osgiBean) {
		_beans.put(beanClass, osgiBean);
	}

	private final Map<String, OSGiBean> _beans;
	private final BeansXml _beansXml;
	private final List<Throwable> _errors;
	private final List<Class<?>> _qualifierBlackList;

}