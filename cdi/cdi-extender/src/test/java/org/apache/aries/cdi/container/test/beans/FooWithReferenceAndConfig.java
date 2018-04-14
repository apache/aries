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

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.osgi.service.cdi.annotations.ComponentScoped;
import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.reference.ReferenceEvent;

@ComponentScoped
public class FooWithReferenceAndConfig {

	void watchNumbers(@Observes ReferenceEvent<Integer> numbers) {
		numbers.onAddingServiceReference(number -> System.out.println("Added: " + number));
		numbers.onUpdateServiceReference(number -> System.out.println("Updated: " + number));
		numbers.onRemoveServiceReference(number -> System.out.println("Removed: " + number));
	}

	@Inject
	@Reference
	public Foo fooReference;

	@Inject
	@Configuration
	public Config config;

}
