package org.apache.aries.util.filesystem.impl;

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
 *
 * Implementation for this class was based on org.apache.commons.io.Java7Support
 * from the Apache commons-io library which is licensed under Apache License, version 2
 * https://github.com/apache/commons-io
 *
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * The only modifications to the original code are accessibility changes of methods and
 * the removal of unused methods.
 */

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class Java7Support {

	private static final boolean IS_JAVA7;

	private static Method isSymbolicLink;

	private static Method toPath;

	static {
		boolean isJava7x = true;
		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			Class<?> files = cl.loadClass("java.nio.file.Files");
			Class<?> path = cl.loadClass("java.nio.file.Path");
			isSymbolicLink = files.getMethod("isSymbolicLink", path);
			toPath = File.class.getMethod("toPath");
		} catch (ClassNotFoundException e) {
			isJava7x = false;
		} catch (NoSuchMethodException e){
			isJava7x = false;
		}
		IS_JAVA7 = isJava7x;
	}

	/**
	 * Invokes java7 isSymbolicLink
	 * @param file The file to check
	 * @return true if the file is a symbolic link
	 */
	static boolean isSymLink(File file) {
		try {
			Object path = toPath.invoke(file);
			Boolean result = (Boolean) isSymbolicLink.invoke(null, path);
			return result.booleanValue();
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch(InvocationTargetException e){
			throw new RuntimeException(e);
		}
	}

	/**
	 * Indicates if the current vm has java7 lubrary support
	 * @return true if java7 library support
	 */
	static boolean isAtLeastJava7() {
		return IS_JAVA7;
	}

}