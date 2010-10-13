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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;

/**
 * A scope is used to issolate a collection of bundles
 * with a sharing policy.  Scopes can be nested as
 * children scopes.
 *
 */
public interface Scope {
	/**
	 * The name of this scope.  A name has no meaning
	 * at runtime and is only for informational purposes.
	 * @return the name of this scope.
	 */
	String getName();

	/**
	 * The collection of bundles contained in this scope.
	 * @return an unmodifiable collection of bundles
	 * contained in this scope.
	 */
	Collection<Bundle> getBundles();

	/**
	 * Returns a map containing the sharing policies for this scope.
	 * The key is the name space of the policy and the value is the
	 * list of policies with the same name space. 
	 * @param type the type of policy to return.  Must be
	 *        of type {@link SharePolicy#TYPE_EXPORT EXPORT} or
	 *        {@link SharePolicy#TYPE_IMPORT IMPORT}.  Any other type
	 *        results in an exception.
	 * @return an unmodifiable map containing the sharing policies of this scope.
	 *         each list value in the map is also unmodifiable.
	 * @throws IllegalArgumentException if the type is not
	 *         {@link SharePolicy#TYPE_EXPORT EXPORT} or
	 *         {@link SharePolicy#TYPE_IMPORT IMPORT}.
	 */
	Map<String, List<SharePolicy>> getSharePolicies(String type);

	/**
	 * Returns the collection of children scopes for this scope.
	 * @return an unmodifiable collection of children scopes.
	 */
	Collection<Scope> getChildren();
	
	/**
	 * Returns the id for the scope
	 * @return   id for the scope
	 */
	long getId();
	
	/**
	 * destoy the scope
	 */
	void destroy();
	
	/**
	 * Returns the install location String of the scope
	 * @return   the install location String of the scope
	 */
	String getLocation();
}
