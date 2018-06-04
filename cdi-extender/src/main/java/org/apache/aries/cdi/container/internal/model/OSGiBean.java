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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.cdi.container.internal.bean.ComponentPropertiesBean;
import org.apache.aries.cdi.container.internal.bean.ReferenceBean;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.apache.aries.cdi.container.internal.util.Syncro;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO;

public class OSGiBean implements Comparable<OSGiBean> {

	public static class Builder {

		public Builder(Logs logs, Class<?> beanClass) {
			Objects.requireNonNull(beanClass);
			_logs = logs;
			_beanClass = beanClass;
		}

		public OSGiBean build() {
			return new OSGiBean(_logs, _beanClass);
		}

		private final Class<?> _beanClass;
		private final Logs _logs;

	}

	private OSGiBean(Logs logs, Class<?> beanClass) {
		_logs = logs;
		_beanClass = beanClass;
	}

	public synchronized void addConfiguration(ConfigurationTemplateDTO dto) {
		try (Syncro syncro = _lock.open()) {
			if (_componentTemplate == null) {
				_configurationsQueue.add(dto);
			}
			else {
				((ExtendedConfigurationTemplateDTO)dto).bean = new ComponentPropertiesBean(
					_componentTemplate, (ExtendedConfigurationTemplateDTO)dto);

				_componentTemplate.configurations.add(dto);
			}
		}
	}

	public synchronized void addReference(ReferenceTemplateDTO dto) {
		try (Syncro syncro = _lock.open()) {
			if (_componentTemplate == null) {
				_referencesQueue.add(dto);
			}
			else {
				((ExtendedReferenceTemplateDTO)dto).bean = new ReferenceBean(
					_logs, _componentTemplate, (ExtendedReferenceTemplateDTO)dto);

				_componentTemplate.references.add(dto);
			}
		}
	}

	@Override
	public int compareTo(OSGiBean other) {
		return _beanClass.getName().compareTo(other._beanClass.getName());
	}

	public boolean found() {
		return _found.get();
	}

	public void found(boolean found) {
		_found.set(found);
	}

	public Class<?> getBeanClass() {
		return _beanClass;
	}

	public synchronized ComponentTemplateDTO geComponentTemplateDTO() {
		try (Syncro syncro = _lock.open()) {
			return _componentTemplate;
		}
	}

	public ComponentTemplateDTO getComponent() {
		return _componentTemplate;
	}

	public void setComponent(ComponentTemplateDTO componentTemplate) {
		try (Syncro syncro = _lock.open()) {
			if (_componentTemplate != null) {
				return;
			}
			_componentTemplate = componentTemplate;
			_configurationsQueue.removeIf(
				dto -> {
					((ExtendedConfigurationTemplateDTO)dto).bean = new ComponentPropertiesBean(
						_componentTemplate, (ExtendedConfigurationTemplateDTO)dto);

					_componentTemplate.configurations.add(dto);
					return true;
				}
			);
			_referencesQueue.removeIf(
				dto -> {
					((ExtendedReferenceTemplateDTO)dto).bean = new ReferenceBean(
						_logs, _componentTemplate, (ExtendedReferenceTemplateDTO)dto);

					_componentTemplate.references.add(dto);
					return true;
				}
			);
		}
	}

	@Override
	public String toString() {
		if (_string == null) {
			_string = String.format("OSGiBean[%s]", _beanClass.getName());
		}
		return _string;
	}

	private final Syncro _lock = new Syncro(true);
	private final Logs _logs;
	private final Class<?> _beanClass;
	private final List<ConfigurationTemplateDTO> _configurationsQueue = new CopyOnWriteArrayList<>();
	private final List<ReferenceTemplateDTO> _referencesQueue = new CopyOnWriteArrayList<>();
	private volatile ComponentTemplateDTO _componentTemplate;
	private final AtomicBoolean _found = new AtomicBoolean();
	private volatile String _string;
}
