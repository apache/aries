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
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.aries.subsystem.core.archive.BundleManifest;
import org.apache.aries.subsystem.core.archive.ExportPackageHeader;
import org.apache.aries.subsystem.core.archive.ImportPackageHeader;
import org.apache.aries.subsystem.core.archive.RequireBundleHeader;
import org.apache.aries.subsystem.core.archive.RequireBundleRequirement;
import org.apache.aries.subsystem.core.archive.RequireCapabilityHeader;
import org.apache.aries.subsystem.core.archive.RequireCapabilityRequirement;
import org.apache.aries.subsystem.core.internal.OsgiIdentityCapability;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.RepositoryContent;

public class BundleResource implements Resource, RepositoryContent {
	public static BundleResource newInstance(URL content) throws IOException {
		BundleResource result = new BundleResource(content);
		result.capabilities.add(new OsgiIdentityCapability(result, result.manifest));
		return result;
	}
	
	private final List<Capability> capabilities = new ArrayList<Capability>();
	private final URL content;
	private final BundleManifest manifest;
	private final List<Requirement> requirements = new ArrayList<Requirement>();
	
	private BundleResource(URL content) throws IOException {
		this.content = content;
		JarInputStream jis = new JarInputStream(content.openStream());
		try {
			Manifest manifest = jis.getManifest();
			if (manifest == null)
				throw new IllegalArgumentException("The jar file contained no manifest");
			this.manifest = new BundleManifest(manifest);
		}
		finally {
			try {
				jis.close();
			}
			catch (IOException e) {}
		}
		ExportPackageHeader eph = (ExportPackageHeader)manifest.getHeader(ExportPackageHeader.NAME);
		if (eph != null)
			capabilities.addAll(eph.toCapabilities(this));
		ImportPackageHeader iph = (ImportPackageHeader)manifest.getHeader(ImportPackageHeader.NAME);
		if (iph != null)
			requirements.addAll(iph.getRequirements(this));
		RequireCapabilityHeader rch = (RequireCapabilityHeader)manifest.getHeader(RequireCapabilityHeader.NAME);
		if (rch != null)
			for (RequireCapabilityHeader.Clause clause : rch.getClauses())
				requirements.add(new RequireCapabilityRequirement(clause));
		RequireBundleHeader rbh = (RequireBundleHeader)manifest.getHeader(RequireBundleHeader.NAME);
		if (rbh != null)
			for (RequireBundleHeader.Clause clause : rbh.getClauses())
				requirements.add(new RequireBundleRequirement(clause));
	}
	
	private BundleResource(String content) throws IOException {
		/*
		 * TODO
		 * Capabilities
		 * 		Export-Package
		 * 		Provide-Capability
		 * 		BSN + Version (host)
		 * 		osgi.identity
		 * Requirements
		 * 		Import-Package
		 * 		Require-Bundle
		 * 		Require-Capability
		 * 		Fragment-Host
		 */
		this(new URL(content));
	}

	public List<Capability> getCapabilities(String namespace) {
		ArrayList<Capability> result = new ArrayList<Capability>(capabilities.size());
		for (Capability capability : capabilities) {
			if (namespace == null || namespace.equals(capability.getNamespace()))
				result.add(capability);
		}
		return result;
	}
	
	@Override
	public InputStream getContent() {
		try {
			return content.openStream();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public List<Requirement> getRequirements(String namespace) {
		/* Requirements
		 * 		Import-Package
		 * 		Require-Bundle
		 * 		Require-Capability
		 * 		Fragment-Host
		 */
		ArrayList<Requirement> result = new ArrayList<Requirement>();
		for (Requirement requirement : requirements) {
			if (namespace == null || namespace.equals(requirement.getNamespace()))
				result.add(requirement);
		}
		return result;
	}
}
