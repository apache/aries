package org.apache.aries.cdi.container.internal.model;

import org.xml.sax.Attributes;

public class AbstractModel {

	String getValue(String uri, String localName, Attributes attributes) {
		String value = attributes.getValue(uri, localName);

		if (value == null) {
			value = attributes.getValue("", localName);
		}

		if (value != null) {
			value = value.trim();
		}

		return value;
	}

}
