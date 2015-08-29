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
import org.osgi.resource.Resource;

public class ProvideCapabilityHeader extends AbstractClauseBasedHeader<ProvideCapabilityHeader.Clause> implements CapabilityHeader<ProvideCapabilityHeader.Clause> {	

    public static class Clause extends AbstractClause {
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
		
		public Clause(String clause) {
            super(clause);
		}

        @Override
        protected void processClauseString(String clauseString)
                throws IllegalArgumentException {
            Matcher matcher = PATTERN_NAMESPACE.matcher(clauseString);
            if (!matcher.find())
                throw new IllegalArgumentException("Missing namespace path: " + clauseString);
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

        public String getNamespace() {
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
	
	public ProvideCapabilityHeader(String value) {
		super(value);
	}
	
    @Override
    protected Collection<Clause> processHeader(String header) {
        Matcher matcher = PATTERN.matcher(header);
        Set<Clause> lclauses = new HashSet<Clause>();
        while (matcher.find())
            lclauses.add(new Clause(matcher.group()));
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
	public List<ProvideCapabilityCapability> toCapabilities(Resource resource) {
		List<ProvideCapabilityCapability> result = new ArrayList<ProvideCapabilityCapability>();
		for (Clause clause : clauses)
			result.add(clause.toCapability(resource));
		return result;
	}
	
}
