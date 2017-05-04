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

import static org.apache.aries.cdi.container.internal.util.Reflection.cast;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.inject.spi.InjectionPoint;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

public class ConfigurationDependency {

	public ConfigurationDependency(
		BundleContext bundleContext, String[] pids, boolean required, String defaultPid,
		InjectionPoint injectionPoint) {

		_bundleContext = bundleContext;
		_pids = pids;
		_required = required;
		_injectionPoint = injectionPoint;
		_beanClass = getConfigurationType();

		for (int i = 0; i < _pids.length; i++) {
			if ("$".equals(_pids[i])) {
				_pids[i] = defaultPid;
			}
		}
	}

	public void close() {
		for (Entry entry : _registrations) {
			entry.getValue().unregister();
		}

		_registrations.clear();
	}

	public Class<?> getBeanClass() {
		return _beanClass;
	}

	public InjectionPoint getInjectionPoint() {
		return _injectionPoint;
	}

	public Map<String, Object> getConfiguration() {
		Map<String, Object> map = new HashMap<>();

		for (Entry entry : _registrations) {
			Dictionary<String, ?> properties = entry.getKey().getProperties();

			if (properties == null) {
				continue;
			}

			Enumeration<String> keys = properties.keys();

			while (keys.hasMoreElements()) {
				String key = keys.nextElement();

				if (Constants.SERVICE_PID.equals(key)) continue;

				if (!map.containsKey(key)) {
					map.put(key, properties.get(key));
				}
			}
		}

		return map;
	}

	public Object isRequired() {
		return _required;
	}

	public boolean isResolved() {
		for (Entry entry : _registrations) {
			ConfigurationManagedService configurationManagedService = entry.getKey();

			if (!configurationManagedService.isResolved()) {
				return false;
			}
		}

		return true;
	}

	public boolean isResolved(String pid) {
		for (Entry entry : _registrations) {
			ConfigurationManagedService configurationManagedService = entry.getKey();

			if ((configurationManagedService._pid.equals(pid)) && configurationManagedService.isResolved()) {
				return true;
			}
		}

		return false;
	}

	public void open(ConfigurationResolveAction resolveAction) {
		for (String pid : pids()) {
			Dictionary<String, Object> properties = new Hashtable<>();

			properties.put("service.pid", pid);

			ConfigurationManagedService managedService = new ConfigurationManagedService(
				pid, _required, _injectionPoint, resolveAction);

			ServiceRegistration<ManagedService> serviceRegistration = _bundleContext.registerService(
				ManagedService.class, managedService, properties);

			_registrations.add(new Entry(managedService, serviceRegistration));
		}
	}

	public String[] pids() {
		return Arrays.copyOf(_pids, _pids.length);
	}

	@Override
	public String toString() {
		return _injectionPoint.getMember() + Arrays.toString(_pids);
	}

	private Class<?> getConfigurationType() {
		Type type = _injectionPoint.getType();

		if (!(type instanceof ParameterizedType)) {
			return cast(type);
		}

		ParameterizedType parameterizedType = cast(type);

		Type rawType = parameterizedType.getRawType();

		return cast(rawType);
	}

	private final Class<?> _beanClass;
	private final BundleContext _bundleContext;
	private final InjectionPoint _injectionPoint;
	private final String[] _pids;
	private final List<Entry> _registrations = new CopyOnWriteArrayList<>();
	private final boolean _required;

}
