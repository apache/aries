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

import static org.apache.aries.cdi.container.internal.model.Constants.BEAN_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.CDI10_URI;
import static org.apache.aries.cdi.container.internal.model.Constants.CDI_URIS;
import static org.apache.aries.cdi.container.internal.model.Constants.CLASS_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.NAME_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.QUALIFIER_ELEMENT;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.cdi.container.internal.component.OSGiBean;
import org.apache.aries.cdi.container.internal.exception.BeanElementException;
import org.apache.aries.cdi.container.internal.exception.BlacklistQualifierException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OSGiBeansHandler extends DefaultHandler {

	public OSGiBeansHandler(List<URL> beanDescriptorURLs, ClassLoader classLoader) {
		_beanDescriptorURLs = beanDescriptorURLs;
		_classLoader = classLoader;
	}

	public BeansModel createBeansModel() {
		return new BeansModel(_beans, _qualifierBlackList, _errors, _beanDescriptorURLs);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (matches(BEAN_ELEMENT, uri, localName)) {
			String className = Model.getValue(CDI10_URI, CLASS_ATTRIBUTE, attributes);

			try {
				Class<?> clazz = _classLoader.loadClass(className);

				_beanModel = new OSGiBean.Builder(clazz);
			}
			catch (ReflectiveOperationException roe) {
				_errors.add(
					new BeanElementException(
						"Error loading bean class from element: " + className,
						roe));
			}
		}
		if (matches(QUALIFIER_ELEMENT, uri, localName)) {
			String className = Model.getValue(CDI10_URI, NAME_ATTRIBUTE, attributes);

			try {
				_qualifier = _classLoader.loadClass(className);
			}
			catch (ReflectiveOperationException roe) {
				_errors.add(
					new BlacklistQualifierException(
						"Error loading blacklisted qualifier from element: " + className,
						roe));
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (matches(BEAN_ELEMENT, uri, localName)) {
			OSGiBean osgiBean = _beanModel.build();
			_beans.put(osgiBean.getBeanClass().getName(), osgiBean);
			_beanModel = null;
		}
		if (matches(QUALIFIER_ELEMENT, uri, localName) && (_qualifier != null)) {
			_qualifierBlackList.add(_qualifier);
			_qualifier = null;
		}
	}

	private boolean matches(String elementName, String uri, String localName) {
		if (localName.equals(elementName) && ("".equals(uri) || CDI_URIS.contains(uri))) {
			return true;
		}
		return false;
	}

	private final static Logger _log = LoggerFactory.getLogger(
		OSGiBeansHandler.class);

	private final ClassLoader _classLoader;
	private final Map<String, OSGiBean> _beans = new HashMap<>();
	private final List<URL> _beanDescriptorURLs;
	private OSGiBean.Builder _beanModel;
	private List<Throwable> _errors = new ArrayList<>();
	private Class<?> _qualifier;
	private final List<Class<?>> _qualifierBlackList = new ArrayList<>();

}
