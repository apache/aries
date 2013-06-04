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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class BundleRequiredExecutionEnvironmentHeader implements RequirementHeader<BundleRequiredExecutionEnvironmentHeader.Clause> {
	public static class Clause implements org.apache.aries.subsystem.core.archive.Clause {
		public static class ExecutionEnvironment {
			public static class Parser {
				private static final String BREE = "(" + Grammar.TOKEN + ")(?:-(" + Grammar.VERSION + "))?(?:/(" + Grammar.TOKEN + ")(?:-(" + Grammar.VERSION + "))?)?";
				private static final Pattern PATTERN = Pattern.compile(BREE);
				
				public ExecutionEnvironment parse(String clause) {
					Matcher matcher = PATTERN.matcher(clause);
					if (matcher.matches() && versionsMatch(matcher)) {
						return new ExecutionEnvironment(
								computeName(matcher),
								computeVersion(matcher));
					}
					else
						return new ExecutionEnvironment(clause);
				}
				
				private String computeName(Matcher matcher) {
					return computeName(matcher.group(1), matcher.group(3));
				}
				
				private String computeName(String left, String right) {
					if (left.equalsIgnoreCase("J2SE"))
						left = "JavaSE";
					if (right == null)
						return left;
					return new StringBuilder(left).append('/').append(right).toString();
				}
				
				private Version computeVersion(Matcher matcher) {
					String version = matcher.group(2);
					if (version == null)
						version = matcher.group(4);
					if (version == null)
						return null;
					return Version.parseVersion(version);
				}
				
				private boolean versionsMatch(Matcher matcher) {
					String version1 = matcher.group(2);
					String version2 = matcher.group(4);
					if (version1 == null || version2 == null)
						return true;
					return version1.equals(version2);
				}
			}
			
			private final String name;
			private final Version version;
			
			public ExecutionEnvironment(String name) {
				this(name, null);
			}
			
			public ExecutionEnvironment(String name, Version version) {
				if (name == null)
					throw new NullPointerException();
				this.name = name;
				this.version = version;
			}
			
			public String getName() {
				return name;
			}
			
			public Version getVersion() {
				return version;
			}
		}
		
		private final ExecutionEnvironment executionEnvironment;
		private final String path;
		
		public Clause(String clause) {
			path = clause;
			executionEnvironment = new ExecutionEnvironment.Parser().parse(clause);
		}
		
		@Override
		public Attribute getAttribute(String name) {
			return null;
		}

		@Override
		public Collection<Attribute> getAttributes() {
			return Collections.emptyList();
		}

		@Override
		public Directive getDirective(String name) {
			return null;
		}

		@Override
		public Collection<Directive> getDirectives() {
			return Collections.emptyList();
		}
		
		public ExecutionEnvironment getExecutionEnvironment() {
			return executionEnvironment;
		}

		@Override
		public Parameter getParameter(String name) {
			return null;
		}

		@Override
		public Collection<Parameter> getParameters() {
			return Collections.emptyList();
		}

		@Override
		public String getPath() {
			return path;
		}
		
		public OsgiExecutionEnvironmentRequirement toRequirement(Resource resource) {
			return new OsgiExecutionEnvironmentRequirement(this, resource);
		}
		
		@Override
		public String toString() {
			return getPath();
		}
	}
	
	@SuppressWarnings("deprecation")
	public static final String NAME = Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT;
	
	private final Collection<Clause> clauses;

	public BundleRequiredExecutionEnvironmentHeader(String value) {
		ClauseTokenizer tokenizer = new ClauseTokenizer(value);
		clauses = new ArrayList<Clause>(tokenizer.getClauses().size());
		for (String clause : tokenizer.getClauses())
			clauses.add(new Clause(clause));
	}

	@Override
	public Collection<Clause> getClauses() {
		return Collections.unmodifiableCollection(clauses);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getValue() {
		StringBuilder builder = new StringBuilder();
		for (Clause clause : getClauses()) {
			builder.append(clause).append(',');
		}
		// Remove the trailing comma. Note at least one clause is guaranteed to exist.
		builder.deleteCharAt(builder.length() - 1);
		return builder.toString();
	}

	@Override
	public List<? extends Requirement> toRequirements(Resource resource) {
		return Collections.singletonList(new OsgiExecutionEnvironmentRequirement(getClauses(), resource));
	}
	
	@Override
	public String toString() {
		return getValue();
	}
}
