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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ModelledResourceManager;
import org.apache.aries.application.modelling.ModellerException;
import org.apache.aries.subsystem.core.archive.BundleManifest;
import org.apache.aries.subsystem.core.archive.BundleSymbolicNameHeader;
import org.apache.aries.subsystem.core.archive.BundleVersionHeader;
import org.apache.aries.subsystem.core.archive.ExportPackageHeader;
import org.apache.aries.subsystem.core.archive.ImportPackageHeader;
import org.apache.aries.subsystem.core.archive.ProvideBundleCapability;
import org.apache.aries.subsystem.core.archive.ProvideCapabilityCapability;
import org.apache.aries.subsystem.core.archive.ProvideCapabilityHeader;
import org.apache.aries.subsystem.core.archive.RequireBundleHeader;
import org.apache.aries.subsystem.core.archive.RequireBundleRequirement;
import org.apache.aries.subsystem.core.archive.RequireCapabilityHeader;
import org.apache.aries.subsystem.core.archive.RequireCapabilityRequirement;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.ICloseableDirectory;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.io.IOUtils;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.service.subsystem.SubsystemException;

public class BundleResource implements Resource, RepositoryContent {
	private static BundleManifest computeManifest(IDirectory directory) {
		return new BundleManifest(org.apache.aries.util.manifest.BundleManifest
				.fromBundle(directory)
				.getRawManifest());
	}
	
	private final List<Capability> capabilities = new ArrayList<Capability>();
	private final URL content;
	private final BundleManifest manifest;
	private final List<Requirement> requirements = new ArrayList<Requirement>();
	
	public BundleResource(URL content) throws IOException, ModellerException {
		this.content = content;
		InputStream is = content.openStream();
		try {
			ICloseableDirectory directory = FileSystem.getFSRoot(is);
			try {
				manifest = computeManifest(directory);
				// TODO Could use ModelledResourceManager.getServiceElements
				// instead. Only the service dependency info is being used
				// right now.
				ModelledResource resource = getModelledResourceManager().getModelledResource(directory);
				computeRequirements(resource);
				computeCapabilities(resource);
			}
			finally {
				IOUtils.close(directory);
			}
		}
		finally {
			// Although FileSystem.getFSRoot ultimately tries to close the
			// provided input stream, it is possible an exception will be thrown
			// before that happens.
			IOUtils.close(is);
		}
	}
	
	public BundleResource(String content) throws IOException, ModellerException {
		this(new URL(content));
	}

	public List<Capability> getCapabilities(String namespace) {
		if (namespace == null)
			return Collections.unmodifiableList(capabilities);
		ArrayList<Capability> result = new ArrayList<Capability>(capabilities.size());
		for (Capability capability : capabilities)
			if (namespace.equals(capability.getNamespace()))
				result.add(capability);
		result.trimToSize();
		return Collections.unmodifiableList(result);
	}
	
	@Override
	public InputStream getContent() {
		try {
			return content.openStream();
		}
		catch (IOException e) {
			throw new SubsystemException(e);
		}
	}

	public List<Requirement> getRequirements(String namespace) {
		if (namespace == null)
			return Collections.unmodifiableList(requirements);
		ArrayList<Requirement> result = new ArrayList<Requirement>(requirements.size());
		for (Requirement requirement : requirements)
			if (namespace.equals(requirement.getNamespace()))
				result.add(requirement);
		result.trimToSize();
		return Collections.unmodifiableList(result);
	}
	
	@Override
	public String toString() {
        return content.toExternalForm();
    }
	
	private void computeCapabilities(ModelledResource resource) {
		computeOsgiIdentityCapability();
		computeOsgiWiringPackageCapabilities();
		computeOsgiWiringBundleCapability();
		computeGenericCapabilities();
		computeOsgiServiceCapabilities(resource);
	}
	
	private void computeGenericCapabilities() {
		ProvideCapabilityHeader pch = (ProvideCapabilityHeader)manifest.getHeader(ProvideCapabilityHeader.NAME);
		if (pch != null)
			for (ProvideCapabilityHeader.Clause clause : pch.getClauses())
				capabilities.add(new ProvideCapabilityCapability(clause, this));
	}
	
	private void computeGenericRequirements() {
		RequireCapabilityHeader rch = (RequireCapabilityHeader)manifest.getHeader(RequireCapabilityHeader.NAME);
		if (rch != null)
			for (RequireCapabilityHeader.Clause clause : rch.getClauses())
				requirements.add(new RequireCapabilityRequirement(clause, this));
	}
	
	private void computeOsgiIdentityCapability() {
		capabilities.add(new OsgiIdentityCapability(this, manifest));
	}
	
	private void computeOsgiServiceCapabilities(ModelledResource resource) {
		Collection<? extends ExportedService> services = resource.getExportedServices();
		for (ExportedService service : services)
			capabilities.add(new BasicCapability.Builder()
					.namespace(ServiceNamespace.SERVICE_NAMESPACE)
					.attribute(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE, new ArrayList<String>(service.getInterfaces()))
					.attributes(service.getServiceProperties())
					.resource(this)
					.build());
	}
	
	private void computeOsgiServiceRequirements(ModelledResource resource) {
		Collection<? extends ImportedService> services = resource.getImportedServices();
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
			requirements.add(new BasicRequirement.Builder()
					.namespace(ServiceNamespace.SERVICE_NAMESPACE)
					.directive(Namespace.REQUIREMENT_FILTER_DIRECTIVE, builder.toString())
					.directive(
							Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE, 
							service.isOptional() ? Namespace.RESOLUTION_OPTIONAL : Namespace.RESOLUTION_MANDATORY)
					.resource(this)
					.build());
		}
	}
	
	private void computeOsgiWiringBundleCapability() {
		// TODO The osgi.wiring.bundle capability should not be provided for fragments. Nor should the host capability.
		BundleSymbolicNameHeader bsnh = (BundleSymbolicNameHeader)manifest.getHeader(BundleSymbolicNameHeader.NAME);
		BundleVersionHeader bvh = (BundleVersionHeader)manifest.getHeader(BundleVersionHeader.NAME);
		capabilities.add(new ProvideBundleCapability(bsnh, bvh, this));
	}
	
	private void computeOsgiWiringBundleRequirements() {
		RequireBundleHeader rbh = (RequireBundleHeader)manifest.getHeader(RequireBundleHeader.NAME);
		if (rbh != null)
			for (RequireBundleHeader.Clause clause : rbh.getClauses())
				requirements.add(new RequireBundleRequirement(clause, this));
	}
	
	private void computeOsgiWiringPackageCapabilities() {
		ExportPackageHeader eph = (ExportPackageHeader)manifest.getHeader(ExportPackageHeader.NAME);
		if (eph != null)
			capabilities.addAll(eph.toCapabilities(this));
	}
	
	private void computeOsgiWiringPackageRequirements() {
		ImportPackageHeader iph = (ImportPackageHeader)manifest.getHeader(ImportPackageHeader.NAME);
		if (iph != null)
			requirements.addAll(iph.toRequirements(this));
	}

	private void computeRequirements(ModelledResource resource) {
		computeOsgiWiringPackageRequirements();
		computeGenericRequirements();
		computeOsgiWiringBundleRequirements();
		computeOsgiServiceRequirements(resource);
		// TODO Bundle-RequiredExecutionEnvironment
	}
	
	private ModelledResourceManager getModelledResourceManager() {
		return Activator.getInstance().getModelledResourceManager();
	}
}
