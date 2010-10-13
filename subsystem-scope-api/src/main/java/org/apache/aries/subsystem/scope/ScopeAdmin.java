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

/**
 * Scope Admin is used to update the scopes for the system.
 * When a bundle obtains a scope admin service it retrieves
 * a scope admin service for the scope the bundle belongs to.
 *
 */
public interface ScopeAdmin {
	/**
	 * The root scope of this scope admin.
	 * @return the root scope of this scope admin
	 */
	Scope getScope();

	/**
	 * Creates a new scope update for updating
	 * the root scope of this scope admin.
	 * @return a new scope update.
	 */
	ScopeUpdate newScopeUpdate();

	/**
	 * Returns the parent scope for the root 
	 * scope of this scope admin.
	 * @return
	 */
	Scope getParentScope();
}
