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

import static org.apache.aries.cdi.container.internal.util.Reflection.cast;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Named;

import org.apache.aries.cdi.container.internal.container.ConfigurationDependency;
import org.apache.aries.cdi.container.internal.literal.AnyLiteral;
import org.apache.aries.cdi.container.internal.literal.DefaultLiteral;
import org.apache.aries.cdi.container.internal.util.Conversions;
import org.apache.aries.cdi.container.internal.util.Sets;
import org.jboss.weld.injection.CurrentInjectionPoint;
import org.jboss.weld.injection.EmptyInjectionPoint;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.Decorators;

public class ConfigurationBean implements Bean<Object> {

	public ConfigurationBean(
		ConfigurationDependency configurationDependency, BeanManagerImpl beanManagerImpl, Type injectionPointType,
		Set<Annotation> qualifiers) {

		_configurationDependency = configurationDependency;
		_beanManagerImpl = beanManagerImpl;
		_typesForMatchingBeansToInjectionPoints = Sets.immutableHashSet(injectionPointType, Object.class);
		_currentInjectionPoint = _beanManagerImpl.getServices().get(CurrentInjectionPoint.class);
		_qualifiers = Sets.hashSet(qualifiers, DefaultLiteral.INSTANCE, AnyLiteral.INSTANCE);

		for (Annotation qualifier : _qualifiers) {
			if (qualifier.annotationType().equals(Named.class)) {
				_name = ((Named)qualifier).value();
			}
		}
	}

	@Override
	public Object create(CreationalContext<Object> creationalContext) {
		return create0(creationalContext);
	}

	@Override
	public void destroy(Object arg0, CreationalContext<Object> arg1) {
	}

	@Override
	public Class<?> getBeanClass() {
		return _configurationDependency.getBeanClass();
	}

	@Override
	public Set<InjectionPoint> getInjectionPoints() {
		return Collections.emptySet();
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public Set<Annotation> getQualifiers() {
		return _qualifiers;
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
		return _typesForMatchingBeansToInjectionPoints;
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
		return "ConfigurationBean[" + _currentInjectionPoint + "]";
	}

	protected <T> T create0(CreationalContext<T> creationalContext) {
		Map<String, Object> map = new AbstractMap<String, Object>() {

			@Override
			public Set<java.util.Map.Entry<String, Object>> entrySet() {
				return _configurationDependency.getConfiguration().entrySet();
			}

		};

		T instance = cast(Conversions.c().convert(map).to(_configurationDependency.getBeanClass()));
		InjectionPoint ip = getInjectionPoint(_currentInjectionPoint);
		if (ip == null) {
			return instance;
		}
		List<Decorator<?>> decorators = getDecorators(ip);
		if (decorators.isEmpty()) {
			return instance;
		}
		return Decorators.getOuterDelegate(
			cast(this), instance, creationalContext, cast(getBeanClass()), ip, _beanManagerImpl, decorators);
	}

	protected List<Decorator<?>> getDecorators(InjectionPoint ip) {
		return _beanManagerImpl.resolveDecorators(Collections.singleton(ip.getType()), getQualifiers());
	}

	protected InjectionPoint getInjectionPoint(CurrentInjectionPoint currentInjectionPoint) {
		InjectionPoint ip = currentInjectionPoint.peek();
		return EmptyInjectionPoint.INSTANCE.equals(ip) ? null : ip;
	}

	private final BeanManagerImpl _beanManagerImpl;
	private final ConfigurationDependency _configurationDependency;
	private final CurrentInjectionPoint _currentInjectionPoint;
	private String _name;
	private final Set<Annotation> _qualifiers;
	private final Set<Type> _typesForMatchingBeansToInjectionPoints;

}
