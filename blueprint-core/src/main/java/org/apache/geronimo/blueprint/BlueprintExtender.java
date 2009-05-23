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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.geronimo.blueprint.container.BlueprintContainerImpl;
import org.apache.geronimo.blueprint.container.DefaultBlueprintEventSender;
import org.apache.geronimo.blueprint.namespace.NamespaceHandlerRegistryImpl;
import org.apache.geronimo.blueprint.utils.HeaderParser;
import org.apache.geronimo.blueprint.utils.HeaderParser.PathElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.blueprint.container.BlueprintContainer;
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

    private BundleContext context;
    private ScheduledExecutorService executors;
    private Map<Bundle, BlueprintContainerImpl> containers;
    private BlueprintEventSender sender;
    private NamespaceHandlerRegistry handlers;

    public void start(BundleContext context) {
        LOGGER.debug("Starting blueprint extender...");

        this.context = context;
        sender = new DefaultBlueprintEventSender(context);
        handlers = new NamespaceHandlerRegistryImpl(context);
        executors = Executors.newScheduledThreadPool(1);
        containers = new HashMap<Bundle, BlueprintContainerImpl>();

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
        // TODO: we should order the blueprint container destruction wrt service exports / dependencies
        // TODO: also if a blueprint bundle is being stopped at the same time (this could happen if the framework
        // TODO: is shut down, we should not wait for the blueprint container to be destroyed if it is already being
        // TODO: destroyed by the extender
        List<Bundle> bundles = new ArrayList<Bundle>(containers.keySet());
        for (Bundle bundle : bundles) {
            destroyContext(bundle);
        }
        context.removeBundleListener(this);
        this.sender.destroy();
        this.handlers.destroy();
        executors.shutdown();
        LOGGER.debug("Blueprint extender stopped");
    }

    public void bundleChanged(BundleEvent event) {
        Bundle bundle = event.getBundle();
        if (event.getType() == BundleEvent.LAZY_ACTIVATION) {
            checkBundle(bundle, true);
        } else if (event.getType() == BundleEvent.STARTED) {
            BlueprintContainerImpl blueprintContainer = containers.get(bundle);
            if (blueprintContainer == null) {
                checkBundle(bundle, false);
            }
        } else if (event.getType() == BundleEvent.STOPPING) {
            destroyContext(bundle);
        }
    }

    private void destroyContext(Bundle bundle) {
        BlueprintContainerImpl blueprintContainer = containers.remove(bundle);
        if (blueprintContainer != null) {
            LOGGER.debug("Destroying BlueprintContainer for bundle {}", bundle.getSymbolicName());
            blueprintContainer.destroy();
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
                    final BlueprintContainerImpl blueprintContainer = new BlueprintContainerImpl(bundle.getBundleContext(), context.getBundle(), sender, handlers, executors, urls, lazyActivation);
                    containers.put(bundle, blueprintContainer);
                    // run synchronous when bundle is lazy activated
                    blueprintContainer.run(lazyActivation ? false: true);
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
            Class clazz = bundle.getBundleContext().getBundle().loadClass(BlueprintContainer.class.getName());
            compatible = (clazz == BlueprintContainer.class);
        } catch (ClassNotFoundException e) {
            compatible = true;
        }
        return compatible;
    }
}
