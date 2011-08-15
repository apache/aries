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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;

import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.Capability;
import org.osgi.framework.wiring.Resource;
import org.osgi.framework.wiring.ResourceConstants;

public class OsgiIdentityCapability implements Capability {
	private final Map<String, Object> attributes = new HashMap<String, Object>();
	private final Resource resource;
	
	public OsgiIdentityCapability(Resource resource, String symbolicName) {
		this(resource, symbolicName, Version.emptyVersion);
	}
	
	public OsgiIdentityCapability(Resource resource, String symbolicName, Version version) {
		this(resource, symbolicName, version, ResourceConstants.IDENTITY_TYPE_BUNDLE);
	}
	
	public OsgiIdentityCapability(Resource resource, String symbolicName, Version version, String type) {
		this.resource = resource;
		attributes.put(
				ResourceConstants.IDENTITY_NAMESPACE, 
				symbolicName);
		attributes.put(
				ResourceConstants.IDENTITY_VERSION_ATTRIBUTE, 
				version);
		attributes.put(
				ResourceConstants.IDENTITY_TYPE_ATTRIBUTE, 
				type);
		// TODO Add directives, particularly "effective" and "singleton".
	}
	
	public OsgiIdentityCapability(Resource resource, SubsystemManifest manifest) {
		this(
				resource,
				manifest.getSubsystemSymbolicName().getSymbolicName(),
				manifest.getSubsystemVersion().getVersion(),
				manifest.getSubsystemType().getValue());
	}
	
	public OsgiIdentityCapability(Resource resource, Manifest manifest) {
		this(
				resource,
				manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME),
				Version.parseVersion(manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION)),
				ResourceConstants.IDENTITY_TYPE_BUNDLE);
	}

	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	public Map<String, String> getDirectives() {
		return Collections.emptyMap();
	}

	public String getNamespace() {
		return ResourceConstants.IDENTITY_NAMESPACE;
	}

	public Resource getResource() {
		return resource;
	}
}
