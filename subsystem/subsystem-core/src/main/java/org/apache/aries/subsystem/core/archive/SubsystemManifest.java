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
import java.util.List;

import org.apache.aries.subsystem.core.internal.OsgiIdentityCapability;
import org.osgi.framework.Version;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.resource.ResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubsystemManifest extends Manifest implements Resource {
	public static final String IDENTITY_TYPE = "org.apache.aries.subsystem.manifest";
	
	private static final Logger logger = LoggerFactory.getLogger(SubsystemManifest.class);
	
	public static SubsystemManifest newInstance(String symbolicName, Version version, Collection<Resource> resources) {
		if (logger.isDebugEnabled())
			logger.debug(LOG_ENTRY, "newInstance", new Object[]{symbolicName, version, resources});
		SubsystemManifest manifest = new SubsystemManifest();
		manifest.headers.put(SubsystemTypeHeader.NAME, new SubsystemTypeHeader());
		manifest.headers.put(ManifestVersionHeader.NAME, new ManifestVersionHeader());
		manifest.headers.put(SubsystemManifestVersionHeader.NAME, new SubsystemManifestVersionHeader());
		manifest.headers.put(ManifestVersionHeader.NAME, new ManifestVersionHeader());
		manifest.headers.put(SubsystemSymbolicNameHeader.NAME, new SubsystemSymbolicNameHeader(symbolicName));
		manifest.headers.put(SubsystemVersionHeader.NAME, new SubsystemVersionHeader(version.toString()));
		// TODO Leaving out Subsystem-Name and Subsystem-Description.
		// TODO Better way than using StringBuilder? Would require a more robust SubsystemContentHeader in order to fulfill the Header contract.
		StringBuilder content = new StringBuilder();
		for (Resource resource : resources) {
			Capability osgiIdentity = resource.getCapabilities(ResourceConstants.IDENTITY_NAMESPACE).get(0);
			String resourceSymbolicName = (String)osgiIdentity.getAttributes().get(ResourceConstants.IDENTITY_NAMESPACE);
			Version resourceVersion = (Version)osgiIdentity.getAttributes().get(ResourceConstants.IDENTITY_VERSION_ATTRIBUTE);
			String type = (String)osgiIdentity.getAttributes().get(ResourceConstants.IDENTITY_TYPE_ATTRIBUTE);
			content.append(resourceSymbolicName).append(';')
				// TODO Add to constants.
				.append("version").append('=').append(resourceVersion).append(';')
				// TODO Add to constants.
				.append("type").append('=').append(type).append(',');
		}
		if (content.length() != 0) {
			// Remove the trailing comma.
			content.deleteCharAt(content.length() - 1);
			manifest.headers.put(SubsystemContentHeader.NAME, new SubsystemContentHeader(content.toString()));
		}
		logger.debug(LOG_EXIT, "newInstance", manifest);
		return manifest;
	}

	public SubsystemManifest(InputStream in) throws IOException {
		super(in);
	}

	public SubsystemManifest(File file) throws IOException {
		super(file);
	}

	private SubsystemManifest() {}
	
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
	
	@Override
	public List<Requirement> getRequirements(String namespace) {
		return Collections.emptyList();
	}
	
	public SubsystemContentHeader getSubsystemContent() {
		return (SubsystemContentHeader)getHeader(SubsystemContentHeader.NAME);
	}
	
	public SubsystemSymbolicNameHeader getSubsystemSymbolicName() {
		return (SubsystemSymbolicNameHeader)getHeader(SubsystemSymbolicNameHeader.NAME);
	}
	
	public SubsystemTypeHeader getSubsystemType() {
		SubsystemTypeHeader result = (SubsystemTypeHeader)getHeader(SubsystemTypeHeader.NAME);
		if (result == null)
			return SubsystemTypeHeader.DEFAULT;
		return result;
	}
	
	public SubsystemVersionHeader getSubsystemVersion() {
		SubsystemVersionHeader result = (SubsystemVersionHeader)getHeader(SubsystemVersionHeader.NAME);
		if (result == null)
			return SubsystemVersionHeader.DEFAULT;
		return result;
	}
}
