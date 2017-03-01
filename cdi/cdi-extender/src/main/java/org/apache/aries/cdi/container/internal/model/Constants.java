package org.apache.aries.cdi.container.internal.model;

import java.util.Set;

import org.apache.aries.cdi.container.internal.util.Sets;

public class Constants {

	private Constants() {
		// no instances
	}

	public static final String CDI10_URI = "http://www.osgi.org/xmlns/cdi/v1.0.0";
	public static final Set<String> CDI_URIS = Sets.immutableHashSet(CDI10_URI);

	public static final String BEAN_CLASS_ATTRIBUTE = "beanClass";
	public static final String BEAN_ELEMENT = "bean";
	public static final String BEANS_ELEMENT = "beans";
	public static final String CLASS_ATTRIBUTE = "class";
	public static final String CONFIGURATION_ELEMENT = "configuration";
	public static final String INTERFACE_ATTRIBUTE = "interface";
	public static final String NAME_ATTRIBUTE = "name";
	public static final String PID_ATTRIBUTE = "pid";
	public static final String PROPERTY_ELEMENT = "property";
	public static final String PROVIDE_ELEMENT = "provide";
	public static final String REFERENCE_ELEMENT = "reference";
	public static final String SERVICE_ELEMENT = "service";
	public static final String TARGET_ATTRIBUTE = "target";
	public static final String TYPE_ATTRIBUTE = "type";
	public static final String VALUE_ATTRIBUTE = "value";

}
