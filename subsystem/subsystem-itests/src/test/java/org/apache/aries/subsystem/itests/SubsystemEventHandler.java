package org.apache.aries.subsystem.itests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.Version;
import org.osgi.service.subsystem.Subsystem.State;
import org.osgi.service.subsystem.SubsystemConstants;

public class SubsystemEventHandler implements ServiceListener {
	static class ServiceEventInfo {
		private final ServiceEvent event;
		private final long id;
		private final State state;
		private final String symbolicName;
		private final String type;
		private final Version version;
		
		public ServiceEventInfo(ServiceEvent event) {
			id = (Long)event.getServiceReference().getProperty(SubsystemConstants.SUBSYSTEM_ID_PROPERTY);
			state = (State)event.getServiceReference().getProperty(SubsystemConstants.SUBSYSTEM_STATE_PROPERTY);
			symbolicName = (String)event.getServiceReference().getProperty(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME_PROPERTY);
			type = (String)event.getServiceReference().getProperty(SubsystemConstants.SUBSYSTEM_TYPE_PROPERTY);
			version = (Version)event.getServiceReference().getProperty(SubsystemConstants.SUBSYSTEM_VERSION_PROPERTY);
			this.event = event;
		}
		
		public int getEventType() {
			return event.getType();
		}
		
		public long getId() {
			return id;
		}
		
		public State getState() {
			return state;
		}
		
		public String getSymbolicName() {
			return symbolicName;
		}
		
		public String getType() {
			return type;
		}
		
		public Version getVersion() {
			return version;
		}
	}
	
	private final Map<Long, List<SubsystemEventHandler.ServiceEventInfo>> subsystemIdToEvents = new HashMap<Long, List<SubsystemEventHandler.ServiceEventInfo>>();
	
	public void clear() {
		synchronized (subsystemIdToEvents) {
			subsystemIdToEvents.clear();
		}
	}
	
	public SubsystemEventHandler.ServiceEventInfo poll(long subsystemId) throws InterruptedException {
		return poll(subsystemId, 0);
	}
	
	public SubsystemEventHandler.ServiceEventInfo poll(long subsystemId, long timeout) throws InterruptedException {
		List<SubsystemEventHandler.ServiceEventInfo> events;
		synchronized (subsystemIdToEvents) {
			events = subsystemIdToEvents.get(subsystemId);
			if (events == null) {
				events = new ArrayList<SubsystemEventHandler.ServiceEventInfo>();
				subsystemIdToEvents.put(subsystemId, events);
			}
		}
		synchronized (events) {
			if (events.isEmpty()) {
				events.wait(timeout);
				if (events.isEmpty()) {
					return null;
				}
			}
			return events.remove(0);
		}
	}
	
	public void serviceChanged(ServiceEvent event) {
		Long subsystemId = (Long)event.getServiceReference().getProperty(SubsystemConstants.SUBSYSTEM_ID_PROPERTY);
		synchronized (subsystemIdToEvents) {
			List<SubsystemEventHandler.ServiceEventInfo> events = subsystemIdToEvents.get(subsystemId);
			if (events == null) {
				events = new ArrayList<SubsystemEventHandler.ServiceEventInfo>();
				subsystemIdToEvents.put(subsystemId, events);
			}
			synchronized (events) {
				events.add(new ServiceEventInfo(event));
				events.notify();
			}
		}
	}
	
	public int size() {
		synchronized (subsystemIdToEvents) {
			return subsystemIdToEvents.size();
		}
	}
}