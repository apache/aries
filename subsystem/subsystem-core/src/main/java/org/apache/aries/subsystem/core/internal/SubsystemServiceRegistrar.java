package org.apache.aries.subsystem.core.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

public class SubsystemServiceRegistrar {
	private final BundleContext context;
	private final Map<Subsystem, ServiceRegistration<Subsystem>> map = new HashMap<Subsystem, ServiceRegistration<Subsystem>>();
	
	public SubsystemServiceRegistrar(BundleContext context) {
		if (context == null)
			throw new NullPointerException("Missing required parameter: context");
		this.context = context;
	}
	
	public synchronized void register(Subsystem subsystem) {
		if (map.containsKey(subsystem))
			throw new IllegalStateException("Subsystem '" + subsystem + "' already has service registration '" + map.get(subsystem) + "'");
		Dictionary<String, Object> properties = properties(subsystem);
		ServiceRegistration<Subsystem> registration = context.registerService(Subsystem.class, subsystem, properties);
		map.put(subsystem, registration);
	}
	
	public synchronized void unregister(Subsystem subsystem) {
		ServiceRegistration<Subsystem> registration = map.remove(subsystem);
		if (registration == null)
			throw new IllegalStateException("Subsystem '" + subsystem + "' is not registered");
		registration.unregister();
	}
	
	public synchronized void unregisterAll() {
		for (Iterator<ServiceRegistration<Subsystem>> i = map.values().iterator(); i.hasNext();) {
			ServiceRegistration<Subsystem> registration = i.next();
			registration.unregister();
			i.remove();
		}
	}
	
	public synchronized void update(Subsystem subsystem) {
		ServiceRegistration<Subsystem> registration = map.get(subsystem);
		if (registration == null)
			throw new IllegalStateException("Subsystem '" + subsystem + "' is not registered");
		Dictionary<String, Object> properties = properties(subsystem);
		registration.setProperties(properties);
	}
	
	private Dictionary<String, Object> properties(Subsystem subsystem) {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(SubsystemConstants.SUBSYSTEM_ID_PROPERTY, subsystem.getSubsystemId());
		properties.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME_PROPERTY, subsystem.getSymbolicName());
		properties.put(SubsystemConstants.SUBSYSTEM_VERSION_PROPERTY, subsystem.getVersion());
		properties.put(SubsystemConstants.SUBSYSTEM_TYPE_PROPERTY, subsystem.getType());
		properties.put(SubsystemConstants.SUBSYSTEM_STATE_PROPERTY, subsystem.getState());
		return properties;
	}
}
