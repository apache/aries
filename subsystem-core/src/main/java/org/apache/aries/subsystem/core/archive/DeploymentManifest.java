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
package org.apache.aries.subsystem.core.archive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.core.internal.Activator;
import org.apache.aries.subsystem.core.internal.OsgiIdentityRequirement;
import org.apache.aries.subsystem.core.obr.SubsystemEnvironment;
import org.osgi.framework.wiring.Requirement;
import org.osgi.framework.wiring.Resource;
import org.osgi.framework.wiring.Wire;

public class DeploymentManifest extends Manifest {
	public static DeploymentManifest newInstance(SubsystemManifest manifest, SubsystemEnvironment environment) {
		DeploymentManifest result = new DeploymentManifest();
		result.headers.put(ManifestVersionHeader.NAME, manifest.getManifestVersion());
		Collection<Requirement> requirements = new ArrayList<Requirement>();
		for (SubsystemContentHeader.Content content : manifest.getSubsystemContent().getContents()) {
			Requirement requirement = OsgiIdentityRequirement.newInstance(content);
			requirements.add(requirement);
		}
		// TODO This does not validate that all content bundles were found.
		Map<Resource, List<Wire>> resolution = Activator.getResolver().resolve(environment, requirements.toArray(new Requirement[requirements.size()]));
		// TODO Once we have a resolver that actually returns lists of wires, we can use them to compute other manifest headers such as Import-Package.
		Collection<Resource> deployedContent = new ArrayList<Resource>();
		Collection<Resource> provisionResource = new ArrayList<Resource>();
		for (Resource resource : resolution.keySet()) {
			if (environment.isContentResource(resource))
				deployedContent.add(resource);
			else
				provisionResource.add(resource);
		}
		result.headers.put(DeployedContentHeader.NAME, DeployedContentHeader.newInstance(deployedContent));
		if (!provisionResource.isEmpty())
			result.headers.put(ProvisionResourceHeader.NAME, ProvisionResourceHeader.newInstance(provisionResource));
		result.headers.put(SubsystemSymbolicNameHeader.NAME, manifest.getSubsystemSymbolicName());
		result.headers.put(SubsystemVersionHeader.NAME, manifest.getSubsystemVersion());
		SubsystemTypeHeader typeHeader = manifest.getSubsystemType();
		result.headers.put(SubsystemTypeHeader.NAME, typeHeader);
		// TODO Add to constants.
		if ("osgi.application".equals(typeHeader.getValue())) {
			// TODO Compute additional headers for an application.
		}
		// TODO Add to constants.
		else if ("osgi.composite".equals(typeHeader.getValue())) {
			// TODO Compute additional headers for a composite. 
		}
		// Features require no additional headers.
		return result;
	}
	
	public DeploymentManifest(File manifestFile) throws IOException {
		super(manifestFile);
	}
	
	public DeploymentManifest(InputStream in) throws IOException {
		super(in);
	}
	
	private DeploymentManifest() {}
	
	public DeploymentManifest(Collection<Resource> deployedContent, Collection<Resource> provisionResource) {
		headers.put(ManifestVersionHeader.NAME, new ManifestVersionHeader("1.0"));
		headers.put(DeployedContentHeader.NAME, DeployedContentHeader.newInstance(deployedContent));
		if (!provisionResource.isEmpty())
			headers.put(ProvisionResourceHeader.NAME, ProvisionResourceHeader.newInstance(provisionResource));
	}
	
	public DeployedContentHeader getDeployedContent() {
		return (DeployedContentHeader)getHeader(DeployedContentHeader.NAME);
	}
	
	public ProvisionResourceHeader getProvisionResource() {
		return (ProvisionResourceHeader)getHeader(ProvisionResourceHeader.NAME);
	}
}
