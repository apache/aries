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

public class GenericHeader extends AbstractClauseBasedHeader<GenericHeader.Clause> {
	public static class Clause extends AbstractClause {
		public Clause(String clause) {
			super(
					clause,
					Collections.<String, Parameter>emptyMap(),
	        		generateDefaultParameters());
		}
	}
	
	private final String name;
	
	public GenericHeader(String name, Collection<Clause> clauses) {
	    super(clauses);
		this.name = name;
	}
	
	public GenericHeader(String name, String value) {
		super(
				value, 
				new ClauseFactory<Clause>() {
					@Override
					public Clause newInstance(String clause) {
						return new Clause(clause);
					}
				});
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getValue() {
		return toString();
	}
}
