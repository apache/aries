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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Named;

import org.apache.aries.cdi.container.internal.container.BindType;
import org.apache.aries.cdi.container.internal.literal.AnyLiteral;
import org.apache.aries.cdi.container.internal.literal.DefaultLiteral;
import org.apache.aries.cdi.container.internal.util.Sets;
import org.jboss.weld.injection.CurrentInjectionPoint;
import org.jboss.weld.injection.EmptyInjectionPoint;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.Decorators;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceBean implements Bean<Object> {

	public ReferenceBean(
		BeanManagerImpl beanManagerImpl, BundleContext bundleContext, Type injectionPointType, Class<?> beanClass,
		BindType bindType, ServiceReference<?> serviceReference) {

		_beanManagerImpl = beanManagerImpl;
		_bundleContext = bundleContext;
		_typesForMatchingBeansToInjectionPoints = Sets.immutableHashSet(injectionPointType, Object.class);
		_beanClass = beanClass;
		_bindType = bindType;
		_serviceReference = serviceReference;
		_currentInjectionPoint = _beanManagerImpl.getServices().get(CurrentInjectionPoint.class);
		_qualifiers = Sets.hashSet(DefaultLiteral.INSTANCE, AnyLiteral.INSTANCE);

		for (Annotation qualifier : _qualifiers) {
			if (qualifier.annotationType().equals(Named.class)) {
				_name = ((Named)qualifier).value();
			}
		}
	}

	public void addQualifier(Annotation annotation) {
		_qualifiers.add(annotation);
	}

	public void addQualifiers(Set<Annotation> qualifiers) {
		_qualifiers.addAll(qualifiers);
	}

	@Override
	public Object create(CreationalContext<Object> creationalContext) {
		return create0(creationalContext);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void destroy(Object instance, CreationalContext creationalContext) {
		if (_serviceObjects == null) {
			return;
		}

		try {
			_serviceObjects.ungetService(instance);
		}
		catch (Throwable t) {
			if (_log.isWarnEnabled()) {
				_log.warn("CDIe - UngetService resulted in error", t);
			}
		}
	}

	@Override
	public Class<?> getBeanClass() {
		return _beanClass;
	}

	public BindType getBindType() {
		return _bindType;
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
		return "ReferenceBean[" + _serviceReference + "]";
	}

	protected <T> T create0(CreationalContext<T> creationalContext) {
		T instance = cast(getServiceImpl());
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

	protected Object getServiceImpl() {
		if (ServiceReference.class.equals(_beanClass)) {
			return _serviceReference;
		}
		else if (Map.class.equals(_beanClass)) {
			Map<String, Object> properties = new HashMap<>();

			for (String key : _serviceReference.getPropertyKeys()) {
				properties.put(key, _serviceReference.getProperty(key));
			}

			return properties;
		}

		if (_serviceObjects == null) {
			_serviceObjects = _bundleContext.getServiceObjects(_serviceReference);
		}

		return _serviceObjects.getService();
	}

	private static final Logger _log = LoggerFactory.getLogger(ReferenceBean.class);

	private final Class<?> _beanClass;
	private final BeanManagerImpl _beanManagerImpl;
	private final BindType _bindType;
	private final BundleContext _bundleContext;
	private final CurrentInjectionPoint _currentInjectionPoint;
	private String _name;
	private final Set<Annotation> _qualifiers;
	@SuppressWarnings("rawtypes")
	private volatile ServiceObjects _serviceObjects;
	private final ServiceReference<?> _serviceReference;
	private final Set<Type> _typesForMatchingBeansToInjectionPoints;

}