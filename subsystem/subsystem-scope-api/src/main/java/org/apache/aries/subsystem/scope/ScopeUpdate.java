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
import org.osgi.framework.BundleException;

/**
 * A scope update represents a snapshot of a scope and its children.
 * The get methods return modifiable collections or maps that represent the
 * bundles contained in the scope, the sharing policies, the
 * children scopes, and optionally what bundles to install.
 * The collections and maps may be modified with changes that will be 
 * committed when the commit method is called.
 */
public interface ScopeUpdate {
	/**
	 * Returns the name of the scope represented by this scope update.
	 * @return the name of the scope.
	 * @see Scope#getName()
	 */
	String getName();
	
	/**
	 * Returns the collection of bundles contained in this scope.
	 * Bundles may be added or removed from this collection.
	 * <p>
	 * Adding a bundle to the collection will add the bundle
	 * to this scope when commit is called.  A bundle
	 * must belong to one and only one scope at a time.
	 * If a bundle is contained in multiple scopes 
	 * when commit is called then the commit will fail
	 * with an exception.
	 * <p>
	 * Removing a bundle from the collection will remove the
	 * bundle from this scope when commit is called.
	 * If a bundle is removed from this collection and is
	 * not added to the collection of another scope then the
	 * bundle will be uninstalled when commit is called.
	 * @return the collection of bundles contained in this scope.
	 */
	Collection<Bundle> getBundles();

	/**
	 * Returns a map containing the sharing policies for this scope.
	 * The key is the name space of the policy and the value is the
	 * list of policies with the same name space.  
	 * <p>
	 * Policies may be removed or added to the lists.  If adding a 
	 * policy then the policy must have the same name space as the 
	 * other policies in the list.  A new name space list may also be 
	 * added to the map.  The same rules apply to lists being 
	 * added, each policy in an added list must have the same 
	 * name space name space key being added.
	 * <p>
	 * The map will be check for validity on commit.  If invalid then
	 * the commit will fail with an exception.
	 * @param type the type of policy to return.  Must be
	 *        of type {@link SharePolicy#TYPE_EXPORT EXPORT} or
	 *        {@link SharePolicy#TYPE_IMPORT IMPORT}.  Any other type
	 *        results in an exception.
	 * @return a map containing the sharing policies of this scope.
	 * @throws IllegalArgumentException if the type is not
	 *         {@link SharePolicy#TYPE_EXPORT EXPORT} or
	 *         {@link SharePolicy#TYPE_IMPORT IMPORT}.
	 */
	Map<String, List<SharePolicy>> getSharePolicies(String type);

	/**
	 * Returns the collection of children scopes.
	 * The children scope updates can be used to update
	 * children scopes when the root scope is committed.
	 * <p>
	 * Note that only the root scope update (the one
	 * returned by {@link ScopeAdmin#createUpdate() createUpdate}
	 * may be used to commit changes.
	 * <p>
	 * Scope updates may be added or removed from this collection.
	 * Adding a scope to the collection will add the scope
	 * to the list of children of this scope when commit is called.
	 * A scope must be a child to one and only one scope at a time
	 * except the scope with id zero, this scope has no parent.
	 * If a scope is a child of multiple scopes 
	 * when commit is called then the commit will fail
	 * with an exception.
	 * <p>
	 * Removing a scope from the list will remove the
	 * scope as a child of this scope when commit is called.
	 * If a scope is removed from this list and is
	 * not added to the children of another scope then the
	 * scope will be uninstalled when commit is called.  
	 * This will result in all bundles and children scopes
	 * of the removed scope to be uninstalled.
	 * @return the collection of children scopes.
	 */
	Collection<ScopeUpdate> getChildren();

	/**
	 * Returns the list of install infos for bundles
	 * that will be installed into this scope when
	 * commit is called.  Initially this list is empty
	 * @return the list of install infos.
	 */
	List<InstallInfo> getBundlesToInstall();

