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

package org.apache.aries.cdi.container.internal.component;

import static org.apache.aries.cdi.container.internal.model.Model.*;
import static org.apache.aries.cdi.container.internal.model.Constants.CDI10_URI;
import static org.apache.aries.cdi.container.internal.model.Constants.NAME_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.SERVICE_SCOPE_ATTRIBUTE;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.cdi.container.internal.configuration.ConfigurationModel;
import org.apache.aries.cdi.container.internal.reference.ReferenceModel;
import org.osgi.service.cdi.annotations.ServiceScope;
import org.xml.sax.Attributes;

public class ComponentModel {

	public static class Builder {

		public Builder(Class<?> beanClass) {
			Objects.requireNonNull(beanClass);
			_beanClass = beanClass;
		}

		public Builder attributes(Attributes attributes) {
			_name = getValue(CDI10_URI, NAME_ATTRIBUTE, attributes, _beanClass.getName());
			_scope = ServiceScope.get(getValue(
					CDI10_URI, SERVICE_SCOPE_ATTRIBUTE, attributes, ServiceScope.DEFAULT.toString()));
			return this;
		}

		public ComponentModel build() {
			if ((_name == null) || (_name.length() == 0)) {
				_name = _beanClass.getName();
			}

			if (_scope == null) {
				if (!_provides.isEmpty()) {
					_scope = ServiceScope.PROTOTYPE;
				}
				else {
					_scope = ServiceScope.DEFAULT;
				}
			}

			return new ComponentModel(_beanClass, _name, _provides, _scope, _properties, _configurations, _references);
		}

		public Builder configuration(ConfigurationModel configurationModel) {
			_configurations.add(configurationModel);
			return this;
		}

		public Builder name(String name) {
			_name = name;
			return this;
		}

		public Builder provide(String className) {
			_provides.add(className);
			return this;
		}

		public Builder property(String property) {
			_properties.add(property);
			return this;
		}

		public Builder reference(ReferenceModel referenceModel) {
			_references.add(referenceModel);
			return this;
		}

		public Builder scope(ServiceScope scope) {
			_scope = scope;
			return this;
		}

		private Class<?> _beanClass;
		private final List<ConfigurationModel> _configurations = new CopyOnWriteArrayList<>();
		private String _name;
		private final List<String> _properties = new CopyOnWriteArrayList<>();
		private final List<String> _provides = new CopyOnWriteArrayList<>();
		private final List<ReferenceModel> _references = new CopyOnWriteArrayList<>();
		private ServiceScope _scope;

	}

	private ComponentModel(
		Class<?> beanClass,
		String name,
		List<String> provides,
		ServiceScope scope,
		List<String> properties,
		List<ConfigurationModel> configurations,
		List<ReferenceModel> references) {

		_beanClass = beanClass;
		_name = name;
		_properties = properties;
		_provides = provides;
		_scope = scope;
		_configurations = configurations;
		_references = references;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ComponentModel other = (ComponentModel) obj;
		if (_beanClass.getName() == null) {
			if (other._beanClass.getName() != null)
				return false;
		} else if (!_beanClass.getName().equals(other._beanClass.getName()))
			return false;
		return true;
	}

	public boolean found() {
		return _found.get();
	}

	public void found(boolean found) {
		_found.set(found);
	}

	public Class<?> getBeanClass() {
		return _beanClass;
	}

	public List<ConfigurationModel> getConfigurations() {
		return _configurations;
	}

	public String getName() {
		return _name;
	}

	public String[] getProperties() {
		return _properties.toArray(new String[0]);
	}

	public List<String> getProvides() {
		return _provides;
	}

	public List<ReferenceModel> getReferences() {
		return _references;
	}

	public ServiceScope getServiceScope() {
		return _scope;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_beanClass.getName() == null) ? 0 : _beanClass.getName().hashCode());
		return result;
	}

	@Override
	public String toString() {
		if (_string == null) {
			_string = String.format("ComponentModel[name='%s', beanClass='%s', scope='%s', provides='%s']", _name, _beanClass.getName(), _scope, _provides);
		}
		return _string;
	}

	private final Class<?> _beanClass;
	private final AtomicBoolean _found = new AtomicBoolean();
	private String _name;
	private final List<String> _properties;
	private final List<String> _provides;
	private final List<ConfigurationModel> _configurations;
	private final List<ReferenceModel> _references;
	private ServiceScope _scope;
	private volatile String _string;

}
