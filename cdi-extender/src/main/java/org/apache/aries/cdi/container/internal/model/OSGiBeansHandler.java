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

import static org.apache.aries.cdi.container.internal.model.Constants.ARRAY_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.CLASS_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.CDI10_URI;
import static org.apache.aries.cdi.container.internal.model.Constants.CDI_URIS;
import static org.apache.aries.cdi.container.internal.model.Constants.COMPONENT_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.CONFIGURATION_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.INTERFACE_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.LIST_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.NAME_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.PROPERTY_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.PROVIDE_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.REFERENCE_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.SERVICE_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.SET_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.TYPE_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.VALUE_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.VALUE_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.VALUE_TYPE_ATTRIBUTE;

import java.net.URL;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.cdi.container.internal.component.ComponentModel;
import org.apache.aries.cdi.container.internal.configuration.ConfigurationModel;
import org.apache.aries.cdi.container.internal.reference.ReferenceModel;
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
		return new BeansModel(_components, _beanDescriptorURLs);
	}

	@Override
	public void characters(char[] c, int start, int length) {
		if (_propertySB == null) {
			return;
		}

		_propertySB.append(c, start, length);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (matches(ARRAY_ELEMENT, uri, localName) && (_propertyValue == null)) {
			_collectionType = CollectionType.ARRAY;
		}
		if (matches(COMPONENT_ELEMENT, uri, localName)) {
			try {
				Class<?> clazz = _classLoader.loadClass(
					Model.getValue(CDI10_URI, CLASS_ATTRIBUTE, attributes));

				_componentModel = new ComponentModel.Builder(clazz).attributes(attributes);
			}
			catch (ClassNotFoundException cnfe) {
				if (_log.isErrorEnabled()) {
					_log.error("CDIe - {}", cnfe.getMessage(), cnfe);
				}
			}
		}
		if (matches(CONFIGURATION_ELEMENT, uri, localName)) {
			try {
				Class<?> clazz = _classLoader.loadClass(
					Model.getValue(CDI10_URI, TYPE_ATTRIBUTE, attributes));

				_componentModel.configuration(new ConfigurationModel.Builder(clazz).attributes(attributes).build());
			}
			catch (ClassNotFoundException cnfe) {
				if (_log.isErrorEnabled()) {
					_log.error("CDIe - {}", cnfe.getMessage(), cnfe);
				}
			}
		}
		if (matches(LIST_ELEMENT, uri, localName) && (_propertyValue == null)) {
			_collectionType = CollectionType.LIST;
		}
		if (matches(PROPERTY_ELEMENT, uri, localName)) {
			_propertyName = attributes.getValue(NAME_ATTRIBUTE).trim();
			_propertyType = attributes.getValue(VALUE_TYPE_ATTRIBUTE);
			if (_propertyType == null) {
				_propertyType = "String";
			}
			String value = attributes.getValue(uri, VALUE_ATTRIBUTE);
			if (value != null) {
				_propertyValue = value.trim();
			}
			else {
				_propertySB = new StringBuilder();
			}
		}
		if (matches(PROVIDE_ELEMENT, uri, localName)) {
			String value = attributes.getValue(INTERFACE_ATTRIBUTE).trim();
			_componentModel.provide(value);
		}
		if (matches(REFERENCE_ELEMENT, uri, localName)) {
			try {
				Class<?> clazz = _classLoader.loadClass(
					Model.getValue(CDI10_URI, SERVICE_ATTRIBUTE, attributes));

				_referenceModel = new ReferenceModel.Builder(attributes).service(clazz).build();
			}
			catch (ClassNotFoundException cnfe) {
				if (_log.isErrorEnabled()) {
					_log.error("CDIe - {}", cnfe.getMessage(), cnfe);
				}
			}
		}
		if (matches(SET_ELEMENT, uri, localName) && (_propertyValue == null)) {
			_collectionType = CollectionType.SET;
		}
		if (matches(VALUE_ELEMENT, uri, localName) && (_collectionType != null)) {
			_propertySB = new StringBuilder();
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (matches(ARRAY_ELEMENT, uri, localName)) {
			_collectionType = null;
		}
		if (matches(COMPONENT_ELEMENT, uri, localName)) {
			ComponentModel componentModel = _componentModel.build();
			_components.put(componentModel.getBeanClass().getName(), componentModel);
			_componentModel = null;
		}
		if (matches(LIST_ELEMENT, uri, localName)) {
			_collectionType = null;
		}
		if (matches(PROPERTY_ELEMENT, uri, localName)) {
			if ((_propertyValue == null) && (_propertySB != null)) {
				_propertyValue = _propertySB.toString().trim();
			}
			if (_propertyValue != null) {
				try (Formatter f = new Formatter()) {
					f.format("%s:%s=%s", _propertyName, _propertyType, _propertyValue);
					_componentModel.property(f.toString());
				}
			}
			_propertySB = null;
			_propertyName = null;
			_propertyType = null;
			_propertyValue = null;
		}
		if (matches(REFERENCE_ELEMENT, uri, localName)) {
			_componentModel.reference(_referenceModel);
			_referenceModel = null;
		}
		if (matches(SET_ELEMENT, uri, localName)) {
			_collectionType = null;
		}
		if (matches(VALUE_ELEMENT, uri, localName) && (_collectionType != null)) {
			StringBuilder sb = new StringBuilder();
			sb.append(_propertyName);
			sb.append(":");
			if (_collectionType == CollectionType.LIST) {
				sb.append("List<");
				sb.append(_propertyType);
				sb.append(">");
			}
			else if (_collectionType == CollectionType.SET) {
				sb.append("Set<");
				sb.append(_propertyType);
				sb.append(">");
			}
			else {
				sb.append(_propertyType);
			}
			sb.append("=");
			sb.append(_propertySB.toString().trim());

			_componentModel.property(sb.toString());
			_propertySB = null;
		}
	}

	private boolean matches(String elementName, String uri, String localName) {
		if (localName.equals(elementName) && ("".equals(uri) || CDI_URIS.contains(uri))) {
			return true;
		}
		return false;
	}

	enum CollectionType {
		ARRAY, LIST, SET
	}

	private final static Logger _log = LoggerFactory.getLogger(
		OSGiBeansHandler.class);

	private final ClassLoader _classLoader;
	private final Map<String, ComponentModel> _components = new HashMap<>();
	private final List<URL> _beanDescriptorURLs;
	private String _propertyName;
	private StringBuilder _propertySB;
	private String _propertyType;
	private String _propertyValue;
	private CollectionType _collectionType;
	private ReferenceModel _referenceModel;
	private ComponentModel.Builder _componentModel;

}