	/**
	 * Creates a new child scope update for this scope.  To
	 * add the returned child scope to this scope the child
	 * scope must be added to the collection 
	 * of children returned by {@link ScopeUpdate#getChildren()
	 * getChildren} before calling {@link #commit() commit} on
	 * this scope update.
	 * @param name the name to assign the new child scope.
	 * @return a scope update for a child scope.
	 */
	ScopeUpdate newChild(String name);
	
	   /**
     * Creates a new child scope update for this scope.  To
     * add the returned child scope to this scope the child
     * scope must be added to the collection 
     * of children returned by {@link ScopeUpdate#getChildren()
     * getChildren} before calling {@link #commit() commit} on
     * this scope update.
     * @param name the name to assign the new child scope.
     * @return a scope update for a child scope.
     */
    ScopeUpdate newChild(String name, String location);

	/**
	 * Commit this update. If no changes have been made to the scopes
	 * since this update was created, then this method will
	 * update the scopes for the system. This method may only be 
	 * successfully called once on this object.
	 * <p>
	 * The following steps will be done to commit this scope:
	 * <ul>
	 *   <li> If this update was not one returned by {@link
	 *   ScopeAdmin#newScopeUpdate()} then an {@link
	 *   UnsupportedOperationException} is thrown.</li>
	 *   <li> If this update is not valid then an
	 *   {@link IllegalStateException} is thrown.
	 *   //TODO need to fill in the details of illegal state
	 *   </li>
	 *   <li> All currently unresolved bundles are disabled from
	 *   resolving until the end of the commit operation.
	 *   </li>
	 *   <li> Any bundle installs or uninstalls are performed.
	 *   Any bundles installed will be disabled from resolving
	 *   until the end of the commit operation.  If a 
	 *   {@link BundleException} is thrown during a bundle install
	 *   or uninstall then the commit operation is terminated and
	 *   the exception is propagated to the caller.  Any bundle operations
	 *   that may have succeeded are left in place and not rolled back.
	 *   </li>
	 *   <li> Scope uninstallation is performed.  If a scope is uninstalled
	 *   then all of its bundles are uninstalled and all of its children
	 *   scopes are uninstalled.  If a {@link BundleException} is thrown
	 *   during a bundle uninstall operation then the commit operation
	 *   is terminated and the exception is propagated to the caller.
	 *   </li>
	 *   <li> Scope installation is performed.  If a {@link BundleException}
	 *   is thrown during a bundle install operation then the commit 
	 *   operation is terminated and the exception is propagated to the
	 *   caller.  Any bundle operations that may have succeeded are left
	 *   in place and not rolled back.
	 *   </li>
	 *   <li> This scope's sharing policy is updated.
	 *   </li>
	 *   <li> Bundles enabled for resolution.  Not this must happen
	 *   even on exception.
	 *   </li>
	 * </ul>
	 * <p>
	 * This method returns <code>false</code> if the commit did not occur
	 * because another scope commit has been performed since the
	 * creation of this update.
	 * 
	 * @return <code>true</code> if the commit was successful.
	 *         <code>false</code> if the commit did not occur because another
	 *         update has been committed since the creation of this update.
	 * @throws SecurityException If the caller does not have the necessary
	 *         permission to perform the update.  For example, if the 
	 *         update involves installing or uninstalling bundles.
	 * @throws IllegalStateException If this update's state is 
	 *         not valid or inconsistent. For example, this update tries to
	 *         place a bundle in multiple scopes.
	 * @throws UnsupportedOperationException If this update was not one
	 *         returned by {@link ScopeAdmin#newScopeUpdate()}.
	 * @throws BundleException if a bundle lifecycle operation failed.
	 */
	boolean commit() throws BundleException;
	
	/**
	 * Returns the children scope to be removed
	 * @return   the to be removed children.   
	 */
	Collection<Scope> getToBeRemovedChildren();
	
	/**
	 * Returns the scope it is updating
	 * @return   the scope it is updating
	 */
	public Scope getScope();
}
