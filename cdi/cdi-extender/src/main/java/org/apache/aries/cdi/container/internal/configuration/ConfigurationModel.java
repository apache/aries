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

package org.apache.aries.cdi.container.internal.configuration;

import static org.apache.aries.cdi.container.internal.model.Model.*;
import static org.apache.aries.cdi.container.internal.model.Constants.CDI10_URI;
import static org.apache.aries.cdi.container.internal.model.Constants.CONFIGURATION_PID_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.CONFIGURATION_POLICY_ATTRIBUTE;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.inject.spi.InjectionPoint;

import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.service.cdi.annotations.ConfigurationPolicy;
import org.xml.sax.Attributes;

public class ConfigurationModel {

	public static class Builder {

		public Builder(Type type) {
			Objects.requireNonNull(type);
			_type = type;
		}

		public Builder attributes(Attributes attributes) {
			_policy = ConfigurationPolicy.get(getValue(
					CDI10_URI, CONFIGURATION_POLICY_ATTRIBUTE, attributes, ConfigurationPolicy.DEFAULT.toString()));
			_pid = getValues(CDI10_URI, CONFIGURATION_PID_ATTRIBUTE, attributes, new String[] {Configuration.NAME});
			return this;
		}

		public ConfigurationModel build() {
			_pid = ((_pid == null) || (_pid.length == 0))? new String[] {Configuration.NAME}: _pid;

			if (_policy == null) {
				_policy = ConfigurationPolicy.OPTIONAL;
			}

			return new ConfigurationModel(_type, _pid, _policy, _qualifiers);
		}

		public Builder injectionPoint(InjectionPoint injectionPoint) {
			_qualifiers = injectionPoint.getQualifiers();
			Configuration configuration = injectionPoint.getAnnotated().getAnnotation(Configuration.class);
			if (configuration != null) {
				_policy = configuration.configurationPolicy();
				_pid = configuration.value();
			}
			return this;
		}

		public Builder pid(String[] pid) {
			_pid = pid;
			return this;
		}

		public Builder policy(ConfigurationPolicy policy) {
			_policy = policy;
			return this;
		}

		public Builder qualifiers(Set<Annotation> qualifiers) {
			_qualifiers = qualifiers;
			return this;
		}

		private String[] _pid;
		private ConfigurationPolicy _policy;
		private Set<Annotation> _qualifiers;
		private Type _type;

	}

	private ConfigurationModel(Type type, String[] pids, ConfigurationPolicy policy, Set<Annotation> qualifiers) {
		_type = type;
		_pid = pids;
		_policy = policy;
		_qualifiers = new LinkedHashSet<>();
		if (qualifiers != null) {
			_qualifiers.addAll(qualifiers);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_policy == null) ? 0 : _policy.hashCode());
		result = prime * result + ((_qualifiers == null) ? 0 : _qualifiers.hashCode());
		result = prime * result + ((_type == null) ? 0 : _type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConfigurationModel other = (ConfigurationModel) obj;
		if (_policy != other._policy)
			return false;
		if (_qualifiers == null) {
			if (other._qualifiers != null)
				return false;
		} else if (!_qualifiers.equals(other._qualifiers))
			return false;
		if (_type == null) {
			if (other._type != null)
				return false;
		} else if (!_type.equals(other._type))
			return false;
		return true;
	}

	public ConfigurationPolicy getConfigurationPolicy() {
		return _policy;
	}

	public boolean found() {
		return _found.get();
	}

	public void found(boolean found) {
		_found.set(found);
	}

	public String[] getPid() {
		return _pid;
	}

	public Set<Annotation> getQualifiers() {
		return _qualifiers;
	}

	public Type getType() {
		return _type;
	}

	public void setQualifiers(Set<Annotation> qualifiers) {
		_qualifiers.clear();
		_qualifiers.addAll(qualifiers);
	}

	@Override
	public String toString() {
		if (_string == null) {
			_string = String.format("configuration[type='%s', policy='%s', pid='%s']", _type, _policy, Arrays.toString(_pid));
		}
		return _string;
	}

	private final AtomicBoolean _found = new AtomicBoolean();
	private final String[] _pid;
	private final ConfigurationPolicy _policy;
	private final Set<Annotation> _qualifiers;
	private volatile String _string;
	private final Type _type;

}
