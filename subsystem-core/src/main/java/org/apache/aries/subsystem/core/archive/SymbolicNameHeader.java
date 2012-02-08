package org.apache.aries.subsystem.core.archive;

import java.util.regex.Pattern;

public abstract class SymbolicNameHeader extends AbstractHeader {
	public SymbolicNameHeader(String name, String value) {
		super(name, value);
		if (getClauses().size() != 1)
			throw new IllegalArgumentException("Symbolic name headers must have one, and only one, clause: " + getClauses().size());
		if (!Pattern.matches(Grammar.SYMBOLICNAME, getClauses().get(0).getPath()))
			throw new IllegalArgumentException("Invalid symbolic name: " + getClauses().get(0).getPath());
	}
	
	public String getSymbolicName() {
		return getClauses().get(0).getPath();
	}
}
