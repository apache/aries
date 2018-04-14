package org.apache.aries.cdi.container.internal.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.ObserverMethod;

import org.apache.aries.cdi.container.internal.util.Sets;
import org.osgi.service.cdi.reference.ReferenceEvent;

public class ObserverMethodAnnotated implements Annotated {

	public ObserverMethodAnnotated(ObserverMethod<ReferenceEvent<?>> observerMethod) {
		_observerMethod = observerMethod;
		_qualifiers = _observerMethod.getObservedQualifiers();
	}

	@Override
	public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
		for (Annotation annotation : _qualifiers) {
			if (annotationType.isInstance(annotation)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<Type> getTypeClosure() {
		return Sets.immutableHashSet(getBaseType());
	}

	@Override
	public Type getBaseType() {
		return _observerMethod.getObservedType();
	}

	@Override
	public Set<Annotation> getAnnotations() {
		return _qualifiers;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Annotation> Set<T> getAnnotations(Class<T> annotationType) {
		Set<T> annotations = new HashSet<>();

		for (Annotation annotation : _qualifiers) {
			if (annotationType.isInstance(annotation)) {
				annotations.add((T)annotation);
			}
		}
		return annotations;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		for (Annotation annotation : _qualifiers) {
			if (annotationType.isInstance(annotation)) {
				return (T)annotation;
			}
		}
		return null;
	}

	private final ObserverMethod<ReferenceEvent<?>> _observerMethod;
	private final Set<Annotation> _qualifiers;

}