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
import java.util.HashMap;
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
import org.osgi.service.subsystem.SubsystemConstants;

public class SubsystemImportServiceHeader implements RequirementHeader<SubsystemImportServiceHeader.Clause> {
	public static class Clause implements org.apache.aries.subsystem.core.archive.Clause {
		public static final String DIRECTIVE_EFFECTIVE = Constants.EFFECTIVE_DIRECTIVE;
		public static final String DIRECTIVE_FILTER = Constants.FILTER_DIRECTIVE;
		public static final String DIRECTIVE_RESOLUTION = Constants.RESOLUTION_DIRECTIVE;
		
		private static final Pattern PATTERN_NAMESPACE = Pattern.compile('(' + Grammar.NAMESPACE + ")(?=;|\\z)");
		private static final Pattern PATTERN_PARAMETER = Pattern.compile('(' + Grammar.PARAMETER + ")(?=;|\\z)");
		
		private static void fillInDefaults(Map<String, Parameter> parameters) {
			Parameter parameter = parameters.get(DIRECTIVE_EFFECTIVE);
			if (parameter == null)
				parameters.put(DIRECTIVE_EFFECTIVE, EffectiveDirective.ACTIVE);
			parameter = parameters.get(DIRECTIVE_RESOLUTION);
			if (parameter == null)
				parameters.put(DIRECTIVE_RESOLUTION, ResolutionDirective.MANDATORY);
		}
		
		private final String path;
		private final Map<String, Parameter> parameters = new HashMap<String, Parameter>();
		
		public Clause(String clause) {
			Matcher matcher = PATTERN_NAMESPACE.matcher(clause);
			if (!matcher.find())
				throw new IllegalArgumentException("Missing namespace path: " + clause);
			path = matcher.group();
			matcher.usePattern(PATTERN_PARAMETER);
			while (matcher.find()) {
				Parameter parameter = ParameterFactory.create(matcher.group());
				parameters.put(parameter.getName(), parameter);
			}
			fillInDefaults(parameters);
		}
		
		public Clause(Requirement requirement) {
			path = requirement.getNamespace();
			for (Entry<String, String> directive : requirement.getDirectives().entrySet())
				parameters.put(directive.getKey(), DirectiveFactory.createDirective(directive.getKey(), directive.getValue()));
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

		@Override
		public Parameter getParameter(String name) {
			return parameters.get(name);
		}

		@Override
		public Collection<Parameter> getParameters() {
			return Collections.unmodifiableCollection(parameters.values());
		}

		@Override
		public String getPath() {
			return path;
		}
		
		public SubsystemImportServiceRequirement toRequirement(Resource resource) {
			return new SubsystemImportServiceRequirement(this, resource);
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
	
	public static final String NAME = SubsystemConstants.SUBSYSTEM_IMPORTSERVICE;
	
	// TODO Subsystem-ImportService currently does not have its own grammar, but it's similar to Require-Capability.
	private static final Pattern PATTERN = Pattern.compile('(' + Grammar.REQUIREMENT + ")(?=,|\\z)");
	
	private static Collection<Clause> processHeader(String header) {
		Matcher matcher = PATTERN.matcher(header);
		Set<Clause> clauses = new HashSet<Clause>();
		while (matcher.find())
			clauses.add(new Clause(matcher.group()));
		return clauses;
	}
	
	private final Set<Clause> clauses;
	
	public SubsystemImportServiceHeader(String value) {
		this(processHeader(value));
	}
	
	public SubsystemImportServiceHeader(Collection<Clause> clauses) {
		if (clauses.isEmpty())
			throw new IllegalArgumentException("A " + NAME + " header must have at least one clause");
		this.clauses = new HashSet<Clause>(clauses);
	}
	
	@Override
	public Collection<SubsystemImportServiceHeader.Clause> getClauses() {
		return Collections.unmodifiableSet(clauses);
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
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Clause clause : getClauses()) {
			builder.append(clause).append(',');
		}
		// Remove the trailing comma. Note at least one clause is guaranteed to exist.
		builder.deleteCharAt(builder.length() - 1);
		return builder.toString();
	}
}
