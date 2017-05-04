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

import java.util.ArrayList;
import java.util.List;

public class ServiceModel {

	public ServiceModel(String beanClass) {
		_beanClass = beanClass;
	}

	public void addProvide(String className) {
		_provides.add(className);
	}

	public void addProperty(String property) {
		_properties.add(property);
	}

	public String getBeanClass() {
		return _beanClass;
	}

	public String[] getProperties() {
		return _properties.toArray(new String[0]);
	}

	public List<String> getProvides() {
		return _provides;
	}

	private final String _beanClass;
	private List<String> _properties = new ArrayList<>();
	private List<String> _provides = new ArrayList<>();
}
