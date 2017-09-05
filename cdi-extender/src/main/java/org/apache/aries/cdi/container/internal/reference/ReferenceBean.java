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

import static org.apache.aries.cdi.container.internal.util.Reflection.cast;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Named;

import org.apache.aries.cdi.container.internal.component.ComponentModel;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.jboss.weld.injection.CurrentInjectionPoint;
import org.jboss.weld.injection.EmptyInjectionPoint;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.Decorators;
import org.osgi.service.cdi.annotations.ReferenceCardinality;

public class ReferenceBean implements Bean<Object> {

	public ReferenceBean(
		ContainerState containerState,
		ReferenceModel referenceModel,
		ComponentModel componentModel,
		Annotated annotated,
		Set<Annotation> qualifiers,
		BeanManagerImpl beanManagerImpl) {

		_containerState = containerState;
		_referenceModel = referenceModel;
		_componentModel = componentModel;
		_qualifiers = qualifiers;
		_beanManagerImpl = beanManagerImpl;

		Named named = annotated.getAnnotation(Named.class);

		_name = named != null ? named.value() : null;
	}

	@Override
	public Object create(CreationalContext<Object> creationalContext) {
		Object instance = _getInjectedInstance();
		InjectionPoint ip = _getInjectionPoint();
		if (ip == null) {
			return instance;
		}
		List<Decorator<?>> decorators = _getDecorators(ip);
		if (decorators.isEmpty()) {
			return instance;
		}
		return Decorators.getOuterDelegate(
			cast(this), instance, creationalContext, cast(getBeanClass()), ip, _beanManagerImpl, decorators);
	}

	@Override
	public void destroy(Object instance, CreationalContext<Object> creationalContext) {
	}

	@Override
	public Class<?> getBeanClass() {
		return _referenceModel.getBeanClass();
	}

	@Override
	public Set<InjectionPoint> getInjectionPoints() {
		return Collections.emptySet();
	}

	@Override
	public String getName() {
		return _name; //_referenceModel.getName();
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
		return _referenceModel.getTypes();
	}

	@Override
	public boolean isAlternative() {
		return false;
	}

	@Override
	public boolean isNullable() {
		return _referenceModel.getCardinality() == ReferenceCardinality.OPTIONAL ? true : false;
	}

	@Override
	public String toString() {
		return "ReferenceBean[" + _referenceModel + "]";
	}

	private List<Decorator<?>> _getDecorators(InjectionPoint ip) {
		return _beanManagerImpl.resolveDecorators(Collections.singleton(ip.getType()), getQualifiers());
	}

	private Object _getInjectedInstance() {
		Object instance = null;

		Map<String, ReferenceCallback> map = _containerState.referenceCallbacks().get(_componentModel);

		ReferenceCallback referenceCallback = map.get(_referenceModel.getName());

		Type injectionPointType = _referenceModel.getInjectionPointType();

		switch (_referenceModel.getCollectionType()) {
			case PROPERTIES:
				instance = referenceCallback.tracked().values().iterator().next();
				break;
			case REFERENCE:
				instance = referenceCallback.tracked().values().iterator().next();
				break;
			case SERVICE:
				instance = referenceCallback.tracked().values().iterator().next();
				break;
			case SERVICEOBJECTS:
				instance = referenceCallback.tracked().values().iterator().next();
				break;
			case TUPLE:
				instance = referenceCallback.tracked().values().iterator().next();
				break;
		}

		return instance;
	}

	private InjectionPoint _getInjectionPoint() {
		CurrentInjectionPoint currentInjectionPoint = _beanManagerImpl.getServices().get(CurrentInjectionPoint.class);
		InjectionPoint ip = currentInjectionPoint.peek();
		return EmptyInjectionPoint.INSTANCE.equals(ip) ? null : ip;
	}

	private final BeanManagerImpl _beanManagerImpl;
	private final ComponentModel _componentModel;
	private final ContainerState _containerState;
	private final String _name;
	private final Set<Annotation> _qualifiers;
	private final ReferenceModel _referenceModel;

}