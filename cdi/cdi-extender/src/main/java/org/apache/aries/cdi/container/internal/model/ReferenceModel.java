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

package org.apache.aries.cdi.container.internal.model;

import static org.apache.aries.cdi.container.internal.util.Filters.*;
import static org.apache.aries.cdi.container.internal.util.Reflection.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import javax.inject.Qualifier;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.ReferencePolicy;
import org.osgi.service.cdi.ReferencePolicyOption;
import org.osgi.service.cdi.annotations.Greedy;
import org.osgi.service.cdi.annotations.PrototypeRequired;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.reference.BindObject;
import org.osgi.service.cdi.reference.BindServiceObjects;
import org.osgi.service.cdi.reference.BindServiceReference;
import org.osgi.service.cdi.reference.BeanServiceObjects;

public class ReferenceModel {

	public static class Builder {

		public Builder() {}

		public Builder(AnnotatedField<?> annotated) {
			_annotated = annotated;
			_declaringClass = annotated.getDeclaringType().getJavaClass();
		}

		public Builder(AnnotatedMethod<?> annotated) {
			_annotated = annotated;
			_declaringClass = annotated.getDeclaringType().getJavaClass();
		}

		public Builder(AnnotatedParameter<?> annotated) {
			_annotated = annotated;
			_declaringClass = annotated.getDeclaringCallable().getDeclaringType().getJavaClass();
		}

		public ReferenceModel build() {
			Objects.requireNonNull(_annotated);
			Objects.requireNonNull(_declaringClass);
			Objects.requireNonNull(_type);
			return new ReferenceModel(_type, _declaringClass, _annotated);
		}

		public Builder injectionPoint(InjectionPoint injectionPoint) {
			_annotated = injectionPoint.getAnnotated();
			_type = injectionPoint.getType();

			if (_annotated instanceof AnnotatedParameter) {
				AnnotatedParameter<?> parameter = (AnnotatedParameter<?>)_annotated;

				_declaringClass = parameter.getDeclaringCallable().getDeclaringType().getJavaClass();
			}
			else if (_annotated instanceof AnnotatedField) {
				AnnotatedField<?> field = (AnnotatedField<?>)_annotated;

				_declaringClass = field.getDeclaringType().getJavaClass();
			}

			return this;
		}

		public Builder type(Type type) {
			_type = type;
			return this;
		}

		private Annotated _annotated;
		private Class<?> _declaringClass;
		private Type _type;

	}

	public static enum Scope {
		BUNDLE, PROTOTYPE, SINGLETON
	}

	private ReferenceModel(
		Type injectionPointType,
		Class<?> declaringClass,
		Annotated annotated) {

		_annotated = annotated;
		_injectionPointType = injectionPointType;
		_declaringClass = declaringClass;

		_reference = _annotated.getAnnotation(Reference.class);

		_referenceType = getReferenceType();
		_referenceTarget = getReferenceTarget();
		_prototype = getQualifiers().stream().filter(
			ann -> ann.annotationType().equals(PrototypeRequired.class)
		).findFirst().isPresent();

		calculateServiceType(_injectionPointType);

		_referenceType.ifPresent(c -> {
			if ((_serviceType != null) && !_serviceType.isAssignableFrom(c)) {
				throw new IllegalArgumentException(
					"The service type specified in @Reference (" + c +
						") is not compatible with the type calculated from the injection point: " +
							_serviceType + " on " + _annotated);
			}

			_serviceType = c;
		});

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

		_targetFilter = buildFilter();
	}

	private Optional<String> getReferenceTarget() {
		if ((_reference != null) && (_reference.target().length() > 0)) {
			return Optional.of(_reference.target());
		}
		return Optional.empty();
	}

