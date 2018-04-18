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
import org.osgi.service.cdi.ConfigurationPolicy;
import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.service.cdi.annotations.Greedy;
import org.osgi.service.cdi.annotations.PID;
import org.osgi.service.cdi.annotations.PrototypeRequired;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.Service;

public class BarAnnotated {

	@Inject
	@Greedy
	@Reference
	Foo foo;

	@Inject
	@Reference
	Optional<Foo> fooOptional;

	@Inject
	@Reference
	Provider<Collection<Foo>> dynamicFoos;

	@Inject
	@Reference
	Collection<Map.Entry<Map<String, Object>, Integer>> tupleIntegers;

	@Inject
	@PrototypeRequired
	@Reference
	Collection<ServiceReference<Foo>> serviceReferencesFoos;

	@Inject
	@Reference(Foo.class)
	Collection<Map<String, Object>> propertiesFoos;

	@Inject
	@PID(value = "foo.config", policy = ConfigurationPolicy.REQUIRED)
	@Configuration
	Config config;

	@Produces
	@Service
	@JaxrsResource
	Baz baz = new Baz() {};

}
