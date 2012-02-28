package org.apache.aries.subsystem.core.archive;

import org.osgi.framework.Version;

public class TypedAttribute extends AbstractAttribute {
	public static enum Type {
		Double,
		Long,
		String,
		Version
	}
	
	private static Object parseValue(String value, Type type) {
		switch (type) {
			case Double:
				return Double.valueOf(value);
			case Long:
				return Long.valueOf(value);
			case Version:
				return Version.parseVersion(value);
			default:
				return value;
		}
	}
	
	private final Type type;
	
	public TypedAttribute(String name, String value, String type) {
		this(name, value, Type.valueOf(type));
	}
	
	public TypedAttribute(String name, String value, Type type) {
		super(name, parseValue(value, type));
		this.type = type;
	}
	
	public Type getType() {
		return type;
	}
}
