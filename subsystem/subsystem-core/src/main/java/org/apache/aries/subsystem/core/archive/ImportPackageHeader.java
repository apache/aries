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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.wiring.BundleRevision;

public class ImportPackageHeader implements Header<ImportPackageHeader.Clause> {
	public static class Clause implements org.apache.aries.subsystem.core.archive.Clause {
		private static final String REGEX1 = '(' + Grammar.PACKAGENAMES + ")(?=;|\\z)";
		private static final String REGEX2 = '(' + Grammar.PARAMETER + ")(?=;|\\z)";
		private static final Pattern PATTERN1 = Pattern.compile(REGEX1);
		private static final Pattern PATTERN2 = Pattern.compile(REGEX2);
		
		private final Map<String, Parameter> myParameters = new HashMap<String, Parameter>();
		private final String myPath;
		
		public Clause(String clause) {
			Matcher matcher = PATTERN1.matcher(clause);
			if (matcher.find())
				myPath = matcher.group();
			else
				throw new IllegalArgumentException("Invalid " + Constants.IMPORT_PACKAGE + " header clause: " + clause);
			matcher.usePattern(PATTERN2);
			while (matcher.find()) {
				Parameter parameter = ParameterFactory.create(matcher.group());
				myParameters.put(parameter.getName(), parameter);
			}
//			Attribute attribute = new GenericAttribute(BundleRevision.PACKAGE_NAMESPACE, getPath());
//			myParameters.put(attribute.getName(), attribute);
//			attribute = getAttribute(Constants.VERSION_ATTRIBUTE);
//			if (attribute == null) {
//				attribute = new VersionRangeAttribute();
//				myParameters.put(attribute.getName(), attribute);
//			}
//			Directive directive = getDirective(Constants.FILTER_DIRECTIVE);
//			if (directive == null) {
//				StringBuilder builder = new StringBuilder("(&");
//				for (Attribute a : getAttributes()) {
//					a.appendToFilter(builder);
//				}
//				directive = new GenericDirective(Constants.FILTER_DIRECTIVE, builder.append(')').toString());
//				myParameters.put(directive.getName(), directive);
//			}
		}
		
		public Attribute getAttribute(String name) {
			Parameter result = myParameters.get(name);
			if (result instanceof Attribute) {
				return (Attribute)result;
			}
			return null;
		}
		
		public Collection<Attribute> getAttributes() {
			ArrayList<Attribute> attributes = new ArrayList<Attribute>(myParameters.size());
			for (Parameter parameter : myParameters.values()) {
				if (parameter instanceof Attribute) {
					attributes.add((Attribute)parameter);
				}
			}
			attributes.trimToSize();
			return attributes;
		}
		
		public Directive getDirective(String name) {
			Parameter result = myParameters.get(name);
			if (result instanceof Directive) {
				return (Directive)result;
			}
			return null;
		}
		
		public Collection<Directive> getDirectives() {
			ArrayList<Directive> directives = new ArrayList<Directive>(myParameters.size());
			for (Parameter parameter : myParameters.values()) {
				if (parameter instanceof Directive) {
					directives.add((Directive)parameter);
				}
			}
			directives.trimToSize();
			return directives;
		}
		
		public Collection<String> getPackageNames() {
			return Arrays.asList(myPath.split(";"));
		}
		
		public Parameter getParameter(String name) {
			return myParameters.get(name);
		}
		
		public Collection<Parameter> getParameters() {
			return Collections.unmodifiableCollection(myParameters.values());
		}
		
		public String getPath() {
			return myPath;
		}
		
