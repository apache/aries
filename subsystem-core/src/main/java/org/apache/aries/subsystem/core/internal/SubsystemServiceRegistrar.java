package org.apache.aries.subsystem.core.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.equinox.region.Region;
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
	
	public synchronized void addRegion(AriesSubsystem subsystem, Region region) {
		ServiceRegistration<Subsystem> registration = map.get(subsystem);
		if (registration == null)
			throw new IllegalStateException("Subsystem '" + subsystem + "' is not registered");
		Collection<String> currentRegions = (Collection<String>)registration.getReference().getProperty(Constants.SubsystemServicePropertyRegions);
		String regionName = region.getName();
		if (currentRegions.contains(regionName))
			return;
		Collection<String> newRegions = new HashSet<String>(currentRegions.size() + 1);
		newRegions.addAll(currentRegions);
		newRegions.add(regionName);
		Dictionary<String, Object> properties = properties(subsystem);
		properties.put(Constants.SubsystemServicePropertyRegions, Collections.unmodifiableCollection(newRegions));
		registration.setProperties(properties);
	}
	
	public synchronized void register(AriesSubsystem child, AriesSubsystem parent) {
		if (map.containsKey(child))
			throw new IllegalStateException("Subsystem '" + child + "' already has service registration '" + map.get(child) + "'");
		Dictionary<String, Object> properties = properties(child, parent);
		ServiceRegistration<Subsystem> registration = context.registerService(Subsystem.class, child, properties);
		map.put(child, registration);
	}
	
	public synchronized void removeRegion(AriesSubsystem subsystem, Region region) {
		ServiceRegistration<Subsystem> registration = map.get(subsystem);
		if (registration == null)
			return;
		Collection<String> regions = (Collection<String>)registration.getReference().getProperty(Constants.SubsystemServicePropertyRegions);
		String regionName = region.getName();
		if (regions == null || !regions.contains(regionName))
			return;
		regions = new HashSet<String>(regions);
		regions.remove(regionName);
		Dictionary<String, Object> properties = properties(subsystem);
		properties.put(Constants.SubsystemServicePropertyRegions, Collections.unmodifiableCollection(regions));
		registration.setProperties(properties);
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
	
	public synchronized void update(AriesSubsystem subsystem) {
		ServiceRegistration<Subsystem> registration = map.get(subsystem);
		if (registration == null)
			throw new IllegalStateException("Subsystem '" + subsystem + "' is not registered");
		Dictionary<String, Object> properties = properties(subsystem, registration);
		registration.setProperties(properties);
	}
	
	private Dictionary<String, Object> properties(AriesSubsystem subsystem) {
		Dictionary<String, Object> result = new Hashtable<String, Object>();
		result.put(SubsystemConstants.SUBSYSTEM_ID_PROPERTY, subsystem.getSubsystemId());
		result.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME_PROPERTY, subsystem.getSymbolicName());
		result.put(SubsystemConstants.SUBSYSTEM_VERSION_PROPERTY, subsystem.getVersion());
		result.put(SubsystemConstants.SUBSYSTEM_TYPE_PROPERTY, subsystem.getType());
		result.put(SubsystemConstants.SUBSYSTEM_STATE_PROPERTY, subsystem.getState());
		result.put(Constants.SubsystemServicePropertyRegions, Collections.singleton(subsystem.getRegion().getName()));
		return result;
	}
	
	private Dictionary<String, Object> properties(AriesSubsystem child, AriesSubsystem parent) {
		Dictionary<String, Object> result = properties(child);
		if (parent == null)
			return result;
		Collection<String> currentRegions = (Collection<String>)result.get(Constants.SubsystemServicePropertyRegions);
		Collection<String> newRegions = new HashSet<String>(currentRegions.size() + 1);
		newRegions.addAll(currentRegions);
		newRegions.add(parent.getRegion().getName());
		result.put(Constants.SubsystemServicePropertyRegions, Collections.unmodifiableCollection(newRegions));
		return result;
	}
	
	private Dictionary<String, Object> properties(AriesSubsystem subsystem, ServiceRegistration<Subsystem> registration) {
		Dictionary<String, Object> result = properties(subsystem);
		Collection<String> regions = (Collection<String>)registration.getReference().getProperty(Constants.SubsystemServicePropertyRegions);
		if (regions == null)
			return result;
		result.put(Constants.SubsystemServicePropertyRegions, regions);
		return result;
	}
}
