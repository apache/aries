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
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.aries.cdi.container.internal.model.CollectionType;
import org.apache.aries.cdi.container.internal.util.Conversions;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.annotations.Greedy;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.reference.ReferenceServiceObjects;
import org.osgi.service.cdi.runtime.dto.template.MaximumCardinality;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO.Policy;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO.PolicyOption;
import org.osgi.util.converter.TypeReference;

public class ReferenceModel {

	public static class Builder {

		public ReferenceModel build() {
			return new ReferenceModel(_annotated, _member, _qualifiers, _type, _referenceClass);
		}

		public Builder injectionPoint(InjectionPoint injectionPoint) {
			_annotated = injectionPoint.getAnnotated();
			_member = injectionPoint.getMember();
			_qualifiers = injectionPoint.getQualifiers();
			_type = injectionPoint.getType();

			if (_annotated instanceof AnnotatedParameter) {
				AnnotatedParameter<?> parameter = (AnnotatedParameter<?>)_annotated;

				_referenceClass = parameter.getDeclaringCallable().getDeclaringType().getJavaClass();
			}
			if (_annotated instanceof AnnotatedField) {
				AnnotatedField<?> field = (AnnotatedField<?>)_annotated;

				_referenceClass = field.getDeclaringType().getJavaClass();
			}

			return this;
		}

		private Annotated _annotated;
		private Member _member;
		private Set<Annotation> _qualifiers;
		private Class<?> _referenceClass;
		private Type _type;

	}

	public static enum Scope {
		BUNDLE, PROTOTYPE, SINGLETON
	}

	private ReferenceModel(
		Annotated annotated,
		Member member,
		Set<Annotation> qualifiers,
		Type type,
		Class<?> referenceClass) {

		_annotated = annotated;
		_member = member;
		_qualifiers = qualifiers;
		_injectionPointType = type;
		_referenceClass = referenceClass;

		calculateServiceType(_injectionPointType);

		Reference reference = _annotated.getAnnotation(Reference.class);

		if ((reference != null) && (reference.value() != null) && (reference.value() != Object.class)) {
			if (!_serviceType.isAssignableFrom(reference.value())) {
				throw new IllegalArgumentException(
					"The service type specified in @Reference (" + reference.value() +
						") is not compatible with the type calculated from the injection point: " +
							_serviceType);
			}
			_serviceType = reference.value();
		}

		Type rawType = _injectionPointType;

		if (rawType instanceof ParameterizedType) {
			ParameterizedType pt = cast(_injectionPointType);

			rawType = pt.getRawType();
		}

		_beanClass = cast(rawType);

		_name = calculateName(_serviceType, _annotated);

		if (_annotated.isAnnotationPresent(Greedy.class)) {
			_greedy = true;
		}
	}

	public Annotated getAnnotated() {
		return _annotated;
	}

