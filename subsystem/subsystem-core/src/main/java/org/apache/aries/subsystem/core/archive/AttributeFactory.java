/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.archive;

import org.osgi.framework.Constants;

public class AttributeFactory {
	public static Attribute createAttribute(String name, String value) {
		if (Constants.VERSION_ATTRIBUTE.equals(name)) {
			if (Character.isDigit(value.charAt(0)))
				return new VersionAttribute(value);
            return new VersionRangeAttribute(value);
		}
		if (TypeAttribute.NAME.equals(name))
			return new TypeAttribute(value);
		if (DeployedVersionAttribute.NAME.equals(name))
			return new DeployedVersionAttribute(value);
		if (BundleVersionAttribute.NAME.equals(name))
			return new BundleVersionAttribute(value);
		return new GenericAttribute(name, value);
	}
}
