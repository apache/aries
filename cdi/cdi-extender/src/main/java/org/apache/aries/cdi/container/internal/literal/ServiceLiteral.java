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

import org.osgi.service.cdi.annotations.Service;

public class ServiceLiteral extends AnnotationLiteral<Service> implements Service {

	private static final long serialVersionUID = 1L;

	/**
	 * @param classes an array of types under which to publish the service.
	 * @param properties the set of properties for the service.
	 * @return a literal instance of {@link Service}
	 */
	public static ServiceLiteral from(Class<?>[] classes, String[] properties) {
		return new ServiceLiteral(classes, properties);
	}

	public ServiceLiteral(Class<?>[] classes, String[] properties) {
		_type = classes;
		_properties = properties;
	}

	@Override
	public Class<?>[] type() {
		return _type;
	}

	@Override
	public String[] property() {
		return _properties;
	}

	private final String[] _properties;
	private final Class<?>[] _type;

}
