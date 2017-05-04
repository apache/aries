package org.apache.aries.cdi.extension.jndi;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class JndiExtensionFactory implements ServiceFactory {

	@Override
	public Object getService(Bundle bundle, ServiceRegistration registration) {
		return new JndiExtension();
	}

	@Override
	public void ungetService(
		Bundle bundle, ServiceRegistration registration, Object extension) {
	}

}
