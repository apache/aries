package org.apache.aries.cdi.container.internal.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.aries.cdi.container.internal.literal.AnyLiteral;
import org.apache.aries.cdi.container.internal.literal.DefaultLiteral;
import org.apache.aries.cdi.container.internal.literal.ReferenceLiteral;
import org.apache.aries.cdi.container.internal.util.Sets;

public class ReferenceInjectionPoint implements InjectionPoint {

	public ReferenceInjectionPoint(Class<?> beanClass, String target) {
		_beanClass = beanClass;
		_qualifiers = Sets.hashSet(DefaultLiteral.INSTANCE, AnyLiteral.INSTANCE, ReferenceLiteral.fromTarget(target));
	}

	@Override
	public Type getType() {
		return _beanClass;
	}

	@Override
	public Set<Annotation> getQualifiers() {
		return _qualifiers;
	}

	@Override
	public Bean<?> getBean() {
		return null;
	}

	@Override
	public Member getMember() {
		return null;
	}

	@Override
	public Annotated getAnnotated() {
		return null;
	}

	@Override
	public boolean isDelegate() {
		return false;
	}

	@Override
	public boolean isTransient() {
		return false;
	}

	private final Class<?> _beanClass;
	private final Set<Annotation> _qualifiers;

}