	private Optional<Class<?>> getReferenceType() {
		if ((_reference != null) && (_reference.value() != null) && (_reference.value() != Object.class)) {
			return Optional.of(_reference.value());
		}
		return Optional.empty();
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

	public Set<Annotation> getQualifiers() {
		return _annotated.getAnnotations().stream().filter(
			ann -> ann.annotationType().isAnnotationPresent(Qualifier.class)
		).collect(Collectors.toSet());
	}

	public Class<?> getServiceType() {
		return _serviceType;
	}

	public String getTarget() {
		return _targetFilter;
	}

	public boolean dynamic() {
		return _dynamic;
	}

	public boolean optional() {
		return _optional;
	}

	public ExtendedReferenceTemplateDTO toDTO() {
		ExtendedReferenceTemplateDTO dto = new ExtendedReferenceTemplateDTO();
		dto.beanClass = _beanClass;
		dto.collectionType = _collectionType;
		dto.declaringClass = _declaringClass;
		dto.injectionPointType = _injectionPointType;
		dto.maximumCardinality = _multiplicity;
		dto.minimumCardinality = (_multiplicity == MaximumCardinality.ONE) ? (_optional?0:1) : (0);
		dto.name = _name;
		dto.policy = (_dynamic) ? ReferencePolicy.DYNAMIC : ReferencePolicy.STATIC;
		dto.policyOption = (_greedy) ? ReferencePolicyOption.GREEDY: ReferencePolicyOption.RELUCTANT;
		dto.serviceClass = _serviceType;
		dto.serviceType = _serviceType.getName();
		dto.targetFilter = _targetFilter;

		return dto;
	}

	@Override
	public String toString() {
		if (_string == null) {
			_string = toDTO().toString();
		}
		return _string;
	}

	public boolean unary() {
		return _multiplicity == MaximumCardinality.ONE;
	}

	private String buildFilter() {
		String targetFilter = _referenceTarget.orElse(_emptyFilter);
		boolean filterValid = false;

		int targetFilterLength = targetFilter.length();

		if ((targetFilterLength > 0) && isValid(targetFilter)) {
			filterValid = true;
		}

		StringBuilder sb = new StringBuilder();

		if (_prototype && filterValid) {
			sb.append("(&");
		}

		if (_prototype) {
			sb.append("(");
			sb.append(Constants.SERVICE_SCOPE);
			sb.append("=");
			sb.append(Constants.SCOPE_PROTOTYPE);
			sb.append(")");
		}

		if (filterValid) {
			sb.append(targetFilter);
		}

//		for (Annotation qualifier : getQualifiers()) {
//			Class<? extends Annotation> annotationType = qualifier.annotationType();
//
//			if (annotationType.equals(Reference.class) ||
//				annotationType.equals(Prototype.class)) {
//
//				// TODO filter out blacklisted qualifiers
//
//				continue;
//			}
//
//			Map<String, String> map = Conversions.convert(qualifier).sourceAs(qualifier.annotationType()).to(_mapType);
//
//			Maps.appendFilter(sb, map);
//		}

		if (_prototype && filterValid) {
			sb.append(")");
		}

		return sb.toString();
	}

	private String calculateName(Class<?> service, Annotated annotated) {
		Named named = annotated.getAnnotation(Named.class);

		if (named != null) {
			if (named.value() == null | named.value().equals("")) {
				throw new IllegalArgumentException(
					"It's illegal to specify @Name without specifying a value with @Reference: " +
						annotated + " on " + _annotated);
			}
			return named.value();
		}

		String prefix = _declaringClass.getName() + ".";

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
		else if (annotated instanceof AnnotatedMethod) {
			AnnotatedMethod<?> method = (AnnotatedMethod<?>)annotated;

			return prefix + method.getJavaMember().getName();
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
					"The service type must not be generic: " + type + " on " + _annotated);
			}

			Class<?> clazz = cast(type);

			if (Map.class == clazz) {
				throw new IllegalArgumentException(
					"Map must specify a generic type arguments: " + clazz);
			}
			else if (Map.Entry.class == clazz) {
				throw new IllegalArgumentException(
					"Map.Entry must specify a generic type arguments: " + clazz);
			}
			else if ((BeanServiceObjects.class == clazz) && !_referenceType.isPresent()) {
				throw new IllegalArgumentException(
					"ReferenceServiceObjects must specify a generic type argument: " + clazz);
			}
			else if ((ServiceReference.class == clazz) && !_referenceType.isPresent()) {
				throw new IllegalArgumentException(
					"ServiceReference must specify a generic type argument: " + type);
			}
			else if ((Collection.class == clazz || Iterable.class == clazz || List.class == clazz) &&
					!_referenceType.isPresent()) {

				throw new IllegalArgumentException(
					type + " must specify a generic type argument");
			}
			else if (BeanServiceObjects.class == clazz) {
				_collectionType = CollectionType.SERVICEOBJECTS;
				return;
			}
			else if (ServiceReference.class == clazz) {
				_collectionType = CollectionType.REFERENCE;
				return;
			}
			else if (Collection.class == clazz || Iterable.class == clazz || List.class == clazz) {
				_collectionType = CollectionType.SERVICE;
				_multiplicity = MaximumCardinality.MANY;
				_optional = true;
				return;
			}

			_serviceType = clazz;

			return;
		}

		ParameterizedType parameterizedType = cast(type);

		Type rawType = parameterizedType.getRawType();

		Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

		Type argument = actualTypeArguments[0];

		if (Instance.class == cast(rawType)) {
			throw new IllegalArgumentException(
				"Instance<T> is not supported with @Reference: " + type);
		}

