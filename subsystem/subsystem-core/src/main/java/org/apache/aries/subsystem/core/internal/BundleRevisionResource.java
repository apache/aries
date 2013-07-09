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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ModelledResourceManager;
import org.apache.aries.application.modelling.ModellerException;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.SubsystemException;

public class BundleRevisionResource implements Resource {
	private final BundleRevision revision;
	
	private volatile ModelledResource resource;
	
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
	
	private ModelledResource computeModelledResource() {
		Activator activator = Activator.getInstance();
		ModelledResourceManager manager = activator.getModelledResourceManager();
		if (manager == null)
			return null;
		BundleDirectory directory = new BundleDirectory(revision.getBundle());
		try {
			return manager.getModelledResource(directory);
		}
		catch (ModellerException e) {
			throw new SubsystemException(e);
		}
	}
	
	private List<Capability> computeServiceCapabilities() {
		ModelledResource resource = getModelledResource();
		if (resource == null)
			return Collections.emptyList();
		Collection<? extends ExportedService> services = resource.getExportedServices();
		if (services.isEmpty())
			return Collections.emptyList();
		List<Capability> result = new ArrayList<Capability>(services.size());
		for (ExportedService service : services)
			result.add(new BasicCapability.Builder()
					.namespace(ServiceNamespace.SERVICE_NAMESPACE)
					.attribute(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE, new ArrayList<String>(service.getInterfaces()))
					.attributes(service.getServiceProperties())
					.resource(this)
					.build());
		return result;
	}
	
	private List<Requirement> computeServiceRequirements() {
		ModelledResource resource = getModelledResource();
		if (resource == null)
			return Collections.emptyList();
		Collection<? extends ImportedService> services = resource.getImportedServices();
		if (services.isEmpty())
			return Collections.emptyList();
		List<Requirement> result = new ArrayList<Requirement>(services.size());
		for (ImportedService service : services) {
			StringBuilder builder = new StringBuilder("(&(")
					.append(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE)
					.append('=')
					.append(service.getInterface())
					.append(')');
			String filter = service.getFilter();
			if (filter != null)
				builder.append('(').append(filter).append(')');
			builder.append(')');
			result.add(new BasicRequirement.Builder()
					.namespace(ServiceNamespace.SERVICE_NAMESPACE)
					.directive(Namespace.REQUIREMENT_FILTER_DIRECTIVE, builder.toString())
					.directive(
							Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE, 
							service.isOptional() ? Namespace.RESOLUTION_OPTIONAL : Namespace.RESOLUTION_MANDATORY)
					.resource(this)
					.build());
		}
		return result;
	}

	private ModelledResource getModelledResource() {
		ModelledResource result = resource;
		if (result == null) {
			synchronized (this) {
				result = resource;
				if (result == null)
					resource = result = computeModelledResource();
			}
		}
		return result;
	}
}
