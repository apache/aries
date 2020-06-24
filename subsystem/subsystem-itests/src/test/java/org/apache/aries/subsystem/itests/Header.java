package org.apache.aries.subsystem.itests;

public class Header {
//IC see: https://issues.apache.org/jira/browse/ARIES-1199
	String key;
	String value;
	
	public Header(String key, String value) {
		this.key = key;
		this.value = value;
	}
}
