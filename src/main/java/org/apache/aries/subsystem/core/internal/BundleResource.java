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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ModelledResourceManager;
import org.apache.aries.application.modelling.ModellerException;
import org.apache.aries.subsystem.core.archive.BundleManifest;
import org.apache.aries.subsystem.core.archive.BundleRequiredExecutionEnvironmentHeader;
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
import org.apache.aries.subsystem.core.archive.RequirementHeader;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;
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
	private final IFile content;
	private final BundleManifest manifest;
	private final List<Requirement> requirements = new ArrayList<Requirement>();
	
	public BundleResource(IFile content) throws ModellerException {
		this.content = content;
		IDirectory dir = content.isDirectory() ? content.convert() : content.convertNested();
		manifest = computeManifest(dir);
		computeRequirementsAndCapabilities(dir);
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
	
	public String getLocation() {
		return getFileName(content);
	}
	
	@Override
	public InputStream getContent() {
		try {
			if (content.isFile())
				return content.open();
			try {
				// Give the IDirectory a shot at opening in case it supports it.
				return content.open();
			}
			catch (UnsupportedOperationException e) {
				// As a last ditch effort, try to jar up the contents.
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				JarOutputStream out = new JarOutputStream(baos, manifest.getManifest());
				try {
					jar(out, "", content.convert());
				}
				finally {
					IOUtils.close(out);
				}
				return new ByteArrayInputStream(baos.toByteArray());
			}
		}
		catch (Exception e) {
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
        return content.toString();
    }
	
	private void computeCapabilitiesOtherThanService() {
		computeOsgiIdentityCapability();
		computeOsgiWiringPackageCapabilities();
		computeOsgiWiringBundleCapability();
		computeGenericCapabilities();
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
	
	private void computeOsgiExecutionEnvironmentRequirement() {
		RequirementHeader<?> header = (RequirementHeader<?>)manifest.getHeader(BundleRequiredExecutionEnvironmentHeader.NAME);
		if (header == null)
			return;
		requirements.addAll(header.toRequirements(this));
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
				builder.append(filter);
			builder.append(')');
			requirements.add(new BasicRequirement.Builder()
					.namespace(ServiceNamespace.SERVICE_NAMESPACE)
					.directive(Namespace.REQUIREMENT_FILTER_DIRECTIVE, builder.toString())
					.directive(
							Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE, 
							service.isOptional() ? Namespace.RESOLUTION_OPTIONAL : Namespace.RESOLUTION_MANDATORY)
					.directive(
							Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE, 
							service.isMultiple() ? Namespace.CARDINALITY_MULTIPLE : Namespace.CARDINALITY_SINGLE)
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
	
	private void computeRequirementsAndCapabilities(IDirectory directory) throws ModellerException {
		computeRequirementsOtherThanService();
		computeCapabilitiesOtherThanService();
		ModelledResourceManager manager = getModelledResourceManager();
		if (manager == null)
			return;
		// TODO Could use ModelledResourceManager.getServiceElements instead. 
		// Only the service dependency info is being used right now.
		ModelledResource resource = manager.getModelledResource(directory);
		computeOsgiServiceRequirements(resource);
		computeOsgiServiceCapabilities(resource);
	}
	
	private void computeRequirementsOtherThanService() {
		computeOsgiWiringPackageRequirements();
		computeGenericRequirements();
		computeOsgiWiringBundleRequirements();
		computeOsgiExecutionEnvironmentRequirement();
	}
	
	private String getFileName(IFile file) {
		String name = file.getName();
		if ("".equals(name)) {
			// The file is the root directory of an archive. Use the URL
			// instead. Using the empty string will likely result in duplicate
			// locations during installation.
			try {
				name = file.toURL().toString();
			}
			catch (MalformedURLException e) {
				throw new SubsystemException(e);
			}
		}
		int index = name.lastIndexOf('/');
		if (index == -1 || index == name.length() - 1)
			return name;
		return name.substring(index + 1);
	}

	private ModelledResourceManager getModelledResourceManager() {
		return Activator.getInstance().getModelledResourceManager();
	}

	private void jar(JarOutputStream out, String prefix, IDirectory directory) throws IOException {
		List<IFile> files = directory.listFiles();
		for (IFile f : files) {        
			String fileName; 
			if (f.isDirectory())
				fileName = prefix + getFileName(f) + "/";
			else
				fileName = prefix + getFileName(f);
			if ("META-INF/".equalsIgnoreCase(fileName) || "META-INF/MANIFEST.MF".equalsIgnoreCase(fileName))
				continue;
			JarEntry entry = new JarEntry(fileName);
			entry.setSize(f.getSize());
			entry.setTime(f.getLastModified());
			out.putNextEntry(entry);
			if (f.isDirectory()) 
				jar(out, fileName, f.convert());
			else
				IOUtils.copy(f.open(), out);
		}
	}
}
