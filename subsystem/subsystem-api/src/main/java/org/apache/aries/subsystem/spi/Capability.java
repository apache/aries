/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.spi;

import java.util.Map;

/**
 * A capability that has been declared from a {@link Resource}.
 * 
 * @ThreadSafe
 * @version $Id: bf96ffda6b06297b308c208afe3d2136b4dda2f1 $
 */
public interface  Capability {
  
	/**
	 * Returns the name space of this capability.
	 * 
	 * @return The name space of this capability.
	 */
	String getNamespace();

	/**
	 * Returns the directives of this capability.
	 * 
	 * @return An unmodifiable map of directive names to directive values for
	 *         this capability, or an empty map if this capability has no
	 *         directives.
	 */
	Map<String, String> getDirectives();

	/**
	 * Returns the attributes of this capability.
	 * 
	 * @return An unmodifiable map of attribute names to attribute values for
	 *         this capability, or an empty map if this capability has no
	 *         attributes.
	 */
	Map<String, Object> getAttributes();

	/**
	 * The resource that declares this capability.
	 * 
	 * @return the resource
	 */
	Resource getResource();
}
