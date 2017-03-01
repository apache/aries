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

package org.apache.aries.cdi.container.internal.container;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Singleton;

import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.ServiceProperty;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.StandardConverter;
import org.osgi.util.converter.TypeReference;

public class ServiceDeclaration {

	@SuppressWarnings({ "rawtypes"})
	public ServiceDeclaration(Service service, Bean bean, CreationalContext creationalContext) {
		_service = service;
		_bean = bean;
		_creationalContext = creationalContext;

		Dictionary<String, Object> properties = new Hashtable<>();

		for (Object object : bean.getQualifiers()) {
			Annotation annotation = (Annotation)object;
			Map<String, Object> map = _converter.convert(annotation).sourceAs(annotation.annotationType()).to(_mapType);

			for (Map.Entry<String, Object> entry : map.entrySet()) {
				properties.put(entry.getKey(), entry.getValue());
			}
		}

		for (ServiceProperty serviceProperty : _service.properties()) {
			Object object = getValue(serviceProperty);

			properties.put(serviceProperty.key(), object);
		}

		_properties = properties;
	}

	@SuppressWarnings("rawtypes")
	public Bean getBean() {
		return _bean;
	}

	public String[] getClassNames() {
		List<String> classNames = new ArrayList<>();

		Class<?>[] types = _service.type();

		if (types.length > 0) {
			for (Type type : types) {
				classNames.add(type.getTypeName());
			}
		}
		else {
			types = _bean.getBeanClass().getInterfaces();

			if (types.length > 0) {
				for (Type type : types) {
					classNames.add(type.getTypeName());
				}
			}
			else {
				classNames.add(_bean.getBeanClass().getName());
			}
		}

		return classNames.toArray(new String[0]);
	}

	public Dictionary<String, Object> getProperties() {
		return _properties;
	}

	@SuppressWarnings({ "unchecked" })
	public Object getServiceInstance() {
		Class<?> scope = _bean.getScope();
		if (Singleton.class.isAssignableFrom(scope)) {
			return _bean.create(_creationalContext);
		}
		else if (ApplicationScoped.class.isAssignableFrom(scope)) {
			return new BundleScopeWrapper();
		}

		return new PrototypeScopeWrapper();
	}

	Object getValue(ServiceProperty serviceProperty) {
		Type type = serviceProperty.type().getType();
		String[] value = serviceProperty.value();
		Object object = _converter.convert(value).to(type);
		return object;
	}

	private static final Converter _converter = new StandardConverter();

	private static final TypeReference<Map<String, Object>> _mapType = new TypeReference<Map<String, Object>>(){};

	@SuppressWarnings("rawtypes")
	private final Bean _bean;
	@SuppressWarnings("rawtypes")
	private final CreationalContext _creationalContext;
	private final Dictionary<String, Object> _properties;
	private final Service _service;

	@SuppressWarnings({"rawtypes", "unchecked"})
	private class BundleScopeWrapper implements ServiceFactory {

		@Override
		public Object getService(Bundle bundle, ServiceRegistration registration) {
			return _bean.create(_creationalContext);
		}

		@Override
		public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
			_bean.destroy(service, _creationalContext);
		}

	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private class PrototypeScopeWrapper implements PrototypeServiceFactory {

		@Override
		public Object getService(Bundle bundle, ServiceRegistration registration) {
			return _bean.create(_creationalContext);
		}

		@Override
		public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
			_bean.destroy(service, _creationalContext);
		}

	}

}
