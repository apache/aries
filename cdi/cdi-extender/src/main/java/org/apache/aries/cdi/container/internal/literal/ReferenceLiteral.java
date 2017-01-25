package org.apache.aries.cdi.container.internal.literal;

import javax.enterprise.util.AnnotationLiteral;

import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.ReferenceScope;

public class ReferenceLiteral extends AnnotationLiteral<Reference> implements Reference {

	private static final long serialVersionUID = 1L;
	public static final Reference INSTANCE = new ReferenceLiteral();

	@Override
	public ReferenceScope scope() {
		return ReferenceScope.BUNDLE;
	}

	@Override
	public String target() {
		return "";
	}

	@Override
	public Class<?> service() {
		return Object.class;
	}

}
