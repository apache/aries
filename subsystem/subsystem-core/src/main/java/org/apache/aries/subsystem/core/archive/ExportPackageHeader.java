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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.Capability;
import org.osgi.framework.wiring.Resource;
import org.osgi.framework.wiring.ResourceConstants;

public class ExportPackageHeader extends AbstractHeader {
	public static final String NAME = Constants.EXPORT_PACKAGE;
	
	public ExportPackageHeader(String value) {
		super(NAME, value);
	}
	
	public List<Capability> getCapabilities(final Resource resource) {
		List<Capability> capabilities = new ArrayList<Capability>(clauses.size());
		for (final Clause clause : clauses) {
			String[] exportedPackages = clause.getPath().split(";");
			for (final String exportedPackage : exportedPackages) {
				capabilities.add(new Capability() {
					@Override
					public String getNamespace() {
						return ResourceConstants.WIRING_PACKAGE_NAMESPACE;
					}

					@Override
					public Map<String, String> getDirectives() {
						Collection<Directive> directives = clause.getDirectives();
						Map<String, String> result = new HashMap<String, String>(directives.size());
						for (Directive directive : directives)
							result.put(directive.getName(), directive.getValue());
						return result;
					}

					@Override
					public Map<String, Object> getAttributes() {
						Collection<Attribute> attributes = clause.getAttributes();
						Map<String, Object> result = new HashMap<String, Object>(attributes.size() + 1);
						for (Attribute attribute : attributes)
							result.put(attribute.getName(), attribute.getValue());
						// Add the namespace attribute.
						result.put(ResourceConstants.WIRING_PACKAGE_NAMESPACE, exportedPackage);
						// Add the default version, if necessary.
						if (result.get(Constants.VERSION_ATTRIBUTE) == null)
							result.put(Constants.VERSION_ATTRIBUTE, Version.emptyVersion);
						return result;
					}

					@Override
					public Resource getResource() {
						return resource;
					}
				});
			}
		}
		return capabilities;
	}
}
