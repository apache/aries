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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;

public abstract class AbstractHeader implements Header<Clause> {
	protected static final String REGEX = Grammar.CLAUSE + "(?=,|\\z)";
	protected static final Pattern PATTERN = Pattern.compile(REGEX);
	
	// TODO This is specific to deployment manifests and shouldn't be at this level.
	protected static void appendResource(Resource resource, StringBuilder builder) {
		Map<String, Object> attributes = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0).getAttributes();
		String symbolicName = (String)attributes.get(IdentityNamespace.IDENTITY_NAMESPACE);
		Version version = (Version)attributes.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
		String namespace = (String)attributes.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
		builder.append(symbolicName)
			.append(';')
			.append(DeployedVersionAttribute.NAME)
			.append('=')
			.append(version.toString())
			.append(';')
			.append(TypeAttribute.NAME)
			.append('=')
			.append(namespace);
	}
	

    protected final List<Clause> clauses = new ArrayList<Clause>();
	protected final String name;
	protected final String value;
	
	public AbstractHeader(String name, String value) {
		this.name = name;
		this.value = value;
		Matcher matcher = PATTERN.matcher(value);
		while (matcher.find()){
			clauses.add(new GenericClause(matcher.group()));
		}
		if (clauses.isEmpty())
			throw new IllegalArgumentException("Invalid header syntax -> " + name + ": " + value);
	}
	
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractHeader other = (AbstractHeader) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else
            if (!name.equals(other.name))
                return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else
            if (!value.equals(other.value))
                return false;
        if (clauses == null) {
            if (other.clauses != null)
                return false;
        } else
            if (!clauses.equals(other.clauses))
                return false;
        return true;
    }

	@Override
    public List<Clause> getClauses() {
		return Collections.unmodifiableList(clauses);
	}

	@Override
    public String getName() {
		return name;
	}
	
	@Override
    public String getValue() {
		return value;
	}
	
	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + ((name == null) ? 0 : name.hashCode());
	    result = prime * result + ((value == null) ? 0 : value.hashCode());
	    result = prime * result + ((clauses == null) ? 0 : clauses.hashCode());
	    return result;
	}

	@Override
    public String toString() {
		return new StringBuilder(getClass().getName())
		.append(": name=")
		.append(name)
		.append(", value=")
		.append(value)
		.toString();
	}
}
