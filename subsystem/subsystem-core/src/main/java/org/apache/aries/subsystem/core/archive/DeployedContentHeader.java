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

import org.apache.aries.subsystem.core.internal.ResourceHelper;
import org.apache.aries.subsystem.core.internal.Utils;
import org.osgi.framework.Version;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.SubsystemConstants;

public class DeployedContentHeader implements RequirementHeader<DeployedContentHeader.Clause> {
	public static class Clause implements org.apache.aries.subsystem.core.archive.Clause {
		public static final String ATTRIBUTE_DEPLOYEDVERSION = DeployedVersionAttribute.NAME;
		public static final String ATTRIBUTE_RESOURCEID = "resourceId";
		public static final String ATTRIBUTE_TYPE = TypeAttribute.NAME;
		public static final String DIRECTIVE_REFERENCE = ReferenceDirective.NAME;
		public static final String DIRECTIVE_STARTORDER = StartOrderDirective.NAME;
		
		private static final Pattern PATTERN_SYMBOLICNAME = Pattern.compile('(' + Grammar.SYMBOLICNAME + ")(?=;|\\z)");
		private static final Pattern PATTERN_PARAMETER = Pattern.compile('(' + Grammar.PARAMETER + ")(?=;|\\z)");
		
		private static void fillInDefaults(Map<String, Parameter> parameters) {
			Parameter parameter = parameters.get(ATTRIBUTE_TYPE);
			if (parameter == null)
				parameters.put(ATTRIBUTE_TYPE, TypeAttribute.DEFAULT);
			parameter = parameters.get(DIRECTIVE_REFERENCE);
			if (parameter == null)
				parameters.put(DIRECTIVE_REFERENCE, ReferenceDirective.TRUE);
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
				parameters.put(parameter.getName(), parameter);
			}
			fillInDefaults(parameters);
		}
		
		public Clause(Resource resource) {
			this(resource, true);
		}
		
		public Clause(Resource resource, boolean referenced) {
			this(appendResource(resource, new StringBuilder(), referenced).toString());
		}
		
		public boolean contains(Resource resource) {
			return getSymbolicName().equals(
					ResourceHelper.getSymbolicNameAttribute(resource))
					&& getDeployedVersion().equals(
							ResourceHelper.getVersionAttribute(resource))
					&& getType().equals(
							ResourceHelper.getTypeAttribute(resource));
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof Clause))
				return false;
			Clause that = (Clause)o;
			return getSymbolicName().equals(that.getSymbolicName())
					&& getDeployedVersion().equals(that.getDeployedVersion())
					&& getType().equals(that.getType());
		}
		
		@Override
		public int hashCode() {
			int result = 17;
			result = 31 * result + getSymbolicName().hashCode();
			result = 31 * result + getDeployedVersion().hashCode();
			result = 31 * result + getType().hashCode();
			return result;
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
		
		public Version getDeployedVersion() {
			return ((DeployedVersionAttribute)getAttribute(ATTRIBUTE_DEPLOYEDVERSION)).getVersion();
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
		
		public int getStartOrder() {
			return ((StartOrderDirective)getAttribute(DIRECTIVE_STARTORDER)).getStartOrder();
		}
		
		public String getType() {
			return ((TypeAttribute)getAttribute(ATTRIBUTE_TYPE)).getType();
		}
		
		public boolean isReferenced() {
			return ((ReferenceDirective)getDirective(DIRECTIVE_REFERENCE)).isReferenced();
		}
		
		public DeployedContentRequirement toRequirement(Resource resource) {
			return new DeployedContentRequirement(this, resource);
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
	
	public static final String NAME = SubsystemConstants.DEPLOYED_CONTENT;
	
	public static DeployedContentHeader newInstance(Collection<Resource> resources) {
		StringBuilder builder = new StringBuilder();
		for (Resource resource : resources) {
			appendResource(resource, builder, true);
			builder.append(',');
		}
		// Remove the trailing comma.
		// TODO Intentionally letting the exception propagate since there must be at least one resource.
		builder.deleteCharAt(builder.length() - 1);
		return new DeployedContentHeader(builder.toString());
	}
	
	private static StringBuilder appendResource(Resource resource, StringBuilder builder, boolean referenced) {
		String symbolicName = ResourceHelper.getSymbolicNameAttribute(resource);
		Version version = ResourceHelper.getVersionAttribute(resource);
		String type = ResourceHelper.getTypeAttribute(resource);
		builder.append(symbolicName)
			.append(';')
			.append(Clause.ATTRIBUTE_DEPLOYEDVERSION)
			.append('=')
			.append(version.toString())
			.append(';')
			.append(Clause.ATTRIBUTE_TYPE)
			.append('=')
			.append(type)
			.append(';')
			.append(Clause.ATTRIBUTE_RESOURCEID)
			.append('=')
			.append(Utils.getId(resource))
			.append(';')
			.append(Clause.DIRECTIVE_REFERENCE)
			.append(":=")
			.append(referenced);
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
	
	public DeployedContentHeader(Collection<Clause> clauses) {
		if (clauses.isEmpty())
			throw new IllegalArgumentException("A " + NAME + " header must have at least one clause");
		this.clauses = new HashSet<Clause>(clauses);
	}
	
	public DeployedContentHeader(String value) {
		this(processHeader(value));
	}
	
	public boolean contains(Resource resource) {
		for (Clause clause : getClauses())
			if (clause.contains(resource))
				return true;
		return false;
	}
	
	public Clause getClause(Resource resource) {
		String symbolicName = ResourceHelper.getSymbolicNameAttribute(resource);
		Version version = ResourceHelper.getVersionAttribute(resource);
		String type = ResourceHelper.getTypeAttribute(resource);
		for (Clause clause : clauses) {
			if (symbolicName.equals(clause.getPath())
					&& clause.getDeployedVersion().equals(version)
					&& type.equals(clause.getType()))
				return clause;
		}
		return null;
	}
	
	@Override
	public Collection<DeployedContentHeader.Clause> getClauses() {
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
	
	public boolean isReferenced(Resource resource) {
		DeployedContentHeader.Clause clause = getClause(resource);
		if (clause == null)
			return false;
		return clause.isReferenced();
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
