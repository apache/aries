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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.SubsystemConstants;

public class SubsystemExportServiceHeader implements Header<SubsystemExportServiceHeader.Clause> {
	public static class Clause implements org.apache.aries.subsystem.core.archive.Clause {
		public static final String DIRECTIVE_FILTER = Constants.FILTER_DIRECTIVE;
		
		private static final Pattern PATTERN_OBJECTCLASS = Pattern.compile('(' + Grammar.OBJECTCLASS + ")(?=;|\\z)");
		private static final Pattern PATTERN_PARAMETER = Pattern.compile('(' + Grammar.PARAMETER + ")(?=;|\\z)");
		
		private static void fillInDefaults(Map<String, Parameter> parameters) {
			// No defaults.
		}
		
		private final String path;
		private final Map<String, Parameter> parameters = new HashMap<String, Parameter>();
		
		public Clause(String clause) {
			Matcher main = PATTERN_OBJECTCLASS.matcher(clause);
			if (!main.find())
				throw new IllegalArgumentException("Missing objectClass path: " + clause);
			path = main.group();
			main.usePattern(PATTERN_PARAMETER);
			while (main.find()) {
				Parameter parameter = ParameterFactory.create(main.group());
				parameters.put(parameter.getName(), parameter);
			}
			fillInDefaults(parameters);
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
		
		public String getObjectClass() {
			return path;
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
		
		public List<Capability> toCapabilities(Resource resource) throws InvalidSyntaxException {
			List<Capability> capabilities = resource.getCapabilities(ServiceNamespace.SERVICE_NAMESPACE);
			if (capabilities.isEmpty())
				return capabilities;
			Filter filter = computeFilter();
			ArrayList<Capability> result = new ArrayList<Capability>(capabilities.size());
			for (Capability capability : capabilities)
				if (filter.matches(capability.getAttributes()))
					result.add(capability);
			result.trimToSize();
			return result;
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
		
		private Filter computeFilter() throws InvalidSyntaxException {
			return FrameworkUtil.createFilter(computeFilterString());
		}
		
		private String computeFilterString() {
			Directive directive = getDirective(DIRECTIVE_FILTER);
			return new StringBuilder()
					.append("(&(")
					.append(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE)
					.append('=')
					.append(path)
					.append(')')
					.append(directive == null ? "" : directive.getValue())
					.append(')')
					.toString();
		}
	}
	
	public static final String NAME = SubsystemConstants.SUBSYSTEM_EXPORTSERVICE;
	
	private static final Pattern PATTERN = Pattern.compile('(' + Grammar.SERVICE + ")(?=,|\\z)");
	
	private final Set<Clause> clauses = new HashSet<Clause>();
	
	public SubsystemExportServiceHeader(String value) {
		Matcher matcher = PATTERN.matcher(value);
		while (matcher.find())
			clauses.add(new Clause(matcher.group()));
		if (clauses.isEmpty())
			throw new IllegalArgumentException("A " + NAME + " header must have at least one clause");
	}
	
	@Override
	public Collection<SubsystemExportServiceHeader.Clause> getClauses() {
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
	
	public List<Capability> toCapabilities(Resource resource) throws InvalidSyntaxException {
		List<Capability> result = new ArrayList<Capability>();
		for (Clause clause : clauses)
			result.addAll(clause.toCapabilities(resource));
		return result;
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
