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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.subsystem.SubsystemConstants;

public class SubsystemTypeHeader implements Header<SubsystemTypeHeader.Clause> {
	public static class Clause implements org.apache.aries.subsystem.core.archive.Clause {
		private static final Pattern PATTERN_TYPE = Pattern.compile('(' + TYPE_APPLICATION + '|' + TYPE_COMPOSITE + '|' + TYPE_FEATURE + ")(?=;|\\z)");
		private static final Pattern PATTERN_PARAMETER = Pattern.compile('(' + Grammar.PARAMETER + ")(?=;|\\z)");
		private static final Pattern PATTERN_PROVISION_POLICY = Pattern.compile(PROVISION_POLICY_ACCEPT_DEPENDENCIES + '|' + PROVISION_POLICY_REJECT_DEPENDENCIES);
		
		private static void fillInDefaults(Map<String, Parameter> parameters) {
			Parameter parameter = parameters.get(DIRECTIVE_PROVISION_POLICY);
			if (parameter == null)
				parameter = ProvisionPolicyDirective.REJECT_DEPENDENCIES;
			String value = ((Directive)parameter).getValue();
			if (!PATTERN_PROVISION_POLICY.matcher(value).matches())
				throw new IllegalArgumentException("Invalid " + DIRECTIVE_PROVISION_POLICY + " directive: " + value);
			parameters.put(DIRECTIVE_PROVISION_POLICY, parameter);
		}
		
		private final String path;
		private final Map<String, Parameter> parameters = new HashMap<String, Parameter>();
		
		public Clause(String clause) {
			Matcher matcher = PATTERN_TYPE.matcher(clause);
			if (!matcher.find())
				throw new IllegalArgumentException("Invalid subsystem type: " + clause);
			path = matcher.group();
			matcher.usePattern(PATTERN_PARAMETER);
			while (matcher.find()) {
				Parameter parameter = ParameterFactory.create(matcher.group());
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
		
		public ProvisionPolicyDirective getProvisionPolicyDirective() {
			return (ProvisionPolicyDirective)getDirective(DIRECTIVE_PROVISION_POLICY);
		}
		
		public String getType() {
			return path;
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
	
	public static final String DIRECTIVE_PROVISION_POLICY = SubsystemConstants.PROVISION_POLICY_DIRECTIVE;
	public static final String NAME = SubsystemConstants.SUBSYSTEM_TYPE;
	public static final String PROVISION_POLICY_ACCEPT_DEPENDENCIES = SubsystemConstants.PROVISION_POLICY_ACCEPT_DEPENDENCIES;
	public static final String PROVISION_POLICY_REJECT_DEPENDENCIES = SubsystemConstants.PROVISION_POLICY_REJECT_DEPENDENCIES;
	public static final String TYPE_APPLICATION = SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION;
	public static final String TYPE_COMPOSITE = SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE;
	public static final String TYPE_FEATURE = SubsystemConstants.SUBSYSTEM_TYPE_FEATURE;
	
	public static final SubsystemTypeHeader DEFAULT = new SubsystemTypeHeader(TYPE_APPLICATION);
	
	private final Clause clause;
	
	public SubsystemTypeHeader(Clause clause) {
		if (clause == null)
			throw new NullPointerException("Missing required parameter: clause");
		this.clause = clause;
	}
	
	public SubsystemTypeHeader(String value) {
		this(new Clause(value));
	}
	
	public Clause getClause() {
		return clause;
	}
	
	@Override
	public Collection<SubsystemTypeHeader.Clause> getClauses() {
		return Collections.singleton(clause);
	}

	@Override
	public String getName() {
		return NAME;
	}
	
	public ProvisionPolicyDirective getProvisionPolicyDirective() {
		return clause.getProvisionPolicyDirective();
	}
	
	public String getType() {
		return clause.getType();
	}

	@Override
	public String getValue() {
		return toString();
	}
	
	public boolean isApplication() {
		return this == DEFAULT || TYPE_APPLICATION.equals(getType());
	}
	
	public boolean isComposite() {
		return TYPE_COMPOSITE.equals(getType());
	}
	
	public boolean isFeature() {
		return TYPE_FEATURE.equals(getType());
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
