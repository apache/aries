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

import static org.apache.aries.cdi.container.internal.model.Constants.BEAN_CLASS_ATTRIBUTE;
import static org.apache.aries.cdi.container.internal.model.Constants.CDI10_URI;
import static org.apache.aries.cdi.container.internal.model.Constants.TARGET_ATTRIBUTE;

import org.xml.sax.Attributes;

public class ReferenceModel extends AbstractModel {

	public ReferenceModel(Attributes attributes) {
		_beanClass = getValue(CDI10_URI, BEAN_CLASS_ATTRIBUTE, attributes);
		_target = getValue(CDI10_URI, TARGET_ATTRIBUTE, attributes);
	}

	public String getBeanClass() {
		return _beanClass;
	}

	public String getTarget() {
		return _target;
	}

	private final String _beanClass;
	private final String _target;

}
