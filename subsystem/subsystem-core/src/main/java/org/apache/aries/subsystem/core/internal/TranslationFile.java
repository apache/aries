package org.apache.aries.subsystem.core.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class TranslationFile {
	private static String normalizeName(String name) {
		int index = name.lastIndexOf('/');
		if (index == -1)
			return name;
		return name.substring(index + 1);
	}
	
	private final String name;
	private final Properties properties;
	
	public TranslationFile(String name, Properties properties) {
		if (name == null || properties == null)
			throw new NullPointerException();
		if (name.isEmpty())
			throw new IllegalArgumentException();
		this.name = normalizeName(name);
		this.properties = properties;
	}
	
	public void write(File directory) throws IOException {
		FileOutputStream fos = new FileOutputStream(new File(directory, name));
		try {
			properties.store(fos, null);
		}
		finally {
			fos.close();
		}
	}
}
