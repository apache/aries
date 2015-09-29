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
import java.util.List;

import org.osgi.framework.Constants;
import org.osgi.resource.Resource;

public class ProvideCapabilityHeader extends AbstractClauseBasedHeader<ProvideCapabilityHeader.Clause> implements CapabilityHeader<ProvideCapabilityHeader.Clause> {	
    public static class Clause extends AbstractClause {
		public static final String DIRECTIVE_EFFECTIVE = EffectiveDirective.NAME;
		public static final String DIRECTIVE_USES = Constants.USES_DIRECTIVE;
		
		private static final Collection<Parameter> defaultParameters = generateDefaultParameters(
				EffectiveDirective.DEFAULT);
		
		public Clause(String clause) {
            super(
            		parsePath(clause, Patterns.NAMESPACE, false),
            		parseTypedParameters(clause),
            		defaultParameters);
		}

        public String getNamespace() {
            return path;
        }

		public ProvideCapabilityCapability toCapability(Resource resource) {
			return new ProvideCapabilityCapability(this, resource);
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
	
	public static final String NAME = Constants.PROVIDE_CAPABILITY;
	
	public ProvideCapabilityHeader(String value) {
		super(
				value, 
				new ClauseFactory<Clause>() {
					@Override
					public Clause newInstance(String clause) {
						return new Clause(clause);
					}
				});
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
	public List<ProvideCapabilityCapability> toCapabilities(Resource resource) {
		List<ProvideCapabilityCapability> result = new ArrayList<ProvideCapabilityCapability>();
		for (Clause clause : clauses)
			result.add(clause.toCapability(resource));
		return result;
	}
	
}
