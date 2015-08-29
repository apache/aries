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

import org.osgi.framework.Constants;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class RequireBundleHeader extends AbstractClauseBasedHeader<RequireBundleHeader.Clause>implements RequirementHeader<RequireBundleHeader.Clause> {

    public static class Clause extends AbstractClause {
		public static final String ATTRIBUTE_BUNDLEVERSION = Constants.BUNDLE_VERSION_ATTRIBUTE;
		public static final String DIRECTIVE_RESOLUTION = Constants.RESOLUTION_DIRECTIVE;
		public static final String DIRECTIVE_VISIBILITY = Constants.VISIBILITY_DIRECTIVE;
		
		private static final Pattern PATTERN_SYMBOLICNAME = Pattern.compile('(' + Grammar.SYMBOLICNAME + ")(?=;|\\z)");
		private static final Pattern PATTERN_PARAMETER = Pattern.compile('(' + Grammar.PARAMETER + ")(?=;|\\z)");
		
		private static void fillInDefaults(Map<String, Parameter> parameters) {
			Parameter parameter = parameters.get(DIRECTIVE_VISIBILITY);
			if (parameter == null)
				parameters.put(DIRECTIVE_VISIBILITY, VisibilityDirective.PRIVATE);
			parameter = parameters.get(DIRECTIVE_RESOLUTION);
			if (parameter == null)
				parameters.put(DIRECTIVE_RESOLUTION, ResolutionDirective.MANDATORY);
		}
		
		public Clause(String clause) {
            super(clause);
		}
		
		private static final String REGEX = "\\((" + RequireBundleRequirement.NAMESPACE + ")(=)([^\\)]+)\\)";
		private static final Pattern PATTERN = Pattern.compile(REGEX);
		
		public Clause(Requirement requirement) {
			if (!RequireBundleRequirement.NAMESPACE.equals(requirement.getNamespace()))
				throw new IllegalArgumentException("Requirement must be in the '" + RequireBundleRequirement.NAMESPACE + "' namespace");
			String filter = requirement.getDirectives().get(RequireBundleRequirement.DIRECTIVE_FILTER);
			String lpath = null;
			Matcher matcher = PATTERN.matcher(filter);
			while (matcher.find()) {
				String name = matcher.group(1);
				String operator = matcher.group(2);
				String value = matcher.group(3);
				if (RequireBundleRequirement.NAMESPACE.equals(name)) {
					lpath = value;
				}
				else if (ATTRIBUTE_BUNDLEVERSION.equals(name)) {
					// TODO Parse the version range from the filter.
				}
			}
			if (lpath == null)
				throw new IllegalArgumentException("Missing filter key: " + RequireBundleRequirement.NAMESPACE);
			this.path = lpath;
		}

		public String getSymbolicName() {
			return path;
		}
        
        @Override
        protected void processClauseString(String clauseString)
                throws IllegalArgumentException {
            Matcher matcher = PATTERN_SYMBOLICNAME.matcher(clauseString);
            if (!matcher.find())
                throw new IllegalArgumentException("Missing bundle description path: " + clauseString);
            path = matcher.group();
            matcher.usePattern(PATTERN_PARAMETER);
            while (matcher.find()) {
                Parameter parameter = ParameterFactory.create(matcher.group());
                parameters.put(parameter.getName(), parameter);
            }
            fillInDefaults(parameters);
        }
		
		public RequireBundleRequirement toRequirement(Resource resource) {
			return new RequireBundleRequirement(this, resource);
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
	
	public static final String NAME = Constants.REQUIRE_BUNDLE;
	
	@Override
    protected Collection<Clause> processHeader(String header) {
	    Collection<String> clauseStrs = new ClauseTokenizer(header).getClauses();
	    Set<Clause> lclauses = new HashSet<Clause>(clauseStrs.size());
        for (String clause : clauseStrs)
            lclauses.add(new Clause(clause));
        return lclauses;
	}
	
	public RequireBundleHeader(Collection<Clause> clauses) {
		super(clauses);
	}
	
	public RequireBundleHeader(String value) {
		super(value);
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
	public List<RequireBundleRequirement> toRequirements(Resource resource) {
		List<RequireBundleRequirement> requirements = new ArrayList<RequireBundleRequirement>(clauses.size());
		for (Clause clause : clauses)
			requirements.add(clause.toRequirement(resource));
		return requirements;
	}
}
