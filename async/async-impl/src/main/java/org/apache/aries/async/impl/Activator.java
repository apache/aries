package org.apache.aries.async.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.async.Async;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
	
	private final ExecutorService executor = Executors.newFixedThreadPool(10, new ThreadFactory() {
		
		private final AtomicInteger count = new AtomicInteger();
		
		public Thread newThread(final Runnable r) {
			Thread t = new Thread(new Runnable(){
				public void run() {
					AccessController.doPrivileged(new PrivilegedAction<Void>() {
						public Void run() {
							r.run();
							return null;
						}
					});
				}
			}, "Asynchronous Execution Service Thread " + count.incrementAndGet());
			return t;
		}
	});
	
	private volatile ServiceTracker<LogService, LogService> logServiceTracker;
	
	public void start(BundleContext context) throws Exception {
		logServiceTracker = new ServiceTracker<LogService, LogService>(context, LogService.class, null);
		logServiceTracker.open();
		
		context.registerService(Async.class.getName(), new AsyncServiceFactory(executor, logServiceTracker), new Hashtable<String, Object>());
	}

	public void stop(BundleContext context) throws Exception {
		executor.shutdownNow();
		logServiceTracker.close();
	}
}
