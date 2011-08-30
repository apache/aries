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
import java.util.Collections;
import java.util.List;

import org.osgi.framework.Version;
import org.osgi.framework.resource.Resource;

public class DeployedContentHeader extends AbstractHeader {
	public static class DeployedContent {
		private final Version deployedVersion;
		private final String name;
		private final String namespace;
		
		public DeployedContent(String name, Version deployedVersion, String namespace) {
			this.name = name;
			this.deployedVersion = deployedVersion;
			this.namespace = namespace;
		}
		
		public Version getDeployedVersion() {
			return deployedVersion;
		}
		
		public String getName() {
			return name;
		}
		
		public String getNamespace() {
			return namespace;
		}
	}
	
	// TODO Needs to be added to SubsystemConstants.
	public static final String NAME = "Deployed-Content";
	
	public static DeployedContentHeader newInstance(Collection<Resource> resources) {
		StringBuilder builder = new StringBuilder();
		for (Resource resource : resources) {
			appendResource(resource, builder);
			builder.append(',');
		}
		// Remove the trailing comma.
		// TODO Intentionally letting the exception propagate since there must be at least one resource.
		builder.deleteCharAt(builder.length() - 1);
		return new DeployedContentHeader(builder.toString());
	}
	
	private final List<DeployedContent> deployedContents;
	
	public DeployedContentHeader(String value) {
		super(NAME, value);
		deployedContents = new ArrayList<DeployedContent>(clauses.size());
		for (Clause clause : clauses) {
			DeployedVersionAttribute attribute = (DeployedVersionAttribute)clause.getAttribute(DeployedVersionAttribute.NAME);
			TypeAttribute typeAttribute = (TypeAttribute)clause.getAttribute(TypeAttribute.NAME);
			deployedContents.add(
					new DeployedContent(
							clause.getPath(),
							attribute == null ? Version.emptyVersion : attribute.getDeployedVersion(),
							typeAttribute == null ? TypeAttribute.DEFAULT_VALUE : typeAttribute.getType()));
		}
	}

	public List<DeployedContent> getDeployedContents() {
		return Collections.unmodifiableList(deployedContents);
	}
}
