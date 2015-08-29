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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Resource;

public class DynamicImportPackageHeader extends AbstractClauseBasedHeader<DynamicImportPackageHeader.Clause> implements RequirementHeader<DynamicImportPackageHeader.Clause> {

    public static class Clause extends AbstractClause {
		private static final String REGEX1 = '(' + Grammar.WILDCARD_NAMES + ")(?=;|\\z)";
		private static final String REGEX2 = '(' + Grammar.PARAMETER + ")(?=;|\\z)";
		private static final Pattern PATTERN1 = Pattern.compile(REGEX1);
		private static final Pattern PATTERN2 = Pattern.compile(REGEX2);

		private static void fillInDefaults(Map<String, Parameter> parameters) {
			Parameter parameter = parameters.get(Constants.VERSION_ATTRIBUTE);
			if (parameter == null)
				parameters.put(Constants.VERSION_ATTRIBUTE, new VersionRangeAttribute());
		}
		
		public Clause(String clause) {
		    super(clause);
		}
		
		public Collection<String> getPackageNames() {
			return Arrays.asList(path.split(";"));
		}
		
		public VersionRangeAttribute getVersionRangeAttribute() {
			return (VersionRangeAttribute)parameters.get(Constants.VERSION_ATTRIBUTE);
		}

        @Override
        protected void processClauseString(String clauseString)
                throws IllegalArgumentException {

            Matcher matcher = PATTERN1.matcher(clauseString);
            if (matcher.find())
                path = matcher.group().replaceAll("\\s", "");
            else
                throw new IllegalArgumentException("Invalid " + Constants.IMPORT_PACKAGE + " header clause: " + clauseString);
            matcher.usePattern(PATTERN2);
            while (matcher.find()) {
                Parameter parameter = ParameterFactory.create(matcher.group());
                // TODO Revisit the following fix.
                // All version attributes on an ImportPackage header are ranges. The ParameterFactory will return
                // a VersionAttribute when the value is a single version (e.g., version=1.0.0). This causes a
                // ClassCastException in getVersionRangeAttribute().
                if (parameter instanceof VersionAttribute)
                    parameter = new VersionRangeAttribute(String.valueOf(parameter.getValue()));
                parameters.put(parameter.getName(), parameter);
            }
            fillInDefaults(parameters);
        
        }
        
		public List<DynamicImportPackageRequirement> toRequirements(Resource resource) {
			Collection<String> pkgs = getPackageNames();
			List<DynamicImportPackageRequirement> result = new ArrayList<DynamicImportPackageRequirement>(pkgs.size());
			for (String pkg : pkgs) {
				result.add(new DynamicImportPackageRequirement(pkg, this, resource));
			}
			return result;
		}
	}
	
	public static final String ATTRIBUTE_BUNDLE_SYMBOLICNAME = PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE;
	public static final String ATTRIBUTE_BUNDLE_VERSION = PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
	public static final String ATTRIBUTE_VERSION = PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE;
	public static final String NAME = Constants.DYNAMICIMPORT_PACKAGE;
	
	@Override
    protected Collection<Clause> processHeader(String header) {
		Set<Clause> lclauses = new HashSet<Clause>();
		for (String clause : new ClauseTokenizer(header).getClauses())
			lclauses.add(new Clause(clause));
		return lclauses;
	}
	
	public DynamicImportPackageHeader(Collection<Clause> clauses) {
		super(clauses);
	}
	
	public DynamicImportPackageHeader(String header) {
		super(header);
	}
	
	@Override
    public String getName() {
		return Constants.IMPORT_PACKAGE;
	}
	
	@Override
	public String getValue() {
		return toString();
	}
	
	@Override
	public List<DynamicImportPackageRequirement> toRequirements(Resource resource) {
		Collection<Clause> lclauses = getClauses();
		List<DynamicImportPackageRequirement> result = new ArrayList<DynamicImportPackageRequirement>(lclauses.size());
		for (Clause clause : lclauses) {
			result.addAll(clause.toRequirements(resource));
		}
		return result;
	}
}
