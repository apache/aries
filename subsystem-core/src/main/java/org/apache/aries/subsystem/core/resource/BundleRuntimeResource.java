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
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;

public class BundleRuntimeResource extends AbstractRuntimeResource {	
	private volatile Bundle bundle;
	
	public BundleRuntimeResource(BundleResource resource, ResourceListener listener, AriesSubsystem subsystem) {
		super(resource, listener, subsystem);
	}
	
	public Bundle getBundle() {
		return bundle;
	}

	@Override
	protected void doInstall(Coordination coordination) throws BundleException, IOException {
		URL content = subsystem.getEnvironment().getContent(resource);
		AriesSubsystem subsystem = this.subsystem;
		if (subsystem.isFeature()) {
			// Feature resources should be provisioned into the first parent that's not a feature.
			while (subsystem.getRegion() == null)
				subsystem = subsystem.getParent();
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
