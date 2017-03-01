package org.apache.aries.cdi.container.internal.literal;

import javax.enterprise.util.AnnotationLiteral;

import org.osgi.service.cdi.annotations.PropertyType;
import org.osgi.service.cdi.annotations.ServiceProperty;

public class ServicePropertyLiteral extends AnnotationLiteral<ServiceProperty> implements ServiceProperty {

	private static final long serialVersionUID = 1L;

	public static ServicePropertyLiteral from(String key, String[] value, PropertyType type) {
		return new ServicePropertyLiteral(key, value, type);
	}

	public ServicePropertyLiteral(String key, String[] value, PropertyType type) {
		_key = key;
		_value = value;
		_type = type;
	}

	@Override
	public String key() {
		return _key;
	}

	@Override
	public String[] value() {
		return _value;
	}

	@Override
	public PropertyType type() {
		return _type;
	}

	private final String _key;
	private final PropertyType _type;
	private final String[] _value;

}
