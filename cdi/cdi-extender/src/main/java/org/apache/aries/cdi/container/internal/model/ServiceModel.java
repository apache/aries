package org.apache.aries.cdi.container.internal.model;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.cdi.annotations.ServiceProperty;

public class ServiceModel {

	public ServiceModel(String beanClass) {
		_beanClass = beanClass;
	}

	public void addProvide(String className) {
		_provides.add(className);
	}

	public void addProperty(ServiceProperty serviceProperty) {
		_properties.add(serviceProperty);
	}

	public String getBeanClass() {
		return _beanClass;
	}

	public ServiceProperty[] getProperties() {
		return _properties.toArray(new ServiceProperty[0]);
	}

	public List<String> getProvides() {
		return _provides;
	}

	private final String _beanClass;
	private List<ServiceProperty> _properties = new ArrayList<>();
	private List<String> _provides = new ArrayList<>();
}
