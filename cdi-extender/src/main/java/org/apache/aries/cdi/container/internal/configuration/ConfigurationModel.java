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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.inject.spi.InjectionPoint;

import org.osgi.service.cdi.annotations.PID;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationPolicy;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.MaximumCardinality;

public class ConfigurationModel {

	public static class Builder {

		public Builder(Type type) {
			Objects.requireNonNull(type);
			_type = type;
		}

		public ConfigurationModel build() {
			return new ConfigurationModel(_type, _pid, _qualifiers);
		}

		public Builder injectionPoint(InjectionPoint injectionPoint) {
			_qualifiers = injectionPoint.getQualifiers();
			_pid = injectionPoint.getAnnotated().getAnnotation(PID.class);
			return this;
		}

		private PID _pid;
		private Set<Annotation> _qualifiers;
		private Type _type;

	}

	private ConfigurationModel(Type type, PID pid, Set<Annotation> qualifiers) {
		_type = type;
		_pid = pid;
		_qualifiers = new LinkedHashSet<>();
		if (qualifiers != null) {
			_qualifiers.addAll(qualifiers);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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

	public PID getPid() {
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

	public ConfigurationTemplateDTO toDTO() {
		if (_pid != null) {
			ConfigurationTemplateDTO dto = new ConfigurationTemplateDTO();

			dto.componentConfiguration = false;
			dto.maximumCardinality = MaximumCardinality.ONE;
			dto.pid = _pid.value();
			dto.policy = (_pid.policy().toString().equals(ConfigurationPolicy.REQUIRED.toString()))
				? ConfigurationPolicy.REQUIRED : ConfigurationPolicy.OPTIONAL;
		}

		return null;
	}

	@Override
	public String toString() {
		if (_string == null) {
			_string = String.format("configuration[type='%s', pid='%s', policy='%s']", _type, _pid.value(), _pid.policy());
		}
		return _string;
	}

	private final PID _pid;
	private final Set<Annotation> _qualifiers;
	private volatile String _string;
	private final Type _type;
}
