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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Registrator<T> {

	public abstract void registerService(String[] classNames, T service, Dictionary<String, ?> properties);

	public void registerService(T service, Object... keyValues) {
		if ((keyValues.length % 2) != 0) throw new IllegalArgumentException("keyValues must be [String, Object]+");

		Dictionary<String, Object> properties = new Hashtable<>();

		if (keyValues.length > 0) {
			for (int i = 0; i < keyValues.length; i += 2) {
				properties.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
			}
		}

		registerService(null, service, properties);
	}

	public int size() {
		return registrations.size();
	}

	public void close() {
		registrations.removeIf(
			r -> {
				try {
					r.unregister();
				}
				catch (IllegalStateException ise) {
					if (_log.isTraceEnabled()) {
						_log.trace("Service already unregistered {}", r);
					}
				}

				return true;
			}
		);
	}

	private static final Logger _log = LoggerFactory.getLogger(Registrator.class);

	protected final List<ServiceRegistration<?>> registrations = new CopyOnWriteArrayList<>();

}
