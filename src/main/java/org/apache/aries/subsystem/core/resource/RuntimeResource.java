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
package org.apache.aries.subsystem.core.resource;

import org.osgi.framework.wiring.Resource;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.subsystem.SubsystemConstants;

/*
 * TODO 
 * (1) fill in missing Javadoc comments.
 * (2) Don't like open(). Implies there needs to be a close() as well. Maybe getContent()?
 * (3) Can this be merged with Resource in OBR?
 */

/**
 * A resource is the representation of a uniquely identified and typed data. 
 * For example, a bundle is represented as a resource with a type {@link 
 * SubsystemConstants#RESOURCE_NAMESPACE_BUNDLE RESOURCE_NAMESPACE_BUNDLE}.
 * 
 * Resources should decide whether or not it's possible to participate within
 * the provided Coordination. This decision might be based on some
 * preprocessing or immediately attempting to carry out the request. If this
 * step fails, instead of throwing an exception, a Resource should fail the
 * Coordination instead of participating and return.
 * 
 * In the typical case, a Resource will immediately attempt to carry out the
 * requested operation. If the operation fails, a Resource must not
 * participate in the Coordination but fail it instead. If the operation 
 * succeeds, a Resource must participate in the Coordination before returning.
 * Participant.ended is used for any necessary cleanup and, as the last thing,
 * a call to the appropriate ResourceListener method occurs. Participant.failed
 * is used to rollback the operation.
 * 
 * However, it may be desirable in some cases, particularly those that are
 * difficult or impossible to rollback, to not carry out the operation until it
 * is known that the Coordination has succeeded within the Participant.ended
 * method. In this case, a Resource would perform any necessary preprocessing,
 * participate in the Coordination, and return without carrying out the
 * operation. Note that if this approach is taken and the operation fails
 * during the Participant.ended method, there will be no opportunity to fail
 * the Coordination and rollback work done by other Resources.
 * 
 * @ThreadSafe
 */
public interface RuntimeResource extends Resource {
	public void install(Coordination coordination) throws Exception;
	
	public void start(Coordination coordination) throws Exception;
	
	public void stop(Coordination coordination) throws Exception;
	
	public void uninstall(Coordination coordination) throws Exception;
}
