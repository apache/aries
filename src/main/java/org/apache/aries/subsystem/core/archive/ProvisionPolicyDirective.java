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

import org.osgi.service.subsystem.SubsystemConstants;

public class ProvisionPolicyDirective extends AbstractDirective {
	public static final String NAME = SubsystemConstants.PROVISION_POLICY_DIRECTIVE;
	public static final String VALUE_ACCEPT_DEPENDENCIES = SubsystemConstants.PROVISION_POLICY_ACCEPT_DEPENDENCIES;
	public static final String VALUE_REJECT_DEPENDENCIES = SubsystemConstants.PROVISION_POLICY_REJECT_DEPENDENCIES;
	
	public static final ProvisionPolicyDirective ACCEPT_DEPENDENCIES = new ProvisionPolicyDirective(VALUE_ACCEPT_DEPENDENCIES);
	public static final ProvisionPolicyDirective REJECT_DEPENDENCIES = new ProvisionPolicyDirective(VALUE_REJECT_DEPENDENCIES);
	
	public static final ProvisionPolicyDirective DEFAULT = REJECT_DEPENDENCIES;
	
	public static ProvisionPolicyDirective getInstance(String value) {
		if (VALUE_ACCEPT_DEPENDENCIES.equals(value))
			return ACCEPT_DEPENDENCIES;
		if (VALUE_REJECT_DEPENDENCIES.equals(value))
			return REJECT_DEPENDENCIES;
		return new ProvisionPolicyDirective(value);
	}
	
	public ProvisionPolicyDirective(String value) {
		super(NAME, value);
		if (!(VALUE_ACCEPT_DEPENDENCIES.equals(value)
				|| VALUE_REJECT_DEPENDENCIES.equals(value))) {
			throw new IllegalArgumentException("Invalid " + NAME + " directive value: " + value);
		}
	}
	
	public String getProvisionPolicy() {
		return getValue();
	}
	
	public boolean isAcceptDependencies() {
		return this == ACCEPT_DEPENDENCIES || VALUE_ACCEPT_DEPENDENCIES.equals(getProvisionPolicy());
	}
	
	public boolean isRejectDependencies() {
		return this == REJECT_DEPENDENCIES || VALUE_REJECT_DEPENDENCIES.equals(getProvisionPolicy());
	}
}
