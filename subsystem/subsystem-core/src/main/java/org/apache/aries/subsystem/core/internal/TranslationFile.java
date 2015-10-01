/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
