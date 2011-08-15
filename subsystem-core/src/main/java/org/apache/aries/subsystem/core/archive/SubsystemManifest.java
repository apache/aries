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
import java.util.Collection;

import org.osgi.framework.Version;
import org.osgi.framework.wiring.Capability;
import org.osgi.framework.wiring.Resource;
import org.osgi.framework.wiring.ResourceConstants;
import org.osgi.service.subsystem.SubsystemException;

public class SubsystemManifest extends Manifest {
	public static SubsystemManifest newInstance(String symbolicName, Version version, Collection<Resource> resources) {
		if (resources.isEmpty())
			throw new SubsystemException("A subsystem must have content");
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
		// Remove the trailing comma.
		content.deleteCharAt(content.length() - 1);
		manifest.headers.put(SubsystemContentHeader.NAME, new SubsystemContentHeader(content.toString()));
		return manifest;
	}

	public SubsystemManifest(InputStream in) throws IOException {
		super(in);
	}

	public SubsystemManifest(File file) throws IOException {
		super(file);
	}

	private SubsystemManifest() {}
	
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
