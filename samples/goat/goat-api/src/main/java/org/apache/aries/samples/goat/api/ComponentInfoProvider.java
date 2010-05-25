/**
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
package org.apache.aries.samples.goat.api;

import java.util.List;

/**
 * Provides information about components within a model.
 *
 * Good usage practice would be to subscribe a listener .. and THEN call getComponents.. 
 * (doing it the other way round risks leaving a window during which a change could occur, and you not be informed).
 * (doing it this way round, at worst, you'll see an update before you handle getComponents, 
 *  and since an update can be an add, you'll just process it twice) 
 *
 */
public interface ComponentInfoProvider {
	
	/**
	 * Callback interface implemented by users of the ComponentInfoProvider interface, allowing 
	 * notification of changes, or deletions to components they have been informed about.
	 */
	static interface ComponentInfoListener {
		//called to add, or update a component.
		public void updateComponent(ComponentInfo b);
		public void removeComponent(ComponentInfo b);
	};
	
	/**
	 * Gets the current set of 'top level' components in this model.
	 * 
	 * Any nested components are only obtainable via the 'getChildren' method on ComponentInfo.
	 * 
	 * @return
	 */
	List<ComponentInfo> getComponents();
	
	/**
	 * Gets a component for an id previously returned via getComponents, or updateComponent
	 * @param id
	 * @return component, or null if component id is either unknown, or deleted.
	 */
	ComponentInfo getComponentForId(String id);
	
	/**
	 * Add a listener to this Info Provider, to be informed of changes/deletions.
	 * @param listener
	 */
	public void registerComponentInfoListener(ComponentInfoListener listener);
	
	//TODO: unregisterComponentInfoListener ;-) 

}
