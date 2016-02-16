package org.apache.aries.tx.control.service.local.impl;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Optional;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator, ServiceTrackerCustomizer<Coordinator, Coordinator> {

	private static final Logger logger = LoggerFactory.getLogger(Activator.class);
	
	private BundleContext context;
	
	private ServiceTracker<Coordinator, Coordinator> tracker;
	
	private Coordinator inUse;
	private ServiceRegistration<TransactionControl> reg;
	
	private Object lock = new Object();
	
	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		tracker = new ServiceTracker<>(context, Coordinator.class, this);
		tracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		tracker.close();
	}

	@Override
	public Coordinator addingService(ServiceReference<Coordinator> reference) {
		Coordinator c = context.getService(reference);
		checkAndRegister(c);
		return c;
	}

	private void checkAndRegister(Coordinator c) {
		boolean register = false;
		synchronized (lock) {
			if(inUse == null) {
				inUse = c;
				register = true;
			}
		}
		
		if(register) {
			logger.info("Registering a new local-only TransactionControl service");
			ServiceRegistration<TransactionControl> reg = context.registerService(
					TransactionControl.class, new TransactionControlImpl(c), getProperties());
			synchronized (lock) {
				this.reg = reg;
			}
		}
	}

	private Dictionary<String, Object> getProperties() {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("osgi.local.enabled", Boolean.TRUE);
		return props;
	}

	@Override
	public void modifiedService(ServiceReference<Coordinator> reference, Coordinator service) {
	}

	@Override
	public void removedService(ServiceReference<Coordinator> reference, Coordinator service) {
		ServiceRegistration<TransactionControl> toUnregister = null;
		synchronized (lock) {
			if(inUse == service) {
				inUse = null;
				toUnregister = reg;
				reg = null;
			}
		}
		
		if(toUnregister != null) {
			try {
				toUnregister.unregister();
			} catch (IllegalStateException ise) {
				logger.debug("An exception occurred when unregistering the Transaction Control service", ise);
			}
			
			Optional<?> check = tracker.getTracked().values().stream()
				.filter(c -> {
					checkAndRegister(c);
					synchronized (lock) {
						return reg != null;
					}
				}).findFirst();
			
			if(!check.isPresent()) {
				logger.info("No replacement Coordinator service was available. The Transaction Control service will remain unavailable until a new Coordinator can be found");
			}
		}
		context.ungetService(reference);
	}

}
