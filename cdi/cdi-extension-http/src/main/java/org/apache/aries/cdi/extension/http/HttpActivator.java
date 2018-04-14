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

import java.util.Dictionary;
import java.util.Hashtable;

import javax.enterprise.inject.spi.Extension;

import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Header;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.service.cdi.annotations.RequireCDIImplementation;

@Capability(
	attribute = {
		"objectClass:List<String>=javax.enterprise.inject.spi.Extension",
		"osgi.cdi.extension=aries.cdi.http"},
	namespace = ServiceNamespace.SERVICE_NAMESPACE
)
@Header(
	name = Constants.BUNDLE_ACTIVATOR,
	value = "org.apache.aries.cdi.extension.http.HttpActivator"
)
@Requirement(
	name = "osgi.http",
	namespace = ImplementationNamespace.IMPLEMENTATION_NAMESPACE,
	version = "1.0.0"
)
@RequireCDIImplementation
public class HttpActivator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("osgi.cdi.extension", "aries.cdi.http");
		_serviceRegistration = context.registerService(
			Extension.class, new HttpExtensionFactory(), properties);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		_serviceRegistration.unregister();
	}

	private ServiceRegistration<Extension> _serviceRegistration;

}
