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

package org.apache.aries.cdi.container.internal.component;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import javax.enterprise.inject.spi.Bean;

import org.apache.aries.cdi.container.internal.configuration.ConfigurationCallback;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.util.Conversions;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.util.converter.TypeReference;

public class ComponentProperties {

	public ComponentProperties bean(Bean<?> bean) {
		_bean = bean;
		return this;
	}

	public ComponentProperties componentModel(ComponentModel componentModel) {
		_componentModel = componentModel;
		return this;
	}

	public ComponentProperties containerState(ContainerState containerState) {
		_containerState = containerState;
		return this;
	}

	public Dictionary<String, ?> build() {
		Dictionary<String, Object> componentProperties = new Hashtable<>();

		_bean.getQualifiers().stream().forEach(
			annotation -> {
				Map<String, Object> map = Conversions.convert(
					annotation).sourceAs(annotation.annotationType()).to(_mapType);

				for (Map.Entry<String, Object> entry : map.entrySet()) {
					componentProperties.put(entry.getKey(), entry.getValue());
				}

			}
		);

		for (Map.Entry<String, Object> entry : Maps.map(_componentModel.getProperties()).entrySet()) {
			componentProperties.put(entry.getKey(), entry.getValue());
		}

		Map<String, ConfigurationCallback> map = _containerState.configurationCallbacks().get(_componentModel);

		if ((_pid == null) || (_pid.length == 0)) {
			_pid = map.keySet().toArray(new String[0]);
		}

		for (String pid : _pid) {
			if (Configuration.NAME.equals(pid)) {
				pid = _componentModel.getName();
			}
			ConfigurationCallback callback = map.get(pid);
			Dictionary<String,?> properties = callback.properties();

			for (Enumeration<String> enumeration = properties.keys(); enumeration.hasMoreElements();) {
				String key = enumeration.nextElement();

				componentProperties.put(key, properties.get(key));
			}
		}

		componentProperties.put("component.name", _componentModel.getName());

		return componentProperties;
	}

	public ComponentProperties pid(String[] pid) {
		_pid = pid;
		return this;
	}

	private static final TypeReference<Map<String, Object>> _mapType = new TypeReference<Map<String, Object>>(){};

	private Bean<?> _bean;
	private String[] _pid;
	private ComponentModel _componentModel;
	private ContainerState _containerState;

}
