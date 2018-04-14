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

package org.apache.aries.cdi.container.test;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;

import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceRegistration;

public class MockServiceRegistration<S> implements ServiceRegistration<S> {

	public MockServiceRegistration(
		MockServiceReference<S> mockServiceReference,
		List<MockServiceRegistration<?>> serviceRegistrations,
		List<Entry<ServiceListener, Filter>> serviceListeners) {

		_mockServiceReference = mockServiceReference;
		_serviceRegistrations = serviceRegistrations;
		_serviceListeners = serviceListeners;

		if (_serviceRegistrations.add(this)) {
			_serviceListeners.stream().filter(
				entry -> entry.getValue().match(_mockServiceReference)
			).forEach(
				entry -> entry.getKey().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, _mockServiceReference))
			);
		}
	}

	@Override
	public MockServiceReference<S> getReference() {
		return _mockServiceReference;
	}

	@Override
	public void setProperties(Dictionary<String, ?> properties) {
		for (Enumeration<String> enu = properties.keys(); enu.hasMoreElements();) {
			String key = enu.nextElement();
			if (key.equals(Constants.OBJECTCLASS) ||
				key.equals(Constants.SERVICE_BUNDLEID) ||
				key.equals(Constants.SERVICE_ID) ||
				key.equals(Constants.SERVICE_SCOPE)) {
				continue;
			}
			_mockServiceReference.setProperty(key, properties.get(key));
		}

		_serviceListeners.stream().filter(
			entry -> entry.getValue().match(_mockServiceReference)
		).forEach(
			entry -> entry.getKey().serviceChanged(new ServiceEvent(ServiceEvent.MODIFIED, _mockServiceReference))
		);
	}

	@Override
	public void unregister() {
		_serviceRegistrations.removeIf(
			reg -> {
				if (reg.getReference().equals(_mockServiceReference)) {
					_serviceListeners.stream().filter(
						entry -> entry.getValue().match(_mockServiceReference)
					).forEach(
						entry -> entry.getKey().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, _mockServiceReference))
					);
					return true;
				}
				return false;
			}
		);
	}

	private final MockServiceReference<S> _mockServiceReference;
	private final List<Entry<ServiceListener, Filter>> _serviceListeners;
	private final List<MockServiceRegistration<?>> _serviceRegistrations;

}

