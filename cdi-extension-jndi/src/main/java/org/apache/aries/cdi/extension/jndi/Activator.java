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

import java.util.Dictionary;
import java.util.Hashtable;

import javax.enterprise.inject.spi.Extension;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.CdiExtenderConstants;
import org.osgi.service.jndi.JNDIConstants;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(CdiExtenderConstants.CDI_EXTENSION, "jndi");

		JndiExtensionFactory jndiExtensionFactory = new JndiExtensionFactory();

		_jndiExtensionFactoryRegistration = bundleContext.registerService(
			Extension.class, jndiExtensionFactory, properties);

		properties = new Hashtable<>();
		properties.put(JNDIConstants.JNDI_URLSCHEME, "java");

		_objectFactoryRegistration = bundleContext.registerService(
			ObjectFactory.class, new JndiObjectFactory(jndiExtensionFactory), properties);
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		_objectFactoryRegistration.unregister();
		_jndiExtensionFactoryRegistration.unregister();
	}

	private ServiceRegistration<?> _jndiExtensionFactoryRegistration;
	private ServiceRegistration<?> _objectFactoryRegistration;

}
