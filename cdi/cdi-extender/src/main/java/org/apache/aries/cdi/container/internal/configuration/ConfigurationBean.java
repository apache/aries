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
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

public class ConfigurationBean implements Bean<Object> {

	@Override
	public Object create(CreationalContext<Object> creationalContext) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void destroy(Object instance, CreationalContext<Object> creationalContext) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<Type> getTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Annotation> getQualifiers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<? extends Annotation> getScope() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Class<? extends Annotation>> getStereotypes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAlternative() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Class<?> getBeanClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<InjectionPoint> getInjectionPoints() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isNullable() {
		// TODO Auto-generated method stub
		return false;
	}

/*	public ConfigurationBean(
		ContainerState containerState,
		ConfigurationModel configurationModel,
		ComponentModel componentModel,
		InjectionPoint injectionPoint,
		BeanManager beanManager) {

		_containerState = containerState;
		_configurationModel = configurationModel;
		_componentModel = componentModel;
		_injectionPoint = injectionPoint;
		_beanManager = beanManager;

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
		List<Decorator<?>> decorators = _getDecorators(_injectionPoint);
		if (decorators.isEmpty()) {
			return instance;
		}
		return instance;
		// TODO
//		return Decorators.getOuterDelegate(
//			this, instance, creationalContext, cast(_beanClass), _injectionPoint, _beanManager, decorators);
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
		return _beanManager.resolveDecorators(
			Collections.singleton(ip.getType()),
			_injectionPoint.getQualifiers().toArray(new Annotation[0]));
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

	private final Class<?> _beanClass;
	private final BeanManager _beanManager;
	private final ComponentModel _componentModel;
	private final ConfigurationModel _configurationModel;
	private final ContainerState _containerState;
	private final InjectionPoint _injectionPoint;
	private String _string;
*/
}
