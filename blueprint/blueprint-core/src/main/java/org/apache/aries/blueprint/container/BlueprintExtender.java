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
package org.apache.aries.blueprint.container;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.*;

import org.apache.aries.blueprint.BlueprintConstants;
import org.apache.aries.blueprint.annotation.service.BlueprintAnnotationScanner;
import org.apache.aries.blueprint.namespace.NamespaceHandlerRegistryImpl;
import org.apache.aries.blueprint.services.ParserService;
import org.apache.aries.blueprint.utils.HeaderParser;
import org.apache.aries.blueprint.utils.HeaderParser.PathElement;
import org.apache.aries.blueprint.utils.threading.ScheduledExecutorServiceWrapper;
import org.apache.aries.blueprint.utils.threading.ScheduledExecutorServiceWrapper.ScheduledExecutorServiceFactory;
import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.util.AriesFrameworkUtil;
import org.apache.aries.util.tracker.RecursiveBundleTracker;
import org.apache.aries.util.tracker.SingleServiceTracker;
import org.apache.aries.util.tracker.SingleServiceTracker.SingleServiceListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the blueprint extender that listens to blueprint bundles.  
 *
 * @version $Rev$, $Date$
 */
public class BlueprintExtender implements BundleActivator, BundleTrackerCustomizer, SynchronousBundleListener {

