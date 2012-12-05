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
import org.osgi.resource.Resource;

public class ProvideCapabilityHeader implements CapabilityHeader<ProvideCapabilityHeader.Clause> {	
	public static class Clause implements org.apache.aries.subsystem.core.archive.Clause {
		public static final String DIRECTIVE_EFFECTIVE = Constants.EFFECTIVE_DIRECTIVE;
		public static final String DIRECTIVE_USES = Constants.USES_DIRECTIVE;
		
		private static final String DIRECTIVE = '(' + Grammar.EXTENDED + ")(:=)(" + Grammar.ARGUMENT + ')';
		private static final String TYPED_ATTR = '(' + Grammar.EXTENDED + ")(?:(\\:)(" + Grammar.TYPE + "))?=(" + Grammar.ARGUMENT + ')';
		private static final Pattern PATTERN_NAMESPACE = Pattern.compile('(' + Grammar.NAMESPACE + ")(?=;|\\z)");
		private static final Pattern PATTERN_PARAMETER = Pattern.compile("(?:(?:" + DIRECTIVE + ")|(?:" + TYPED_ATTR + "))(?=;|\\z)");
		
		private static void fillInDefaults(Map<String, Parameter> parameters) {
			Parameter parameter = parameters.get(DIRECTIVE_EFFECTIVE);
			if (parameter == null)
				parameters.put(DIRECTIVE_EFFECTIVE, EffectiveDirective.DEFAULT);
		}
		
		private static String removeQuotes(String value) {
			if (value == null)
				return null;
			if (value.startsWith("\"") && value.endsWith("\""))
				return value.substring(1, value.length() - 1);
			return value;
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
				if (":=".equals(matcher.group(2))) {
					// This is a directive.
					parameters.put(matcher.group(1), DirectiveFactory.createDirective(matcher.group(1), removeQuotes(matcher.group(3))));
				}
				else if (":".equals(matcher.group(5)))
					// This is a typed attribute with a declared version.
					parameters.put(matcher.group(4), new TypedAttribute(matcher.group(4), removeQuotes(matcher.group(7)), matcher.group(6)));
				else
					// This is a typed attribute without a declared version.
					parameters.put(matcher.group(4), new TypedAttribute(matcher.group(4), removeQuotes(matcher.group(7)), TypedAttribute.Type.String));
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
					attributes.add((TypedAttribute)parameter);
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
		
		public String getNamespace() {
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
		
		public ProvideCapabilityCapability toCapability(Resource resource) {
			return new ProvideCapabilityCapability(this, resource);
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
	
	public static final String NAME = Constants.PROVIDE_CAPABILITY;
	
	private static final Pattern PATTERN = Pattern.compile('(' + Grammar.CAPABILITY + ")(?=,|\\z)");
	
	private final Set<Clause> clauses = new HashSet<Clause>();
	
	public ProvideCapabilityHeader(String value) {
		Matcher matcher = PATTERN.matcher(value);
		while (matcher.find())
			clauses.add(new Clause(matcher.group()));
		if (clauses.isEmpty())
			throw new IllegalArgumentException("A " + NAME + " header must have at least one clause");
	}
	
	@Override
	public Collection<ProvideCapabilityHeader.Clause> getClauses() {
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
	public List<ProvideCapabilityCapability> toCapabilities(Resource resource) {
		List<ProvideCapabilityCapability> result = new ArrayList<ProvideCapabilityCapability>();
		for (Clause clause : clauses)
			result.add(clause.toCapability(resource));
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
