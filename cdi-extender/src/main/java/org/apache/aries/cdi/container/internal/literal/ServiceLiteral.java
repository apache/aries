package org.apache.aries.cdi.container.internal.literal;

import javax.enterprise.util.AnnotationLiteral;

import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.ServiceProperty;

public class ServiceLiteral extends AnnotationLiteral<Service> implements Service {

	private static final long serialVersionUID = 1L;
	public static final Service INSTANCE = new ServiceLiteral(new Class<?>[0], new ServiceProperty[0]);

	public static ServiceLiteral from(Class<?>[] classes, ServiceProperty[] properties) {
		return new ServiceLiteral(classes, properties);
	}

	public ServiceLiteral(Class<?>[] classes, ServiceProperty[] properties) {
		_type = classes;
		_properties = properties;
	}

	@Override
	public Class<?>[] type() {
		return _type;
	}

	@Override
	public ServiceProperty[] properties() {
		return _properties;
	}

	private final ServiceProperty[] _properties;
	private final Class<?>[] _type;

}
