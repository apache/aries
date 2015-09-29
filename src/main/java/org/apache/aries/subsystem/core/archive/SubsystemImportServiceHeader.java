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

import org.apache.aries.subsystem.core.capabilityset.SimpleFilter;
import org.osgi.framework.Constants;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.SubsystemConstants;

public class SubsystemImportServiceHeader extends AbstractClauseBasedHeader<SubsystemImportServiceHeader.Clause> implements RequirementHeader<SubsystemImportServiceHeader.Clause> {
    public static class Clause extends AbstractClause {
    	public static final String DIRECTIVE_CARDINALITY = CardinalityDirective.NAME;
		public static final String DIRECTIVE_EFFECTIVE = EffectiveDirective.NAME;
		public static final String DIRECTIVE_FILTER = FilterDirective.NAME;
		public static final String DIRECTIVE_RESOLUTION = ResolutionDirective.NAME;

		private static final Collection<Parameter> defaultParameters = generateDefaultParameters(
				EffectiveDirective.ACTIVE,
				ResolutionDirective.MANDATORY,
				CardinalityDirective.SINGLE);

		public Clause(String clause) {
			super(
            		parsePath(clause, Patterns.OBJECTCLASS_OR_STAR, false), 
            		parseParameters(clause, false), 
            		defaultParameters);
		}
		
		public Clause(String path, Map<String, Parameter> parameters, Collection<Parameter> defaultParameters) {
			super(path, parameters, defaultParameters);
		}

		public static Clause valueOf(Requirement requirement) {
			String namespace = requirement.getNamespace();
			if (!SubsystemImportServiceRequirement.NAMESPACE.equals(namespace)) {
				throw new IllegalArgumentException("Invalid namespace:" + namespace);
			}
			Map<String, Parameter> parameters = new HashMap<String, Parameter>();
			String filter = null;
			Map<String, String> directives = requirement.getDirectives();
			for (Map.Entry<String, String> entry : directives.entrySet()) {
				String key = entry.getKey();
				if (SubsystemImportServiceRequirement.DIRECTIVE_FILTER.equals(key)) {
					filter = entry.getValue();
				}
				else { 
					parameters.put(key, DirectiveFactory.createDirective(key, entry.getValue()));
				}
			}
			Map<String, List<SimpleFilter>> attributes = SimpleFilter.attributes(filter);
			String path = String.valueOf(attributes.remove(Constants.OBJECTCLASS).get(0).getValue());
			Map<String, Object> map = new HashMap<String, Object>(attributes.size());
			for (Map.Entry<String, List<SimpleFilter>> entry : attributes.entrySet()) {
				map.put(entry.getKey(), entry.getValue().get(0).getValue());
			}
			if (!map.isEmpty()) {
				parameters.put(
						SubsystemImportServiceRequirement.DIRECTIVE_FILTER, 
						DirectiveFactory.createDirective(
								SubsystemImportServiceRequirement.DIRECTIVE_FILTER,
								SimpleFilter.convert(map).toString()));
			}
			return new Clause(path, parameters, defaultParameters);
		}

		public SubsystemImportServiceRequirement toRequirement(Resource resource) {
			return new SubsystemImportServiceRequirement(this, resource);
		}
	}

	public static final String NAME = SubsystemConstants.SUBSYSTEM_IMPORTSERVICE;

	public SubsystemImportServiceHeader(String value) {
		super(
				value, 
				new ClauseFactory<Clause>() {
					@Override
					public Clause newInstance(String clause) {
						return new Clause(clause);
					}
				});
	}

	public SubsystemImportServiceHeader(Collection<Clause> clauses) {
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
	public List<SubsystemImportServiceRequirement> toRequirements(Resource resource) {
		List<SubsystemImportServiceRequirement> requirements = new ArrayList<SubsystemImportServiceRequirement>(clauses.size());
		for (Clause clause : clauses)
			requirements.add(clause.toRequirement(resource));
		return requirements;
	}
}
