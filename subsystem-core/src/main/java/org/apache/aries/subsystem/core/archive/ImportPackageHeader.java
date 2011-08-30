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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.wiring.BundleRevision;

public class ImportPackageHeader implements Header<ImportPackageHeader.Clause> {
	public static class Clause implements org.apache.aries.subsystem.core.archive.Clause {
		private static final String REGEX = '(' + Grammar.PACKAGENAMES + ")(?:\\;(" + Grammar.PARAMETER + "))*";
		private static final Pattern PATTERN = Pattern.compile(REGEX);
		
		private final Map<String, Parameter> myParameters = new HashMap<String, Parameter>();
		private final String myPath;
		
		public Clause(String clause) {
			Matcher matcher = PATTERN.matcher(clause);
			if (!matcher.matches()) {
				throw new IllegalArgumentException("Invalid " + Constants.IMPORT_PACKAGE + " header clause: " + clause);
			}
			myPath = matcher.group(1);
			for (int i = 2; i <= matcher.groupCount(); i++) {
				String group = matcher.group(i);
				if (group != null) {
					Parameter parameter = ParameterFactory.create(group);
					myParameters.put(parameter.getName(), parameter);
				}
			}
			Attribute attribute = new GenericAttribute(BundleRevision.PACKAGE_NAMESPACE, getPath());
			myParameters.put(attribute.getName(), attribute);
			attribute = getAttribute(Constants.VERSION_ATTRIBUTE);
			if (attribute == null) {
				attribute = new VersionRangeAttribute();
				myParameters.put(attribute.getName(), attribute);
			}
			Directive directive = getDirective(Constants.FILTER_DIRECTIVE);
			if (directive == null) {
				StringBuilder builder = new StringBuilder("(&");
				for (Attribute a : getAttributes()) {
					a.appendToFilter(builder);
				}
				directive = new GenericDirective(Constants.FILTER_DIRECTIVE, builder.append(')').toString());
				myParameters.put(directive.getName(), directive);
			}
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
				public String getNamespace() {
					return BundleRevision.PACKAGE_NAMESPACE;
				}

				public Map<String, String> getDirectives() {
					Collection<Directive> directives = Clause.this.getDirectives();
					Map<String, String> result = new HashMap<String, String>(directives.size() + 1);
					for (Directive directive : directives) {
						result.put(directive.getName(), directive.getValue());
					}
					return result;
				}

				public Map<String, Object> getAttributes() {
					Collection<Attribute> attributes = Clause.this.getAttributes();
					Map<String, Object> result = new HashMap<String, Object>(attributes.size() + 1);
					for (Attribute attribute : attributes) {
						result.put(attribute.getName(), attribute.getValue());
					}
					return result;
				}

				public Resource getResource() {
					return resource;
				}

				public boolean matches(Capability capability) {
					if (!getNamespace().equals(capability.getNamespace()))
						return false;
					Filter filter;
					try {
						filter = FrameworkUtil.createFilter(getDirectives().get(Constants.FILTER_DIRECTIVE));
					}
					catch (InvalidSyntaxException e) {
						return false;
					}
					if (!filter.matches(capability.getAttributes()))
							return false;
					
					return true;
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
	
	public static final String NAME = Constants.IMPORT_PACKAGE;
	
	private static final String REGEX = '(' + Grammar.IMPORT + ")(?:\\,(" + Grammar.IMPORT + "))*";
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
	private final Set<Clause> clauses = new HashSet<Clause>();
	private final String value;
	
	public ImportPackageHeader(String header) {
		Matcher matcher = PATTERN.matcher(header);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid " + Constants.IMPORT_PACKAGE + " header: " + header);
		}
		for (int i = 1; i <= matcher.groupCount(); i++) {
			String group = matcher.group(i);
			if (group != null) {
				Clause clause = new Clause(group);
				clauses.add(clause);
			}
		}
		value = header;
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
	
	public String getValue() {
		return value;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder()
				.append(getName())
				.append(": ");
		for (Clause clause : getClauses()) {
			builder.append(clause);
		}
		return builder.toString();
	}
}
