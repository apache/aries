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

package org.apache.aries.cdi.test.tb9;

import java.util.TreeMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.reference.ReferenceEvent;

@ApplicationScoped
@Service
public class ContainerReferenceEventHandler implements Pojo {

	void integers(@Observes ReferenceEvent<Integer> event) {
		event.onAddingServiceObjects(
			so -> {
				ServiceReference<Integer> serviceReference = so.getServiceReference();
				System.out.println("=====ADDING==>>> " + serviceReference);

				_services.put(so.getServiceReference(), "ADDED");
			}
		);
		event.onUpdateServiceObjects(
			so -> {
				System.out.println("=====UPDATING==>>> " + so.getServiceReference());

				_services.put(so.getServiceReference(), "UPDATED");
			}
		);
		event.onRemoveServiceReference(
			sr -> {
				System.out.println("=====REMOVING==>>> " + sr);

				_services.remove(sr);
			}
		);
	}

	@Override
	public String foo(String fooInput) {
		return _services.values().toString();
	}

	@Override
	public int getCount() {
		return _services.size();
	}

	private final TreeMap<ServiceReference<Integer>, String> _services = new TreeMap<>();

}
