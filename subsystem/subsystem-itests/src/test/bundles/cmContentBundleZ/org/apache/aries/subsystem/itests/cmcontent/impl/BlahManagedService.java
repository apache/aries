package org.apache.aries.subsystem.itests.cmcontent.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public class BlahManagedService implements ManagedService {
    private final BundleContext bundleContext;

    public BlahManagedService(BundleContext context) {
        bundleContext = context;
    }

    @Override
    public void updated(Dictionary<String, ?> p) throws ConfigurationException {
        if ("test2".equals(p.get("configVal")) &&
                "test123".equals(p.get("configVal2"))) {
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put("test.pid", p.get(Constants.SERVICE_PID));
            bundleContext.registerService(String.class, "Blah!", props);
        }
    }
}
