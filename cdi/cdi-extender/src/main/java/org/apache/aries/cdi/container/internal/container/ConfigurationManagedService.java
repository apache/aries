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

package org.apache.aries.cdi.container.internal.container;

import java.util.Dictionary;

import javax.enterprise.inject.spi.InjectionPoint;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

class ConfigurationManagedService implements ManagedService {

	public ConfigurationManagedService(
		String pid, boolean required, InjectionPoint injectionPoint, ConfigurationResolveAction resolveAction) {

		_pid = pid;
		_required = required;
		_injectionPoint = injectionPoint;
		_resolveAction = resolveAction;

		_resolveAction.add(this);
	}

	public String getPid() {
		return _pid;
	}

	public synchronized Dictionary<String, ?> getProperties() {
		return _properties;
	}

	public synchronized boolean isResolved() {
		return _required ? (_properties != null) : true;
	}

	@Override
	public synchronized void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		if ((_properties == null) && (properties != null)) {
			_properties = properties;
			_resolveAction.addingConfiguration();
		}
		else if ((_properties != null) && (properties == null)) {
			_properties = properties;
			_resolveAction.removeProperties();
		}
		else {
			_properties = properties;
			_resolveAction.updateProperties();
		}
	}

	@Override
	public String toString() {
		return "ConfigurationManagedService[" + _pid + ", " + _injectionPoint + "]";
	}

	private final InjectionPoint _injectionPoint;
	final String _pid;
	private volatile Dictionary<String, ?> _properties;
	private final boolean _required;
	private final ConfigurationResolveAction _resolveAction;

}