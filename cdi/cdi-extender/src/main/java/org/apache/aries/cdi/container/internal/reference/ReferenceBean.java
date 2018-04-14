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

package org.apache.aries.cdi.container.internal.reference;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Named;

import org.apache.aries.cdi.container.internal.component.OSGiBean;
import org.apache.aries.cdi.container.internal.container.ContainerState;

public class ReferenceBean implements Bean<Object> {

	public ReferenceBean(
		ContainerState containerState,
		ReferenceModel referenceModel,
		OSGiBean componentModel,
		InjectionPoint injectionPoint,
		BeanManager beanManager) {

		_containerState = containerState;
		_referenceModel = referenceModel;
		_componentModel = componentModel;
		_injectionPoint = injectionPoint;
		_beanManager = beanManager;

		Named named = _injectionPoint.getAnnotated().getAnnotation(Named.class);

		_name = named != null ? named.value() : null;
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
//			cast(this), instance, creationalContext, cast(getBeanClass()), _injectionPoint, _beanManager, decorators);
	}

	@Override
	public void destroy(Object instance, CreationalContext<Object> creationalContext) {
	}

	@Override
	public Class<?> getBeanClass() {
		return null; // TODO _referenceModel.getBeanClass();
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
		return null; // TODO _referenceModel.getTypes();
	}

	@Override
	public boolean isAlternative() {
		return false;
	}

	@Override
	public boolean isNullable() {
		return false;//_referenceModel.getCardinality() == ReferenceCardinality.OPTIONAL ? true : false;
	}

	@Override
	public String toString() {
		return "ReferenceBean[" + _referenceModel + "]";
	}

	private List<Decorator<?>> _getDecorators(InjectionPoint ip) {
		return _beanManager.resolveDecorators(
			Collections.singleton(ip.getType()),
			_injectionPoint.getQualifiers().toArray(new Annotation[0]));
	}

	private Object _getInjectedInstance() {
		Map<String, ReferenceCallback> map = _containerState.referenceCallbacks().get(_componentModel);

//		ReferenceCallback referenceCallback = map.get(_referenceModel.getName());

		return null; // TODO referenceCallback.tracked().values().iterator().next();
	}

	private final BeanManager _beanManager;
	private final OSGiBean _componentModel;
	private final ContainerState _containerState;
	private final InjectionPoint _injectionPoint;
	private final String _name;
	private final ReferenceModel _referenceModel;

}
