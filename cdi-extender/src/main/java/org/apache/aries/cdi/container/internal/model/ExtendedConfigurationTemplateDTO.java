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

import java.lang.reflect.Type;

import org.apache.aries.cdi.container.internal.bean.ComponentPropertiesBean;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;

public class ExtendedConfigurationTemplateDTO extends ConfigurationTemplateDTO {

	/**
	 * The bean class which the synthetic bean required to resolve the
	 * injection point must provide.
	 */
	public Class<?> beanClass;

	/**
	 * The class which declared the reference.
	 */
	public Class<?> declaringClass;

	/**
	 * The type of the injection point declaring the configuration.
	 */
	public Type injectionPointType;

	public ComponentPropertiesBean bean;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((beanClass == null) ? 0 : beanClass.hashCode());
		result = prime * result + ((declaringClass == null) ? 0 : declaringClass.hashCode());
		result = prime * result + ((injectionPointType == null) ? 0 : injectionPointType.hashCode());
		result = prime * result + ((maximumCardinality == null) ? 0 : maximumCardinality.hashCode());
		result = prime * result + ((pid == null) ? 0 : pid.hashCode());
		result = prime * result + ((policy == null) ? 0 : policy.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ExtendedConfigurationTemplateDTO other = (ExtendedConfigurationTemplateDTO) obj;
		if (beanClass == null) {
			if (other.beanClass != null) {
				return false;
			}
		} else if (!beanClass.equals(other.beanClass)) {
			return false;
		}
		if (declaringClass == null) {
			if (other.declaringClass != null) {
				return false;
			}
		} else if (!declaringClass.equals(other.declaringClass)) {
			return false;
		}
		if (injectionPointType == null) {
			if (other.injectionPointType != null) {
				return false;
			}
		} else if (!injectionPointType.equals(other.injectionPointType)) {
			return false;
		}
		if (maximumCardinality != other.maximumCardinality) {
			return false;
		}
		if (pid == null) {
			if (other.pid != null) {
				return false;
			}
		} else if (!pid.equals(other.pid)) {
			return false;
		}
		if (policy != other.policy) {
			return false;
		}
		return true;
	}

}
