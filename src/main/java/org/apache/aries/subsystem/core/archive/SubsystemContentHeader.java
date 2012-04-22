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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.core.ResourceHelper;
import org.apache.aries.subsystem.core.internal.OsgiIdentityRequirement;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.SubsystemConstants;

public class SubsystemContentHeader extends AbstractHeader {
	public static class Content {
		private final boolean mandatory;
		private final String name;
		private final int startOrder;
		private final String type;
		private final VersionRange versionRange;
		
		public Content(boolean mandatory, String name, String type, VersionRange versionRange, int startOrder) {
			this.mandatory = mandatory;
			this.name = name;
			this.type = type;
			this.versionRange = versionRange;
			this.startOrder = startOrder;
		}
		
		public String getName() {
			return name;
		}
		
		public int getStartOrder() {
			return startOrder;
		}
		
		public String getType() {
			return type;
		}
		
		public VersionRange getVersionRange() {
			return versionRange;
		}
		
		public boolean isMandatory() {
			return mandatory;
		}
		
		public Requirement toRequirement() {
			return new OsgiIdentityRequirement(name, versionRange, type, false);
		}
		
		public String toString() {
			return new StringBuilder(getName())
				.append(';')
				.append(VersionAttribute.NAME)
				.append('=')
				.append(getVersionRange())
				.append(';')
				.append(TypeAttribute.NAME)
				.append("=")
				.append(getType())
				.append(';')
				.append(ResolutionDirective.NAME)
				.append(":=")
				.append(isMandatory())
				.append(';')
				.append(StartOrderDirective.NAME)
				.append(":=")
				.append(getStartOrder())
				.toString();
		}
	}
	
	public static final String NAME = SubsystemConstants.SUBSYSTEM_CONTENT;
	
	private static String processResources(Collection<Resource> resources) {
		if (resources.isEmpty())
			throw new IllegalArgumentException("At least one resource must be specified");
		StringBuilder sb = new StringBuilder();
		for (Resource resource : resources) {
			Capability c = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0);
			Map<String, Object> a = c.getAttributes();
			String s = (String)a.get(IdentityNamespace.IDENTITY_NAMESPACE);
			Version v = (Version)a.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
			String t = (String)a.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
			sb.append(s).append(';')
				.append(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE).append('=').append(v).append(';')
				.append(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE).append('=').append(t).append(',');
		}
		// Remove the trailing comma.
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	
	private final List<Content> contents;
	
	public SubsystemContentHeader(String value) {
		super(NAME, value);
		contents = new ArrayList<Content>(clauses.size());
		for (Clause clause : clauses) {
			boolean mandatory = true;
			Directive directive = clause.getDirective(ResolutionDirective.NAME);
			if (directive != null)
				mandatory = ((ResolutionDirective)directive).isMandatory();
			String name = clause.getPath();
			// TODO Assumes all resources are bundles.
			String type = TypeAttribute.DEFAULT_VALUE;
			Attribute attribute = clause.getAttribute(TypeAttribute.NAME);
			if (attribute != null)
				type = ((TypeAttribute)attribute).getType();
			VersionRange versionRange = new VersionRange(Version.emptyVersion.toString());
			attribute = clause.getAttribute(Constants.VERSION_ATTRIBUTE);
			if (attribute != null) {
				versionRange = new VersionRange(String.valueOf(attribute.getValue()));
			}
			int startOrder = StartOrderDirective.DEFAULT_VALUE;
			directive = clause.getDirective(StartOrderDirective.NAME);
			if (directive != null)
				startOrder = ((StartOrderDirective)directive).getStartOrder();
			contents.add(new Content(mandatory, name, type, versionRange, startOrder));
		}
		Collections.sort(contents, new Comparator<Content>() {
			@Override
			public int compare(Content content1, Content content2) {
				return ((Integer)content1.getStartOrder()).compareTo(content2.getStartOrder());
			}
		});
	}
	
	public SubsystemContentHeader(Collection<Resource> resources) {
		this(processResources(resources));
	}
	
	public boolean contains(Resource resource) {
		return getContent(resource) != null;
	}

	public Collection<Content> getContents() {
		return Collections.unmodifiableCollection(contents);
	}
	
	public boolean isMandatory(Resource resource) {
		Content content = getContent(resource);
		return content == null ? false : content.isMandatory();
	}
	
	public List<Requirement> toRequirements() {
		ArrayList<Requirement> result = new ArrayList<Requirement>(contents.size());
		for (Content content : contents)
			result.add(content.toRequirement());
		return result;
	}
	
	private Content getContent(Resource resource) {
		String symbolicName = ResourceHelper.getSymbolicNameAttribute(resource);
		Version version = ResourceHelper.getVersionAttribute(resource);
		String type = ResourceHelper.getTypeAttribute(resource);
		for (Content content : contents) {
			if (symbolicName.equals(content.getName())
					&& content.getVersionRange().includes(version)
					&& type.equals(content.getType()))
				return content;
		}
		return null;
	}
}
