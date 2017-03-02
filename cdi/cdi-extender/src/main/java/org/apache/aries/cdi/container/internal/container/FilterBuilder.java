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

import java.util.List;

import javax.enterprise.inject.spi.Extension;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

public class FilterBuilder {

	public static Filter createExtensionFilter(List<ExtensionDependency> extentionDependencies) {
		try {
			StringBuilder sb = new StringBuilder("(&(objectClass=" + Extension.class.getName() + ")");

			if (extentionDependencies.size() > 1) sb.append("(|");

			for (ExtensionDependency dependency : extentionDependencies) {
				sb.append(dependency.toString());
			}

			if (extentionDependencies.size() > 1) sb.append(")");

			sb.append(")");

			return FrameworkUtil.createFilter(sb.toString());
		}
		catch (InvalidSyntaxException ise) {
			throw new RuntimeException(ise);
		}
	}

	public static Filter createReferenceFilter(List<ReferenceDependency> referenceDependencies) {
		try {
			StringBuilder sb = new StringBuilder();

			if (referenceDependencies.size() > 1) sb.append("(|");

			for (ReferenceDependency dependency : referenceDependencies) {
				sb.append(dependency.toString());
			}

			if (referenceDependencies.size() > 1) sb.append(")");

			return FrameworkUtil.createFilter(sb.toString());
		}
		catch (InvalidSyntaxException ise) {
			throw new RuntimeException(ise);
		}
	}

}