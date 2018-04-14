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

import org.apache.aries.cdi.container.internal.bean.ReferenceBean;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO;

public class ExtendedReferenceTemplateDTO extends ReferenceTemplateDTO {

	public ReferenceBean bean;

	public Class<?> beanClass;

	public CollectionType collectionType;

	public Class<?> declaringClass;

	public Type injectionPointType;

	public Class<?> serviceClass;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((beanClass == null) ? 0 : beanClass.hashCode());
		result = prime * result + ((collectionType == null) ? 0 : collectionType.hashCode());
		result = prime * result + ((declaringClass == null) ? 0 : declaringClass.hashCode());
		result = prime * result + ((injectionPointType == null) ? 0 : injectionPointType.hashCode());
		result = prime * result + ((maximumCardinality == null) ? 0 : maximumCardinality.hashCode());
		result = prime * result + minimumCardinality;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((policy == null) ? 0 : policy.hashCode());
		result = prime * result + ((policyOption == null) ? 0 : policyOption.hashCode());
		result = prime * result + ((serviceType == null) ? 0 : serviceType.hashCode());
		result = prime * result + ((targetFilter == null) ? 0 : targetFilter.hashCode());
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
		ExtendedReferenceTemplateDTO other = (ExtendedReferenceTemplateDTO) obj;
		if (beanClass == null) {
			if (other.beanClass != null) {
				return false;
			}
		} else if (!beanClass.equals(other.beanClass)) {
			return false;
		}
		if (collectionType != other.collectionType) {
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
		if (minimumCardinality != other.minimumCardinality) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (policy != other.policy) {
			return false;
		}
		if (policyOption != other.policyOption) {
			return false;
		}
		if (serviceType == null) {
			if (other.serviceType != null) {
				return false;
			}
		} else if (!serviceType.equals(other.serviceType)) {
			return false;
		}
		if (targetFilter == null) {
			if (other.targetFilter != null) {
				return false;
			}
		} else if (!targetFilter.equals(other.targetFilter)) {
			return false;
		}
		return true;
	}

}