		if (BindObject.class.isAssignableFrom(cast(rawType))) {
			_collectionType = CollectionType.BINDER_OBJECT;
			_dynamic = true;
			_multiplicity = MaximumCardinality.MANY;
			_optional = true;
			_greedy = true;

			if (argument instanceof WildcardType ||
				argument instanceof ParameterizedType) {

				throw new IllegalArgumentException(
					"Type argument <S> of BindObject must not be generic: " + argument);
			}

			_serviceType = cast(argument);

			return;
		}

		if (BindServiceReference.class.isAssignableFrom(cast(rawType))) {
			_collectionType = CollectionType.BINDER_REFERENCE;
			_dynamic = true;
			_multiplicity = MaximumCardinality.MANY;
			_optional = true;
			_greedy = true;

			if (argument instanceof WildcardType ||
				argument instanceof ParameterizedType) {

				throw new IllegalArgumentException(
					"Type argument <S> of BindServiceReference must not be generic: " + argument);
			}

			_serviceType = cast(argument);

			return;
		}

		if (BindServiceObjects.class.isAssignableFrom(cast(rawType))) {
			_collectionType = CollectionType.BINDER_SERVICE_OBJECTS;
			_dynamic = true;
			_multiplicity = MaximumCardinality.MANY;
			_optional = true;
			_greedy = true;

			if (argument instanceof WildcardType ||
				argument instanceof ParameterizedType) {

				throw new IllegalArgumentException(
					"Type argument <S> of BindServiceObjects must not be generic: " + argument);
			}

			_serviceType = cast(argument);

			return;
		}

		if ((!_dynamic) && (Provider.class == cast(rawType))) {
			_dynamic = true;

			calculateServiceType(argument);

			return;
		}

		if ((!_optional) && (Optional.class == cast(rawType))) {
			_optional = true;

			if ((argument instanceof WildcardType) && _referenceType.isPresent()) {
				return;
			}

			calculateServiceType(argument);

			return;
		}

		if ((_multiplicity == MaximumCardinality.ONE) &&
			((Collection.class == cast(rawType)) ||
			(Iterable.class == cast(rawType)) ||
			(List.class == cast(rawType)))) {

			_optional = true;
			_multiplicity = MaximumCardinality.MANY;

			if ((argument instanceof WildcardType) && _referenceType.isPresent()) {
				return;
			}

			calculateServiceType(argument);

			return;
		}

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

			if (!_referenceType.isPresent()) {
				throw new IllegalArgumentException(
					"Maps of properties must specify service type with @Reference.value(): " + argument + " on " + _annotated);
			}

			return;
		}

		if (Map.Entry.class == cast(rawType)) {
			if (!checkKey(argument)) {
				throw new IllegalArgumentException(
					"Tuples must have a key of type Map<String, (? | Object)>: " + argument + " on " + _annotated);
			}

			_collectionType = CollectionType.TUPLE;

			Type second = actualTypeArguments[1];

			if ((second instanceof WildcardType) && _referenceType.isPresent()) {
				return;
			}

			if (!(second instanceof Class)) {
				throw new IllegalArgumentException(
					"The service type must not be generic: " + second + " on " + _annotated);
			}

			_serviceType = cast(second);

			return;
		}

		if (BeanServiceObjects.class == cast(rawType)) {
			_collectionType = CollectionType.SERVICEOBJECTS;

			if ((argument instanceof WildcardType) && _referenceType.isPresent()) {
				return;
			}

			calculateServiceType(argument);

			return;
		}

		if (ServiceReference.class == cast(rawType)) {
			_collectionType = CollectionType.REFERENCE;

			if ((argument instanceof WildcardType) && _referenceType.isPresent()) {
				return;
			}

			calculateServiceType(argument);

			return;
		}

		_serviceType = cast(rawType);

		if (_serviceType.getTypeParameters().length > 0) {
			throw new IllegalArgumentException(
				"Illegal service type: " + argument + " on " + _annotated);
		}
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

	private static final String _emptyFilter = "";

	private final Annotated _annotated;
	private Class<?> _beanClass;
	private CollectionType _collectionType = CollectionType.SERVICE;
	private final Class<?> _declaringClass;
	private boolean _dynamic = false;
	private boolean _greedy = false;
	private final Type _injectionPointType;
	private MaximumCardinality _multiplicity = MaximumCardinality.ONE;
	private final String _name;
	private boolean _optional = false;
	private final boolean _prototype;
	private final Reference _reference;
	private final Optional<Class<?>> _referenceType;
	private final Optional<String> _referenceTarget;
	private Class<?> _serviceType;
	private String _string;
	private final String _targetFilter;
}
