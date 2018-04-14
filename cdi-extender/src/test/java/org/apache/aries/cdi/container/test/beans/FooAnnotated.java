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
import javax.inject.Named;

import org.apache.aries.cdi.extra.propertytypes.ServiceRanking;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.SingleComponent;
import org.osgi.service.cdi.reference.ReferenceEvent;

@SingleComponent
@Named("foo.annotated")
@Service(Foo.class)
@ServiceRanking(12)
public class FooAnnotated implements Foo, Cloneable {

	void watchFoos(@Observes ReferenceEvent<Integer> numbers) {
		numbers.onAddingServiceReference(number -> System.out.println("Added: " + number));
		numbers.onUpdateServiceReference(number -> System.out.println("Updated: " + number));
		numbers.onRemoveServiceReference(number -> System.out.println("Removed: " + number));
	}

	@Inject
	FooWithReferenceAndConfig fooWithReferenceAndConfig;

}
