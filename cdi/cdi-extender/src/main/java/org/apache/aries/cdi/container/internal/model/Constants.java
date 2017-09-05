/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.container.internal.model;

import java.util.Set;

import org.apache.aries.cdi.container.internal.util.Sets;

public class Constants {

	private Constants() {
		// no instances
	}

	public static final String CDI10_URI = "http://www.osgi.org/xmlns/cdi/v1.0.0";
	public static final Set<String> CDI_URIS = Sets.immutableHashSet(CDI10_URI);

	public static final String ARRAY_ELEMENT = "array";
	public static final String CARDINALITY_ATTRIBUTE = "cardinality";
	public static final String CLASS_ATTRIBUTE = "class";
	public static final String COMPONENT_ELEMENT = "component";
	public static final String CONFIGURATION_ELEMENT = "configuration";
	public static final String CONFIGURATION_PID_ATTRIBUTE = "configuration-pid";
	public static final String CONFIGURATION_POLICY_ATTRIBUTE = "configuration-policy";
	public static final String INTERFACE_ATTRIBUTE = "interface";
	public static final String LIST_ELEMENT = "list";
	public static final String NAME_ATTRIBUTE = "name";
	public static final String POLICY_ATTRIBUTE = "policy";
	public static final String POLICY_OPTION_ATTRIBUTE = "policy-option";
	public static final String PROPERTY_ELEMENT = "property";
	public static final String PROVIDE_ELEMENT = "provide";
	public static final String REFERENCE_ELEMENT = "reference";
	public static final String SCOPE_ATTRIBUTE= "scope";
	public static final String SERVICE_ATTRIBUTE = "service";
	public static final String SERVICE_SCOPE_ATTRIBUTE = "service-scope";
	public static final String SET_ELEMENT = "set";
	public static final String TARGET_ATTRIBUTE = "target";
	public static final String TYPE_ATTRIBUTE = "type";
	public static final String VALUE_ATTRIBUTE = "value";
	public static final String VALUE_ELEMENT = "value";
	public static final String VALUE_TYPE_ATTRIBUTE = "value-type";

}
