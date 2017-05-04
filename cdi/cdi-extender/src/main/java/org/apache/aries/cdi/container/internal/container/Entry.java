package org.apache.aries.cdi.container.internal.container;

import java.util.AbstractMap.SimpleImmutableEntry;

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

class Entry extends SimpleImmutableEntry<ConfigurationManagedService, ServiceRegistration<ManagedService>> {

	private static final long serialVersionUID = 1L;

	public Entry(ConfigurationManagedService key, ServiceRegistration<ManagedService> value) {
		super(key, value);
	}
}