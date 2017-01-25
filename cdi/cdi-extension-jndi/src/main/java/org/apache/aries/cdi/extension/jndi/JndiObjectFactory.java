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

import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.inject.spi.BeanManager;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class JndiObjectFactory implements ServiceFactory<ObjectFactory> {

	@Override
	public ObjectFactory getService(Bundle bundle, ServiceRegistration<ObjectFactory> registration) {
		if (!_contexts.containsKey(bundle.getBundleContext())) {
			return null;
		}

		return _contexts.get(bundle.getBundleContext());
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<ObjectFactory> registration, ObjectFactory service) {
		_contexts.remove(bundle.getBundleContext());
	}

	public void put(BundleContext bundleContext, BeanManager beanManager) {
		_contexts.putIfAbsent(bundleContext, new InnerObjectFactory(new JndiContext(beanManager)));
	}

	private final ConcurrentMap<BundleContext, ObjectFactory> _contexts = new ConcurrentHashMap<>();

	private class InnerObjectFactory implements ObjectFactory {

		public InnerObjectFactory(JndiContext jndiContext) {
			_jndiContext = jndiContext;
		}

		@Override
		public Object getObjectInstance(
				Object obj, Name name, javax.naming.Context context, Hashtable<?, ?> environment)
			throws Exception {

			if (obj == null) {
				return _jndiContext;
			}

			return null;
		}

		private final JndiContext _jndiContext;

	}

}