    /** The QuiesceParticipant implementation class name */
    private static final String QUIESCE_PARTICIPANT_CLASS = "org.apache.aries.quiesce.participant.QuiesceParticipant";
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintExtender.class);

    private BundleContext context;
    private ScheduledExecutorService executors;
    private final ConcurrentMap<Bundle, BlueprintContainerImpl> containers = new ConcurrentHashMap<Bundle, BlueprintContainerImpl>();
    private final ConcurrentMap<Bundle, FutureTask> destroying = new ConcurrentHashMap<Bundle, FutureTask>();
    private BlueprintEventDispatcher eventDispatcher;
    private NamespaceHandlerRegistry handlers;
    private RecursiveBundleTracker bt;
    private ServiceRegistration parserServiceReg;
    private ServiceRegistration quiesceParticipantReg;
    private SingleServiceTracker<ProxyManager> proxyManager;
    private ExecutorServiceFinder executorServiceFinder;
    private volatile boolean stopping;

    public void start(BundleContext ctx) {
        LOGGER.debug("Starting blueprint extender...");

        this.context = ctx;
        handlers = new NamespaceHandlerRegistryImpl(ctx);
        executors = new ScheduledExecutorServiceWrapper(ctx, "Blueprint Extender", new ScheduledExecutorServiceFactory() {
          public ScheduledExecutorService create(String name)
          {
            return Executors.newScheduledThreadPool(3, new BlueprintThreadFactory(name));
          }
        });
        eventDispatcher = new BlueprintEventDispatcher(ctx, executors);

        // Ideally we'd want to only track STARTING and ACTIVE bundle, but this is not supported
        // when using equinox composites.  This would ensure that no STOPPING event is lost while
        // tracking the initial bundles. To work around this issue, we need to register
        // a synchronous bundle listener that will ensure the stopping event will be correctly
        // handled.
        context.addBundleListener(this);
        int mask = Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING | Bundle.STOPPING | Bundle.ACTIVE;
        bt = new RecursiveBundleTracker(ctx, mask, this);
        
        proxyManager = new SingleServiceTracker<ProxyManager>(ctx, ProxyManager.class, new SingleServiceListener() {
          public void serviceFound() {
            LOGGER.debug("Found ProxyManager service, starting to process blueprint bundles");
            bt.open();
          }
          public void serviceLost() {
            while (!containers.isEmpty()) {
              for (Bundle bundle : getBundlesToDestroy()) {
                destroyContainer(bundle);
              }
            }
            bt.close();
          }
          public void serviceReplaced() {
          }
        });
        proxyManager.open();
        
        // Create and publish a ParserService
        parserServiceReg = ctx.registerService(ParserService.class.getName(), 
            new ParserServiceImpl (handlers), 
            new Hashtable<String, Object>());

        try{
            ctx.getBundle().loadClass(QUIESCE_PARTICIPANT_CLASS);
            //Class was loaded, register

            quiesceParticipantReg = ctx.registerService(QUIESCE_PARTICIPANT_CLASS, 
              new BlueprintQuiesceParticipant(ctx, this), 
              new Hashtable<String, Object>());
        } 
        catch (ClassNotFoundException e) 
        {
            LOGGER.info("No quiesce support is available, so blueprint components will not participate in quiesce operations");
        }
        
        LOGGER.debug("Blueprint extender started");
    }

    public void stop(BundleContext context) {
        LOGGER.debug("Stopping blueprint extender...");

        stopping = true;

        AriesFrameworkUtil.safeUnregisterService(parserServiceReg);

        AriesFrameworkUtil.safeUnregisterService(quiesceParticipantReg);

        // Orderly shutdown of containers
        while (!containers.isEmpty()) {
            for (Bundle bundle : getBundlesToDestroy()) {
                destroyContainer(bundle);
            }
        }

        bt.close();
        proxyManager.close();

        this.eventDispatcher.destroy();
        this.handlers.destroy();
        executors.shutdown();
        LOGGER.debug("Blueprint extender stopped");
    }

    /*
     * SynchronousBundleListener
     */

    public void bundleChanged(BundleEvent event) {
        Bundle bundle = event.getBundle();
        if (bundle.getState() != Bundle.ACTIVE && bundle.getState() != Bundle.STARTING) {
            // The bundle is not in STARTING or ACTIVE state anymore
            // so destroy the context
            destroyContainer(bundle);
            return;
        }
    }

    /*
     * BundleTrackerCustomizer
     */

    public Object addingBundle(Bundle bundle, BundleEvent event) {
        modifiedBundle(bundle, event, bundle);
        return bundle;
    }

    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
        // If the bundle being stopped is the system bundle,
        // do an orderly shutdown of all blueprint contexts now
        // so that service usage can actually be useful
        if (bundle.getBundleId() == 0 && bundle.getState() == Bundle.STOPPING) {
            String val = context.getProperty("org.apache.aries.blueprint.preemptiveShutdown");
            if (val == null || Boolean.parseBoolean(val)) {
                stop(context);
                return;
            }
        }
        if (bundle.getState() != Bundle.ACTIVE && bundle.getState() != Bundle.STARTING) {
            // The bundle is not in STARTING or ACTIVE state anymore
            // so destroy the context
            destroyContainer(bundle);
            return;
        }
        // Do not track bundles given we are stopping
        if (stopping) {
            return;
        }
        // For starting bundles, ensure, it's a lazy activation,
        // else we'll wait for the bundle to become ACTIVE
        if (bundle.getState() == Bundle.STARTING) {
            String activationPolicyHeader = (String) bundle.getHeaders().get(Constants.BUNDLE_ACTIVATIONPOLICY);
            if (activationPolicyHeader == null || !activationPolicyHeader.startsWith(Constants.ACTIVATION_LAZY)) {
                // Do not track this bundle yet
                return;
            }
        }
        createContainer(bundle);
    }

    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        // Nothing to do
        destroyContainer(bundle);
    }

    private boolean createContainer(Bundle bundle) {
        try {
            List<Object> paths = getBlueprintPaths(bundle);
            if (paths == null) {
                // This bundle is not a blueprint bundle, so ignore it
                return false;
            }
            ProxyManager pm = proxyManager.getService();
            if (pm == null) {
                // The pm isn't available.  It may be because it is being untracked
                return false;
            }
            BundleContext bundleContext = bundle.getBundleContext();
            if (bundleContext == null) {
                // The bundle has been stopped in the mean time
                return false;
            }
            BlueprintContainerImpl blueprintContainer = new BlueprintContainerImpl(bundle, bundleContext,
                                                                context.getBundle(), eventDispatcher,
                                                                handlers, getExecutorService(bundle),
                                                                executors, paths, pm);
            synchronized (containers) {
                if (containers.putIfAbsent(bundle, blueprintContainer) != null) {
                    return false;
                }
            }
            String val = context.getProperty("org.apache.aries.blueprint.synchronous");
            if (Boolean.parseBoolean(val)) {
                LOGGER.debug("Starting creation of blueprint bundle {} synchronously", bundle.getSymbolicName());
                blueprintContainer.run();
            } else {
                LOGGER.debug("Scheduling creation of blueprint bundle {} asynchronously", bundle.getSymbolicName());
                blueprintContainer.schedule();
            }
            return true;
        } catch (Throwable t) {
            LOGGER.warn("Error while creating blueprint container for bundle " + bundle, t);
            return false;
        }
    }

    private void destroyContainer(final Bundle bundle) {
        FutureTask future;
        synchronized (containers) {
            LOGGER.debug("Starting BlueprintContainer destruction process for bundle {}", bundle.getSymbolicName());
            future = destroying.get(bundle);
            if (future == null) {
                final BlueprintContainerImpl blueprintContainer = containers.remove(bundle);
                if (blueprintContainer != null) {
                    LOGGER.debug("Scheduling BlueprintContainer destruction for {}.", bundle.getSymbolicName());
                    future = new FutureTask<Void>(new Runnable() {
                        public void run() {
                            LOGGER.info("Destroying BlueprintContainer for bundle {}", bundle.getSymbolicName());
                            try {
                                blueprintContainer.destroy();
                            } finally {
                                LOGGER.debug("Finished destroying BlueprintContainer for bundle {}", bundle.getSymbolicName());
                                eventDispatcher.removeBlueprintBundle(bundle);
                                synchronized (containers) {
                                    destroying.remove(bundle);
                                }
                            }
                        }
                    }, null);
                    destroying.put(bundle, future);
                } else {
                    LOGGER.debug("Not a blueprint bundle or destruction of BlueprintContainer already finished for {}.", bundle.getSymbolicName());
                }
            } else {
                LOGGER.debug("Destruction already scheduled for {}.", bundle.getSymbolicName());
            }
        }
        if (future != null) {
            try {
                LOGGER.debug("Waiting for BlueprintContainer destruction for {}.", bundle.getSymbolicName());
                future.run();
                future.get();
            } catch (Throwable t) {
                LOGGER.warn("Error while destroying blueprint container for bundle " + bundle, t);
            }
        }
    }

    private List<Object> getBlueprintPaths(Bundle bundle) {
        LOGGER.debug("Scanning bundle {} for blueprint application", bundle.getSymbolicName());
        try {
            List<Object> pathList = new ArrayList<Object>();
            String blueprintHeader = (String) bundle.getHeaders().get(BlueprintConstants.BUNDLE_BLUEPRINT_HEADER);
            String blueprintHeaderAnnotation = (String) bundle.getHeaders().get(BlueprintConstants.BUNDLE_BLUEPRINT_ANNOTATION_HEADER);
            if (blueprintHeader == null) {
                blueprintHeader = "OSGI-INF/blueprint/";
            }
            List<PathElement> paths = HeaderParser.parseHeader(blueprintHeader);
            for (PathElement path : paths) {
                String name = path.getName();
                if (name.endsWith("/")) {
                    addEntries(bundle, name, "*.xml", pathList);
                } else {
                    String baseName;
                    String filePattern;
                    int pos = name.lastIndexOf('/');
                    if (pos < 0) {
                        baseName = "/";
                        filePattern = name;
                    } else {
                        baseName = name.substring(0, pos + 1);
                        filePattern = name.substring(pos + 1);
                    }
                    if (hasWildcards(filePattern)) {
                        addEntries(bundle, baseName, filePattern, pathList);
                    } else {
                        addEntry(bundle, name, pathList);
                    }
                }
            }
            // Check annotations
            if (pathList.isEmpty() && blueprintHeaderAnnotation != null && blueprintHeaderAnnotation.trim().equalsIgnoreCase("true")) {
                LOGGER.debug("Scanning bundle {} for blueprint annotations", bundle.getSymbolicName());
                ServiceReference sr = this.context.getServiceReference(BlueprintAnnotationScanner.class.getName());
                if (sr != null) {
                    BlueprintAnnotationScanner bas = (BlueprintAnnotationScanner) this.context.getService(sr);
                    try {
                        // try to generate the blueprint definition XML
                        URL url = bas.createBlueprintModel(bundle);
                        if (url != null) {
                            pathList.add(url);
                        }
                    } finally {
                        this.context.ungetService(sr);
                    }
                }
            }
            if (!pathList.isEmpty()) {
                LOGGER.debug("Found blueprint application in bundle {} with paths: {}", bundle.getSymbolicName(), pathList);
                // Check compatibility
                // TODO: For lazy bundles, the class is either loaded from an imported package or not found, so it should
                // not trigger the activation.  If it does, we need to use something else like package admin or
                // ServiceReference, or just not do this check, which could be quite harmful.
                if (isCompatible(bundle)) {
                    return pathList;
                } else {
                    LOGGER.info("Bundle {} is not compatible with this blueprint extender", bundle.getSymbolicName());
                }
            } else {
                LOGGER.debug("No blueprint application found in bundle {}", bundle.getSymbolicName());
            }
        } catch (Throwable t) {
            if (!stopping) {
                LOGGER.warn("Error creating blueprint container for bundle " + bundle.getSymbolicName(), t);
                eventDispatcher.blueprintEvent(new BlueprintEvent(BlueprintEvent.FAILURE, bundle, context.getBundle(), t));
            }
        }
        return null;
    }

    private List<Bundle> getBundlesToDestroy() {
        List<Bundle> bundlesToDestroy = new ArrayList<Bundle>();
        for (Bundle bundle : containers.keySet()) {
            ServiceReference[] references = bundle.getRegisteredServices();
            int usage = 0;
            if (references != null) {
                for (ServiceReference reference : references) {
                    usage += getServiceUsage(reference);
                }
            }
            LOGGER.debug("Usage for bundle {} is {}", bundle, usage);
            if (usage == 0) {
                bundlesToDestroy.add(bundle);
            }
        }
        if (!bundlesToDestroy.isEmpty()) {
            Collections.sort(bundlesToDestroy, new Comparator<Bundle>() {
                public int compare(Bundle b1, Bundle b2) {
                    return (int) (b2.getLastModified() - b1.getLastModified());
                }
            });
            LOGGER.debug("Selected bundles {} for destroy (no services in use)", bundlesToDestroy);
        } else {
            ServiceReference ref = null;
            for (Bundle bundle : containers.keySet()) {
                ServiceReference[] references = bundle.getRegisteredServices();
                for (ServiceReference reference : references) {
                    if (getServiceUsage(reference) == 0) {
                        continue;
                    }
                    if (ref == null || reference.compareTo(ref) < 0) {
                        LOGGER.debug("Currently selecting bundle {} for destroy (with reference {})", bundle, reference);
                        ref = reference;
                    }
                }
            }
            if (ref != null) {
                bundlesToDestroy.add(ref.getBundle());
            }
            LOGGER.debug("Selected bundle {} for destroy (lowest ranking service)", bundlesToDestroy);
        }
        return bundlesToDestroy;
    }

    private static int getServiceUsage(ServiceReference ref) {
        Bundle[] usingBundles = ref.getUsingBundles();
        return (usingBundles != null) ? usingBundles.length : 0;
    }

    private ExecutorService getExecutorService(Bundle bundle) {
        if (executorServiceFinder != null) {
            return executorServiceFinder.find(bundle);
        } else {
            return executors;
        }
    }

    interface ExecutorServiceFinder {

        public ExecutorService find( Bundle bundle );

    }

    private boolean isCompatible(Bundle bundle) {
        // Check compatibility
        boolean compatible;
        if (bundle.getState() == Bundle.ACTIVE) {
            try {
                Class<?> clazz = bundle.getBundleContext().getBundle().loadClass(BlueprintContainer.class.getName());
                compatible = (clazz == BlueprintContainer.class);
            } catch (ClassNotFoundException e) {
                compatible = true;
            }
        } else {
            // for lazy bundle, we can't load the class, so just assume it's ok
            compatible = true;
        }
        return compatible;
    }
    
    private boolean hasWildcards(String path) {
        return path.indexOf("*") >= 0; 
    }
    
    private String getFilePart(URL url) {
        String path = url.getPath();
        int index = path.lastIndexOf('/');
        return path.substring(index + 1);
    }
    
    private String cachePath(Bundle bundle, String filePath)
    {
      return Integer.toHexString(bundle.hashCode()) + "/" + filePath;
    }    
    
    private URL getOverrideURLForCachePath(String privatePath){
        URL override = null;
        File privateDataVersion = context.getDataFile(privatePath);
        if (privateDataVersion != null
                && privateDataVersion.exists()) {
            try {
                override = privateDataVersion.toURI().toURL();
            } catch (MalformedURLException e) {
                LOGGER.error("Unexpected URL Conversion Issue", e);
            }
        }
        return override;
    }
    
    private URL getOverrideURL(Bundle bundle, String path){
        String cachePath = cachePath(bundle, path);
        return getOverrideURLForCachePath(cachePath);
    }
    
    private URL getOverrideURL(Bundle bundle, URL path, String basePath){
        String cachePath = cachePath(bundle, basePath + getFilePart(path));
        return getOverrideURLForCachePath(cachePath);
    }    
    
    private void addEntry(Bundle bundle, String path, List<Object> pathList) {
        URL override = getOverrideURL(bundle, path);
        if(override == null) {
            pathList.add(path);
        } else {
            pathList.add(override);
        }
    }
    
    private void addEntries(Bundle bundle, String path, String filePattern, List<Object> pathList) {
        Enumeration<?> e = bundle.findEntries(path, filePattern, false);
        while (e != null && e.hasMoreElements()) {
            URL u = (URL) e.nextElement();
            URL override = getOverrideURL(bundle, u, path);
            if(override == null) {
                pathList.add(u);
            } else {
                pathList.add(override);
            }
        }
    }
    
    protected BlueprintContainerImpl getBlueprintContainerImpl(Bundle bundle)
    {
        return containers.get(bundle);
    }
    
}
