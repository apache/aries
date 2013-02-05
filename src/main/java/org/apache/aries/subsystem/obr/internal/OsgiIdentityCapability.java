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
package org.apache.aries.subsystem.obr.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;

public class OsgiIdentityCapability extends AbstractCapability {
	private final Map<String, Object> attributes = new HashMap<String, Object>();
	private final Resource resource;
	
	public OsgiIdentityCapability(Resource resource, String symbolicName, Version version) {
		this(resource, symbolicName, version, IdentityNamespace.TYPE_BUNDLE);
	}
	
	public OsgiIdentityCapability(Resource resource, String symbolicName, Version version, String identityType) {
		this.resource = resource;
		attributes.put(
				IdentityNamespace.IDENTITY_NAMESPACE, 
				symbolicName);
		attributes.put(
				IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, 
				version);
		attributes.put(
				IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, 
				identityType);
		// TODO Add directives, particularly "effective" and "singleton".
	}

	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	public Map<String, String> getDirectives() {
		return Collections.emptyMap();
	}

	public String getNamespace() {
		return IdentityNamespace.IDENTITY_NAMESPACE;
	}

	public Resource getResource() {
		return resource;
	}
}
