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
import java.util.List;

import org.apache.aries.subsystem.core.internal.BasicSubsystem;
import org.apache.aries.subsystem.core.internal.OsgiIdentityRequirement;
import org.osgi.framework.Version;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.SubsystemConstants;

public class AriesSubsystemParentsHeader extends AbstractClauseBasedHeader<AriesSubsystemParentsHeader.Clause> implements RequirementHeader<AriesSubsystemParentsHeader.Clause> {
    public static class Clause extends AbstractClause {
		public static final String ATTRIBUTE_VERSION = VersionRangeAttribute.NAME_VERSION;
		public static final String ATTRIBUTE_RESOURCEID = "resourceId";
		public static final String ATTRIBUTE_TYPE = TypeAttribute.NAME;
		
		public Clause(String clause) {
			super(
					parsePath(clause, Patterns.SYMBOLIC_NAME, false), 
					parseParameters(clause, true), 
					generateDefaultParameters(
							TypeAttribute.newInstance(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION),
							VersionRangeAttribute.DEFAULT_VERSION));
		}
		
		public Clause(BasicSubsystem subsystem, boolean referenceCount) {
			this(appendSubsystem(subsystem, new StringBuilder(), referenceCount).toString());
		}
		
		public boolean contains(BasicSubsystem subsystem) {
			return getSymbolicName().equals(
					subsystem.getSymbolicName())
					&& getVersion().equals(
							subsystem.getVersion())
					&& getType().equals(
							subsystem.getType());
		}

		public long getId() {
			Attribute attribute = getAttribute(ATTRIBUTE_RESOURCEID);
			if (attribute == null)
				return -1;
			return Long.valueOf(String.valueOf(attribute.getValue()));
		}

		public String getSymbolicName() {
			return path;
		}
		
		public String getType() {
			return ((TypeAttribute)getAttribute(ATTRIBUTE_TYPE)).getType();
		}
		
		public Version getVersion() {
			return ((VersionAttribute)getAttribute(ATTRIBUTE_VERSION)).getVersion();
		}
		
		public OsgiIdentityRequirement toRequirement(Resource resource) {
			return new OsgiIdentityRequirement(getSymbolicName(), getVersion(), getType(), false);
		}
	}
	
	public static final String NAME = "AriesSubsystem-Parents";
	
	private static StringBuilder appendSubsystem(BasicSubsystem subsystem, StringBuilder builder, boolean referenceCount) {
		String symbolicName = subsystem.getSymbolicName();
		Version version = subsystem.getVersion();
		String type = subsystem.getType();
		builder.append(symbolicName)
			.append(';')
			.append(Clause.ATTRIBUTE_VERSION)
			.append('=')
			.append(version.toString())
			.append(';')
			.append(Clause.ATTRIBUTE_TYPE)
			.append('=')
			.append(type)
			.append(';')
			.append(Clause.ATTRIBUTE_RESOURCEID)
			.append('=')
			.append(subsystem.getSubsystemId());
		return builder;
	}
	
	public AriesSubsystemParentsHeader(Collection<Clause> clauses) {
		super(clauses);
	}
	
	public AriesSubsystemParentsHeader(String value) {
		super(
				value, 
				new ClauseFactory<Clause>() {
					@Override
					public Clause newInstance(String clause) {
						return new Clause(clause);
					}
				});
	}
	
	public boolean contains(BasicSubsystem subsystem) {
		return getClause(subsystem) != null;
	}
	
	public Clause getClause(BasicSubsystem subsystem) {
		String symbolicName = subsystem.getSymbolicName();
		Version version = subsystem.getVersion();
		String type = subsystem.getType();
		for (Clause clause : clauses) {
			if (symbolicName.equals(clause.getPath())
					&& clause.getVersion().equals(version)
					&& type.equals(clause.getType()))
				return clause;
		}
		return null;
	}
	
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getValue() {
		return toString();
	}

	@Override
	public List<Requirement> toRequirements(Resource resource) {
		List<Requirement> requirements = new ArrayList<Requirement>(clauses.size());
		for (Clause clause : clauses)
			requirements.add(clause.toRequirement(resource));
		return requirements;
	}
}
