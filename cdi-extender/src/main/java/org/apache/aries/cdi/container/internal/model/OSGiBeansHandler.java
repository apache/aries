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
import static org.apache.aries.cdi.container.internal.model.Constants.BEAN_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.CDI_URIS;
import static org.apache.aries.cdi.container.internal.model.Constants.CLASS_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.CONFIGURATION_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.INTERFACE_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.LIST_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.NAME_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.PROPERTY_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.PROVIDE_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.REFERENCE_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.SERVICE_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.SET_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.VALUE_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.VALUE_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.VALUE_TYPE_ATTRIBUTE;

import java.net.URL;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OSGiBeansHandler extends DefaultHandler {

	public OSGiBeansHandler(List<URL> beanDescriptorURLs) {
		_beanDescriptorURLs = beanDescriptorURLs;
	}

	public BeansModel createBeansModel() {
		return new BeansModel(
			_beanClasses, _configurationModels, _referenceModels, _serviceModels, _beanDescriptorURLs);
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
		if (matches(BEAN_ELEMENT, uri, localName)) {
			_beanClass = attributes.getValue(CLASS_ATTRIBUTE).trim();
			if (!_beanClasses.contains(_beanClass)) {
				_beanClasses.add(_beanClass);
			}
		}
		if (matches(CONFIGURATION_ELEMENT, uri, localName)) {
			_configurationModel = new ConfigurationModel(attributes);
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
			_serviceModel.addProvide(value);		}
		if (matches(REFERENCE_ELEMENT, uri, localName)) {
			_referenceModel = new ReferenceModel(attributes);
		}
		if (matches(SERVICE_ELEMENT, uri, localName)) {
			_serviceModel = new ServiceModel(_beanClass);
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
		if (matches(CONFIGURATION_ELEMENT, uri, localName)) {
			_configurationModels.add(_configurationModel);
			_configurationModel = null;
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
					_serviceModel.addProperty(f.toString());
				}
			}
			_propertySB = null;
			_propertyName = null;
			_propertyType = null;
			_propertyValue = null;
		}
		if (matches(REFERENCE_ELEMENT, uri, localName)) {
			_referenceModels.add(_referenceModel);
			_referenceModel = null;
		}
		if (matches(SERVICE_ELEMENT, uri, localName)) {
			_serviceModels.add(_serviceModel);
			_serviceModel = null;
			_beanClass = null;
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

			_serviceModel.addProperty(sb.toString());
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

	private String _beanClass;
	private final List<String> _beanClasses = new ArrayList<String>();
	private final List<URL> _beanDescriptorURLs;
	private ConfigurationModel _configurationModel;
	private final List<ConfigurationModel> _configurationModels = new ArrayList<>();
	private String _propertyName;
	private StringBuilder _propertySB;
	private String _propertyType;
	private String _propertyValue;
	private CollectionType _collectionType;
	private ReferenceModel _referenceModel;
	private final List<ReferenceModel> _referenceModels = new ArrayList<>();
	private ServiceModel _serviceModel;
	private final List<ServiceModel> _serviceModels = new ArrayList<>();

}
