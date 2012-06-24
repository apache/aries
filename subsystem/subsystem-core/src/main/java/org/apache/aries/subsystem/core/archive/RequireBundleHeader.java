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
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class RequireBundleHeader implements RequirementHeader<RequireBundleHeader.Clause> {
	public static class Clause implements org.apache.aries.subsystem.core.archive.Clause {
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
		
		private final String path;
		private final Map<String, Parameter> parameters = new HashMap<String, Parameter>();
		
		public Clause(String clause) {
			Matcher matcher = PATTERN_SYMBOLICNAME.matcher(clause);
			if (!matcher.find())
				throw new IllegalArgumentException("Missing bundle description path: " + clause);
			path = matcher.group();
			matcher.usePattern(PATTERN_PARAMETER);
			while (matcher.find()) {
				Parameter parameter = ParameterFactory.create(matcher.group());
				parameters.put(parameter.getName(), parameter);
			}
			fillInDefaults(parameters);
		}
		
		private static final String REGEX = "\\((" + RequireBundleRequirement.NAMESPACE + ")(=)([^\\)]+)\\)";
		private static final Pattern PATTERN = Pattern.compile(REGEX);
		
		public Clause(Requirement requirement) {
			if (!RequireBundleRequirement.NAMESPACE.equals(requirement.getNamespace()))
				throw new IllegalArgumentException("Requirement must be in the '" + RequireBundleRequirement.NAMESPACE + "' namespace");
			String filter = requirement.getDirectives().get(RequireBundleRequirement.DIRECTIVE_FILTER);
			String path = null;
			Matcher matcher = PATTERN.matcher(filter);
			while (matcher.find()) {
				String name = matcher.group(1);
				String operator = matcher.group(2);
				String value = matcher.group(3);
				if (RequireBundleRequirement.NAMESPACE.equals(name)) {
					path = value;
				}
				else if (ATTRIBUTE_BUNDLEVERSION.equals(name)) {
					// TODO Parse the version range from the filter.
				}
			}
			if (path == null)
				throw new IllegalArgumentException("Missing filter key: " + RequireBundleRequirement.NAMESPACE);
			this.path = path;
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
	
	private static Collection<Clause> processHeader(String header) {
		Set<Clause> clauses = new HashSet<Clause>();
		for (String clause : new ClauseTokenizer(header).getClauses())
			clauses.add(new Clause(clause));
		return clauses;
	}
	
	private final Set<Clause> clauses;
	
	public RequireBundleHeader(Collection<Clause> clauses) {
		if (clauses.isEmpty())
			throw new IllegalArgumentException("A " + NAME + " header must have at least one clause");
		this.clauses = new HashSet<Clause>(clauses);
	}
	
	public RequireBundleHeader(String value) {
		this(processHeader(value));
	}
	
	@Override
	public Collection<RequireBundleHeader.Clause> getClauses() {
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
	public List<RequireBundleRequirement> toRequirements(Resource resource) {
		List<RequireBundleRequirement> requirements = new ArrayList<RequireBundleRequirement>(clauses.size());
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
