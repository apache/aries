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

public class GenericClause implements Clause {
	private static final String REGEX = '(' + Grammar.PATH + "(?:;" + Grammar.PATH + ")*)(?:;\\s*(" + Grammar.PARAMETER + "))*";
	
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	private static final Pattern PATTERN_PARAMETER = Pattern.compile(Grammar.PARAMETER);
	
	private final Map<String, Parameter> parameters = new HashMap<String, Parameter>();
	private final String path;
	
	public GenericClause(String clause) {
		Matcher matcher = PATTERN.matcher(clause);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid clause: " + clause);
		}
		path = matcher.group(1);
		matcher = PATTERN_PARAMETER.matcher(clause);
		while(matcher.find()) {
			String group = matcher.group();
			if (group == null || group.length() == 0)
				continue;
			Parameter parameter = ParameterFactory.create(group);
			parameters.put(parameter.getName(), parameter);
		}
	}
	
	public Attribute getAttribute(String name) {
		Parameter result = parameters.get(name);
		if (result instanceof Attribute) {
			return (Attribute)result;
		}
		return null;
	}
	
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
	
	public Directive getDirective(String name) {
		Parameter result = parameters.get(name);
		if (result instanceof Directive) {
			return (Directive)result;
		}
		return null;
	}
	
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
	
	public Parameter getParameter(String name) {
		return parameters.get(name);
	}
	
	public Collection<Parameter> getParameters() {
		return Collections.unmodifiableCollection(parameters.values());
	}
	
	public String getPath() {
		return path;
	}
}
