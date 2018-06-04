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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.EventMetadata;
import javax.inject.Inject;

import org.osgi.service.cdi.reference.BindService;

@ApplicationScoped
public class ObserverFoo {

	public List<Foo> foos() {
		return _foos;
	}

	@Inject
	void foos(BindService<Foo> event, EventMetadata eventMetadata) {
		event.adding(
			foo -> {
				System.out.printf("Adding %s, %s%n", foo, eventMetadata);
				_foos.add(foo);
			}
		).modified(
			foo -> {
				System.out.printf("Modified %s, %s%n", foo, eventMetadata);
			}
		).removed(
			foo -> {
				System.out.printf("Removed %s, %s%n", foo, eventMetadata);
				_foos.remove(foo);
			}
		).bind();
	}

	private List<Foo> _foos = new CopyOnWriteArrayList<>();
}