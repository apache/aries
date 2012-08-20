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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.subsystem.core.internal.AriesSubsystem;
import org.apache.aries.subsystem.core.internal.OsgiIdentityRequirement;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.SubsystemConstants;

public class AriesSubsystemParentsHeader implements RequirementHeader<AriesSubsystemParentsHeader.Clause> {
	public static class Clause implements org.apache.aries.subsystem.core.archive.Clause {
		public static final String ATTRIBUTE_VERSION = VersionRangeAttribute.NAME;
		public static final String ATTRIBUTE_RESOURCEID = "resourceId";
		public static final String ATTRIBUTE_TYPE = TypeAttribute.NAME;
		
		private static final Pattern PATTERN_SYMBOLICNAME = Pattern.compile('(' + Grammar.SYMBOLICNAME + ")(?=;|\\z)");
		private static final Pattern PATTERN_PARAMETER = Pattern.compile('(' + Grammar.PARAMETER + ")(?=;|\\z)");
		
		private static void fillInDefaults(Map<String, Parameter> parameters) {
			Parameter parameter = parameters.get(ATTRIBUTE_TYPE);
			if (parameter == null)
				parameters.put(ATTRIBUTE_TYPE, TypeAttribute.newInstance(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION));
			parameter = parameters.get(ATTRIBUTE_VERSION);
			if (parameter == null)
				parameters.put(ATTRIBUTE_VERSION, VersionAttribute.DEFAULT);
		}
		
		private final String path;
		private final Map<String, Parameter> parameters = new HashMap<String, Parameter>();
		
		public Clause(String clause) {
			Matcher matcher = PATTERN_SYMBOLICNAME.matcher(clause);
			if (!matcher.find())
				throw new IllegalArgumentException("Missing symbolic name path: " + clause);
			path = matcher.group();
			matcher.usePattern(PATTERN_PARAMETER);
			while (matcher.find()) {
				Parameter parameter = ParameterFactory.create(matcher.group());
				if (parameter instanceof VersionAttribute)
					parameter = new VersionRangeAttribute(new VersionRange(String.valueOf(parameter.getValue())));
				parameters.put(parameter.getName(), parameter);
			}
			fillInDefaults(parameters);
		}
		
		public Clause(AriesSubsystem subsystem, boolean referenceCount) {
			this(appendSubsystem(subsystem, new StringBuilder(), referenceCount).toString());
		}
		
		public boolean contains(AriesSubsystem subsystem) {
			return getSymbolicName().equals(
					subsystem.getSymbolicName())
					&& getVersion().equals(
							subsystem.getVersion())
					&& getType().equals(
							subsystem.getType());
		}
		
		@Override
		public Attribute getAttribute(String name) {
			Parameter result = parameters.get(name);
			if (result instanceof Attribute)
				return (Attribute)result;
			return null;
		}

		@Override
		public Collection<Attribute> getAttributes() {
			ArrayList<Attribute> attributes = new ArrayList<Attribute>(parameters.size());
			for (Parameter parameter : parameters.values())
				if (parameter instanceof Attribute)
					attributes.add((Attribute)parameter);
			attributes.trimToSize();
			return attributes;
		}

		@Override
		public Directive getDirective(String name) {
			Parameter result = parameters.get(name);
			if (result instanceof Directive)
				return (Directive)result;
			return null;
		}

		@Override
		public Collection<Directive> getDirectives() {
			ArrayList<Directive> directives = new ArrayList<Directive>(parameters.size());
			for (Parameter parameter : parameters.values())
				if (parameter instanceof Directive)
					directives.add((Directive)parameter);
			directives.trimToSize();
			return directives;
		}
		
		public long getId() {
			Attribute attribute = getAttribute(ATTRIBUTE_RESOURCEID);
			if (attribute == null)
				return -1;
			return Long.valueOf(String.valueOf(attribute.getValue()));
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
		
		public String getType() {
			return ((TypeAttribute)getAttribute(ATTRIBUTE_TYPE)).getType();
		}
		
		public Version getVersion() {
			return ((VersionAttribute)getAttribute(ATTRIBUTE_VERSION)).getVersion();
		}
		
		public OsgiIdentityRequirement toRequirement(Resource resource) {
			return new OsgiIdentityRequirement(getSymbolicName(), getVersion(), getType(), false);
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
	
	public static final String NAME = "AriesSubsystem-Parents";
	
	private static StringBuilder appendSubsystem(AriesSubsystem subsystem, StringBuilder builder, boolean referenceCount) {
		String symbolicName = subsystem.getSymbolicName();
		Version version = subsystem.getVersion();
		String type = subsystem.getType();
		builder.append(symbolicName)
			.append(';')
			.append(Clause.ATTRIBUTE_VERSION)
			.append('=')
			.append(version.toString())
			.append(';')
			.append(Clause.ATTRIBUTE_TYPE)
			.append('=')
			.append(type)
			.append(';')
			.append(Clause.ATTRIBUTE_RESOURCEID)
			.append('=')
			.append(subsystem.getSubsystemId());
		return builder;
	}
	
	private static Collection<Clause> processHeader(String value) {
		Collection<String> clauseStrs = new ClauseTokenizer(value).getClauses();
		Set<Clause> clauses = new HashSet<Clause>(clauseStrs.size());
		for (String clause : new ClauseTokenizer(value).getClauses())
			clauses.add(new Clause(clause));
		return clauses;
	}
	
	private final Set<Clause> clauses;
	
	public AriesSubsystemParentsHeader(Collection<Clause> clauses) {
		if (clauses.isEmpty())
			throw new IllegalArgumentException("A " + NAME + " header must have at least one clause");
		this.clauses = new HashSet<Clause>(clauses);
	}
	
	public AriesSubsystemParentsHeader(String value) {
		this(processHeader(value));
	}
	
	public boolean contains(AriesSubsystem subsystem) {
		return getClause(subsystem) != null;
	}
	
	public Clause getClause(AriesSubsystem subsystem) {
		String symbolicName = subsystem.getSymbolicName();
		Version version = subsystem.getVersion();
		String type = subsystem.getType();
		for (Clause clause : clauses) {
			if (symbolicName.equals(clause.getPath())
					&& clause.getVersion().equals(version)
					&& type.equals(clause.getType()))
				return clause;
		}
		return null;
	}
	
	@Override
	public Collection<AriesSubsystemParentsHeader.Clause> getClauses() {
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
	public List<Requirement> toRequirements(Resource resource) {
		List<Requirement> requirements = new ArrayList<Requirement>(clauses.size());
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
