package org.apache.aries.subsystem.core.archive;

import java.util.Collection;
import java.util.Collections;

public class AriesSubsystemLocationHeader implements Header<Clause> {
	public static final String NAME = "AriesSubsystem-Location";
	
	private final String value;

	public AriesSubsystemLocationHeader(String value) {
		if (value == null) {
			throw new NullPointerException();
		}
		this.value = value;
	}

	@Override
	public Collection<Clause> getClauses() {
		return Collections.<Clause>singleton(
				new Clause() {
					@Override
					public Attribute getAttribute(String name) {
						return null;
					}

					@Override
					public Collection<Attribute> getAttributes() {
						return Collections.emptyList();
					}

					@Override
					public Directive getDirective(String name) {
						return null;
					}

					@Override
					public Collection<Directive> getDirectives() {
						return Collections.emptyList();
					}

					@Override
					public Parameter getParameter(String name) {
						return null;
					}

					@Override
					public Collection<Parameter> getParameters() {
						return Collections.emptyList();
					}

					@Override
					public String getPath() {
						return value;
					}
				});
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return value;
	}
}
