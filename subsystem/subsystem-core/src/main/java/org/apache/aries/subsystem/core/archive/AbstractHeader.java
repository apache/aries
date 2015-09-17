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

import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;

public abstract class AbstractHeader implements Header<Clause> {
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
	
	protected final List<Clause> clauses;
	protected final String name;
	protected final String value;
	
	public AbstractHeader(String name, String value) {
		if (name == null) {
			throw new NullPointerException();
		}
		ClauseTokenizer tokenizer = new ClauseTokenizer(value);
		List<Clause> clauses = new ArrayList<Clause>(tokenizer.getClauses().size());
		for (String clause : tokenizer.getClauses()) {
			clauses.add(new GenericClause(clause));
		}
		if (clauses.isEmpty()) {
			throw new IllegalArgumentException("Invalid header syntax -> " + name + ": " + value);
		}
		this.name = name;
		this.value = value;
		this.clauses = Collections.synchronizedList(clauses);
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
    public boolean equals(Object o) {
    	if (o == this) {
    		return true;
    	}
    	if (!(o instanceof AbstractHeader)) {
    		return false;
    	}
    	AbstractHeader that = (AbstractHeader)o;
    	return that.name.equals(this.name)
    			&& that.clauses.equals(this.clauses);
    }
	
	@Override
	public int hashCode() {
	    int result = 17;
	    result = 31 * result + name.hashCode();
	    result = 31 * result + clauses.hashCode();
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
