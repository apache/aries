package org.apache.aries.subsystem.core.archive;

import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

public class FilterDirective extends AbstractDirective {
	public static final String NAME = Constants.FILTER_DIRECTIVE;
	
	public FilterDirective(String value) {
		super(NAME, value);
		try {
			FrameworkUtil.createFilter(value);
		}
		catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Invalid filter: " + value, e);
		}
	}

	public String toString() {
		return new StringBuilder()
		.append(getName())
		.append(":=\"")
		.append(getValue())
		.append('\"')
		.toString();
	}
}
