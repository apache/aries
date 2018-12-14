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

package org.apache.aries.cdi.extension.el.jsp;

import javax.enterprise.inject.spi.Extension;

import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class ELJSPExtensionFactory implements PrototypeServiceFactory<Extension> {

	@Override
	public Extension getService(
		Bundle bundle, ServiceRegistration<Extension> registration) {

		return new ELJSPExtension(bundle);
	}

	@Override
	public void ungetService(
		Bundle bundle, ServiceRegistration<Extension> registration, Extension service) {
	}

}
