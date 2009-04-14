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
package org.apache.felix.blueprint;

import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.blueprint.HeaderParser.PathElement;
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
    
    private void checkBundle(Bundle bundle) {
        System.out.println("Checking: " + bundle.getSymbolicName());

        List<URL> urls = new ArrayList<URL>();
        Dictionary headers = bundle.getHeaders();
        String blueprintHeader = (String)headers.get("Bundle-Blueprint");
        if (blueprintHeader != null) {
            List<PathElement> paths = HeaderParser.parseHeader(blueprintHeader);
            for (PathElement path : paths) {
                URL url = bundle.getEntry(path.getName());
                if (url != null) {
                    urls.add(url);
                }
            }
        }
        
        if (urls.isEmpty()) {
            Enumeration e = bundle.findEntries("OSGI-INF/blueprint", "*.xml", true);
            if (e != null) {
                while (e.hasMoreElements()) {
                    URL u = (URL) e.nextElement();
                    urls.add(u);
                }
            }
        }
                
        if (!urls.isEmpty()) {
            System.out.println("Found config files:" + urls);
            ModuleContextImpl moduleContext = new ModuleContextImpl(bundle.getBundleContext(), sender, urls);
            contextMap.put(bundle, moduleContext);
            moduleContext.create();
        }
    }


    private class StopBundleListener implements SynchronousBundleListener {
        public void bundleChanged(BundleEvent event) {
            if (event.getType() == BundleEvent.STOPPING) {
                destroyContext(event.getBundle());
            }
        }
    }

}
