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

package org.apache.aries.cdi.container.internal.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.aries.cdi.container.internal.container.Mark;
import org.apache.aries.cdi.container.internal.model.ExtendedConfigurationTemplateDTO;
import org.apache.aries.cdi.container.internal.util.Conversions;
import org.apache.aries.cdi.container.internal.util.Sets;
import org.osgi.service.cdi.ComponentType;
import org.osgi.service.cdi.annotations.ComponentProperties;
import org.osgi.service.cdi.annotations.ComponentScoped;
import org.osgi.service.cdi.annotations.PID;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;

public class ComponentPropertiesBean implements Bean<Object> {

	public ComponentPropertiesBean(
		ComponentTemplateDTO component,
		ExtendedConfigurationTemplateDTO template) {

		_component = component;
		_template = template;

		_qualifiers = Sets.hashSet(ComponentProperties.Literal.INSTANCE, Default.Literal.INSTANCE);
		_types = Sets.hashSet(_template.injectionPointType, Object.class);

		if (_template.pid != null) {
			_qualifiers.add(PID.Literal.of(_template.pid, _template.policy));
		}
	}

	@Override
	public Object create(CreationalContext<Object> creationalContext) {
		Objects.requireNonNull(_properties);
		return Conversions.convert(_properties).to(_template.injectionPointType);
	}

	@Override
	public void destroy(Object instance, CreationalContext<Object> creationalContext) {
	}

	@Override
	public Set<Type> getTypes() {
		return _types;
	}

	@Override
	public Set<Annotation> getQualifiers() {
		return _qualifiers;
	}

	@Override
	public Class<? extends Annotation> getScope() {
		if (_component.type == ComponentType.CONTAINER) {
			return ApplicationScoped.class;
		}
		return ComponentScoped.class;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Set<Class<? extends Annotation>> getStereotypes() {
		return Collections.emptySet();
	}

	@Override
	public boolean isAlternative() {
		return false;
	}

	@Override
	public Class<?> getBeanClass() {
		return _template.beanClass;
	}

	@Override
	public Set<InjectionPoint> getInjectionPoints() {
		return Collections.emptySet();
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	public void setMark(Mark mark) {
		_qualifiers.add(mark);
	}

	public void setProperties(Map<String, Object> properties) {
		_properties = properties;
	}

	@Override
	public String toString() {
		if (_string == null) {
			_string = "ConfigurationBean[" + ((_template.pid == null)?_component.name:_template.pid) + "]";
		}
		return _string;
	}

	private final ComponentTemplateDTO _component;
	private final Set<Annotation> _qualifiers;
	private final ExtendedConfigurationTemplateDTO _template;
	private final Set<Type> _types;
	private volatile Map<String, Object> _properties;
	private volatile String _string;

}
