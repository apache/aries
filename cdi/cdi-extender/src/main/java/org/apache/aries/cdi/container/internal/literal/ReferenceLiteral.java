package org.apache.aries.cdi.container.internal.literal;

import javax.enterprise.util.AnnotationLiteral;

import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.ReferenceScope;

public class ReferenceLiteral extends AnnotationLiteral<Reference> implements Reference {

	private static final long serialVersionUID = 1L;
	public static final Reference INSTANCE = new ReferenceLiteral("");

	public static ReferenceLiteral fromTarget(String target) {
		return new ReferenceLiteral(target);
	}

	public ReferenceLiteral(String target) {
		_target = target;
	}

	@Override
	public ReferenceScope scope() {
		return ReferenceScope.BUNDLE;
	}

	@Override
	public String target() {
		return _target;
	}

	@Override
	public Class<?> service() {
		return Object.class;
	}

	private final String _target;

}
