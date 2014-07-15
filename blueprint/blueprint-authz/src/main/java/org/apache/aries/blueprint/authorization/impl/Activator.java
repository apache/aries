package org.apache.aries.blueprint.authorization.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.aries.blueprint.NamespaceHandler;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        AuthorizationNsHandler handler = new AuthorizationNsHandler();
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("osgi.service.blueprint.namespace", "http://aries.apache.org/xmlns/authorization/v1.0.0");
        context.registerService(NamespaceHandler.class, handler, props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }

}
