package org.apache.aries.cdi.container.internal.model;

import static org.apache.aries.cdi.container.internal.model.Constants.BEAN_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.CDI_URIS;
import static org.apache.aries.cdi.container.internal.model.Constants.CLASS_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.CONFIGURATION_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.INTERFACE_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.PROPERTY_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.PROVIDE_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.REFERENCE_ELEMENT;
import static org.apache.aries.cdi.container.internal.model.Constants.SERVICE_ELEMENT;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.aries.cdi.container.internal.literal.ServicePropertyLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
		if (_propertyModel == null) {
			return;
		}

		StringBuilder sb = new StringBuilder();

		sb.append(c, start, length);

		_propertyModel.appendValue(sb.toString());
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (matches(BEAN_ELEMENT, uri, localName)) {
			_beanClass = attributes.getValue(uri, CLASS_ATTRIBUTE).trim();
			_beanClasses.add(_beanClass);
		}
		if (matches(CONFIGURATION_ELEMENT, uri, localName)) {
			_configurationModel = new ConfigurationModel(attributes);
		}
		if (matches(PROPERTY_ELEMENT, uri, localName)) {
			_propertyModel = new PropertyModel(attributes);
		}
		if (matches(PROVIDE_ELEMENT, uri, localName)) {
			String value = attributes.getValue(uri, INTERFACE_ATTRIBUTE).trim();
			_serviceModel.addProvide(value);		}
		if (matches(REFERENCE_ELEMENT, uri, localName)) {
			_referenceModel = new ReferenceModel(attributes);
		}
		if (matches(SERVICE_ELEMENT, uri, localName)) {
			_serviceModel = new ServiceModel(_beanClass);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (matches(CONFIGURATION_ELEMENT, uri, localName)) {
			_configurationModels.add(_configurationModel);
			_configurationModel = null;
		}
		if (matches(PROPERTY_ELEMENT, uri, localName)) {
			ServicePropertyLiteral servicePropertyLiteral = ServicePropertyLiteral.from(
				_propertyModel.getName(), _propertyModel.getValue(), _propertyModel.getType());
			_serviceModel.addProperty(servicePropertyLiteral);
			_propertyModel = null;
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
	}

	private boolean matches(String elementName, String uri, String localName) {
		if (localName.equals(elementName) && ("".equals(uri) || CDI_URIS.contains(uri))) {
			return true;
		}
		return false;
	}

	private static final Logger _log = LoggerFactory.getLogger(OSGiBeansHandler.class);

	private String _beanClass;
	private final List<String> _beanClasses = new ArrayList<String>();
	private final List<URL> _beanDescriptorURLs;
	private ConfigurationModel _configurationModel;
	private final List<ConfigurationModel> _configurationModels = new ArrayList<>();
	private PropertyModel _propertyModel;
	private ReferenceModel _referenceModel;
	private final List<ReferenceModel> _referenceModels = new ArrayList<>();
	private ServiceModel _serviceModel;
	private final List<ServiceModel> _serviceModels = new ArrayList<>();

}
