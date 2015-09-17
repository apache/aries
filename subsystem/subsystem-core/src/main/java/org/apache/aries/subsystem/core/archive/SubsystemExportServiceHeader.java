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

public class SubsystemExportServiceHeader extends AbstractClauseBasedHeader<SubsystemExportServiceHeader.Clause> {

    public static class Clause extends AbstractClause {
		public static final String DIRECTIVE_FILTER = Constants.FILTER_DIRECTIVE;

		private static final Pattern PATTERN_OBJECTCLASS_OR_STAR = Pattern.compile("((" + Grammar.OBJECTCLASS + ")|[*])(?=;|\\z)");
		private static final Pattern PATTERN_PARAMETER = Pattern.compile('(' + Grammar.PARAMETER + ")(?=;|\\z)");

		@SuppressWarnings("unused")
        private static void fillInDefaults(Map<String, Parameter> parameters) {
			// No defaults.
		}

		public Clause(String clause) {
            super(clause);
		}

		public String getObjectClass() {
			return path;
		}

        @Override
        protected void processClauseString(String clauseString)
                throws IllegalArgumentException {
            Matcher main = PATTERN_OBJECTCLASS_OR_STAR.matcher(clauseString);
            if (!main.find())
                throw new IllegalArgumentException("Missing objectClass path: " + clauseString);
            path = main.group();
            main.usePattern(PATTERN_PARAMETER);
            while (main.find()) {
                Parameter parameter = ParameterFactory.create(main.group());
                parameters.put(parameter.getName(), parameter);
            }
            fillInDefaults(parameters);
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

    private static final Pattern PATTERN = Pattern.compile("(" + Grammar.SERVICE_OR_WILDCARD + ")(?=,|\\z)");

	public SubsystemExportServiceHeader(String value) {
		super(value);
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
    protected Collection<Clause> processHeader(String header) {
        Matcher matcher = PATTERN.matcher(header);
        Set<Clause> lclauses = new HashSet<Clause>();
        while (matcher.find())
            lclauses.add(new Clause(matcher.group()));
        return lclauses;
    }
}
