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

import static org.apache.aries.cdi.container.internal.model.Model.*;
import static org.apache.aries.cdi.container.internal.util.Reflection.cast;
import static java.lang.String.format;
import static org.apache.aries.cdi.container.internal.model.Constants.CARDINALITY_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.CDI10_URI;
import static org.apache.aries.cdi.container.internal.model.Constants.NAME_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.POLICY_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.POLICY_OPTION_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.SCOPE_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.TARGET_ATTRIBUTE;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedParameter;

import org.apache.aries.cdi.container.internal.model.CollectionType;
import org.apache.aries.cdi.container.internal.util.Conversions;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.internal.util.Sets;
import org.apache.aries.cdi.container.internal.util.Types;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.ReferenceCardinality;
import org.osgi.service.cdi.annotations.ReferencePolicy;
import org.osgi.service.cdi.annotations.ReferencePolicyOption;
import org.osgi.service.cdi.annotations.ReferenceScope;
import org.osgi.service.cdi.annotations.ServiceEvent;
import org.osgi.util.converter.TypeReference;
import org.xml.sax.Attributes;

public class ReferenceModel {

	public static class Builder {

		public Builder(Attributes attributes) {
			_cardinality = _cardinality(attributes);
			_name = _name(attributes);
			_option = _option(attributes);
			_policy = _policy(attributes);
			_scope = _scope(attributes);
			_target = _target(attributes);
		}

		public Builder(Set<Annotation> qualifiers) {
			_qualifiers = qualifiers;
			Reference reference = getQualifier(_qualifiers, Reference.class);
			if (reference != null) {
				_cardinality = reference.cardinality();
				_name = reference.name();
				_option = reference.policyOption();
				_policy = reference.policy();
				_scope = reference.scope();
				_service = reference.service();
				_target = reference.target();
			}
		}

		public ReferenceModel build() {
			if ((_annotated == null) && (_service == null)) {
				throw new IllegalArgumentException(
					"Either injectionPoint or service must be set!");
			}

			if (_annotated == null) {
				_annotated = new ReferenceAnnotated(_service);
			}

			Type type = upwrapCDITypes(_annotated.getBaseType());

			_policy = calculatePolicy(_policy);
			_option = calculatePolicyOption(_option);
			_scope = calculateScope(_scope);
			Multiplicity multiplicity = calculateMultiplicity(_annotated.getBaseType());// we need the pure type to check "Instance"
			_cardinality = calculateCardinality(_cardinality, multiplicity, type);
			CollectionType collectionType = calculateCollectionType(type);
			Class<?> beanClass = calculateBeanClass(type);
			_service = calculateServiceClass(_service, _cardinality, collectionType, type, _annotated);
			_name = calculateName(_name, _service, _annotated);

			return new ReferenceModel(_service, _cardinality, _name, _policy, _option, _target, _scope, _qualifiers, beanClass, type, collectionType);
		}

		public Builder cardinality(ReferenceCardinality cardinality) {
			_cardinality = cardinality;
			return this;
		}

		public Builder annotated(Annotated annotated) {
			_annotated = annotated;
			return this;
		}

		public Builder name(String name) {
			_name = name;
			return this;
		}

		public Builder option(ReferencePolicyOption option) {
			_option = option;
			return this;
		}

		public Builder policy(ReferencePolicy policy) {
			_policy = policy;
			return this;
		}

		public Builder scope(ReferenceScope scope) {
			_scope = scope;
			return this;
		}

		public Builder service(Class<?> service) {
			_service = service;
			return this;
		}

		public Builder target(String target) {
			_target = target;
			return this;
		}

		@SuppressWarnings("unchecked")
		private static <T extends Annotation> T getQualifier(
			Set<Annotation> qualifiers, Class<T> clazz) {
			for (Annotation annotation : qualifiers) {
				if (clazz.isAssignableFrom(annotation.annotationType())) {
					return (T)annotation;
				}
			}
			return null;
		}