		public Requirement getRequirement(final Resource resource) {
			return new Requirement() {
				@Override
				public String getNamespace() {
					return BundleRevision.PACKAGE_NAMESPACE;
				}
				@Override
				public Map<String, String> getDirectives() {
					Collection<Directive> directives = Clause.this.getDirectives();
					Map<String, String> result = new HashMap<String, String>(directives.size() + 1);
					for (Directive directive : directives) {
						result.put(directive.getName(), directive.getValue());
					}
					if (result.get(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE) == null) {
						StringBuilder builder = new StringBuilder("(&");
						for (Entry<String, Object> entry : getAttributes().entrySet())
							builder.append('(').append(entry.getKey()).append('=').append(entry.getValue()).append(')');
						result.put(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE, builder.append(')').toString());
					}
					return result;
				}
				@Override
				public Map<String, Object> getAttributes() {
					Collection<Attribute> attributes = Clause.this.getAttributes();
					Map<String, Object> result = new HashMap<String, Object>(attributes.size() + 1);
					for (Attribute attribute : attributes) {
						result.put(attribute.getName(), attribute.getValue());
					}
					if (result.get(PackageNamespace.PACKAGE_NAMESPACE) == null) {
						result.put(PackageNamespace.PACKAGE_NAMESPACE, getPath());
					}
					if (result.get(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE) == null)
						result.put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, Version.emptyVersion.toString());
					return result;
				}
				@Override
				public Resource getResource() {
					return resource;
				}
			};
		}
		
		public VersionRangeAttribute getVersionRangeAttribute() {
			return (VersionRangeAttribute)myParameters.get(Constants.VERSION_ATTRIBUTE);
		}
		
		public String toString() {
			StringBuilder builder = new StringBuilder()
					.append(getPath());
			for (Parameter parameter : getParameters()) {
				builder.append(';').append(parameter);
			}
			return builder.toString();
		}
	}
	
	public static final String ATTRIBUTE_BUNDLE_SYMBOLICNAME = PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE;
	public static final String ATTRIBUTE_BUNDLE_VERSION = PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
	public static final String ATTRIBUTE_VERSION = PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE;
	public static final String NAME = Constants.IMPORT_PACKAGE;
	public static final String DIRECTIVE_RESOLUTION = PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE;
	public static final String RESOLUTION_MANDATORY = PackageNamespace.RESOLUTION_MANDATORY;
	public static final String RESOLUTION_OPTIONAL = PackageNamespace.RESOLUTION_OPTIONAL;
	
	private static final String REGEX = Grammar.IMPORT + "(?=,|\\z)";
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
//	private static String valueOf(Collection<Clause> clauses) {
//		StringBuilder sb = new StringBuilder();
//		for (Clause clause : clauses) {
//			sb.append(clause).append(',');
//		}
//		if (sb.length() != 0)
//			sb.deleteCharAt(sb.length() - 1);
//		return sb.toString();
//	}
	
	private final Set<Clause> clauses;
//	private final String value;
	
	public ImportPackageHeader(Collection<Clause> clauses) {
		this.clauses = new HashSet<Clause>(clauses);
	}
	
	public ImportPackageHeader(String header) {
		Matcher matcher = PATTERN.matcher(header);
		Set<Clause> clauses = new HashSet<Clause>();
		while (matcher.find())
			clauses.add(new Clause(matcher.group()));
		if (clauses.isEmpty())
			throw new IllegalArgumentException("Invalid header syntax -> " + NAME + ": " + header);
//		value = header;
		this.clauses = clauses;
	}
	
	public Collection<ImportPackageHeader.Clause> getClauses() {
		return Collections.unmodifiableSet(clauses);
	}

	public String getName() {
		return Constants.IMPORT_PACKAGE;
	}
	
	public Collection<Requirement> getRequirements(Resource resource) {
		Collection<Clause> clauses = getClauses();
		Collection<Requirement> result = new HashSet<Requirement>(clauses.size());
		for (Clause clause : clauses) {
			result.add(clause.getRequirement(resource));
		}
		return result;
	}
	
	@Override
	public String getValue() {
		return toString();
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Clause clause : getClauses()) {
			builder.append(clause);
		}
		return builder.toString();
	}
}
