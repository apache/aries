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
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class RequireCapabilityHeader extends AbstractClauseBasedHeader<RequireCapabilityHeader.Clause> implements RequirementHeader<RequireCapabilityHeader.Clause> {
    public static class Clause extends AbstractClause {
    	public static final String DIRECTIVE_CARDINALITY = CardinalityDirective.NAME;
		public static final String DIRECTIVE_EFFECTIVE = EffectiveDirective.NAME;
		public static final String DIRECTIVE_FILTER = FilterDirective.NAME;
		public static final String DIRECTIVE_RESOLUTION = ResolutionDirective.NAME;
		
		private static final Collection<Parameter> defaultParameters = generateDefaultParameters(
				EffectiveDirective.DEFAULT,
				ResolutionDirective.MANDATORY,
				CardinalityDirective.DEFAULT);
		
		public Clause(String clause) {
			super(
            		parsePath(clause, Patterns.NAMESPACE, false), 
            		parseTypedParameters(clause), 
            		defaultParameters);
			
		}
		
		public Clause(String path, Map<String, Parameter> parameters, Collection<Parameter> defaultParameters) {
			super(path, parameters, defaultParameters);
		}
		
		public static Clause valueOf(Requirement requirement) {
			String namespace = requirement.getNamespace();
			if (namespace.startsWith("osgi.wiring.")) {
				throw new IllegalArgumentException();
			}
			Map<String, Object> attributes = requirement.getAttributes();
			Map<String, String> directives = requirement.getDirectives();
			Map<String, Parameter> parameters = new HashMap<String, Parameter>(attributes.size() + directives.size());
			for (Map.Entry<String, Object> entry : attributes.entrySet()) {
				String key = entry.getKey();
				parameters.put(key, new TypedAttribute(key, entry.getValue()));
			}
			for (Map.Entry<String, String> entry : directives.entrySet()) {
				String key = entry.getKey();
				parameters.put(key, DirectiveFactory.createDirective(key, entry.getValue()));
			}
			String path = namespace;
			return new Clause(path, parameters, defaultParameters);
		}

		public String getNamespace() {
			return path;
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
	
	public RequireCapabilityHeader(String value) {
		super(
				value, 
				new ClauseFactory<Clause>() {
					@Override
					public Clause newInstance(String clause) {
						return new Clause(clause);
					}
				});
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
