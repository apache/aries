/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.spifly;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
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
        
	    bt = new BundleTracker(context, Bundle.ACTIVE, 
	            new SPIBundleTrackerCustomizer(this, context.getBundle()));
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
