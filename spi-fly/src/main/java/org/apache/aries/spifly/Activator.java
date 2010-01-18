package org.apache.aries.spifly;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator {
    BundleTracker bt;
    ServiceTracker lst;
    List<LogService> logServices = new ArrayList<LogService>();

    public synchronized void start(final BundleContext context) throws Exception {
        lst = new LogServiceTracker(context, LogService.class.getName(), null);
        lst.open();
        
	    bt = new SPIBundleTracker(context, this);
	    bt.open();
	}

	public synchronized void stop(BundleContext context) throws Exception {
	    bt.close();
	    lst.close();	    
	}
	
	void log(int level, String message) {
	    synchronized (logServices) {
	        for (LogService log : logServices) {
	            log.log(level, message);
	        }
        }
	}

	void log(int level, String message, Throwable th) {
        synchronized (logServices) {
            for (LogService log : logServices) {
                log.log(level, message, th);
            }
        }
    }
    
	private final class LogServiceTracker extends ServiceTracker {
        private LogServiceTracker(BundleContext context, String clazz,
                ServiceTrackerCustomizer customizer) {
            super(context, clazz, customizer);
        }

        @Override
        public Object addingService(ServiceReference reference) {
            Object svc = super.addingService(reference);
            if (svc instanceof LogService) {
                synchronized (logServices) {
                    logServices.add((LogService) svc);
                }
            }
            return svc;
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            synchronized (logServices) {
                logServices.remove(service);
            }
            super.removedService(reference, service);
        }
    }
}
