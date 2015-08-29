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

public abstract class AbstractClause implements Clause {

    protected final Map<String, Parameter> parameters = new HashMap<String, Parameter>();
    protected String path;

    protected AbstractClause() {

    }

    public AbstractClause(String clause) {
        processClauseString(clause);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractClause other = (AbstractClause) obj;
        if (path == null) {
            if (other.path != null)
                return false;
        } else
            if (!path.equals(other.path))
                return false;
        if (parameters == null) {
            if (other.parameters != null)
                return false;
        } else
            if (!parameters.equals(other.parameters))
                return false;
        return true;
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
        final int prime = 31;
        int result = 1;
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result
                + ((parameters == null) ? 0 : parameters.hashCode());
        return result;
    }

    protected abstract void processClauseString(String clauseString)
            throws IllegalArgumentException;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append(getPath());
        for (Parameter parameter : getParameters()) {
            builder.append(';').append(parameter);
        }
        return builder.toString();
    }
}
