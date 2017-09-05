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

package org.apache.aries.cdi.container.internal.literal;

import javax.enterprise.util.AnnotationLiteral;

import org.apache.aries.cdi.container.internal.util.Strings;
import org.osgi.service.cdi.annotations.Component;
import org.osgi.service.cdi.annotations.ServiceScope;

public class ComponentLiteral extends AnnotationLiteral<Component> implements Component {

	private static final long serialVersionUID = 1L;

	/**
	 * @param name the name of the component bean
	 * @return a literal instance of {@link Component}
	 */
	public static ComponentLiteral from(String name) {
		return new ComponentLiteral(name, new Class<?>[0], Strings.EMPTY_ARRAY, ServiceScope.DEFAULT);
	}

	/**
	 * @param name the name of the component bean
	 * @param types an array of types under which to publish the service
	 * @param properties the set of properties for the service
	 * @return a literal instance of {@link Component}
	 */
	public static ComponentLiteral from(String name, Class<?>[] types, String[] properties, ServiceScope scope) {
		return new ComponentLiteral(name, types, properties, scope);
	}

	public ComponentLiteral(
		String name, Class<?>[] types, String[] properties, ServiceScope scope) {

		_name = name;
		_types = types;
		_properties = properties;
		_scope = scope;
	}

	@Override
	public Class<?>[] service() {
		return _types;
	}

	@Override
	public String[] property() {
		return _properties;
	}

	@Override
	public String name() {
		return _name;
	}

	@Override
	public ServiceScope serviceScope() {
		return _scope;
	}

	private final String _name;
	private final String[] _properties;
	private final ServiceScope _scope;
	private final Class<?>[] _types;

}
