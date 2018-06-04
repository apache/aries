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

package org.apache.aries.cdi.container.test.beans;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.aries.cdi.extra.propertytypes.JaxrsResource;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.annotations.ComponentProperties;
import org.osgi.service.cdi.annotations.PrototypeRequired;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.Reluctant;
import org.osgi.service.cdi.annotations.Service;

public class BarAnnotated {

	@Inject
	@Reference
	Foo foo;

	@Inject
	@Reluctant
	@Reference
	Optional<Foo> fooOptional;

	@Inject
	@Reluctant
	@Reference
	Provider<Collection<Foo>> dynamicFoos;

	@Inject
	@Reluctant
	@Reference
	Collection<Map.Entry<Map<String, Object>, Integer>> tupleIntegers;

	@Inject
	@Reluctant
	@PrototypeRequired
	@Reference
	Collection<ServiceReference<Foo>> serviceReferencesFoos;

	@Inject
	@Reluctant
	@Reference(Foo.class)
	Collection<Map<String, Object>> propertiesFoos;

	@Inject
	@ComponentProperties
	Config config;

	@Produces
	@Service
	@JaxrsResource
	Baz baz = new Baz() {};

}
