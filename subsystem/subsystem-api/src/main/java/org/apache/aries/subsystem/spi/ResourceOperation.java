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

import org.osgi.service.coordinator.Coordination;

/**
 * A resource operation serves as a context for a resource processor while
 * processing a resource.
 * 
 *@ThreadSafe
 *@noimplement
 */
public interface ResourceOperation {
	/**
	 * The type of operation to perform on the resource.
	 */
	public static enum Type {
		/**
		 * Install the resource.
		 */
		INSTALL,
		/**
		 * Start the resource.
		 */
		START,
		/**
		 * Stop the resource.
		 */
		STOP,
		/**
		 * Uninstall the resource.
		 */
		UNINSTALL,
		/**
		 * Update the resource.
		 */
		UPDATE
	}
	/**
	 * Called by the resource processor once the operation has successfully and 
	 * fully completed. This would typically be called as the very last thing 
	 * within the Participant.ended method. Calling this method will result in 
	 * an appropriate event notification that the resource has been
	 * successfully processed.
	 */
	public void completed();
	/**
	 * This resource operation is taking place within a larger context that 
	 * potentially involves many other operations on other resources as part of
	 * a subsystem operation. Resource processors are required to participate 
	 * within the coordination controlling the overall process in which this 
	 * resource operation plays a part.
	 * @return The coordination in which the resource processor must 
	 *         participate.
	 */
	public Coordination getCoordination();

	/**
	 * A resource operation is associated with a particular resource.
	 * @return The resource on which this operation must be performed.
	 */
	public Resource getResource();

	/**
	 * 
	 * @return
	 */
	public Map<String, Object> getContext();
	/**
	 * A resource operation is associated with a type that defines the
	 * processing required on the resource.
	 * @return The type of resource operation.
	 */
	public Type getType();
}
