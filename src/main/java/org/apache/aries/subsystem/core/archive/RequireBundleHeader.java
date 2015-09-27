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

public class RequireBundleHeader extends AbstractClauseBasedHeader<RequireBundleHeader.Clause>implements RequirementHeader<RequireBundleHeader.Clause> {
    public static class Clause extends AbstractClause {
		public static final String ATTRIBUTE_BUNDLEVERSION = Constants.BUNDLE_VERSION_ATTRIBUTE;
		public static final String DIRECTIVE_RESOLUTION = Constants.RESOLUTION_DIRECTIVE;
		public static final String DIRECTIVE_VISIBILITY = Constants.VISIBILITY_DIRECTIVE;
		
		private static final Collection<Parameter> defaultParameters = generateDefaultParameters(
				VersionRangeAttribute.DEFAULT_BUNDLEVERSION,
				VisibilityDirective.PRIVATE,
				ResolutionDirective.MANDATORY);
		
		public Clause(String clause) {
			super(
            		parsePath(clause, Patterns.SYMBOLIC_NAME, false),
            		parseParameters(clause, true), 
            		defaultParameters);
		}
		
		public Clause(String path, Map<String, Parameter> parameters, Collection<Parameter> defaultParameters) {
			super(path, parameters, defaultParameters);
		}
		
		public static Clause valueOf(Requirement requirement) {
			String namespace = requirement.getNamespace();
			if (!RequireBundleRequirement.NAMESPACE.equals(namespace)) {
				throw new IllegalArgumentException("Invalid namespace:" + namespace);
			}
			Map<String, Parameter> parameters = new HashMap<String, Parameter>();
			String filter = null;
			Map<String, String> directives = requirement.getDirectives();
			for (Map.Entry<String, String> entry : directives.entrySet()) {
				String key = entry.getKey();
				if (RequireBundleRequirement.DIRECTIVE_FILTER.equals(key)) {
					filter = entry.getValue();
				}
				else {
					parameters.put(key, DirectiveFactory.createDirective(key, entry.getValue()));
				}
			}
			Map<String, List<SimpleFilter>> attributes = SimpleFilter.attributes(filter);
			String path = null;
			for (Map.Entry<String, List<SimpleFilter>> entry : attributes.entrySet()) {
				String key = entry.getKey();
				List<SimpleFilter> value = entry.getValue();
				if (RequireBundleRequirement.NAMESPACE.equals(key)) {
					path = String.valueOf(value.get(0).getValue());
				}
				else if (ATTRIBUTE_BUNDLEVERSION.equals(key)) {
					parameters.put(key, new VersionRangeAttribute(key, parseVersionRange(value)));
				}
				else {
					parameters.put(key, AttributeFactory.createAttribute(key,
							String.valueOf(value.get(0).getValue())));
				}
			}
			return new Clause(path, parameters, defaultParameters);
		}
		
		@Override
		public Attribute getAttribute(String name) {
			Parameter result = parameters.get(name);
			if (result instanceof Attribute) {
				return (Attribute)result;
			}
			return null;
		}

		@Override
		public Collection<Attribute> getAttributes() {
			ArrayList<Attribute> attributes = new ArrayList<Attribute>(parameters.size());
			for (Parameter parameter : parameters.values()) {
				if (parameter instanceof Attribute) {
					attributes.add((Attribute)parameter);
				}
			}
			attributes.trimToSize();
			return attributes;
		}

		@Override
		public Directive getDirective(String name) {
			Parameter result = parameters.get(name);
			if (result instanceof Directive) {
				return (Directive)result;
			}
			return null;
		}

		@Override
		public Collection<Directive> getDirectives() {
			ArrayList<Directive> directives = new ArrayList<Directive>(parameters.size());
			for (Parameter parameter : parameters.values()) {
				if (parameter instanceof Directive) {
					directives.add((Directive)parameter);
				}
			}
			directives.trimToSize();
			return directives;
		}

		public String getSymbolicName() {
			return path;
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
	
	public RequireBundleHeader(Collection<Clause> clauses) {
		super(clauses);
	}
	
	public RequireBundleHeader(String value) {
		super(
				value, 
				new ClauseFactory<Clause>() {
					@Override
					public Clause newInstance(String clause) {
						return new Clause(clause);
					}
				});
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
