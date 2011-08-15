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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.apache.aries.subsystem.core.internal.OsgiIdentityCapability;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.Capability;
import org.osgi.framework.wiring.Requirement;
import org.osgi.framework.wiring.Resource;

public class SubsystemResource implements Resource {
	public static SubsystemResource newInstance(File file) throws IOException, URISyntaxException {
		SubsystemResource result = new SubsystemResource(file);
		result.capabilities.add(new OsgiIdentityCapability(result, result.manifest));
		return result;
	}
	
	private final List<Capability> capabilities = new ArrayList<Capability>(1);
	private final SubsystemManifest manifest;
	/*
	 * TODO
	 * Support subsystem archives without a manifest.
	 * Determine whether or not the deployment manifest comes into play here (don't think so).
	 * Capabilities other than osgi.identity (composites only?).
	 * Requirements (applications and composites).
	 */
	private SubsystemResource(File file) throws IOException, URISyntaxException {
		IDirectory directory = FileSystem.getFSRoot(file);
		try {
			IFile manifest = directory.getFile("OSGI-INF/SUBSYSTEM.MF");
			if (manifest == null) {
				Collection<Resource> resources = new ArrayList<Resource>();
				for (IFile f : directory.listFiles()) {
					if (f.isDirectory())
						continue;
					resources.add(new ResourceFactory().newResource(f.toURL()));
				}
				// TODO Need to track how specification standardizes file name parsing for default symbolic name and version.
				this.manifest = SubsystemManifest.newInstance(file.getName(), Version.emptyVersion, resources);
			}
			else {
				this.manifest = new SubsystemManifest(manifest.open());
			}
		}
		finally {
			directory.toCloseable().close();
		}
	}
	
	@Override
	public List<Capability> getCapabilities(String namespace) {
		return Collections.unmodifiableList(capabilities);
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		return Collections.emptyList();
	}

}
