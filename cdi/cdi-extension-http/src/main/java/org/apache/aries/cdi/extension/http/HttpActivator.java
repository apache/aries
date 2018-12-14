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

package org.apache.aries.cdi.extension.http;

import static org.osgi.framework.Constants.BUNDLE_ACTIVATOR;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_VENDOR;
import static org.osgi.service.cdi.CDIConstants.CDI_EXTENSION_PROPERTY;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.enterprise.inject.spi.Extension;

import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

@Header(
	name = BUNDLE_ACTIVATOR,
	value = "${@class}"
)
public class HttpActivator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(CDI_EXTENSION_PROPERTY, "aries.cdi.http");
		properties.put(SERVICE_DESCRIPTION, "Aries CDI - HTTP Portable Extension Factory");
		properties.put(SERVICE_VENDOR, "Apache Software Foundation");

		_serviceRegistration = context.registerService(
			Extension.class, new HttpExtensionFactory(), properties);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		_serviceRegistration.unregister();
	}

	private ServiceRegistration<Extension> _serviceRegistration;

}
