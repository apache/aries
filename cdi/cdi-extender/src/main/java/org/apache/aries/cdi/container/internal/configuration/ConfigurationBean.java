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

import static org.apache.aries.cdi.container.internal.util.Reflection.cast;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.aries.cdi.container.internal.component.ComponentModel;
import org.apache.aries.cdi.container.internal.component.ComponentProperties;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.util.Conversions;
import org.jboss.weld.injection.CurrentInjectionPoint;
import org.jboss.weld.injection.EmptyInjectionPoint;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.Decorators;

public class ConfigurationBean implements Bean<Object> {

	public ConfigurationBean(
		ContainerState containerState,
		ConfigurationModel configurationModel,
		ComponentModel componentModel,
		InjectionPoint injectionPoint,
		BeanManagerImpl beanManagerImpl) {

		_containerState = containerState;
		_configurationModel = configurationModel;
		_componentModel = componentModel;
		_injectionPoint = injectionPoint;
		_beanManagerImpl = beanManagerImpl;

		Type type = _injectionPoint.getType();

		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)type;

			type = pt.getRawType();
		}

		_beanClass = cast(type);
	}

	@Override
	public Object create(CreationalContext<Object> creationalContext) {
		Object instance = _getInjectedInstance();
		InjectionPoint ip = _getInjectionPoint();
		List<Decorator<?>> decorators = _getDecorators(ip);
		if (decorators.isEmpty()) {
			return instance;
		}
		return Decorators.getOuterDelegate(
			this, instance, creationalContext, cast(_beanClass), ip, _beanManagerImpl, decorators);
	}

	@Override
	public void destroy(Object instance, CreationalContext<Object> creationalContext) {
	}

	@Override
	public Class<?> getBeanClass() {
		return _beanClass;
	}

	@Override
	public Set<InjectionPoint> getInjectionPoints() {
		return Collections.emptySet();
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Set<Annotation> getQualifiers() {
		return _injectionPoint.getQualifiers();
	}

	@Override
	public Class<? extends Annotation> getScope() {
		return Dependent.class;
	}

	@Override
	public Set<Class<? extends Annotation>> getStereotypes() {
		return Collections.emptySet();
	}

	@Override
	public Set<Type> getTypes() {
		return Collections.singleton(_injectionPoint.getType());
	}

	@Override
	public boolean isAlternative() {
		return false;
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public String toString() {
		if (_string == null) {
			_string = "ConfigurationBean[" + _configurationModel + "]";
		}

		return _string;
	}

	private List<Decorator<?>> _getDecorators(InjectionPoint ip) {
		return _beanManagerImpl.resolveDecorators(Collections.singleton(ip.getType()), getQualifiers());
	}

	private Object _getInjectedInstance() {
		Dictionary<String,?> dictionary = new ComponentProperties().bean(
			_injectionPoint.getBean()
		).componentModel(
			_componentModel
		).containerState(
			_containerState
		).pid(
			_configurationModel.getPid()
		).build();

		return Conversions.convert(dictionary).to(_injectionPoint.getType());
	}

	private InjectionPoint _getInjectionPoint() {
		CurrentInjectionPoint currentInjectionPoint = _beanManagerImpl.getServices().get(CurrentInjectionPoint.class);
		InjectionPoint ip = currentInjectionPoint.peek();
		return EmptyInjectionPoint.INSTANCE.equals(ip) ? null : ip;
	}

	private final Class<?> _beanClass;
	private final BeanManagerImpl _beanManagerImpl;
	private final ComponentModel _componentModel;
	private final ConfigurationModel _configurationModel;
	private final ContainerState _containerState;
	private final InjectionPoint _injectionPoint;
	private String _string;

}
