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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.internal.AbstractCapability;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Resource;

public class ExportPackageCapability extends AbstractCapability {
	public static final String NAMESPACE = PackageNamespace.PACKAGE_NAMESPACE;
	
	private final Map<String, Object> attributes = new HashMap<String, Object>();
	private final Map<String, String> directives = new HashMap<String, String>();
	private final Resource resource;
	
	public ExportPackageCapability(String packageName, Collection<Parameter> parameters, Resource resource) {
		attributes.put(NAMESPACE, packageName);
		for (Parameter parameter : parameters) {
			if (parameter instanceof Attribute)
				attributes.put(parameter.getName(), parameter.getValue());
			else
				directives.put(parameter.getName(), ((Directive)parameter).getValue());
		}
		this.resource = resource;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	@Override
	public Map<String, String> getDirectives() {
		return Collections.unmodifiableMap(directives);
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	@Override
	public Resource getResource() {
		return resource;
	}
}
