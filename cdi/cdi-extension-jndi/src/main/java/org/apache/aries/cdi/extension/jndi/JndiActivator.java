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

package org.apache.aries.cdi.extension.jndi;

import static org.osgi.framework.Constants.BUNDLE_ACTIVATOR;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_VENDOR;
import static org.osgi.service.cdi.CDIConstants.CDI_EXTENSION_PROPERTY;
import static org.osgi.service.jndi.JNDIConstants.JNDI_URLSCHEME;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.enterprise.inject.spi.Extension;
import javax.naming.spi.ObjectFactory;

import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LoggerFactory;
import org.osgi.util.tracker.ServiceTracker;

@Header(
	name = BUNDLE_ACTIVATOR,
	value = "${@class}"
)
public class JndiActivator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		_lft = new ServiceTracker<>(context, LoggerFactory.class, null);
		_lft.open();

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(CDI_EXTENSION_PROPERTY, "aries.cdi.jndi");
		properties.put(JNDI_URLSCHEME, "java");
		properties.put(SERVICE_DESCRIPTION, "Aries CDI - JNDI Portable Extension Factory");
		properties.put(SERVICE_VENDOR, "Apache Software Foundation");

		_serviceRegistration = context.registerService(
			new String[] {Extension.class.getName(), ObjectFactory.class.getName()},
			new JndiExtensionFactory(_lft.getService()), properties);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		_serviceRegistration.unregister();
		_lft.close();
	}

	private volatile ServiceTracker<LoggerFactory, LoggerFactory> _lft;
	private ServiceRegistration<?> _serviceRegistration;

}
