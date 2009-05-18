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
package org.apache.geronimo.blueprint;

import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.geronimo.blueprint.utils.HeaderParser.PathElement;
import org.apache.geronimo.blueprint.context.DefaultBlueprintContextEventSender;
import org.apache.geronimo.blueprint.context.BlueprintContextImpl;
import org.apache.geronimo.blueprint.namespace.NamespaceHandlerRegistryImpl;
import org.apache.geronimo.blueprint.utils.HeaderParser;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.blueprint.context.BlueprintContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class BlueprintExtender implements BundleActivator, SynchronousBundleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintExtender.class);

    private ExecutorService executors;
    private Timer timer;
    private Map<Bundle, BlueprintContextImpl> contextMap;
    private BlueprintContextEventSender sender;
    private NamespaceHandlerRegistry handlers;

    public void start(BundleContext context) {
        LOGGER.debug("Starting blueprint extender...");

        sender = new DefaultBlueprintContextEventSender(context);
        handlers = new NamespaceHandlerRegistryImpl(context);
        executors = Executors.newSingleThreadExecutor();
        timer = new Timer("BlueprintExtender-Timer", true);
        contextMap = new HashMap<Bundle, BlueprintContextImpl>();

        context.addBundleListener(this);

        Bundle[] bundles = context.getBundles();
        for (Bundle b : bundles) {
            if (b.getState() == Bundle.ACTIVE) {
                checkBundle(b, false);
            }
        }
        LOGGER.debug("Blueprint extender started");
    }


    public void stop(BundleContext context) {
        LOGGER.debug("Stopping blueprint extender...");
        // TODO: we should order the blueprint context destruction wrt service exports / dependencies
        // TODO: also if a blueprint bundle is being stopped at the same time (this could happen if the framework
        //        is shut down, we should not wait for the blueprint context to be destroyed if it is already being
        //        destroyed by the extender
        List<Bundle> bundles = new ArrayList<Bundle>(contextMap.keySet());
        for (Bundle bundle : bundles) {
            destroyContext(bundle);
        }
        context.removeBundleListener(this);
        this.sender.destroy();
        this.handlers.destroy();
        executors.shutdown();
        timer.cancel();
        LOGGER.debug("Blueprint extender stopped");
    }

    public void bundleChanged(BundleEvent event) {
        Bundle bundle = event.getBundle();
        if (event.getType() == BundleEvent.LAZY_ACTIVATION) {
            checkBundle(bundle, true);
        } else if (event.getType() == BundleEvent.STARTED) {
            BlueprintContextImpl blueprintContext = contextMap.get(bundle);
            if (blueprintContext == null) {
                checkBundle(bundle, false);
            } else {
                blueprintContext.triggerActivation();
            }
        } else if (event.getType() == BundleEvent.STOPPING) {
            destroyContext(bundle);
        }
    }

    private void destroyContext(Bundle bundle) {
        BlueprintContextImpl blueprintContext = contextMap.remove(bundle);
        if (blueprintContext != null) {
            LOGGER.debug("Destroying BlueprintContext for bundle {}", bundle.getSymbolicName());
            blueprintContext.destroy();
        }
    }
    
    private void checkBundle(Bundle bundle, boolean lazyActivation) {
        LOGGER.debug("Scanning bundle {} for blueprint application (lazy: {})", bundle.getSymbolicName(), lazyActivation);
        try {
            List<URL> urls = new ArrayList<URL>();
            Dictionary headers = bundle.getHeaders();
            String blueprintHeader = (String)headers.get(BlueprintConstants.BUNDLE_BLUEPRINT_HEADER);
            if (blueprintHeader != null) {
                List<PathElement> paths = HeaderParser.parseHeader(blueprintHeader);
                for (PathElement path : paths) {
                    URL url = bundle.getEntry(path.getName());
                    if (url != null) {
                        urls.add(url);
                    } else {
                        throw new IllegalArgumentException("Unable to find bundle entry for config file " + path.getName());
                    }
                }
            }
            if (urls.isEmpty()) {
                Enumeration e = bundle.findEntries("OSGI-INF/blueprint", "*.xml", false);
                if (e != null) {
                    while (e.hasMoreElements()) {
                        URL u = (URL) e.nextElement();
                        urls.add(u);
                    }
                }
            }
            if (!urls.isEmpty()) {
                LOGGER.debug("Found blueprint application in bundle {} with urls: {}", bundle.getSymbolicName(), urls);

                // Check compatibility
                // XXX: we can't check compatibility when dealing with lazy activated bundles since that will trigger
                // the bundle to be fully started
                boolean compatible = lazyActivation || isCompatible(bundle);
                if (compatible) {
                    final BlueprintContextImpl blueprintContext = new BlueprintContextImpl(bundle.getBundleContext(), sender, handlers, executors, timer, urls, lazyActivation);
                    contextMap.put(bundle, blueprintContext);
                    // run synchronous when bundle is lazy activated
                    blueprintContext.run(lazyActivation ? false: true);
                } else {
                    LOGGER.info("Bundle {} is not compatible with this blueprint extender", bundle.getSymbolicName());
                }

            } else {
                LOGGER.debug("No blueprint application found in bundle {}", bundle.getSymbolicName());
            }
        } catch (Throwable t) {
            sender.sendFailure(bundle, t);
        }
    }

    private boolean isCompatible(Bundle bundle) {
        // Check compatibility
        boolean compatible;
        try {
            Class clazz = bundle.getBundleContext().getBundle().loadClass(BlueprintContext.class.getName());
            compatible = (clazz == BlueprintContext.class);
        } catch (ClassNotFoundException e) {
            compatible = true;
        }
        return compatible;
    }
}
