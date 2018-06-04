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

package org.apache.aries.cdi.container.internal.model;

import static org.apache.aries.cdi.container.internal.util.Reflection.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.inject.spi.InjectionPoint;

import org.osgi.service.cdi.ConfigurationPolicy;
import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.annotations.PID;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;

public class ComponentPropertiesModel {

	public static class Builder {

		public Builder(Type injectionPointType) {
			_injectionPointType = injectionPointType;
		}

		public ComponentPropertiesModel build() {
			Objects.requireNonNull(_delcaringClass);
			Objects.requireNonNull(_injectionPointType);
			return new ComponentPropertiesModel(_injectionPointType, _delcaringClass, _pid, _qualifiers);
		}

		public Builder declaringClass(Class<?> delcaringClass) {
			_delcaringClass = delcaringClass;
			return this;
		}

		public Builder injectionPoint(InjectionPoint injectionPoint) {
			_qualifiers = injectionPoint.getQualifiers();
			_pid = injectionPoint.getAnnotated().getAnnotation(PID.class);
			return this;
		}

		private Class<?> _delcaringClass;
		private PID _pid;
		private Set<Annotation> _qualifiers;
		private Type _injectionPointType;

	}

	private ComponentPropertiesModel(
		Type injectionPointType,
		Class<?> delcaringClass,
		PID pid,
		Set<Annotation> qualifiers) {

		_injectionPointType = injectionPointType;
		_pid = pid;
		_qualifiers = new LinkedHashSet<>();
		_declaringClass = delcaringClass;

		if (qualifiers != null) {
			_qualifiers.addAll(qualifiers);
		}

		Type rawType = injectionPointType;

		if (_injectionPointType instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)_injectionPointType;

			rawType = pt.getRawType();
		}

		_beanClass = cast(rawType);
	}

	public PID getPid() {
		return _pid;
	}

	public Set<Annotation> getQualifiers() {
		return _qualifiers;
	}

	public Type getType() {
		return _injectionPointType;
	}

	public ConfigurationTemplateDTO toDTO() {
		ExtendedConfigurationTemplateDTO dto = new ExtendedConfigurationTemplateDTO();

		dto.beanClass = _beanClass;
		dto.declaringClass = _declaringClass;
		dto.injectionPointType = _injectionPointType;
		dto.maximumCardinality = MaximumCardinality.ONE;

		if (_pid != null) {
			dto.pid = _pid.value();
		}

		dto.policy = (_pid != null) ? _pid.policy() : ConfigurationPolicy.OPTIONAL;

		return dto;
	}

	@Override
	public String toString() {
		if (_string == null) {
			_string = String.format("configuration[type='%s', pid='%s', policy='%s']", _injectionPointType, _pid.value(), _pid.policy());
		}
		return _string;
	}

	private final Class<?> _beanClass;
	private final Class<?> _declaringClass;
	private final PID _pid;
	private final Set<Annotation> _qualifiers;
	private volatile String _string;
	private final Type _injectionPointType;
}
