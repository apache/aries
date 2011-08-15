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

import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.Capability;
import org.osgi.framework.wiring.Requirement;
import org.osgi.framework.wiring.Resource;
import org.osgi.framework.wiring.ResourceConstants;
import org.osgi.service.subsystem.SubsystemException;

public class OsgiIdentityRequirement implements Requirement {
	public static OsgiIdentityRequirement newInstance(SubsystemContentHeader.Content content) {
		return new OsgiIdentityRequirement(null, content.getName(), content.getVersion(), content.getType());
	}
	
	private final Map<String, String> directives = new HashMap<String, String>();
	private final Filter filter;
	private final Resource resource;
	
	public OsgiIdentityRequirement(Resource resource, String symbolicName, Version version) {
		this(resource, symbolicName, version, ResourceConstants.IDENTITY_TYPE_BUNDLE);
	}
	
	public OsgiIdentityRequirement(Resource resource, String symbolicName, Version version, String type) {
		this.resource = resource;
		StringBuilder builder = new StringBuilder("(&(")
			.append(ResourceConstants.IDENTITY_NAMESPACE)
			.append('=')
			.append(symbolicName)
			.append(")(")
			.append(ResourceConstants.IDENTITY_VERSION_ATTRIBUTE)
			.append('=')
			.append(version)
			.append(")(")
			.append(ResourceConstants.IDENTITY_TYPE_ATTRIBUTE)
			.append('=')
			.append(type)
			.append("))");
		try {
			filter = FrameworkUtil.createFilter(builder.toString());
		}
		catch (InvalidSyntaxException e) {
			throw new SubsystemException(e);
		}
		directives.put(ResourceConstants.IDENTITY_SINGLETON_DIRECTIVE, Boolean.FALSE.toString());
		directives.put(Constants.EFFECTIVE_DIRECTIVE, Constants.EFFECTIVE_RESOLVE);
	}

	public Map<String, Object> getAttributes() {
		return Collections.emptyMap();
	}

	public Map<String, String> getDirectives() {
		return Collections.unmodifiableMap(directives);
	}

	public String getNamespace() {
		return ResourceConstants.IDENTITY_NAMESPACE;
	}

	public Resource getResource() {
		return resource;
	}

	public boolean matches(Capability capability) {
		if (capability == null) return false;
		if (!capability.getNamespace().equals(getNamespace())) return false;
		if (!filter.matches(capability.getAttributes())) return false;
		// TODO Check directives.
		return true;
	}

}
