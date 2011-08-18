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

import java.io.IOException;
import java.net.URL;

import org.apache.aries.subsystem.core.internal.AriesSubsystem;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.Resource;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;

public class BundleRuntimeResource extends AbstractRuntimeResource {	
	private volatile Bundle bundle;
	
	public BundleRuntimeResource(Resource resource, ResourceListener listener, AriesSubsystem subsystem) {
		super(resource, listener, subsystem);
	}
	
	public Bundle getBundle() {
		return bundle;
	}

	@Override
	protected void doInstall(Coordination coordination) throws BundleException, IOException {
		AriesSubsystem subsystem = this.subsystem;
		URL content = subsystem.getEnvironment().getContent(resource);
		if (subsystem.isTransitive(resource)) {
			// Transitive dependencies should be provisioned into the highest possible level.
			// Transitive dependencies become constituents of the susbsytem into which they were provisioned.
			// TODO Assumes root is always the appropriate level.
			while (subsystem.getParent() != null)
				subsystem = subsystem.getParent();
			subsystem.addConstituent(this);
		}
		else if (subsystem.isFeature()) {
			// Feature resources should be provisioned into the first parent that's not a feature.
			// Feature resources become constituents of the feature.
			while (subsystem.getRegion() == null)
				subsystem = subsystem.getParent();
			this.subsystem.addConstituent(this);
		}
		else {
			// Application and composite resources are provisioned into the application or composite;
			// Application and composite resources become constituents of the application or composite.
			subsystem.addConstituent(this);
		}
		String location = subsystem.getSubsystemId() + '@' + subsystem.getSymbolicName() + '@' + content;
		bundle = subsystem.getRegion().installBundle(location, content.openStream());
		if (coordination == null)
			return;
		coordination.addParticipant(new Participant() {
			public void ended(Coordination coordination) throws Exception {
				// noop
			}

			public void failed(Coordination coordination) throws Exception {
				// TODO Remove the constituent?
				bundle.uninstall();
			}
		});
	}
	
	@Override
	protected void doStart(Coordination coordination) throws BundleException {
		bundle.start(Bundle.START_TRANSIENT);
		if (coordination == null)
			return;
		coordination.addParticipant(new Participant() {
			public void ended(Coordination coordination) throws Exception {
				// noop
			}

			public void failed(Coordination coordination) throws Exception {
				bundle.stop();
			}
		});
	}
	
	@Override
	protected void doStop(Coordination coordination) throws BundleException {
		if (coordination == null) {
			bundle.stop();
			return;
		}
		coordination.addParticipant(new Participant() {
			public void ended(Coordination coordination) throws Exception {
				bundle.stop();
			}

			public void failed(Coordination coordination) throws Exception {
				// noop
			}
		});
	}

	@Override
	protected void doUninstall(Coordination coordination) throws BundleException {
		if (coordination == null) {
			bundle.uninstall();
			return;
		}
		coordination.addParticipant(new Participant() {
			public void ended(Coordination coordination) throws Exception {
				bundle.uninstall();
			}

			public void failed(Coordination coordination) throws Exception {
				// noop
			}
		});
	}
}
