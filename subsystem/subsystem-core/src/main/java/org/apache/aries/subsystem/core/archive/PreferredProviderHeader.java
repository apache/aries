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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.subsystem.core.internal.ResourceHelper;
import org.osgi.framework.VersionRange;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.SubsystemConstants;

public class PreferredProviderHeader extends AbstractClauseBasedHeader<PreferredProviderHeader.Clause> implements RequirementHeader<PreferredProviderHeader.Clause> {

    public static class Clause extends AbstractClause {
		public static final String ATTRIBUTE_TYPE = TypeAttribute.NAME;
		public static final String ATTRIBUTE_VERSION = VersionAttribute.NAME;
		
		private static final Pattern PATTERN_SYMBOLICNAME = Pattern.compile('(' + Grammar.SYMBOLICNAME + ")(?=;|\\z)");
		private static final Pattern PATTERN_PARAMETER = Pattern.compile('(' + Grammar.PARAMETER + ")(?=;|\\z)");
		
		private static void fillInDefaults(Map<String, Parameter> parameters) {
			Parameter parameter = parameters.get(ATTRIBUTE_VERSION);
			if (parameter == null)
				parameters.put(ATTRIBUTE_VERSION, VersionRangeAttribute.DEFAULT);
			parameter = parameters.get(ATTRIBUTE_TYPE);
			if (parameter == null)
				parameters.put(ATTRIBUTE_TYPE, TypeAttribute.newInstance(SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE));
		}
		
		public Clause(String clause) {
            super(clause);
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
		
        @Override
        protected void processClauseString(String clauseString)
                throws IllegalArgumentException {
            Matcher matcher = PATTERN_SYMBOLICNAME.matcher(clauseString);
            if (!matcher.find())
                throw new IllegalArgumentException("Missing resource path: " + clauseString);
            path = matcher.group();
            matcher.usePattern(PATTERN_PARAMETER);
            while (matcher.find()) {
                Parameter parameter = ParameterFactory.create(matcher.group());
                if (parameter instanceof VersionAttribute)
                    parameter = new VersionRangeAttribute(new VersionRange(String.valueOf(parameter.getValue())));
                parameters.put(parameter.getName(), parameter);
            }
            fillInDefaults(parameters);
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

	private static final Pattern PATTERN = Pattern.compile('(' + Grammar.RESOURCE + ")(?=,|\\z)");
	
    @Override
    protected Collection<Clause> processHeader(String header) {
		Matcher matcher = PATTERN.matcher(header);
		Set<Clause> lclauses = new HashSet<Clause>();
		while (matcher.find())
			lclauses.add(new Clause(matcher.group()));
		return lclauses;
	}
	
	public PreferredProviderHeader(Collection<Clause> clauses) {
		super(clauses);
	}
	
	public PreferredProviderHeader(String value) {
		super(value);
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
