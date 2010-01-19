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

import org.osgi.framework.Filter;

/**
 * Represents a service imported or exported by an Aries application. 
 * @see <a href="http://incubator.apache.org/aries/applications.html">
 * http://incubator.apache.org/aries/applications.html</a>. 
 */
public interface ServiceDeclaration {

	/**
	 * get the interface name for the service
	 * @return The name of the service's interface class. 
	 */
	public abstract String getInterfaceName();

	/**
	 * get the filter for the service
	 * @return the filter for the service or null if there is no filter defined
	 */
	public abstract Filter getFilter();

}
