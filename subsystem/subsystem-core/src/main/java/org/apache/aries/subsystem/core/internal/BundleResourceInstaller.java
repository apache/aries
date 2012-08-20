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
package org.apache.aries.subsystem.core.internal;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Resource;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.service.subsystem.SubsystemException;

public class BundleResourceInstaller extends ResourceInstaller {
	public BundleResourceInstaller(Coordination coordination, Resource resource, AriesSubsystem subsystem) {
		super(coordination, resource, subsystem);
	}
	
	public Resource install() {
		BundleRevision revision;
		if (resource instanceof BundleRevision)
			revision = (BundleRevision)resource;
		else {
			ThreadLocalSubsystem.set(provisionTo);
			revision = installBundle();
		}
		addReference(revision);
		addConstituent(revision);
		return revision;
	}
	
	private BundleRevision installBundle() {
		final Bundle bundle;
		try {
			bundle = provisionTo.getRegion().installBundle(getLocation(), ((RepositoryContent)resource).getContent());
		}
		catch (BundleException e) {
			throw new SubsystemException(e);
		}
		coordination.addParticipant(new Participant() {
			public void ended(Coordination coordination) throws Exception {
				// Nothing
			}

			public void failed(Coordination coordination) throws Exception {
				provisionTo.getRegion().removeBundle(bundle);
			}
		});
		return bundle.adapt(BundleRevision.class);
	}
}
