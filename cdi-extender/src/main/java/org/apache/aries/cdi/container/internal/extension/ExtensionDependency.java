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

package org.apache.aries.cdi.container.internal.extension;

import javax.enterprise.inject.spi.Extension;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.CdiConstants;

public class ExtensionDependency {

	public ExtensionDependency(BundleContext bundleContext, Long bundleId, String name) {
		_string = "(&(" + org.osgi.framework.Constants.SERVICE_BUNDLEID + "=" + bundleId + ")(" +
			CdiConstants.CDI_EXTENSION_NAMESPACE + "=" + name + "))";

		try {
			_filter = bundleContext.createFilter(_string);
		}
		catch (InvalidSyntaxException ise) {
			throw new RuntimeException(ise);
		}
	}

	public boolean matches(ServiceReference<Extension> reference) {
		return _filter.match(reference);
	}

	@Override
	public String toString() {
		return _string;
	}

	private final Filter _filter;
	private final String _string;

}