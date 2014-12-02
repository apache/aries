package org.apache.aries.subsystem.itests.cmcontent.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;

public class Activator implements BundleActivator {
    @Override
    public void start(BundleContext context) throws Exception {
        Dictionary<String, Object> blahProps = new Hashtable<String, Object>();
        blahProps.put(Constants.SERVICE_PID, "com.blah.Blah");
        context.registerService(ManagedService.class, new BlahManagedService(context), blahProps);

        Dictionary<String, Object> barProps = new Hashtable<String, Object>();
        barProps.put(Constants.SERVICE_PID, "org.foo.Bar");
        context.registerService(ManagedService.class, new BarManagedService(context), barProps);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
}
