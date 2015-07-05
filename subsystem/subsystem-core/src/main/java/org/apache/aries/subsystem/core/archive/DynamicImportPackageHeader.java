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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Resource;

public class DynamicImportPackageHeader implements RequirementHeader<DynamicImportPackageHeader.Clause> {
	public static class Clause implements org.apache.aries.subsystem.core.archive.Clause {
		private static final String REGEX1 = '(' + Grammar.WILDCARD_NAMES + ")(?=;|\\z)";
		private static final String REGEX2 = '(' + Grammar.PARAMETER + ")(?=;|\\z)";
		private static final Pattern PATTERN1 = Pattern.compile(REGEX1);
		private static final Pattern PATTERN2 = Pattern.compile(REGEX2);

		private static void fillInDefaults(Map<String, Parameter> parameters) {
			Parameter parameter = parameters.get(Constants.VERSION_ATTRIBUTE);
			if (parameter == null)
				parameters.put(Constants.VERSION_ATTRIBUTE, new VersionRangeAttribute());
		}
		
		private final Map<String, Parameter> myParameters = new HashMap<String, Parameter>();
		private final String myPath;
		
		public Clause(String clause) {
			Matcher matcher = PATTERN1.matcher(clause);
			if (matcher.find())
				myPath = matcher.group().replaceAll("\\s", "");
			else
				throw new IllegalArgumentException("Invalid " + Constants.IMPORT_PACKAGE + " header clause: " + clause);
			matcher.usePattern(PATTERN2);
			while (matcher.find()) {
				Parameter parameter = ParameterFactory.create(matcher.group());
				// TODO Revisit the following fix.
				// All version attributes on an ImportPackage header are ranges. The ParameterFactory will return
				// a VersionAttribute when the value is a single version (e.g., version=1.0.0). This causes a
				// ClassCastException in getVersionRangeAttribute().
				if (parameter instanceof VersionAttribute)
					parameter = new VersionRangeAttribute(String.valueOf(parameter.getValue()));
				myParameters.put(parameter.getName(), parameter);
			}
			fillInDefaults(myParameters);
		}
		
		@Override
		public Attribute getAttribute(String name) {
			Parameter result = myParameters.get(name);
			if (result instanceof Attribute) {
				return (Attribute)result;
			}
			return null;
		}
		
		@Override
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
		
		@Override
		public Directive getDirective(String name) {
			Parameter result = myParameters.get(name);
			if (result instanceof Directive) {
				return (Directive)result;
			}
			return null;
		}
		
		@Override
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
		
		@Override
		public Parameter getParameter(String name) {
			return myParameters.get(name);
		}
		
		@Override
		public Collection<Parameter> getParameters() {
			return Collections.unmodifiableCollection(myParameters.values());
		}
		
		@Override
		public String getPath() {
			return myPath;
		}
		
		public VersionRangeAttribute getVersionRangeAttribute() {
			return (VersionRangeAttribute)myParameters.get(Constants.VERSION_ATTRIBUTE);
		}
		
		public List<DynamicImportPackageRequirement> toRequirements(Resource resource) {
			Collection<String> pkgs = getPackageNames();
			List<DynamicImportPackageRequirement> result = new ArrayList<DynamicImportPackageRequirement>(pkgs.size());
			for (String pkg : pkgs) {
				result.add(new DynamicImportPackageRequirement(pkg, this, resource));
			}
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
	}
	
	public static final String ATTRIBUTE_BUNDLE_SYMBOLICNAME = PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE;
	public static final String ATTRIBUTE_BUNDLE_VERSION = PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
	public static final String ATTRIBUTE_VERSION = PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE;
	public static final String NAME = Constants.DYNAMICIMPORT_PACKAGE;
	
	private static Collection<Clause> processHeader(String header) {
		Set<Clause> clauses = new HashSet<Clause>();
		for (String clause : new ClauseTokenizer(header).getClauses())
			clauses.add(new Clause(clause));
		return clauses;
	}
	
	private final Set<Clause> clauses;
	
	public DynamicImportPackageHeader(Collection<Clause> clauses) {
		if (clauses.isEmpty())
			throw new IllegalArgumentException("A DynamicImport-Package header must have at least one clause");
		this.clauses = new HashSet<Clause>(clauses);
	}
	
	public DynamicImportPackageHeader(String header) {
		this(processHeader(header));
	}
	
	public Collection<DynamicImportPackageHeader.Clause> getClauses() {
		return Collections.unmodifiableSet(clauses);
	}

	public String getName() {
		return Constants.IMPORT_PACKAGE;
	}
	
	@Override
	public String getValue() {
		return toString();
	}
	
	@Override
	public List<DynamicImportPackageRequirement> toRequirements(Resource resource) {
		Collection<Clause> clauses = getClauses();
		List<DynamicImportPackageRequirement> result = new ArrayList<DynamicImportPackageRequirement>(clauses.size());
		for (Clause clause : clauses) {
			result.addAll(clause.toRequirements(resource));
		}
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
