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

import org.osgi.service.subsystem.SubsystemConstants;

public class SubsystemTypeHeader extends AbstractClauseBasedHeader<SubsystemTypeHeader.Clause> {
    public static class Clause extends AbstractClause {
    	private static final Collection<Parameter> defaultParameters = generateDefaultParameters(
    			ProvisionPolicyDirective.REJECT_DEPENDENCIES);
    	
		public Clause(String clause) {
			super(
            		parsePath(clause, Patterns.SUBSYSTEM_TYPE, false), 
            		parseParameters(clause, false), 
            		defaultParameters);
		}
		
		public AriesProvisionDependenciesDirective getProvisionDependenciesDirective() {
			return (AriesProvisionDependenciesDirective)getDirective(DIRECTIVE_PROVISION_DEPENDENCIES);
		}
				
		public ProvisionPolicyDirective getProvisionPolicyDirective() {
			return (ProvisionPolicyDirective)getDirective(DIRECTIVE_PROVISION_POLICY);
		}
		
		public String getType() {
			return path;
		}
	}
	
	public static final String DIRECTIVE_PROVISION_DEPENDENCIES = AriesProvisionDependenciesDirective.NAME;
    public static final String DIRECTIVE_PROVISION_POLICY = ProvisionPolicyDirective.NAME;
	public static final String NAME = SubsystemConstants.SUBSYSTEM_TYPE;
	public static final String ARIES_PROVISION_DEPENDENCIES_INSTALL = AriesProvisionDependenciesDirective.VALUE_INSTALL;
	public static final String ARIES_PROVISION_DEPENDENCIES_RESOLVE = AriesProvisionDependenciesDirective.VALUE_RESOLVE;
	public static final String PROVISION_POLICY_ACCEPT_DEPENDENCIES = ProvisionPolicyDirective.VALUE_ACCEPT_DEPENDENCIES;
	public static final String PROVISION_POLICY_REJECT_DEPENDENCIES = ProvisionPolicyDirective.VALUE_REJECT_DEPENDENCIES;
	public static final String TYPE_APPLICATION = SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION;
	public static final String TYPE_COMPOSITE = SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE;
	public static final String TYPE_FEATURE = SubsystemConstants.SUBSYSTEM_TYPE_FEATURE;
	
	public static final SubsystemTypeHeader DEFAULT = new SubsystemTypeHeader(TYPE_APPLICATION);
	
	public SubsystemTypeHeader(Clause clause) {
		super(Collections.singleton(clause));
	}
	
	public SubsystemTypeHeader(String value) {
		super(
				value, 
				new ClauseFactory<Clause>() {
					@Override
					public Clause newInstance(String clause) {
						return new Clause(clause);
					}
				});
	}

    public Clause getClause() {
		return clauses.iterator().next();
	}

	@Override
	public String getName() {
		return NAME;
	}
	
	public AriesProvisionDependenciesDirective getAriesProvisionDependenciesDirective() {
		return clauses.iterator().next().getProvisionDependenciesDirective();
	}
	
	public ProvisionPolicyDirective getProvisionPolicyDirective() {
		return clauses.iterator().next().getProvisionPolicyDirective();
	}
	
	public String getType() {
		return clauses.iterator().next().getType();
	}

	@Override
	public String getValue() {
		return toString();
	}
	
	public boolean isApplication() {
		return this == DEFAULT || TYPE_APPLICATION.equals(getType());
	}
	
	public boolean isComposite() {
		return TYPE_COMPOSITE.equals(getType());
	}
	
	public boolean isFeature() {
		return TYPE_FEATURE.equals(getType());
	}
}
