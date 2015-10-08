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

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.apache.aries.subsystem.core.internal.BasicRequirement;
import org.junit.Test;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/*
 * https://issues.apache.org/jira/browse/ARIES-1427
 * 
 * org.osgi.service.subsystem.SubsystemException: 
 * java.lang.IllegalArgumentException: Invalid filter: (version=*)
 */
public class Aries1427Test {
	@Test
	public void testRequirementConversionWithVersionPresence() {
		VersionRange range = VersionRange.valueOf("(1.0,2.0)");
		String filter = new StringBuilder()
				.append("(&(")
				.append(PackageNamespace.PACKAGE_NAMESPACE)
				.append("=com.acme.tnt")
				.append(')')
				.append(range.toFilterString(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE))
				.append(')')
				.toString();
		Requirement requirement = new BasicRequirement.Builder()
				.namespace(PackageNamespace.PACKAGE_NAMESPACE)
				.directive(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE, filter)
				.resource(new Resource() {
					@Override
					public List<Capability> getCapabilities(String namespace) {
						return Collections.emptyList();
					}

					@Override
					public List<Requirement> getRequirements(String namespace) {
						return Collections.emptyList();
					}
				})
				.build();
		ImportPackageHeader.Clause expected = new ImportPackageHeader.Clause(
				"com.acme.tnt;version=\"(1.0,2.0)\"");
		ImportPackageHeader.Clause actual = ImportPackageHeader.Clause.valueOf(requirement);
		assertEquals("Wrong clause", expected, actual);
	}
}
