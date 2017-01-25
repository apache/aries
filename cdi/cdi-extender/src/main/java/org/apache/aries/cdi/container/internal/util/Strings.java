package org.apache.aries.cdi.container.internal.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Strings {

	private Strings() {
		// no instances
	}

	public static String camelCase(String name) {
		name = name.replaceFirst("^(.)", Character.toLowerCase(name.charAt(0)) + "");
		Matcher m = PATTERN.matcher(name);
		StringBuffer sb2 = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb2, "." + m.group(0).toLowerCase());
		}
		m.appendTail(sb2);
		return sb2.toString();
	}

	private static final Pattern PATTERN = Pattern.compile("(?<=[a-z])[A-Z]");

}
