package org.apache.aries.cdi.container.internal.util;

import java.util.Map;

public class Maps {

	private Maps() {
		// no instances
	}

	public static void appendFilter(StringBuilder sb, Map<String, String> map) {
		if (map.isEmpty()) {
			return;
		}

		for (Map.Entry<String, String> entry : map.entrySet()) {
			sb.append("(");
			sb.append(entry.getKey());
			sb.append("=");
			sb.append(entry.getValue());
			sb.append(")");
		}

	}

}
