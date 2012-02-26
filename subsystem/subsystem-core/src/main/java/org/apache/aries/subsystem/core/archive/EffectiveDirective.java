package org.apache.aries.subsystem.core.archive;

import org.osgi.framework.Constants;

public class EffectiveDirective extends AbstractDirective {
	public static final String NAME = Constants.EFFECTIVE_DIRECTIVE;
	public static final String VALUE_ACTIVE = Constants.EFFECTIVE_ACTIVE;
	public static final String VALUE_RESOLVE = Constants.EFFECTIVE_RESOLVE;
	
	public static final EffectiveDirective ACTIVE = new EffectiveDirective(VALUE_ACTIVE);
	public static final EffectiveDirective RESOLVE = new EffectiveDirective(VALUE_RESOLVE);
	
	public static EffectiveDirective getInstance(String value) {
		if (VALUE_ACTIVE.equals(value))
			return ACTIVE;
		if (VALUE_RESOLVE.equals(value))
			return RESOLVE;
		return new EffectiveDirective(value);
	}
	
	public EffectiveDirective() {
		this(Constants.EFFECTIVE_RESOLVE);
	}
	
	public EffectiveDirective(String value) {
		super(NAME, value);
	}
	
	public boolean isActive() {
		return ACTIVE == this || Constants.EFFECTIVE_ACTIVE.equals(getValue());
	}
	
	public boolean isResolve() {
		return RESOLVE == this || Constants.EFFECTIVE_RESOLVE.equals(getValue());
	}
}
