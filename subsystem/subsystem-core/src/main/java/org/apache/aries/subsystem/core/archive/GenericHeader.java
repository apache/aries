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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GenericHeader implements Header<GenericHeader.Clause> {
	public static class Clause implements org.apache.aries.subsystem.core.archive.Clause {
		private final String path;
		
		public Clause(String clause) {
			path = clause;
		}
		
		public Attribute getAttribute(String name) {
			return null;
		}
		
		public Collection<Attribute> getAttributes() {
			return Collections.emptyList();
		}
		
		public Directive getDirective(String name) {
			return null;
		}
		
		public Collection<Directive> getDirectives() {
			return Collections.emptyList();
		}
		
		public Parameter getParameter(String name) {
			return null;
		}
		
		public Collection<Parameter> getParameters() {
			return Collections.emptyList();
		}
		
		public String getPath() {
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
	
	private final Set<Clause> clauses;
	private final String name;
	
	public GenericHeader(String name, Collection<Clause> clauses) {
		this.name = name;
		this.clauses = new HashSet<Clause>(clauses);
	}
	
	public GenericHeader(String name, String value) {
		this(name, Collections.singletonList(new Clause(value)));
	}

	@Override
	public Collection<Clause> getClauses() {
		return Collections.unmodifiableSet(clauses);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getValue() {
		return toString();
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
