/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.subsystem.core.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.aries.subsystem.core.archive.BundleManifest;
import org.apache.aries.subsystem.core.archive.BundleRequiredExecutionEnvironmentHeader;
import org.apache.aries.subsystem.core.archive.BundleSymbolicNameHeader;
import org.apache.aries.subsystem.core.archive.BundleVersionHeader;
import org.apache.aries.subsystem.core.archive.ExportPackageHeader;
import org.apache.aries.subsystem.core.archive.FragmentHostCapability;
import org.apache.aries.subsystem.core.archive.FragmentHostHeader;
import org.apache.aries.subsystem.core.archive.FragmentHostRequirement;
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
import org.osgi.framework.Constants;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.SubsystemException;

public class BundleResource implements Resource, org.apache.aries.subsystem.core.repository.RepositoryContent {
	private static BundleManifest computeManifest(IDirectory directory, IFile content) {
		org.apache.aries.util.manifest.BundleManifest bm = 
				org.apache.aries.util.manifest.BundleManifest.fromBundle(directory);
		if (bm == null) {
			throw new IllegalArgumentException("File \"" + content.getName() + "\" contains no bundle manifest META-INF/MANIFEST.MF.");
		}
		Manifest m = bm.getRawManifest();
		BundleManifest result = new BundleManifest(m);
		if (result.getHeader(Constants.BUNDLE_SYMBOLICNAME) == null) {
			throw new IllegalArgumentException("File \"" + content.getName() + "\" has a META-INF/MANIFEST.MF with no Bundle-SymbolicName header.");
		}
		return result;
	}
	
	private final List<Capability> capabilities = new ArrayList<Capability>();
	private final IFile content;
	private final BundleManifest manifest;
	private final List<Requirement> requirements = new ArrayList<Requirement>();
	
	public BundleResource(IFile content) {
		this.content = content;
		IDirectory dir = content.isDirectory() ? content.convert() : content.convertNested();
		manifest = computeManifest(dir, content);
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
	
	private void computeOsgiWiringBundleCapability() {
		if (manifest.getHeader(org.osgi.framework.Constants.FRAGMENT_HOST) != null) {
	        // The osgi.wiring.bundle capability is not provided by fragments.
	        return;
	    }
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
	
	private void computeOsgiWiringHostCapability() {
	    if (manifest.getHeader(org.osgi.framework.Constants.FRAGMENT_HOST) != null) {
            // The osgi.wiring.host capability is not provided by fragments.
            return;
        }
        BundleSymbolicNameHeader bsnh = (BundleSymbolicNameHeader)manifest.getHeader(BundleSymbolicNameHeader.NAME);
        BundleVersionHeader bvh = (BundleVersionHeader)manifest.getHeader(BundleVersionHeader.NAME);
        capabilities.add(new FragmentHostCapability(bsnh, bvh, this));
    }
	
	private void computeOsgiWiringHostRequirement() {
        FragmentHostHeader fhh = (FragmentHostHeader)manifest.getHeader(FragmentHostHeader.NAME);
        if (fhh != null) {
            requirements.add(new FragmentHostRequirement(fhh.getClauses().iterator().next(), this));
        }
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
	
	private void computeRequirementsAndCapabilities(IDirectory directory) {
		// Compute all requirements and capabilities other than those related
		// to services.
		computeRequirementsOtherThanService();
		computeCapabilitiesOtherThanService();
		// OSGi RFC 201 for R6: The presence of any Require/Provide-Capability
		// clauses in the osgi.service namespace overrides any service related
		// requirements or capabilities that might have been found by other
		// means.
		boolean computeServiceRequirements = getRequirements(ServiceNamespace.SERVICE_NAMESPACE).isEmpty();
		boolean computeServiceCapabilities = getCapabilities(ServiceNamespace.SERVICE_NAMESPACE).isEmpty();
		if (!(computeServiceCapabilities || computeServiceRequirements))
			return;
		// Compute service requirements and capabilities if the optional
		// ModelledResourceManager service is present.
		ServiceModeller modeller = getServiceModeller();
		if (modeller == null)
			return;

		ServiceModeller.ServiceModel model = modeller.computeRequirementsAndCapabilities(this, directory);
		if (computeServiceCapabilities)
			capabilities.addAll(model.getServiceCapabilities());
		if (computeServiceRequirements)
			requirements.addAll(model.getServiceRequirements());
	}
	
	private void computeRequirementsOtherThanService() {
		computeOsgiWiringPackageRequirements();
		computeGenericRequirements();
		computeOsgiWiringBundleRequirements();
		computeOsgiExecutionEnvironmentRequirement();
		computeOsgiWiringHostRequirement();
		computeOsgiWiringHostCapability();
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

	private ServiceModeller getServiceModeller() {
		return Activator.getInstance().getServiceModeller();
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
