package org.apache.aries.cdi.container.internal.container;

import java.util.Dictionary;

import javax.enterprise.inject.spi.InjectionPoint;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

class ConfigurationManagedService implements ManagedService {

	public ConfigurationManagedService(
		String pid, boolean required, InjectionPoint injectionPoint, ConfigurationResolveAction resolveAction) {

		_pid = pid;
		_required = required;
		_injectionPoint = injectionPoint;
		_resolveAction = resolveAction;

		_resolveAction.add(this);
	}

	public String getPid() {
		return _pid;
	}

	public synchronized Dictionary<String, ?> getProperties() {
		return _properties;
	}

	public synchronized boolean isResolved() {
		return _required ? (_properties != null) : true;
	}

	@Override
	public synchronized void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		if ((_properties == null) && (properties != null)) {
			_properties = properties;
			_resolveAction.addingConfiguration();
		}
		else if ((_properties != null) && (properties == null)) {
			_properties = properties;
			_resolveAction.removeProperties();
		}
		else {
			_properties = properties;
			_resolveAction.updateProperties();
		}
	}

	@Override
	public String toString() {
		return "ConfigurationManagedService[" + _pid + ", " + _injectionPoint + "]";
	}

	private final InjectionPoint _injectionPoint;
	final String _pid;
	private volatile Dictionary<String, ?> _properties;
	private final boolean _required;
	private final ConfigurationResolveAction _resolveAction;

}