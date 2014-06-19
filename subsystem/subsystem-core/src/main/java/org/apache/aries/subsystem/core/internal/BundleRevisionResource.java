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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.wiring.BundleRevision;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class BundleRevisionResource implements Resource {
	private final BundleRevision revision;

	public BundleRevisionResource(BundleRevision revision) {
		if (revision == null)
			throw new NullPointerException();
		this.revision = revision;
	}

	@Override
	public List<Capability> getCapabilities(String namespace) {
		if (namespace == null) {
			List<Capability> rCaps = revision.getCapabilities(namespace);
			List<Capability> sCaps = computeServiceCapabilities();
			List<Capability> result = new ArrayList<Capability>(rCaps.size() + sCaps.size());
			result.addAll(rCaps);
			result.addAll(sCaps);
			return Collections.unmodifiableList(result);
		}
		if (ServiceNamespace.SERVICE_NAMESPACE.equals(namespace))
			return Collections.unmodifiableList(computeServiceCapabilities());
		return revision.getCapabilities(namespace);
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		if (namespace == null) {
			List<Requirement> rReqs = revision.getRequirements(namespace);
			List<Requirement> sReqs = computeServiceRequirements();
			List<Requirement> result = new ArrayList<Requirement>(rReqs.size() + sReqs.size());
			result.addAll(rReqs);
			result.addAll(sReqs);
			return Collections.unmodifiableList(result);
		}
		if (ServiceNamespace.SERVICE_NAMESPACE.equals(namespace))
			return Collections.unmodifiableList(computeServiceRequirements());
		return revision.getRequirements(namespace);
	}

	private List<Capability> computeServiceCapabilities() {
        Activator activator = Activator.getInstance();
        ServiceModeller modeller = activator.getServiceModeller();
        if (modeller == null)
            return Collections.emptyList();
        ServiceModeller.ServiceModel model =
                modeller.computeRequirementsAndCapabilities(this, new BundleDirectory(revision.getBundle()));
        return model.getServiceCapabilities();
	}

	private List<Requirement> computeServiceRequirements() {
        Activator activator = Activator.getInstance();
        ServiceModeller modeller = activator.getServiceModeller();
        if (modeller == null)
            return Collections.emptyList();
        ServiceModeller.ServiceModel model =
                modeller.computeRequirementsAndCapabilities(this, new BundleDirectory(revision.getBundle()));
        return model.getServiceRequirements();
	}

}