		private ReferenceCardinality _cardinality;
		private Annotated _annotated;
		private String _name;
		private ReferencePolicyOption _option;
		private ReferencePolicy _policy;
		private Set<Annotation> _qualifiers;
		private ReferenceScope _scope;
		private Class<?> _service;
		private String _target;

	}

	private ReferenceModel(
		Class<?> service,
		ReferenceCardinality cardinality,
		String name,
		ReferencePolicy policy,
		ReferencePolicyOption option,
		String target,
		ReferenceScope scope,
		Set<Annotation> qualifiers,
		Class<?> beanClass,
		Type injectionPointType,
		CollectionType collectionType) {

		_service = service;
		_cardinality = cardinality;
		_name = name;
		_policy = policy;
		_option = option;
		_target = target;
		_scope = scope;
		_qualifiers = new LinkedHashSet<>();
		if (qualifiers != null) {
			_qualifiers.addAll(qualifiers);
		}
		_beanClass = beanClass;
		_injectionPointType = injectionPointType;
		_collectionType = collectionType;

		_types = Sets.immutableHashSet(_injectionPointType, Object.class);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_beanClass == null) ? 0 : _beanClass.hashCode());
		result = prime * result + ((_cardinality == null) ? 0 : _cardinality.hashCode());
		result = prime * result + ((_name == null) ? 0 : _name.hashCode());
		result = prime * result + ((_option == null) ? 0 : _option.hashCode());
		result = prime * result + ((_policy == null) ? 0 : _policy.hashCode());
		result = prime * result + ((_scope == null) ? 0 : _scope.hashCode());
		result = prime * result + ((_service == null) ? 0 : _service.hashCode());
		result = prime * result + ((_target == null) ? 0 : _target.hashCode());
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
		ReferenceModel other = (ReferenceModel) obj;
		if (_beanClass == null) {
			if (other._beanClass != null)
				return false;
		} else if (!_beanClass.equals(other._beanClass))
			return false;
		if (_cardinality != other._cardinality)
			return false;
		if (_name == null) {
			if (other._name != null)
				return false;
		} else if (!_name.equals(other._name))
			return false;
		if (_option != other._option)
			return false;
		if (_policy != other._policy)
			return false;
		if (_scope != other._scope)
			return false;
		if (_service == null) {
			if (other._service != null)
				return false;
		} else if (!_service.equals(other._service))
			return false;
		if (_target == null) {
			if (other._target != null)
				return false;
		} else if (!_target.equals(other._target))
			return false;
		return true;
	}

	public boolean found() {
		return _found.get();
	}

	public void found(boolean found) {
		_found.set(found);
	}

	public Class<?> getBeanClass() {
		return _beanClass;
	}

	public ReferenceCardinality getCardinality() {
		return _cardinality;
	}

	public CollectionType getCollectionType() {
		return _collectionType;
	}

	public Type getInjectionPointType() {
		return _injectionPointType;
	}

	public String getName() {
		return _name;
	}

	public ReferencePolicy getPolicy() {
		return _policy;
	}

	public ReferencePolicyOption getPolicyOption() {
		return _option;
	}

	public Set<Annotation> getQualifiers() {
		return _qualifiers;
	}

	public ReferenceScope getScope() {
		return _scope;
	}

	public Class<?> getServiceClass() {
		return _service;
	}

	public String getTarget() {
		return _target;
	}

	public Set<Type> getTypes() {
		return _types;
	}

	public void setQualifiers(Set<Annotation> qualifiers) {
		_qualifiers.clear();
		_qualifiers.addAll(qualifiers);
	}

	@Override
	public String toString() {
		if (_string == null) {
			_string = String.format("reference[name='%s', service='%s', scope='%s', target='%s']", _name, _service, _scope, _target);
		}
		return _string;
	}

	public static String buildFilter(
			Class<?> serviceType,
			String target,
			ReferenceScope scope,
			Set<Annotation> qualifiers)
		throws InvalidSyntaxException {

		StringBuilder sb = new StringBuilder();

		sb.append("(&(");
		sb.append(Constants.OBJECTCLASS);
		sb.append("=");
		sb.append(serviceType.getName());
		sb.append(")");

		if (scope == ReferenceScope.PROTOTYPE) {
			sb.append("(");
			sb.append(Constants.SERVICE_SCOPE);
			sb.append("=");
			sb.append(Constants.SCOPE_PROTOTYPE);
			sb.append(")");
		}
		else if (scope == ReferenceScope.SINGLETON) {
			sb.append("(");
			sb.append(Constants.SERVICE_SCOPE);
			sb.append("=");
			sb.append(Constants.SCOPE_SINGLETON);
			sb.append(")");
		}
		else if (scope == ReferenceScope.BUNDLE) {
			sb.append("(");
			sb.append(Constants.SERVICE_SCOPE);
			sb.append("=");
			sb.append(Constants.SCOPE_BUNDLE);
			sb.append(")");
		}

		String targetFilter = target == null ? "" : target;

		int targetFilterLength = targetFilter.length();

		if (targetFilterLength > 0) {
			FrameworkUtil.createFilter(targetFilter);

			sb.append(targetFilter);
		}

		if (qualifiers != null) {
			for (Annotation qualifier : qualifiers) {
				Class<? extends Annotation> annotationType = qualifier.annotationType();

				if (annotationType.equals(Reference.class)) {
					continue;
				}

				Map<String, String> map = Conversions.convert(qualifier).sourceAs(qualifier.annotationType()).to(_mapType);

				Maps.appendFilter(sb, map);
			}
		}

		sb.append(")");

		return sb.toString();
	}

	private static Class<?> calculateBeanClass(Type type) {
		if (type instanceof ParameterizedType) {
			ParameterizedType pType = (ParameterizedType)type;

			type = pType.getRawType();
		}
		else if (type instanceof WildcardType) {
			throw new IllegalArgumentException(
				"Cannot use a wildcard as the bean: " + type);
		}

		return cast(type);
	}

	private static CollectionType calculateCollectionType(Type type) {
		if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = cast(type);

			Type rawType = parameterizedType.getRawType();

			if ((List.class == cast(rawType)) ||
				Collection.class.isAssignableFrom(cast(rawType))) {

				Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

				return calculateCollectionType(actualTypeArguments[0]);
			}
			else if (Map.class == cast(rawType)) {
				Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

				Type first = actualTypeArguments[0];
				Type second = actualTypeArguments[1];

				if (!(first instanceof ParameterizedType) &&
					String.class.isAssignableFrom(cast(first))) {

					if ((!(second instanceof ParameterizedType) && (second == Object.class)) ||
						(second instanceof WildcardType)) {

						return CollectionType.PROPERTIES;
					}
				}
			}
			else if (Map.Entry.class == cast(rawType)) {
				return CollectionType.TUPLE;
			}
			else if (ServiceObjects.class == cast(rawType)) {
				return CollectionType.SERVICEOBJECTS;
			}
			else if (ServiceReference.class == cast(rawType)) {
				return CollectionType.REFERENCE;
			}
		}
		else if (Map.Entry.class == cast(type)) {
			return CollectionType.TUPLE;
		}
		else if (ServiceObjects.class == cast(type)) {
			return CollectionType.SERVICEOBJECTS;
		}
		else if (ServiceReference.class == cast(type)) {
			return CollectionType.REFERENCE;
		}

		return CollectionType.SERVICE;
	}

	private static ReferenceCardinality calculateCardinality(
		ReferenceCardinality cardinality, Multiplicity multiplicity, Type type) {

		if ((multiplicity == Multiplicity.UNARY) &&
			((cardinality == ReferenceCardinality.AT_LEAST_ONE) || (cardinality == ReferenceCardinality.MULTIPLE))) {

			throw new IllegalArgumentException(
				format(
					"Unary injection point type %s cannot be defined by multiple cardinality %s",
					type, cardinality));
		}
		else if ((multiplicity == Multiplicity.MULTIPLE) &&
				((cardinality == ReferenceCardinality.OPTIONAL) || (cardinality == ReferenceCardinality.MANDATORY))) {

			throw new IllegalArgumentException(
				format(
					"Multiple injection point type %s cannot be defined by unary cardinality %s",
					type, cardinality));
		}

		if ((cardinality == null) || (cardinality == ReferenceCardinality.DEFAULT)) {
			switch(multiplicity) {
				case MULTIPLE:
					return ReferenceCardinality.MULTIPLE;
				case UNARY:
					return ReferenceCardinality.MANDATORY;
			}
		}

		return cardinality;
	}

	private static Multiplicity calculateMultiplicity(Type type) {
		if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = cast(type);

			Type rawType = parameterizedType.getRawType();

			if ((Instance.class == cast(rawType)) ||
				Collection.class.isAssignableFrom(cast(rawType)) ||
				ServiceEvent.class == cast(rawType)) {

				return Multiplicity.MULTIPLE;
			}
		}

		return Multiplicity.UNARY;
	}

	public static String calculateName(String name, Class<?> service, Annotated annotated) {
		if ((name != null) && (name.length() > 0)) {
			return name;
		}

		if (annotated != null) {
			if (annotated instanceof AnnotatedParameter) {
				AnnotatedParameter<?> annotatedParameter = (AnnotatedParameter<?>)annotated;

				return Types.getName(service) + annotatedParameter.getPosition();
			}
			if (annotated instanceof AnnotatedField) {
				AnnotatedField<?> annotatedField = (AnnotatedField<?>)annotated;

				return annotatedField.getJavaMember().getName();
			}
		}

		return Types.getName(service);
	}

	private static ReferencePolicy calculatePolicy(ReferencePolicy policy) {
		if ((policy == null) || (policy == ReferencePolicy.DEFAULT)) {
			return ReferencePolicy.STATIC;
		}

		return policy;
	}

	private static ReferencePolicyOption calculatePolicyOption(ReferencePolicyOption option) {
		if ((option == null) || (option == ReferencePolicyOption.DEFAULT)) {
			return ReferencePolicyOption.RELUCTANT;
		}

		return option;
	}

	private static ReferenceScope calculateScope(ReferenceScope scope) {
		if ((scope == null) || (scope == ReferenceScope.DEFAULT)) {
			return ReferenceScope.DEFAULT;
		}

		return scope;
	}

	private static Class<?> calculateServiceClass(Type injectionPointType) {
		Type type = injectionPointType;

		if (!(type instanceof ParameterizedType)) {
			return cast(type);
		}

		ParameterizedType parameterizedType = cast(type);

		Type rawType = parameterizedType.getRawType();

		if ((List.class == cast(rawType)) ||
			Collection.class.isAssignableFrom(cast(rawType))) {

			Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

			type = actualTypeArguments[0];

			if (type instanceof ParameterizedType) {
				parameterizedType = (ParameterizedType)type;

				rawType = parameterizedType.getRawType();
			}
			else if ((type instanceof WildcardType) ||
					Map.Entry.class.isAssignableFrom(cast(type))) {

				return null;
			}
			else {
				rawType = type;
			}
		}

		if (!Map.Entry.class.isAssignableFrom(cast(rawType)) &&
			!ServiceObjects.class.isAssignableFrom(cast(rawType)) &&
			!ServiceReference.class.isAssignableFrom(cast(rawType))) {

			return cast(rawType);
		}

		Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

		Type argument = actualTypeArguments[0];

		if (Map.Entry.class.isAssignableFrom(cast(rawType))) {
			if (!checkKey(argument)) {
				throw new IllegalArgumentException(
					"Tuples must have a key of type Map<String, [? or Object]>: " + argument);
			}

			argument = actualTypeArguments[1];
		}

		if (argument instanceof ParameterizedType) {
			ParameterizedType parameterizedType1 = cast(argument);

			return cast(parameterizedType1.getRawType());
		}
		else if (argument instanceof WildcardType) {
			WildcardType wildcardType = (WildcardType)argument;

			if ((wildcardType.getUpperBounds().length == 1)) {
				argument = wildcardType.getUpperBounds()[0];

				if (Object.class.equals(argument)) {
					return null;
				}
				else if (argument instanceof Class) {
					return cast(argument);
				}
			}

			throw new IllegalArgumentException(
				"@Reference cannot use nested parameterized types or multiple upper bounds: " + injectionPointType);
		}

		return cast(argument);
	}

	private static Class<?> calculateServiceClass(
		Class<?> service, ReferenceCardinality cardinality, CollectionType collectionType, Type injectionPointType, Annotated annotated) {

		Class<?> calculatedServiceClass = calculateServiceClass(injectionPointType);

		if ((service == null) || (service == Object.class)) {
			if (calculatedServiceClass == null) {
				throw new IllegalArgumentException(
					"Could not determine the service type from @Reference on annotated " +
						annotated);
			}

			switch(collectionType) {
				case PROPERTIES:
					if (calculatedServiceClass == Map.class) {
						throw new IllegalArgumentException(
							"A @Reference cannot bind service properties to a Map<String, Object> without " +
								"specifying the @Reference.service property: " + annotated);
					}
					break;
				case REFERENCE:
					if (calculatedServiceClass == ServiceReference.class) {
						throw new IllegalArgumentException(
							"A @Reference cannot bind a ServiceReference without specifying either the " +
								"@Reference.service property or a generic type argument (e.g. ServiceReference<Foo>: " +
									annotated);
					}
					break;
				case SERVICEOBJECTS:
					if	(calculatedServiceClass == ServiceObjects.class) {
						throw new IllegalArgumentException(
							"A @Reference cannot bind a ServiceObjects without specifying either the " +
								"@Reference.service property or a generic type argument (e.g. ServiceObjects<Foo>: " +
									annotated);
					}
					break;
				case TUPLE:
					if (calculatedServiceClass == Map.Entry.class) {
						throw new IllegalArgumentException(
							"A @Reference cannot bind a Map.Entry without specifying either the " +
								"@Reference.service property or a generic type argument (e.g. Map.Entry<Map<String, Object>, Foo>: " +
									annotated);
					}
					break;
				default:
			}

			return calculatedServiceClass;
		}

		switch(collectionType) {
			case PROPERTIES:
				if (Map.class.isAssignableFrom(calculatedServiceClass)) {
					return service;
				}
				break;
			case REFERENCE:
				if ((calculatedServiceClass == null) ||
					ServiceReference.class.isAssignableFrom(calculatedServiceClass)) {
					return service;
				}
				break;
			case SERVICEOBJECTS:
				if ((calculatedServiceClass == null) ||
					ServiceObjects.class.isAssignableFrom(calculatedServiceClass)) {
					return service;
				}
				break;
			case TUPLE:
				if ((calculatedServiceClass != null) &&
					Map.Entry.class.isAssignableFrom(calculatedServiceClass)) {

					if (!checkKey(calculatedServiceClass)) {
						throw new IllegalArgumentException(
							"Tuples must have a key of type Map<String, [? or Object]>: " + calculatedServiceClass);
					}

					return service;
				}
				else if ((calculatedServiceClass == null) ||
					calculatedServiceClass.isAssignableFrom(service)) {

					return service;
				}
				break;
			case SERVICE:
				if (((calculatedServiceClass == null) &&
						((cardinality == ReferenceCardinality.MULTIPLE) ||
						(cardinality == ReferenceCardinality.AT_LEAST_ONE))) ||
					((calculatedServiceClass != null) &&
						calculatedServiceClass.isAssignableFrom(service))) {
					return service;
				}
		}

		throw new IllegalArgumentException(
			"@Reference.service " + service + " is not compatible with annotated " + annotated);
	}

	// check the key type to make sure it complies with Map<String, ?> OR Map<String, Object>
	private static boolean checkKey(Type mapEntryType) {
		if (!(mapEntryType instanceof ParameterizedType)) {
			return false;
		}

		ParameterizedType parameterizedKeyType = (ParameterizedType)mapEntryType;

		if ((!Map.class.isAssignableFrom(cast(parameterizedKeyType.getRawType()))) ||
			(!parameterizedKeyType.getActualTypeArguments()[0].equals(String.class))) {

			return false;
		}

		Type valueType = parameterizedKeyType.getActualTypeArguments()[1];

		if ((!valueType.equals(Object.class) &&
			(
				(!(valueType instanceof WildcardType)) ||
				(((WildcardType)valueType).getUpperBounds().length != 1) ||
				(!((WildcardType)valueType).getUpperBounds()[0].equals(Object.class))))) {

			return false;
		}

		return true;
	}

	private static ReferenceCardinality _cardinality(Attributes attributes) {
		return ReferenceCardinality.get(
			getValue(CDI10_URI, CARDINALITY_ATTRIBUTE, attributes, ReferenceCardinality.DEFAULT.toString()));
	}

	private static String _name(Attributes attributes) {
		return getValue(CDI10_URI, NAME_ATTRIBUTE, attributes);
	}

	private static ReferencePolicyOption _option(Attributes attributes) {
		return ReferencePolicyOption.get(
			getValue(CDI10_URI, POLICY_OPTION_ATTRIBUTE, attributes, ReferencePolicyOption.DEFAULT.toString()));
	}

	private static ReferencePolicy _policy(Attributes attributes) {
		return ReferencePolicy.get(
			getValue(CDI10_URI, POLICY_ATTRIBUTE, attributes, ReferencePolicy.DEFAULT.toString()));
	}

	private static ReferenceScope _scope(Attributes attributes) {
		return ReferenceScope.get(getValue(
			CDI10_URI, SCOPE_ATTRIBUTE, attributes, ReferenceScope.DEFAULT.toString()));
	}

	private static String _target(Attributes attributes) {
		return getValue(CDI10_URI, TARGET_ATTRIBUTE, attributes);
	}

	private static Type upwrapCDITypes(Type type) {
		if (type instanceof ParameterizedType) {
			ParameterizedType pType = (ParameterizedType)type;

			Type rawType = pType.getRawType();

			if (Instance.class == cast(rawType) ||
				ServiceEvent.class == cast(rawType)) {

				type = pType.getActualTypeArguments()[0];
			}
		}

		return type;
	}

	private static final TypeReference<Map<String, String>> _mapType = new TypeReference<Map<String, String>>(){};

	private final Class<?> _beanClass;
	private final ReferenceCardinality _cardinality;
	private final CollectionType _collectionType;
	private final AtomicBoolean _found = new AtomicBoolean();
	private final Type _injectionPointType;
	private final String _name;
	private final ReferencePolicyOption _option;
	private final ReferencePolicy _policy;
	private final Set<Annotation> _qualifiers;
	private final ReferenceScope _scope;
	private final Class<?> _service;
	private String _string;
	private final String _target;
	private final Set<Type> _types;

	private static class ReferenceAnnotated implements Annotated {

		public ReferenceAnnotated(Class<?> service) {
			_service = service;
		}

		@Override
		public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
			return null;
		}

		@Override
		public Set<Annotation> getAnnotations() {
			return Collections.emptySet();
		}

		@Override
		public <T extends Annotation> Set<T> getAnnotations(Class<T> annotationType) {
			return null;
		}

		@Override
		public Type getBaseType() {
			return _service;
		}

		@Override
		public Set<Type> getTypeClosure() {
			return Sets.hashSet(_service);
		}

		@Override
		public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
			return false;
		}

		private final Class<?> _service;

	}

}
