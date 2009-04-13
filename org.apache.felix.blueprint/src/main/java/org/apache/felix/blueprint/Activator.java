package org.apache.felix.blueprint;

import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.blueprint.context.DefaultModuleContextEventSender;
import org.apache.felix.blueprint.context.ModuleContextImpl;
import org.apache.felix.blueprint.namespace.NamespaceHandlerRegistryImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.SynchronousBundleListener;

/**
 * TODO: javadoc
 *
 * TODO: handle ModuleContextListener
 */
public class Activator implements BundleActivator, BundleListener {

    private StopBundleListener stopBundleListener = new StopBundleListener();
    private Map<Bundle, ModuleContextImpl> contextMap = new HashMap<Bundle, ModuleContextImpl>();
    private ModuleContextEventSender sender;
    private NamespaceHandlerRegistry handlers;

    public void start(BundleContext context) {
        System.out.println("Starting to listen for bundle events.");
        context.addBundleListener(stopBundleListener);
        context.addBundleListener(this);

        sender = new DefaultModuleContextEventSender(context);
        handlers = new NamespaceHandlerRegistryImpl(context);

        Bundle[] bundles = context.getBundles();
        for (Bundle b : bundles) {
            if (b.getState() == Bundle.ACTIVE) {
                checkBundle(b);
            }
        }
    }


    public void stop(BundleContext context) {
        context.removeBundleListener(stopBundleListener);
        context.removeBundleListener(this);
        this.sender.destroy();
        this.handlers.destroy();
        System.out.println("Stopped listening for bundle events.");
    }

    public void bundleChanged(BundleEvent event) {
        System.out.println("bundle changed:" + event.getBundle().getSymbolicName() + "  "+ event.getType());
        if (event.getType() == BundleEvent.STARTED) {
            checkBundle(event.getBundle());
        }
    }

    private void destroyContext(Bundle bundle) {
        ModuleContextImpl moduleContext = contextMap.remove(bundle);
        if (moduleContext != null) {
            moduleContext.destroy();
        }
    }

    private void checkBundle(Bundle b) {
        System.out.println("Checking: " + b.getSymbolicName());

        List<URL> urls = new ArrayList<URL>();
        Enumeration e = b.findEntries("OSGI-INF/blueprint", "*.xml", true);
        if (e != null) {
            while (e.hasMoreElements()) {
                URL u = (URL) e.nextElement();
                System.out.println("found xml config:" + u);
                urls.add(u);
            }
        }
        if (urls.size() > 0) {
            ModuleContextImpl moduleContext = new ModuleContextImpl(b.getBundleContext(), sender, urls.toArray(new URL[urls.size()]));
            contextMap.put(b, moduleContext);
            moduleContext.create();
        }

        Dictionary d = b.getHeaders();
        System.out.println(d.get("Bundle-Blueprint"));
    }


    private class StopBundleListener implements SynchronousBundleListener {
        public void bundleChanged(BundleEvent event) {
            if (event.getType() == BundleEvent.STOPPING) {
                destroyContext(event.getBundle());
            }
        }
    }

}
