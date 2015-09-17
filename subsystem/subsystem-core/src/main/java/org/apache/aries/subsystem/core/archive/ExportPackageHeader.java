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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;
import org.osgi.resource.Resource;

public class ExportPackageHeader extends AbstractClauseBasedHeader<ExportPackageHeader.Clause> implements CapabilityHeader<ExportPackageHeader.Clause> {

    public static class Clause extends AbstractClause {
		public static final String ATTRIBUTE_VERSION = Constants.VERSION_ATTRIBUTE;
		public static final String DIRECTIVE_EXCLUDE = Constants.EXCLUDE_DIRECTIVE;
		public static final String DIRECTIVE_INCLUDE = Constants.INCLUDE_DIRECTIVE;
		public static final String DIRECTIVE_MANDATORY = Constants.MANDATORY_DIRECTIVE;
		public static final String DIRECTIVE_USES = Constants.USES_DIRECTIVE;
		
		private static final Pattern PATTERN_PACKAGENAME = Pattern.compile('(' + Grammar.PACKAGENAME + ")(?=;|\\z)");
		private static final Pattern PATTERN_PACKAGENAMES = Pattern.compile('(' + Grammar.PACKAGENAMES + ")(?=;|\\z)");
		private static final Pattern PATTERN_PARAMETER = Pattern.compile('(' + Grammar.PARAMETER + ")(?=;|\\z)");
		
		private static void fillInDefaults(Map<String, Parameter> parameters) {
			Parameter parameter = parameters.get(ATTRIBUTE_VERSION);
			if (parameter == null)
				parameters.put(ATTRIBUTE_VERSION, VersionAttribute.DEFAULT);
		}
		
		private Collection<String> packageNames;
		
		public Clause(String clause) {
            super(clause);
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
		
		public Collection<String> getPackageNames() {
			return Collections.unmodifiableCollection(packageNames);
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
		
        @Override
        protected void processClauseString(String clauseString)
                throws IllegalArgumentException {
            packageNames = new HashSet<String>();
            Matcher main = PATTERN_PACKAGENAMES.matcher(clauseString);
            if (!main.find())
                throw new IllegalArgumentException("Missing package names path: " + clauseString);
            path = main.group();
            Matcher lpath = PATTERN_PACKAGENAME.matcher(this.path);
            while (lpath.find()){
                packageNames.add(main.group());
            }
            main.usePattern(PATTERN_PARAMETER);
            while (main.find()) {
                Parameter parameter = ParameterFactory.create(main.group());
                parameters.put(parameter.getName(), parameter);
            }
            fillInDefaults(parameters);
        }
        
		public Collection<ExportPackageCapability> toCapabilities(Resource resource) {
			Collection<ExportPackageCapability> result = new ArrayList<ExportPackageCapability>(packageNames.size());
			for (String packageName : packageNames)
				result.add(new ExportPackageCapability(packageName, parameters.values(), resource));
			return result;
		}
	}
	
	public static final String NAME = Constants.EXPORT_PACKAGE;
	
	public ExportPackageHeader(String value) {
		super(value);
	}
	
    @Override
    protected Collection<Clause> processHeader(String value) {
        Collection<String> clauseStrs = new ClauseTokenizer(value).getClauses();
        Set<Clause> lclauses = new HashSet<Clause>(clauseStrs.size());
        for (String clause : new ClauseTokenizer(value).getClauses())
            lclauses.add(new Clause(clause));
        return lclauses;
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
	public List<ExportPackageCapability> toCapabilities(Resource resource) {
		List<ExportPackageCapability> result = new ArrayList<ExportPackageCapability>();
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
