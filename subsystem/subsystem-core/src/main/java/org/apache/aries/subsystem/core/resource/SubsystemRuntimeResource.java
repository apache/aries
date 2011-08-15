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

import org.apache.aries.subsystem.core.internal.AriesSubsystem;
import org.osgi.service.coordinator.Coordination;

public class SubsystemRuntimeResource extends AbstractRuntimeResource {
	public SubsystemRuntimeResource(SubsystemResource resource, ResourceListener listener, AriesSubsystem subsystem) {
		super(resource, listener, subsystem);
	}

	@Override
	protected void doInstall(Coordination coordination) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void doStart(Coordination coordination) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	protected void doStop(Coordination coordination) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	protected void doUninstall(Coordination coordination) throws Exception {
		// TODO Auto-generated method stub

	}
}
