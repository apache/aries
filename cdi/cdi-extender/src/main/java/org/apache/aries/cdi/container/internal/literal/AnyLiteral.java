package org.apache.aries.cdi.container.internal.literal;

import javax.enterprise.inject.Any;
import javax.enterprise.util.AnnotationLiteral;

public class AnyLiteral extends AnnotationLiteral<Any> implements Any {

	private static final long serialVersionUID = 1L;
	public static final Any INSTANCE = new AnyLiteral();

	private AnyLiteral() {
	}

}
