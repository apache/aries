package org.apache.aries.cdi.extension.jndi;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.spi.Extension;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class JndiExtensionFactory implements ServiceFactory<Extension> {

	@Override
	public Extension getService(Bundle bundle, ServiceRegistration<Extension> registration) {
		JndiExtension jndiExtension = new JndiExtension();
		_map.put(bundle, jndiExtension);
		return jndiExtension;
	}

	@Override
	public void ungetService(
		Bundle bundle, ServiceRegistration<Extension> registration, Extension extension) {

		_map.remove(bundle);
	}

	ObjectFactory getObjectFactory(Bundle bundle) {
		JndiExtension jndiExtension = _map.get(bundle);

		if (jndiExtension == null) {
			return null;
		}

		return new InnerObjectFactory(new JndiContext(jndiExtension.getBeanManager()));
	}

	private final Map<Bundle, JndiExtension> _map = new ConcurrentHashMap<>();

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
