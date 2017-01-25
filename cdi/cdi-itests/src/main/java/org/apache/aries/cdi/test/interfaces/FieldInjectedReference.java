package org.apache.aries.cdi.test.interfaces;

import java.util.Map;

import org.osgi.framework.ServiceReference;

public interface FieldInjectedReference<T> {

	public Map<String, Object> getProperties();

	public ServiceReference<T> getGenericReference();

	@SuppressWarnings("rawtypes")
	public ServiceReference getRawReference();

	public T getService();

}