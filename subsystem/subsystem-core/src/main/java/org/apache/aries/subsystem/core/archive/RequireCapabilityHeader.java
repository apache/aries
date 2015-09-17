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
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class RequireCapabilityHeader extends AbstractClauseBasedHeader<RequireCapabilityHeader.Clause> implements RequirementHeader<RequireCapabilityHeader.Clause> {

    public static class Clause extends AbstractClause {
		public static final String DIRECTIVE_EFFECTIVE = Constants.EFFECTIVE_DIRECTIVE;
		public static final String DIRECTIVE_FILTER = Constants.FILTER_DIRECTIVE;
		public static final String DIRECTIVE_RESOLUTION = Constants.RESOLUTION_DIRECTIVE;
		
		private static final Pattern PATTERN_NAMESPACE = Pattern.compile('(' + Grammar.NAMESPACE + ")(?=;|\\z)");
		private static final Pattern PATTERN_PARAMETER = Pattern.compile('(' + Grammar.PARAMETER + ")(?=;|\\z)");
		
		private static void fillInDefaults(Map<String, Parameter> parameters) {
			Parameter parameter = parameters.get(DIRECTIVE_EFFECTIVE);
			if (parameter == null)
				parameters.put(DIRECTIVE_EFFECTIVE, EffectiveDirective.RESOLVE);
			parameter = parameters.get(DIRECTIVE_RESOLUTION);
			if (parameter == null)
				parameters.put(DIRECTIVE_RESOLUTION, ResolutionDirective.MANDATORY);
		}
		
		public Clause(String clause) {
            super(clause);
		}
		
		public Clause(Requirement requirement) {
			path = requirement.getNamespace();
			for (Entry<String, String> directive : requirement.getDirectives().entrySet())
				parameters.put(directive.getKey(), DirectiveFactory.createDirective(directive.getKey(), directive.getValue()));
		}

		public String getNamespace() {
			return path;
		}

        @Override
        protected void processClauseString(String clauseString)
                throws IllegalArgumentException {
            Matcher matcher = PATTERN_NAMESPACE.matcher(clauseString);
            if (!matcher.find())
                throw new IllegalArgumentException("Missing namespace path: " + clauseString);
            path = matcher.group();
            matcher.usePattern(PATTERN_PARAMETER);
            while (matcher.find()) {
                Parameter parameter = ParameterFactory.create(matcher.group());
                parameters.put(parameter.getName(), parameter);
            }
            fillInDefaults(parameters);
        }
		
		public RequireCapabilityRequirement toRequirement(Resource resource) {
			return new RequireCapabilityRequirement(this, resource);
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
	
	public static final String NAME = Constants.REQUIRE_CAPABILITY;
	
	private static final Pattern PATTERN = Pattern.compile('(' + Grammar.REQUIREMENT + ")(?=,|\\z)");
	
	@Override
    protected Collection<Clause> processHeader(String header) {
		Matcher matcher = PATTERN.matcher(header);
		Set<Clause> lclauses = new HashSet<Clause>();
		while (matcher.find())
			lclauses.add(new Clause(matcher.group()));
		return lclauses;
	}
	
	public RequireCapabilityHeader(String value) {
		super(value);
	}
	
	public RequireCapabilityHeader(Collection<Clause> clauses) {
		super(clauses);
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
	public List<RequireCapabilityRequirement> toRequirements(Resource resource) {
		List<RequireCapabilityRequirement> requirements = new ArrayList<RequireCapabilityRequirement>(clauses.size());
		for (Clause clause : clauses)
			requirements.add(clause.toRequirement(resource));
		return requirements;
	}
	
}
