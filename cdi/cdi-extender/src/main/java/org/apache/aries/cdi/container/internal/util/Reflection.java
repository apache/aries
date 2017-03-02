package org.apache.aries.cdi.container.internal.util;

public class Reflection {

	private Reflection() {
		// no instances
	}

	@SuppressWarnings("unchecked")
	public static <T> T cast(Object obj) {
		return (T) obj;
	}

}