	public Class<?> getBeanClass() {
		return _beanClass;
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

	public Member getMember() {
		return _member;
	}

	public Set<Annotation> getQualifiers() {
		return _qualifiers;
	}

	public Class<?> getServiceType() {
		return _serviceType;
	}

	public String getTarget() {
		return _targetFilter;
	}

	public Set<Type> getTypes() {
		return null; // TODO _types;
	}

	public boolean dynamic() {
		return _dynamic;
	}

	public boolean optional() {
		return _optional;
	}

	public boolean unary() {
		return _multiplicity == MaximumCardinality.ONE;
	}

	@Override
	public String toString() {
		if (_string == null) {
			//_string = String.format("reference[name='%s', service='%s', scope='%s', target='%s']", _name, _service, _scope, _target);
			_string = super.toString();
		}
		return _string;
	}

	public static String buildFilter(
			Class<?> serviceType,
			String target,
			Scope scope,
			Set<Annotation> qualifiers)
		throws InvalidSyntaxException {

		StringBuilder sb = new StringBuilder();

		sb.append("(&(");
		sb.append(Constants.OBJECTCLASS);
		sb.append("=");
		sb.append(serviceType.getName());
		sb.append(")");

		if (scope == Scope.PROTOTYPE) {
			sb.append("(");
			sb.append(Constants.SERVICE_SCOPE);
			sb.append("=");
			sb.append(Constants.SCOPE_PROTOTYPE);
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

//	private static Class<?> calculateBeanClass(Type type) {
//		if (type instanceof ParameterizedType) {
//			ParameterizedType pType = (ParameterizedType)type;
//
//			type = pType.getRawType();
//		}
//		else if (type instanceof WildcardType) {
//			throw new IllegalArgumentException(
//				"Cannot use a wildcard as the bean: " + type);
//		}
//
//		return cast(type);
//	}

//	private static CollectionType calculateCollectionType(Type type) {
//		if (type instanceof ParameterizedType) {
//			ParameterizedType parameterizedType = cast(type);
//
//			Type rawType = parameterizedType.getRawType();
//
//			if ((List.class == cast(rawType)) ||
//				Collection.class.isAssignableFrom(cast(rawType))) {
//
//				Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
//
//				return calculateCollectionType(actualTypeArguments[0]);
//			}
//			else if (Map.class == cast(rawType)) {
//				Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
//
//				Type first = actualTypeArguments[0];
//				Type second = actualTypeArguments[1];
//
//				if (!(first instanceof ParameterizedType) &&
//					String.class.isAssignableFrom(cast(first))) {
//
//					if ((!(second instanceof ParameterizedType) && (second == Object.class)) ||
//						(second instanceof WildcardType)) {
//
//						return CollectionType.PROPERTIES;
//					}
//				}
//			}
//			else if (Map.Entry.class == cast(rawType)) {
//				return CollectionType.TUPLE;
//			}
//			else if (ServiceObjects.class == cast(rawType)) {
//				return CollectionType.SERVICEOBJECTS;
//			}
//			else if (ServiceReference.class == cast(rawType)) {
//				return CollectionType.REFERENCE;
//			}
//		}
//		else if (Map.Entry.class == cast(type)) {
//			return CollectionType.TUPLE;
//		}
//		else if (ServiceObjects.class == cast(type)) {
//			return CollectionType.SERVICEOBJECTS;
//		}
//		else if (ServiceReference.class == cast(type)) {
//			return CollectionType.REFERENCE;
//		}
//
//		return CollectionType.SERVICE;
//	}

//	private static ReferenceCardinality calculateCardinality(
//		ReferenceCardinality cardinality, Multiplicity multiplicity, Type type) {
//
//		if ((multiplicity == Multiplicity.UNARY) &&
//			((cardinality == ReferenceCardinality.AT_LEAST_ONE) || (cardinality == ReferenceCardinality.MULTIPLE))) {
//
//			throw new IllegalArgumentException(
//				format(
//					"Unary injection point type %s cannot be defined by multiple cardinality %s",
//					type, cardinality));
//		}
//		else if ((multiplicity == Multiplicity.MULTIPLE) &&
//				((cardinality == ReferenceCardinality.OPTIONAL) || (cardinality == ReferenceCardinality.MANDATORY))) {
//
//			throw new IllegalArgumentException(
//				format(
//					"Multiple injection point type %s cannot be defined by unary cardinality %s",
//					type, cardinality));
//		}
//
//		if ((cardinality == null) || (cardinality == ReferenceCardinality.DEFAULT)) {
//			switch(multiplicity) {
//				case MULTIPLE:
//					return ReferenceCardinality.MULTIPLE;
//				case UNARY:
//					return ReferenceCardinality.MANDATORY;
//			}
//		}
//
//		return cardinality;
//	}

//	private static Multiplicity calculateMultiplicity(Type type) {
//		if (type instanceof ParameterizedType) {
//			ParameterizedType parameterizedType = cast(type);
//
//			Type rawType = parameterizedType.getRawType();
//
//			if ((Instance.class == cast(rawType)) ||
//				Collection.class.isAssignableFrom(cast(rawType)) ||
//				ServiceEvent.class == cast(rawType)) {
//
//				return Multiplicity.MULTIPLE;
//			}
//		}
//
//		return Multiplicity.UNARY;
//	}

	private String calculateName(Class<?> service, Annotated annotated) {
		Named named = annotated.getAnnotation(Named.class);

		if (named != null) {
			if (named.value() == null | named.value().equals("")) {
				throw new IllegalArgumentException(
					"It's illegal to specify @Name without specifying a value with @Reference: " +
						annotated);
			}
			return named.value();
		}

		String prefix = _referenceClass.getName() + ".";

		if (annotated instanceof AnnotatedParameter) {
			AnnotatedParameter<?> parameter = (AnnotatedParameter<?>)annotated;

			AnnotatedCallable<?> declaringCallable = parameter.getDeclaringCallable();

			if (declaringCallable instanceof AnnotatedConstructor) {
				return prefix + "new" + parameter.getPosition();
			}
			else {
				AnnotatedMethod<?> method = (AnnotatedMethod<?>)declaringCallable;

				return prefix + method.getJavaMember().getName() + parameter.getPosition();
			}
		}
		else {
			AnnotatedField<?> annotatedField = (AnnotatedField<?>)annotated;

			return prefix + annotatedField.getJavaMember().getName();
		}
	}

	private void calculateServiceType(Type type) {
		if (!(type instanceof ParameterizedType)) {
			if (!(type instanceof Class)) {
				throw new IllegalArgumentException(
					"The service type must not be generic: " + type);
			}
			else if (Map.class == cast(type)) {
				throw new IllegalArgumentException(
					"Map must specify a generic type arguments: " + type);
			}
			else if (Map.Entry.class == cast(type)) {
				throw new IllegalArgumentException(
					"Map.Entry must specify a generic type arguments: " + type);
			}
			else if (ReferenceServiceObjects.class == cast(type)) {
				throw new IllegalArgumentException(
					"ReferenceServiceObjects must specify a generic type argument: " + type);
			}
			else if (ServiceReference.class == cast(type)) {
				throw new IllegalArgumentException(
					"ServiceReference must specify a generic type argument: " + type);
			}

			_serviceType = cast(type);

			return;
		}

		ParameterizedType parameterizedType = cast(type);

		Type rawType = parameterizedType.getRawType();

		if (Instance.class == cast(rawType)) {
			throw new IllegalArgumentException(
				"Instance<T> is not supported with @Reference: " + type);
		}

		if ((!_dynamic) && (Provider.class == cast(rawType))) {
			_dynamic = true;

			calculateServiceType(parameterizedType.getActualTypeArguments()[0]);

			return;
		}

		if ((!_optional) && (Optional.class == cast(rawType))) {
			_optional = true;

			calculateServiceType(parameterizedType.getActualTypeArguments()[0]);

			return;
		}

		if ((_multiplicity == MaximumCardinality.ONE) &&
			((Collection.class == cast(rawType)) ||
			(Iterable.class == cast(rawType)) ||
			(List.class == cast(rawType)))) {

			_optional = true;
			_multiplicity = MaximumCardinality.MANY;

			calculateServiceType(parameterizedType.getActualTypeArguments()[0]);

			return;
		}

		Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

		Type argument = actualTypeArguments[0];

		if (Map.class == cast(rawType)) {
			if (String.class != cast(argument)) {
				throw new IllegalArgumentException(
					"Maps of properties must use the form Map<String, (? | Object)>: " + type);
			}

			argument = actualTypeArguments[1];

			if ((Object.class != cast(argument)) &&
				(!(argument instanceof WildcardType))) {

				throw new IllegalArgumentException(
					"Maps of properties must use the form Map<String, (? | Object)>: " + type);
			}

			_collectionType = CollectionType.PROPERTIES;

			Reference reference = _annotated.getAnnotation(Reference.class);

			if ((reference == null) || (reference.value() == null)) {
				throw new IllegalArgumentException(
					"Maps of properties must specify service type with @Reference.value(): " + argument);
			}

			_serviceType = reference.value();

			return;
		}

		if (Map.Entry.class == cast(rawType)) {
			if (!checkKey(argument)) {
				throw new IllegalArgumentException(
					"Tuples must have a key of type Map<String, (? | Object)>: " + argument);
			}

			argument = actualTypeArguments[1];

			if (!(argument instanceof Class)) {
				throw new IllegalArgumentException(
					"The service type must not be generic: " + argument);
			}

			_collectionType = CollectionType.TUPLE;

			_serviceType = cast(argument);

			return;
		}

		if (ReferenceServiceObjects.class == cast(rawType)) {
			_collectionType = CollectionType.SERVICEOBJECTS;

			calculateServiceType(argument);

			return;
		}

		if (ServiceReference.class == cast(rawType)) {
			_collectionType = CollectionType.REFERENCE;

			calculateServiceType(argument);

			return;
		}

		_serviceType = cast(rawType);

		if (_serviceType.getTypeParameters().length > 0) {
			throw new IllegalArgumentException(
				"Illegal service type: " + argument);
		}
	}

//	private static Class<?> calculateServiceClass(
//		Class<?> service, ReferenceCardinality cardinality, CollectionType collectionType, Type injectionPointType, Annotated annotated) {
//
//		Class<?> calculatedServiceClass = calculateServiceClass(injectionPointType);
//
//		if ((service == null) || (service == Object.class)) {
//			if (calculatedServiceClass == null) {
//				throw new IllegalArgumentException(
//					"Could not determine the service type from @Reference on annotated " +
//						annotated);
//			}
//
//			switch(collectionType) {
//				case PROPERTIES:
//					if (calculatedServiceClass == Map.class) {
//						throw new IllegalArgumentException(
//							"A @Reference cannot bind service properties to a Map<String, Object> without " +
//								"specifying the @Reference.service property: " + annotated);
//					}
//					break;
//				case REFERENCE:
//					if (calculatedServiceClass == ServiceReference.class) {
//						throw new IllegalArgumentException(
//							"A @Reference cannot bind a ServiceReference without specifying either the " +
//								"@Reference.service property or a generic type argument (e.g. ServiceReference<Foo>: " +
//									annotated);
//					}
//					break;
//				case SERVICEOBJECTS:
//					if	(calculatedServiceClass == ServiceObjects.class) {
//						throw new IllegalArgumentException(
//							"A @Reference cannot bind a ServiceObjects without specifying either the " +
//								"@Reference.service property or a generic type argument (e.g. ServiceObjects<Foo>: " +
//									annotated);
//					}
//					break;
//				case TUPLE:
//					if (calculatedServiceClass == Map.Entry.class) {
//						throw new IllegalArgumentException(
//							"A @Reference cannot bind a Map.Entry without specifying either the " +
//								"@Reference.service property or a generic type argument (e.g. Map.Entry<Map<String, Object>, Foo>: " +
//									annotated);
//					}
//					break;
//				default:
//			}
//
//			return calculatedServiceClass;
//		}
//
//		switch(collectionType) {
//			case PROPERTIES:
//				if (Map.class.isAssignableFrom(calculatedServiceClass)) {
//					return service;
//				}
//				break;
//			case REFERENCE:
//				if ((calculatedServiceClass == null) ||
//					ServiceReference.class.isAssignableFrom(calculatedServiceClass)) {
//					return service;
//				}
//				break;
//			case SERVICEOBJECTS:
//				if ((calculatedServiceClass == null) ||
//					ServiceObjects.class.isAssignableFrom(calculatedServiceClass)) {
//					return service;
//				}
//				break;
//			case TUPLE:
//				if ((calculatedServiceClass != null) &&
//					Map.Entry.class.isAssignableFrom(calculatedServiceClass)) {
//
//					if (!checkKey(calculatedServiceClass)) {
//						throw new IllegalArgumentException(
//							"Tuples must have a key of type Map<String, [? or Object]>: " + calculatedServiceClass);
//					}
//
//					return service;
//				}
//				else if ((calculatedServiceClass == null) ||
//					calculatedServiceClass.isAssignableFrom(service)) {
//
//					return service;
//				}
//				break;
//			case SERVICE:
//				if (((calculatedServiceClass == null) &&
//						((cardinality == ReferenceCardinality.MULTIPLE) ||
//						(cardinality == ReferenceCardinality.AT_LEAST_ONE))) ||
//					((calculatedServiceClass != null) &&
//						calculatedServiceClass.isAssignableFrom(service))) {
//					return service;
//				}
//		}
//
//		throw new IllegalArgumentException(
//			"@Reference.service " + service + " is not compatible with annotated " + annotated);
//	}

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

//	private static Type upwrapCDITypes(Type type) {
//		if (type instanceof ParameterizedType) {
//			ParameterizedType pType = (ParameterizedType)type;
//
//			Type rawType = pType.getRawType();
//
//			if (Instance.class == Reflection.cast(rawType) ||
//				ServiceEvent.class == Reflection.cast(rawType)) {
//
//				type = pType.getActualTypeArguments()[0];
//			}
//		}
//
//		return type;
//	}

	public ReferenceTemplateDTO toDTO() {
		ReferenceTemplateDTO dto = new ReferenceTemplateDTO();
		dto.maximumCardinality = _multiplicity;
		dto.minimumCardinality = (_multiplicity == MaximumCardinality.ONE) ? (_optional?0:1) : (0);
		dto.name = _name;
		dto.policy = (_dynamic) ? Policy.DYNAMIC : Policy.STATIC;
		dto.policyOption = (_greedy) ? PolicyOption.GREEDY: PolicyOption.RELUCTANT;
		dto.targetFilter = _targetFilter;
		dto.serviceType = _serviceType.getName();
		return dto;
	}

	private static final String _emptyFilter = "";

	private static final TypeReference<Map<String, String>> _mapType = new TypeReference<Map<String, String>>(){};

	private final Annotated _annotated;
	private Class<?> _beanClass;
	private CollectionType _collectionType = CollectionType.SERVICE;
	private boolean _dynamic = false;
	private boolean _greedy = false;
	private final Type _injectionPointType;
	private final Member _member;
	private MaximumCardinality _multiplicity = MaximumCardinality.ONE;
	private String _name;
	private boolean _optional = false;
	private final Set<Annotation> _qualifiers;
	private Class<?> _referenceClass;
	private Class<?> _serviceType;
	private String _string;
	private String _targetFilter = _emptyFilter;
}
