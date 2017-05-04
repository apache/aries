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

package org.apache.aries.cdi.container.internal.container;

import static org.apache.aries.cdi.container.internal.util.Reflection.cast;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.aries.cdi.container.internal.util.Conversions;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.jboss.weld.manager.BeanManagerImpl;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.ReferenceScope;
import org.osgi.util.converter.TypeReference;

public class ReferenceDependency {

	public ReferenceDependency(
			BeanManagerImpl beanManagerImpl, Reference reference, InjectionPoint injectionPoint)
		throws InvalidSyntaxException {

		_beanManagerImpl = beanManagerImpl;
		_reference = reference;
		_injectionPoint = injectionPoint;

		_bindType = getBindType(_injectionPoint.getType());
		_minCardinality = getMinCardinality(_injectionPoint);
		_serviceClass = getServiceType();

		_string = buildFilter(_serviceClass, _injectionPoint.getQualifiers());
		_filter = FrameworkUtil.createFilter(_string);
	}

	public Class<?> getBeanClass() {
		if (_bindType == BindType.SERVICE_REFERENCE) {
			return ServiceReference.class;
		}
		else if (_bindType == BindType.SERVICE_PROPERTIES) {
			return Map.class;
		}

		return _serviceClass;
	}

	public BindType getBindType() {
		return _bindType;
	}

	public InjectionPoint getInjectionPoint() {
		return _injectionPoint;
	}

	public int getMinCardinality() {
		return _minCardinality;
	}

	public BeanManagerImpl getManager() {
		return _beanManagerImpl;
	}

	public Reference getReference() {
		return _reference;
	}

	public Set<ServiceReference<?>> getMatchingReferences() {
		return _matchingReferences;
	}

	public Type getInjectionPointType() {
		Type type = _injectionPoint.getType();

		if ((type instanceof ParameterizedType)) {
			ParameterizedType pType = (ParameterizedType)type;

			if (Instance.class.isAssignableFrom(cast(pType.getRawType()))) {
				type = pType.getActualTypeArguments()[0];
			}
		}

		return type;
	}

	public boolean isResolved() {
		return (_matchingReferences.size() >= _minCardinality);
	}

	public boolean matches(ServiceReference<?> reference) {
		return _filter.match(reference);
	}

	public void resolve(ServiceReference<?> reference) {
		_matchingReferences.add(reference);
	}

	public void unresolve(ServiceReference<?> reference) {
		_matchingReferences.remove(reference);
	}

	@Override
	public String toString() {
		return _string;
	}

	private String buildFilter(Class<?> serviceType, Set<Annotation> qualifiers) throws InvalidSyntaxException {
		StringBuilder sb = new StringBuilder();

		sb.append("(&(");
		sb.append(Constants.OBJECTCLASS);
		sb.append("=");
		sb.append(serviceType.getName());
		sb.append(")");

		// TODO add Bundle scope?

		if (_reference.scope() == ReferenceScope.PROTOTYPE) {
			sb.append("(");
			sb.append(Constants.SERVICE_SCOPE);
			sb.append("=");
			sb.append(Constants.SCOPE_PROTOTYPE);
			sb.append(")");
		}
		else if (_reference.scope() == ReferenceScope.SINGLETON) {
			sb.append("(");
			sb.append(Constants.SERVICE_SCOPE);
			sb.append("=");
			sb.append(Constants.SCOPE_SINGLETON);
			sb.append(")");
		}

		String targetFilter = _reference.target();

		int targetFilterLength = targetFilter.length();

		if (targetFilterLength > 0) {
			FrameworkUtil.createFilter(targetFilter);

			sb.append(targetFilter);
		}

		for (Annotation qualifier : qualifiers) {
			Class<? extends Annotation> annotationType = qualifier.annotationType();
			if (annotationType.equals(Reference.class)) {
				continue;
			}

			Map<String, String> map = Conversions.c().convert(qualifier).sourceAs(qualifier.annotationType()).to(_mapType);

			Maps.appendFilter(sb, map);
		}

		sb.append(")");

		return sb.toString();
	}

	private BindType getBindType(Type type) {
		if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = cast(type);

			Type rawType = parameterizedType.getRawType();

			if (Instance.class.isAssignableFrom(cast(rawType))) {
				_instance = true;

				Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

				return getBindType(actualTypeArguments[0]);
			}
			else if (Map.class.isAssignableFrom(cast(rawType))) {
				Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

				Type first = actualTypeArguments[0];
				Type second = actualTypeArguments[1];

				if (!(first instanceof ParameterizedType) &&
					String.class.isAssignableFrom(cast(first))) {

					if ((!(second instanceof ParameterizedType) && (second == Object.class)) ||
						(second instanceof WildcardType)) {

						return BindType.SERVICE_PROPERTIES;
					}
				}

				return BindType.SERVICE;
			}
			else if (ServiceReference.class.isAssignableFrom(cast(rawType))) {
				return BindType.SERVICE_REFERENCE;
			}

			return BindType.SERVICE;
		}
		else if (ServiceReference.class.isAssignableFrom(cast(type))) {
			return BindType.SERVICE_REFERENCE;
		}

		return BindType.SERVICE;
	}

	private int getMinCardinality(InjectionPoint injectionPoint) {
		int value = 1;

		if (_instance) {
			value = 0;
		}

		return value;
	}

	private Class<?> getServiceType() {
		if (_reference.service() != Object.class) {
			return _reference.service();
		}

		Type type = _injectionPoint.getType();

		if (_bindType == BindType.SERVICE_PROPERTIES) {
			throw new IllegalArgumentException(
				"A @Reference cannot bind service properties to a Map<String, Object> without " +
					"specifying the @Reference.service property: " + _injectionPoint);
		}
		else if ((_bindType == BindType.SERVICE_REFERENCE) && !(type instanceof ParameterizedType)) {
			throw new IllegalArgumentException(
				"A @Reference cannot bind a ServiceReference without specifying either the " +
					"@Reference.service property or a generic type argument (e.g. ServiceReference<Foo>: " +
						_injectionPoint);
		}

		if (!(type instanceof ParameterizedType)) {
			return cast(type);
		}

		ParameterizedType parameterizedType = cast(type);

		Type rawType = parameterizedType.getRawType();

		if (Instance.class.isAssignableFrom(cast(rawType))) {
			Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

			type = actualTypeArguments[0];

			if (type instanceof ParameterizedType) {
				parameterizedType = (ParameterizedType)type;

				rawType = parameterizedType.getRawType();
			}
			else {
				rawType = type;
			}
		}

		if (!ServiceReference.class.isAssignableFrom(cast(rawType))) {
			return cast(rawType);
		}

		Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

		Type first = actualTypeArguments[0];

		if (first instanceof ParameterizedType) {
			ParameterizedType parameterizedType1 = cast(first);

			return cast(parameterizedType1.getRawType());
		}

		return cast(first);
	}

	private static final TypeReference<Map<String, String>> _mapType = new TypeReference<Map<String, String>>(){};

	private final BeanManagerImpl _beanManagerImpl;
	private final BindType _bindType;
	private final int _minCardinality;
	private final Filter _filter;
	private final InjectionPoint _injectionPoint;
	private final Reference _reference;
	private final Set<ServiceReference<?>> _matchingReferences = new ConcurrentSkipListSet<>(Comparator.reverseOrder());
	private final Class<?> _serviceClass;
	private final String _string;

	private boolean _instance = false;

}