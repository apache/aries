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

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.core.internal.Activator;
import org.apache.aries.subsystem.core.internal.OsgiIdentityCapability;
import org.apache.aries.subsystem.core.internal.OsgiIdentityRequirement;
import org.apache.aries.subsystem.core.obr.SubsystemEnvironment;
import org.osgi.framework.Version;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.resource.ResourceConstants;
import org.osgi.framework.resource.Wire;
import org.osgi.service.resolver.Resolver;
import org.osgi.service.subsystem.SubsystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeploymentManifest extends Manifest implements Resource {
	public static final String IDENTITY_TYPE = "org.apache.aries.subsystem.manifest.deployment";
	
	private static final Logger logger = LoggerFactory.getLogger(DeploymentManifest.class);
	
	public static DeploymentManifest newInstance(SubsystemManifest manifest, SubsystemEnvironment environment) {
		if (logger.isDebugEnabled())
			logger.debug(LOG_ENTRY, "newInstance", new Object[]{manifest, environment});
		DeploymentManifest result = new DeploymentManifest();
		result.headers.put(ManifestVersionHeader.NAME, manifest.getManifestVersion());
		Collection<Resource> resources = new HashSet<Resource>();
		SubsystemContentHeader contentHeader = manifest.getSubsystemContent();
		if (contentHeader != null) {
			for (SubsystemContentHeader.Content content : contentHeader.getContents()) {
				OsgiIdentityRequirement requirement = new OsgiIdentityRequirement(content.getName(), content.getVersionRange(), content.getType(), false);
				Resource resource = environment.findResource(requirement);
				// If the resource is null, can't continue.
				// TODO Actually, can continue if resource is optional.
				if (resource == null)
					throw new SubsystemException("Resource does not exist: " + requirement);
				resources.add(resource);
			}
			// TODO This does not validate that all content bundles were found.
			Map<Resource, List<Wire>> resolution = Activator.getInstance().getServiceProvider().getService(Resolver.class).resolve(environment, new ArrayList<Resource>(resources), Collections.EMPTY_LIST);
			// TODO Once we have a resolver that actually returns lists of wires, we can use them to compute other manifest headers such as Import-Package.
			Collection<Resource> deployedContent = new HashSet<Resource>();
			Collection<Resource> provisionResource = new HashSet<Resource>();
			for (Resource resource : resolution.keySet()) {
				if (contentHeader.contains(resource))
					deployedContent.add(resource);
				else
					provisionResource.add(resource);
			}
			// Make sure any already resolved content resources are added back in.
			deployedContent.addAll(resources);
			result.headers.put(DeployedContentHeader.NAME, DeployedContentHeader.newInstance(deployedContent));
			if (!provisionResource.isEmpty())
				result.headers.put(ProvisionResourceHeader.NAME, ProvisionResourceHeader.newInstance(provisionResource));
		}
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
		logger.debug(LOG_EXIT, "newInstance", result);
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
	
	@Override
	public List<Capability> getCapabilities(String namespace) {
		List<Capability> result = new ArrayList<Capability>(1);
		if (namespace == null || namespace.equals(ResourceConstants.IDENTITY_NAMESPACE)) {
			OsgiIdentityCapability capability = new OsgiIdentityCapability(
					this,
					// TODO Reusing IDENTITY_TYPE for the symbolic name here.
					// Since there's only one subsystem manifest per subsystem,
					// this shouldn't cause any technical issues. However, it
					// might be best to use the subsystem's symbolic name here.
					// But there are issues with that as well since type is not
					// part of the unique identity.
					IDENTITY_TYPE,
					Version.emptyVersion,
					IDENTITY_TYPE);
			result.add(capability);
		}
		return result;
	}
	
	public DeployedContentHeader getDeployedContent() {
		return (DeployedContentHeader)getHeader(DeployedContentHeader.NAME);
	}
	
	public ProvisionResourceHeader getProvisionResource() {
		return (ProvisionResourceHeader)getHeader(ProvisionResourceHeader.NAME);
	}
	
	@Override
	public List<Requirement> getRequirements(String namespace) {
		return Collections.emptyList();
	}
	
	public SubsystemSymbolicNameHeader getSubsystemSymbolicName() {
		return (SubsystemSymbolicNameHeader)getHeader(SubsystemSymbolicNameHeader.NAME);
	}
	
	public SubsystemVersionHeader getSubsystemVersion() {
		SubsystemVersionHeader result = (SubsystemVersionHeader)getHeader(SubsystemVersionHeader.NAME);
		if (result == null)
			return SubsystemVersionHeader.DEFAULT;
		return result;
	}
}
