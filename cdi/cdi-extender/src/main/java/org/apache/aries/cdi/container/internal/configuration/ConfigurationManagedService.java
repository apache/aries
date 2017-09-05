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

package org.apache.aries.cdi.container.internal.configuration;

import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public class ConfigurationManagedService implements ManagedService {

	public ConfigurationManagedService(String pid, ConfigurationCallback callback) {
		_pid = pid;
		_callback = callback;
	}

	@Override
	public synchronized void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		if ((_properties == null) && (properties != null)) {
			_properties = properties;
			_callback.added(properties);
		}
		else if ((_properties != null) && (properties != null)) {
			_properties = properties;
			_callback.updated(properties);
		}
		else if ((_properties == null) && (properties == null)) {
			// ignore this
		}
		else {
			_properties = null;
			_callback.removed();
		}
	}

	@Override
	public String toString() {
		if (_string == null) {
			_string = String.format(
				"ConfigurationManagedService[%s, %s]", _pid, _callback.policy());
		}

		return _string;
	}

	private final ConfigurationCallback _callback;
	private final String _pid;
	private volatile Dictionary<String, ?> _properties;
	private volatile String _string;

}