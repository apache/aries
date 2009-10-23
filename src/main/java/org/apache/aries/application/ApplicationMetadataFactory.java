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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.application;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.jar.Manifest;

public class ApplicationMetadataFactory {

	private static String implClass = "org.apache.aries.application.impl.ApplicationMetadataImpl";
	
	/**
	 * Obtain an ApplicationMetadata for the supplied Manifest.
	 * 
	 * @param mf The manifest containing application metadata
	 * @return instance of ApplicationMetadata
	 * @throws IllegalArgumentException if the Manifest does not contain application metadata
	 */
	@SuppressWarnings("unchecked")
	public static ApplicationMetadata getApplicationMetadata(Manifest mf) 
	  throws IllegalArgumentException
	{
		try{
			Class impl = Class.forName(implClass);	
			Constructor c = impl.getConstructor(Manifest.class);
			return (ApplicationMetadata)c.newInstance(mf);			
		}catch(ClassNotFoundException e){
			throw new RuntimeException("Unable to find metadata impl class ",e);
		} catch (SecurityException e) {
			throw new RuntimeException("Unable to access metadata impl constructor ",e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Unable to find metadata impl constructor ",e);
		} catch (InstantiationException e) {
			throw new RuntimeException("Unable to create metadata impl constructor ",e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Unable to access metadata impl constructor ",e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Unable to invoke metadata impl constructor ",e);
		}		
	}
}
