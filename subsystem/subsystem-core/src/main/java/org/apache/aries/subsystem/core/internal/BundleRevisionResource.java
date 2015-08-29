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
	private final List<Capability> serviceCapabilities;
	private final List<Requirement> serviceRequirements;

	public BundleRevisionResource(BundleRevision revision) {
		if (revision == null)
			throw new NullPointerException();
		this.revision = revision;
		ServiceModeller.ServiceModel model = null;
		boolean gotModel = false;
        List<Capability> capabilities = revision.getCapabilities(ServiceNamespace.SERVICE_NAMESPACE);
        // OSGi RFC 201 for R6: The presence of any Provide-Capability clauses
        // in the osgi.service namespace overrides any service related
        // capabilities that might have been found by other means.
        if (capabilities.isEmpty()) {
            model = getModel();
            gotModel = true;
            if (model != null) {
                capabilities = model.getServiceCapabilities();
            }
        }
        serviceCapabilities = capabilities;
        List<Requirement> requirements = revision.getRequirements(ServiceNamespace.SERVICE_NAMESPACE);
        // OSGi RFC 201 for R6: The presence of any Require-Capability clauses
        // in the osgi.service namespace overrides any service related
        // requirements that might have been found by other means.
        if (requirements.isEmpty()) {
            if (model == null && !gotModel) {
                model = getModel();
            }
            if (model != null) {
                requirements = model.getServiceRequirements();
            }
        }
        serviceRequirements = requirements;
	}

	@Override
	public List<Capability> getCapabilities(String namespace) {
	    if (ServiceNamespace.SERVICE_NAMESPACE.equals(namespace)) {
	        return Collections.unmodifiableList(serviceCapabilities);
	    }
	    List<Capability> revisionCapabilities = revision.getCapabilities(namespace);
	    if (namespace == null) {
	        List<Capability> result = new ArrayList<Capability>(revisionCapabilities.size() + serviceCapabilities.size());
	        result.addAll(revisionCapabilities);
	        result.addAll(serviceCapabilities);
	        return Collections.unmodifiableList(result);
	    }
	    return revisionCapabilities;
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
	    if (ServiceNamespace.SERVICE_NAMESPACE.equals(namespace)) {
	        return Collections.unmodifiableList(serviceRequirements);
	    }
	    List<Requirement> revisionRequirements = revision.getRequirements(namespace);
	    if (namespace == null) {
	        List<Requirement> result = new ArrayList<Requirement>(revisionRequirements.size() + serviceRequirements.size());
            result.addAll(revisionRequirements);
            result.addAll(serviceRequirements);
            return Collections.unmodifiableList(result);
	    }
	    return revisionRequirements;
	}
	
	private ServiceModeller.ServiceModel getModel() {
	    Activator activator = Activator.getInstance();
	    ServiceModeller modeller = activator.getServiceModeller();
	    if (modeller == null) {
            return null;
        }
	    ServiceModeller.ServiceModel model = modeller.computeRequirementsAndCapabilities(this,
                new BundleDirectory(revision.getBundle()));
	    return model;
	}
}
