package org.apache.aries.spifly;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;

public class SPIBundleTracker extends BundleTracker {
    public static final String SPI_PROVIDER_URL = "spi.provider.url";
    
    final Activator activator;
    List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();
    
    public SPIBundleTracker(BundleContext context, Activator a) {
        super(context, Bundle.ACTIVE, null);
        activator = a;
    }
    
    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        Object rv = super.addingBundle(bundle, event);
        log(LogService.LOG_INFO, "Bundle Considered for SPI providers: " + bundle.getSymbolicName());
        if (bundle.equals(context.getBundle())) {
            return rv;
        }
        
        Enumeration<?> entries = bundle.findEntries("META-INF/services", "*", false);
        if (entries == null) {
            return rv;
        }
        
        while(entries.hasMoreElements()) {
            URL url = (URL) entries.nextElement();
            log(LogService.LOG_INFO, "Found SPI resource: " + url);
            
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String className = reader.readLine(); 
                // do we need to read more than one class name?
                
                Class<?> cls = bundle.loadClass(className);
                Object o = cls.newInstance();
                log(LogService.LOG_INFO, "Instantiated SPI provider: " + o);
                
                Hashtable<String, Object> props = new Hashtable<String, Object>();
                props.put(SPI_PROVIDER_URL, url);
                
                String s = url.toExternalForm();
                int idx = s.lastIndexOf('/');
                String registrationClassName = className;
                if (s.length() > idx) {
                    registrationClassName = s.substring(idx + 1);
                }
                                
                synchronized (this) {
                    registrations.add(bundle.getBundleContext().registerService(registrationClassName, o, props));
                }
            } catch (Exception e) {
                log(LogService.LOG_INFO, "Could not load SPI implementation referred from " + url, e);
                e.printStackTrace();
            }
        }
        
        return rv;
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        synchronized (this) {
            for (Iterator<ServiceRegistration> it = registrations.iterator(); it.hasNext(); ) {
                ServiceRegistration sr = it.next();
                if (bundle.equals(sr.getReference().getBundle())) {
                    sr.unregister();
                    it.remove();
                }
            }
        }
        
        super.removedBundle(bundle, event, object);
    }        

    @Override
    public void close() {
        super.close();
        
        for (ServiceRegistration sr : registrations) {
            sr.unregister();
        }
        registrations.clear();        
    }

    private void log(int level, String message) {
        activator.log(level, message);
    }
    
    private void log(int level, String message, Throwable th) {
        activator.log(level, message, th);
    }
}
