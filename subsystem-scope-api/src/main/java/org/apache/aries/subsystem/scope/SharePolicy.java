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

import org.osgi.framework.Filter;

/**
 * A share policy is used to control what capabilities
 * are imported and exported from a scope.
 */
public class SharePolicy {
	/**
	 * A type of share policy for importing capabilities
	 * into a scope.
	 */
	public static final String TYPE_IMPORT = "IMPORT";

	/**
	 * A type of share policy for exporting capabilities
	 * out of a scope.
	 */
	public static final String TYPE_EXPORT = "EXPORT";

	private final String type;
	private final String namespace;
	private final Filter filter;

	/**
	 * Constructs a new share policy.
	 * @param type the type of share policy.  Must be either
	 * {@link #TYPE_IMPORT IMPORT} or {@link #TYPE_EXPORT
	 * EXPORT}.
	 * @param namespace the name space of the capability this policy controls.
	 * @param filter the filter for matching capabilities this policy controls.
	 */
	public SharePolicy(String type, String namespace, Filter filter) {
		this.type = type;
		this.namespace = namespace;
		this.filter = filter;
	}

	/**
	 * Returns the type of this policy.
	 * @return the type of this policy.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns the name space of the capability this policy controls.
	 * @return the name space of the capability this policy controls.
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * Returns the filter for matching capabilities this policy controls.
	 * @return the filter for matching capabilities this policy controls.
	 */
	public Filter getFilter() {
		return filter;
	}
}
