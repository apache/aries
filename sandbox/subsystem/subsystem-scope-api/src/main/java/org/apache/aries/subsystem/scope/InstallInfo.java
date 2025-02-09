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
package org.apache.aries.subsystem.scope;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Information for installing a bundle into a {@link ScopeUpdate scope
 * update}.
 */
public class InstallInfo {
	private final InputStream content;
	private final String location;
	
	/**
	 * Constructor for a bundle install info.
	 * @param content the content of the bundle.
	 * @param location the location of the bundle.
	 */
	public InstallInfo(String location, URL content) throws IOException {
		this(location == null ? content.toExternalForm() : location, content.openStream());
	}
	
	   /**
     * Constructor for a bundle install info.
     * @param content the content of the bundle.
     * @param location the location of the bundle.
     */
    public InstallInfo(String location, InputStream content) {
    	if (location == null || location.length() == 0)
    		throw new IllegalArgumentException("Missing required parameter: location");
    	if (content == null)
    		throw new NullPointerException("Missing required parameter: content");
        this.location = location;
        this.content = content;
    }

	/**
	 * Returns a url to the content of the bundle to install.
	 * @return a url to the content of the bundle to install.
	 */
	public InputStream getContent() {
		return content;
	}

	/**
	 * Returns the location to use for bundle installation.
	 * @return the location to use for bundle installation.
	 */
	public String getLocation() {
		return location;
	}
}
