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

import org.apache.aries.subsystem.core.internal.ResourceHelper;
import org.osgi.framework.VersionRange;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.SubsystemConstants;

public class PreferredProviderHeader extends AbstractClauseBasedHeader<PreferredProviderHeader.Clause> implements RequirementHeader<PreferredProviderHeader.Clause> {
    public static class Clause extends AbstractClause {
		public static final String ATTRIBUTE_TYPE = TypeAttribute.NAME;
		public static final String ATTRIBUTE_VERSION = VersionRangeAttribute.NAME_VERSION;
		
		public Clause(String clause) {
			super(
					parsePath(clause, Patterns.SYMBOLIC_NAME, false), 
					parseParameters(clause, true),
					generateDefaultParameters(
							VersionRangeAttribute.DEFAULT_VERSION,
							TypeAttribute.newInstance(SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE)));
		}
		
		public boolean contains(Resource resource) {
			return getSymbolicName().equals(
					ResourceHelper.getSymbolicNameAttribute(resource))
					&& getVersionRange().includes(
							ResourceHelper.getVersionAttribute(resource))
					&& getType().equals(
							ResourceHelper.getTypeAttribute(resource));
		}
		
		public String getSymbolicName() {
			return path;
		}
		
		public String getType() {
			return (String)getAttribute(ATTRIBUTE_TYPE).getValue();
		}
		
		public VersionRange getVersionRange() {
			Attribute attribute = getAttribute(ATTRIBUTE_VERSION);
			if (attribute instanceof VersionRangeAttribute)
				return ((VersionRangeAttribute)attribute).getVersionRange();
			return new VersionRange(attribute.getValue().toString());
		}
        
		public PreferredProviderRequirement toRequirement(Resource resource) {
			return new PreferredProviderRequirement(this, resource);
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder()
					.append(getPath());
			for (Parameter parameter : getParameters()) {
				builder.append(';').append(parameter);
			}
			return builder.toString();
		}
	}
	
	public static final String NAME = SubsystemConstants.PREFERRED_PROVIDER;
	
	public PreferredProviderHeader(Collection<Clause> clauses) {
		super(clauses);
	}
	
	public PreferredProviderHeader(String value) {
		super(
				value, 
				new ClauseFactory<Clause>() {
					@Override
					public Clause newInstance(String clause) {
						return new Clause(clause);
					}
				});
	}
	
	public boolean contains(Resource resource) {
		for (Clause clause : getClauses())
			if (clause.contains(resource))
				return true;
		return false;
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
	public List<PreferredProviderRequirement> toRequirements(Resource resource) {
		List<PreferredProviderRequirement> requirements = new ArrayList<PreferredProviderRequirement>(clauses.size());
		for (Clause clause : clauses)
			requirements.add(clause.toRequirement(resource));
		return requirements;
	}
}
