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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.cdi.container.internal.v2.component.Component;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO;

public class OSGiBean implements Comparable<OSGiBean> {

	public static class Builder {

		public Builder(Class<?> beanClass) {
			Objects.requireNonNull(beanClass);
			_beanClass = beanClass;
		}

		public OSGiBean build() {
			return new OSGiBean(_beanClass);
		}

		private Class<?> _beanClass;

	}

	private OSGiBean(
		Class<?> beanClass) {

		_beanClass = beanClass;
	}

	public void addConfiguration(ConfigurationTemplateDTO dto) {
		if (_component == null) {
			_configurations.add(dto);
		}
		else {
			_component.addConfiguration(dto);
		}
	}

	public void addReference(ReferenceTemplateDTO dto) {
		if (_component == null) {
			_references.add(dto);
		}
		else {
			_component.addReference(dto);
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

	public Component getComponent() {
		return _component;
	}

	public void setComponent(Component component) {
		_component = component;
		_configurations.removeIf(
			dto -> {
				_component.addConfiguration(dto);
				return true;
			}
		);
		_references.removeIf(
			dto -> {
				_component.addReference(dto);
				return true;
			}
		);
	}

	@Override
	public String toString() {
		if (_string == null) {
			_string = String.format("OSGiBean[%s]", _beanClass.getName());
		}
		return _string;
	}

	private final Class<?> _beanClass;
	private final List<ConfigurationTemplateDTO> _configurations = new CopyOnWriteArrayList<>();
	private final List<ReferenceTemplateDTO> _references = new CopyOnWriteArrayList<>();
	private volatile Component _component;
	private final AtomicBoolean _found = new AtomicBoolean();
	private volatile String _string;
}
