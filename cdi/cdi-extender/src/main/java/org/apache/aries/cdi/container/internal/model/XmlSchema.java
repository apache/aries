package org.apache.aries.cdi.container.internal.model;

public enum XmlSchema {

	CDI10("META-INF/cdi.xsd");

	private final String _fileName;

	private XmlSchema(String fileName) {
		_fileName = fileName;
	}

	public String getFileName() {
		return _fileName;
	}

}
