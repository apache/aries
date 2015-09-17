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

import org.osgi.framework.VersionRange;

public abstract class AbstractClause implements Clause {
	protected static Collection<Parameter> generateDefaultParameters(Parameter... parameters) {
		if (parameters == null || parameters.length == 0) {
			return Collections.emptyList();
		}
		Collection<Parameter> defaults = new ArrayList<Parameter>(parameters.length);
		for (Parameter parameter : parameters) {
			defaults.add(parameter);
		}
		return defaults;
	}
	
	protected static Map<String, Parameter> parseParameters(String clause, boolean replaceVersionWithVersionRange) {
    	Map<String, Parameter> parameters = new HashMap<String, Parameter>();
		Matcher matcher = Patterns.PARAMETER.matcher(clause);
		while (matcher.find()) {
            Parameter parameter = ParameterFactory.create(matcher.group());
            if (replaceVersionWithVersionRange && (parameter instanceof VersionAttribute)) {
                parameter = new VersionRangeAttribute(new VersionRange(String.valueOf(parameter.getValue())));
            }
            parameters.put(parameter.getName(), parameter);
        }
		return parameters;
    }
    
    protected static String parsePath(String clause, Pattern pattern, boolean replaceAllWhitespace) {
    	Matcher matcher = pattern.matcher(clause);
        if (!matcher.find())
            throw new IllegalArgumentException("Invalid path: " + clause);
        String path = matcher.group();
        if (replaceAllWhitespace) {
        	path = path.replaceAll("\\s", "");
        }
        return path;
    }
	
	protected final Map<String, Parameter> parameters;
	protected final String path;
    
    public AbstractClause(String path, Map<String, Parameter> parameters, Collection<Parameter> defaultParameters) {
    	if (path == null) {
    		throw new NullPointerException();
    	}
    	for (Parameter parameter : defaultParameters) {
        	String name = parameter.getName();
        	if (parameters.containsKey(name)) {
        		continue;
        	}
        	parameters.put(name, parameter);
    	}
    	this.path = path;
    	this.parameters = Collections.synchronizedMap(parameters);
    }

    @Override
    public Attribute getAttribute(String name) {
        Parameter result = parameters.get(name);
        if (result instanceof Attribute) {
            return (Attribute) result;
        }
        return null;
    }

    @Override
    public Collection<Attribute> getAttributes() {
        ArrayList<Attribute> attributes = new ArrayList<Attribute>(
                parameters.size());
        for (Parameter parameter : parameters.values()) {
            if (parameter instanceof Attribute) {
                attributes.add((Attribute) parameter);
            }
        }
        attributes.trimToSize();
        return attributes;
    }

    @Override
    public Directive getDirective(String name) {
        Parameter result = parameters.get(name);
        if (result instanceof Directive) {
            return (Directive) result;
        }
        return null;
    }

    @Override
    public Collection<Directive> getDirectives() {
        ArrayList<Directive> directives = new ArrayList<Directive>(
                parameters.size());
        for (Parameter parameter : parameters.values()) {
            if (parameter instanceof Directive) {
                directives.add((Directive) parameter);
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

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + path.hashCode();
        result = 31 * result + parameters.hashCode();
        return result;
    }
    
    @Override
    public boolean equals(Object o) {
    	if (o == this) {
    		return true;
    	}
    	if (!(o instanceof AbstractClause)) {
    		return false;
    	}
    	AbstractClause that = (AbstractClause)o;
    	return that.path.equals(this.path)
    			&& that.parameters.equals(this.parameters);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append(getPath());
        for (Parameter parameter : getParameters()) {
            builder.append(';').append(parameter);
        }
        return builder.toString();
    }
}
