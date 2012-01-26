/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.jmx.framework;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.management.openmbean.CompositeData;

import org.apache.aries.jmx.codec.BatchActionResult;
import org.apache.aries.jmx.codec.BatchInstallResult;
import org.apache.aries.jmx.codec.BatchResolveResult;
import org.apache.aries.jmx.util.FrameworkUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.jmx.framework.FrameworkMBean;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

/**
 * <p>
 * <tt>Framework</tt> represents {@link FrameworkMBean} implementation.
 * </p>
 * @see FrameworkMBean
 *
 * @version $Rev$ $Date$
 */
public class Framework implements FrameworkMBean {

    private StartLevel startLevel;
    private PackageAdmin packageAdmin;
    private BundleContext context;

    /**
     * Constructs new FrameworkMBean.
     *
     * @param context bundle context of jmx bundle.
     * @param startLevel @see {@link StartLevel} service reference.
     * @param packageAdmin @see {@link PackageAdmin} service reference.
     */
    public Framework(BundleContext context, StartLevel startLevel, PackageAdmin packageAdmin) {
        this.context = context;
        this.startLevel = startLevel;
        this.packageAdmin = packageAdmin;
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#getDependencyClosureBundles(long[])
     */
    public long[] getDependencyClosure(long[] bundles) throws IOException {
        FrameworkWiring fw = context.getBundle(0).adapt(FrameworkWiring.class);

        List<Bundle> bl = new ArrayList<Bundle>();
        for (int i=0; i < bundles.length; i++) {
            bl.add(context.getBundle(bundles[i]));
        }

        Collection<Bundle> rc = fw.getDependencyClosure(bl);

        Iterator<Bundle> it = rc.iterator();
        long[] result = new long[rc.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = it.next().getBundleId();
        }
        return result;
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#getFrameworkStartLevel()
     */
    public int getFrameworkStartLevel() throws IOException {
        return startLevel.getStartLevel();
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#getInitialBundleStartLevel()
     */
    public int getInitialBundleStartLevel() throws IOException {
        return startLevel.getInitialBundleStartLevel();
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#getProperty(java.lang.String)
     */
    public String getProperty(String key) {
        return context.getProperty(key);
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#getRemovalPendingBundles()
     */
    public long[] getRemovalPendingBundles() throws IOException {
        FrameworkWiring fw = context.getBundle(0).adapt(FrameworkWiring.class);

        Collection<Bundle> rc = fw.getRemovalPendingBundles();
        Iterator<Bundle> it = rc.iterator();
        long[] result = new long[rc.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = it.next().getBundleId();
        }
        return result;
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#installBundle(java.lang.String)
     */
    public long installBundle(String location) throws IOException {
        try {
            Bundle bundle = context.installBundle(location);
            return bundle.getBundleId();
        } catch (Exception e) {
            IOException ioex = new IOException("Installation of a bundle with location " + location + " failed with the message: " + e.getMessage());
            ioex.initCause(e);
            throw ioex;
        }
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#installBundleFromURL(String, String)
     */
    public long installBundleFromURL(String location, String url) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = createStream(url);
            Bundle bundle = context.installBundle(location, inputStream);
            return bundle.getBundleId();
        } catch (Exception e) {
            IOException ioex = new IOException("Installation of a bundle with location " + location + " failed with the message: " + e.getMessage());
            ioex.initCause(e);
            throw ioex;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    public InputStream createStream(String url) throws IOException {
        return new URL(url).openStream();
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#installBundles(java.lang.String[])
     */
    public CompositeData installBundles(String[] locations) throws IOException {
        if(locations == null){
           return new BatchInstallResult("Failed to install bundles locations can't be null").toCompositeData();
        }
        long[] ids = new long[locations.length];
        for (int i = 0; i < locations.length; i++) {
            try {
                long id = installBundle(locations[i]);
                ids[i] = id;
            } catch (Throwable t) {
                long[] completed = new long[i];
                System.arraycopy(ids, 0, completed, 0, i);
                String[] remaining = new String[locations.length - i - 1];
                System.arraycopy(locations, i + 1, remaining, 0, remaining.length);
                return new BatchInstallResult(completed, t.toString(), remaining, locations[i]).toCompositeData();
            }
        }

        return new BatchInstallResult(ids).toCompositeData();
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#installBundlesFromURL(String[], String[])
     */
    public CompositeData installBundlesFromURL(String[] locations, String[] urls) throws IOException {
        if(locations == null || urls == null){
            return new BatchInstallResult("Failed to install bundles arguments can't be null").toCompositeData();
        }

        if(locations.length != urls.length){
            return new BatchInstallResult("Failed to install bundles size of arguments should be same").toCompositeData();
        }
        long[] ids = new long[locations.length];
        for (int i = 0; i < locations.length; i++) {
            try {
                long id = installBundleFromURL(locations[i], urls[i]);
                ids[i] = id;
            } catch (Throwable t) {
                long[] completed = new long[i];
                System.arraycopy(ids, 0, completed, 0, i);
                String[] remaining = new String[locations.length - i - 1];
                System.arraycopy(locations, i + 1, remaining, 0, remaining.length);
                return new BatchInstallResult(completed, t.toString(), remaining, locations[i]).toCompositeData();
            }
        }
        return new BatchInstallResult(ids).toCompositeData();
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#refreshBundle(long)
     */
    public void refreshBundle(long bundleIdentifier) throws IOException {
        Bundle bundle = FrameworkUtils.resolveBundle(context, bundleIdentifier);
        packageAdmin.refreshPackages(new Bundle[] { bundle });
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#refreshBundleAndWait(long)
     */
    public boolean refreshBundleAndWait(long bundleIdentifier) throws IOException {
        Bundle[] bundleArray = new Bundle[1];
        refreshBundlesAndWait(new long[] {bundleIdentifier}, bundleArray);
        return isResolved(bundleArray[0].getState());
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#refreshBundles(long[])
     */
    public void refreshBundles(long[] bundleIdentifiers) throws IOException {
       Bundle[] bundles = null;
       if(bundleIdentifiers != null) {
          bundles = new Bundle[bundleIdentifiers.length];
          for (int i = 0; i < bundleIdentifiers.length; i++) {
              try {
                  bundles[i] = FrameworkUtils.resolveBundle(context, bundleIdentifiers[i]);
              } catch (Exception e) {
                  IOException ex = new IOException("Unable to find bundle with id " + bundleIdentifiers[i]);
                  ex.initCause(e);
                  throw ex;
              }
          }
       }
       packageAdmin.refreshPackages(bundles);
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#refreshBundlesAndWait(long[])
     */
    public CompositeData refreshBundlesAndWait(long[] bundleIdentifiers) throws IOException {
        Bundle [] bundles = bundleIdentifiers != null ? new Bundle[bundleIdentifiers.length] : null;
        refreshBundlesAndWait(bundleIdentifiers, bundles);
        return constructResolveResult(bundles);
    }

    private void refreshBundlesAndWait(long[] bundleIdentifiers, Bundle[] bundles) throws IOException {
        final CountDownLatch latch = new CountDownLatch(1);
        FrameworkListener listener = new FrameworkListener() {
            public void frameworkEvent(FrameworkEvent event) {
                if (FrameworkEvent.PACKAGES_REFRESHED == event.getType()) {
                    latch.countDown();
                }
            }
        };
        try {
            context.addFrameworkListener(listener);
            try {
                if (bundles != null) {
                    for (int i=0; i < bundleIdentifiers.length; i++) {
                        bundles[i] = FrameworkUtils.resolveBundle(context, bundleIdentifiers[i]);
                    }
                }
                packageAdmin.refreshPackages(bundles);

                if (latch.await(30, TimeUnit.SECONDS))
                    return;
                else
                    throw new IOException("Refresh operation timed out");
            } catch (InterruptedException e) {
                IOException ex = new IOException();
                ex.initCause(e);
                throw ex;
            }
        } finally {
            context.removeFrameworkListener(listener);
        }
    }

    private CompositeData constructResolveResult(Bundle[] bundles) {
        if (bundles == null)
            bundles = context.getBundles();

        boolean result = true;
        List<Long> successList = new ArrayList<Long>();
        for (Bundle bundle : bundles) {
            int state = bundle.getState();
            if (isResolved(state)) {
                successList.add(bundle.getBundleId());
            } else
                result = false;
        }

        return new BatchResolveResult(result, successList.toArray(new Long[] {})).toCompositeData();
    }

    private boolean isResolved(int state) {
        return (state & (Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE)) > 0;
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#resolveBundle(long)
     */
    public boolean resolveBundle(long bundleIdentifier) throws IOException {
        Bundle bundle = FrameworkUtils.resolveBundle(context, bundleIdentifier);
        return packageAdmin.resolveBundles(new Bundle[] { bundle });
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#resolveBundles(long[])
     */
    public boolean resolveBundles(long[] bundleIdentifiers) throws IOException {
        Bundle[] bundles = null;
        if (bundleIdentifiers != null)
            bundles = new Bundle[bundleIdentifiers.length];

        return resolveBundles(bundleIdentifiers, bundles);
    }

    private boolean resolveBundles(long[] bundleIdentifiers, Bundle[] bundles) throws IOException {
        if (bundleIdentifiers != null) {
            for (int i = 0; i < bundleIdentifiers.length; i++) {
                try {
                    bundles[i] = FrameworkUtils.resolveBundle(context, bundleIdentifiers[i]);
                } catch (Exception e) {
                    IOException ex = new IOException("Unable to find bundle with id " + bundleIdentifiers[i]);
                    ex.initCause(e);
                    throw ex;
                }
            }
        }

        return packageAdmin.resolveBundles(bundles);
    }

    public CompositeData resolve(long[] bundleIdentifiers) throws IOException {
        Bundle[] bundles = null;
        if (bundleIdentifiers != null)
            bundles = new Bundle[bundleIdentifiers.length];

        resolveBundles(bundleIdentifiers, bundles);
        return constructResolveResult(bundles);
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#restartFramework()
     */
    public void restartFramework() throws IOException {
        Bundle bundle = FrameworkUtils.resolveBundle(context, 0);
        try {
            bundle.update();
        } catch (Exception be) {
            IOException ioex = new IOException("Framework restart failed with message: " + be.getMessage());
            ioex.initCause(be);
            throw ioex;
        }
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#setBundleStartLevel(long, int)
     */
    public void setBundleStartLevel(long bundleIdentifier, int newlevel) throws IOException {
        Bundle bundle = FrameworkUtils.resolveBundle(context, bundleIdentifier);
        try {
            startLevel.setBundleStartLevel(bundle, newlevel);
        } catch (IllegalArgumentException e) {
            IOException ioex = new IOException("Setting the start level for bundle with id " + bundle.getBundleId() + " to level " + newlevel + " failed with message: " + e.getMessage());
            ioex.initCause(e);
            throw ioex;
        }
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#setBundleStartLevels(long[], int[])
     */
    public CompositeData setBundleStartLevels(long[] bundleIdentifiers, int[] newlevels) throws IOException {
        if (bundleIdentifiers == null || newlevels == null) {
            return new BatchActionResult("Failed to setBundleStartLevels arguments can't be null").toCompositeData();
        }

        if (bundleIdentifiers != null && newlevels != null && bundleIdentifiers.length != newlevels.length) {
            return new BatchActionResult("Failed to setBundleStartLevels size of arguments should be same").toCompositeData();
        }
        for (int i = 0; i < bundleIdentifiers.length; i++) {
            try {
                setBundleStartLevel(bundleIdentifiers[i], newlevels[i]);
            } catch (Throwable t) {
                return createFailedBatchActionResult(bundleIdentifiers, i, t);
            }
        }
        return new BatchActionResult(bundleIdentifiers).toCompositeData();
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#setFrameworkStartLevel(int)
     */
    public void setFrameworkStartLevel(int newlevel) throws IOException {
        try {
            startLevel.setStartLevel(newlevel);
        } catch (Exception e) {
            IOException ioex = new IOException("Setting the framework start level to " + newlevel + " failed with message: " + e.getMessage());
            ioex.initCause(e);
            throw ioex;
        }
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#setInitialBundleStartLevel(int)
     */
    public void setInitialBundleStartLevel(int newlevel) throws IOException {
        try {
            startLevel.setInitialBundleStartLevel(newlevel);
        } catch (Exception e) {
            IOException ioex = new IOException("Setting the initial start level to " + newlevel + " failed with message: " + e.getMessage());
            ioex.initCause(e);
            throw ioex;
        }
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#shutdownFramework()
     */
    public void shutdownFramework() throws IOException {
        Bundle bundle = FrameworkUtils.resolveBundle(context, 0);
        try {
            bundle.stop();
        } catch (Exception be) {
            IOException ioex = new IOException("Stopping the framework failed with message: " + be.getMessage());
            ioex.initCause(be);
            throw ioex;
        }
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#startBundle(long)
     */
    public void startBundle(long bundleIdentifier) throws IOException {
        Bundle bundle = FrameworkUtils.resolveBundle(context, bundleIdentifier);
        try {
            bundle.start();
        } catch (Exception be) {
            IOException ioex = new IOException("Start of bundle with id " + bundleIdentifier + " failed with message: " + be.getMessage());
            ioex.initCause(be);
            throw ioex;
        }
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#startBundles(long[])
     */
    public CompositeData startBundles(long[] bundleIdentifiers) throws IOException {
        if (bundleIdentifiers == null) {
            return new BatchActionResult("Failed to start bundles, bundle id's can't be null").toCompositeData();
        }
        for (int i = 0; i < bundleIdentifiers.length; i++) {
            try {
                startBundle(bundleIdentifiers[i]);
            } catch (Throwable t) {
                return createFailedBatchActionResult(bundleIdentifiers, i, t);
            }
        }
        return new BatchActionResult(bundleIdentifiers).toCompositeData();
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#stopBundle(long)
     */
    public void stopBundle(long bundleIdentifier) throws IOException {
        Bundle bundle = FrameworkUtils.resolveBundle(context, bundleIdentifier);
        try {
            bundle.stop();
        } catch (Exception e) {
            IOException ioex = new IOException("Stop of bundle with id " + bundleIdentifier + " failed with message: " + e.getMessage());
            ioex.initCause(e);
            throw ioex;
        }
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#stopBundles(long[])
     */
    public CompositeData stopBundles(long[] bundleIdentifiers) throws IOException {
        if (bundleIdentifiers == null) {
            return new BatchActionResult("Failed to stop bundles, bundle id's can't be null").toCompositeData();
        }
        for (int i = 0; i < bundleIdentifiers.length; i++) {
            try {
                stopBundle(bundleIdentifiers[i]);
            } catch (Throwable t) {
                return createFailedBatchActionResult(bundleIdentifiers, i, t);
            }
        }
        return new BatchActionResult(bundleIdentifiers).toCompositeData();
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#uninstallBundle(long)
     */
    public void uninstallBundle(long bundleIdentifier) throws IOException {
        Bundle bundle = FrameworkUtils.resolveBundle(context, bundleIdentifier);
        try {
            bundle.uninstall();
        } catch (Exception be) {
            IOException ioex = new IOException("Uninstall of bundle with id " + bundleIdentifier + " failed with message: " + be.getMessage());
            ioex.initCause(be);
            throw ioex;
        }
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#uninstallBundles(long[])
     */
    public CompositeData uninstallBundles(long[] bundleIdentifiers) throws IOException {
        if (bundleIdentifiers == null) {
            return new BatchActionResult("Failed uninstall bundles, bundle id's can't be null").toCompositeData();
        }
        for (int i = 0; i < bundleIdentifiers.length; i++) {
            try {
                uninstallBundle(bundleIdentifiers[i]);
            } catch (Throwable t) {
                return createFailedBatchActionResult(bundleIdentifiers, i, t);
            }
        }
        return new BatchActionResult(bundleIdentifiers).toCompositeData();
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#updateBundle(long)
     */
    public void updateBundle(long bundleIdentifier) throws IOException {
        Bundle bundle = FrameworkUtils.resolveBundle(context, bundleIdentifier);
        try {
            bundle.update();
        } catch (Exception be) {
            IOException ioex = new IOException("Update of bundle with id " + bundleIdentifier + " failed with message: " + be.getMessage());
            ioex.initCause(be);
            throw ioex;
        }
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#updateBundleFromURL(long, String)
     */
    public void updateBundleFromURL(long bundleIdentifier, String url) throws IOException {
        Bundle bundle = FrameworkUtils.resolveBundle(context, bundleIdentifier);
        InputStream inputStream = null;
        try {
            inputStream = createStream(url);
            bundle.update(inputStream);
        } catch (Exception be) {
            IOException ioex = new IOException("Update of bundle with id " + bundleIdentifier + " from url " + url + " failed with message: " + be.getMessage());
            ioex.initCause(be);
            throw ioex;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ioe) {

                }
            }
        }
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#updateBundles(long[])
     */
    public CompositeData updateBundles(long[] bundleIdentifiers) throws IOException {
        if (bundleIdentifiers == null) {
            return new BatchActionResult("Failed to update bundles, bundle id's can't be null").toCompositeData();
        }
        for (int i = 0; i < bundleIdentifiers.length; i++) {
            try {
                updateBundle(bundleIdentifiers[i]);
            } catch (Throwable t) {
                return createFailedBatchActionResult(bundleIdentifiers, i, t);
            }
        }
        return new BatchActionResult(bundleIdentifiers).toCompositeData();
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#updateBundlesFromURL(long[], String[])
     */
    public CompositeData updateBundlesFromURL(long[] bundleIdentifiers, String[] urls) throws IOException {
        if(bundleIdentifiers == null || urls == null){
            return new BatchActionResult("Failed to update bundles arguments can't be null").toCompositeData();
        }

        if(bundleIdentifiers != null && urls != null && bundleIdentifiers.length != urls.length){
            return new BatchActionResult("Failed to update bundles size of arguments should be same").toCompositeData();
        }
        for (int i = 0; i < bundleIdentifiers.length; i++) {
            try {
                updateBundleFromURL(bundleIdentifiers[i], urls[i]);
            } catch (Throwable t) {
                return createFailedBatchActionResult(bundleIdentifiers, i, t);
            }
        }
        return new BatchActionResult(bundleIdentifiers).toCompositeData();
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#updateFramework()
     */
    public void updateFramework() throws IOException {
        Bundle bundle = FrameworkUtils.resolveBundle(context, 0);
        try {
            bundle.update();
        } catch (Exception be) {
            IOException ioex = new IOException("Update of framework bundle failed with message: " + be.getMessage());
            ioex.initCause(be);
            throw ioex;
        }
    }

    /**
     * Create {@link BatchActionResult}, when the operation fail.
     *
     * @param bundleIdentifiers bundle ids for operation.
     * @param i index of loop pointing on which operation fails.
     * @param t Throwable thrown by failed operation.
     * @return created BatchActionResult instance.
     */
    private CompositeData createFailedBatchActionResult(long[] bundleIdentifiers, int i, Throwable t) {
        long[] completed = new long[i];
        System.arraycopy(bundleIdentifiers, 0, completed, 0, i);
        long[] remaining = new long[bundleIdentifiers.length - i - 1];
        System.arraycopy(bundleIdentifiers, i + 1, remaining, 0, remaining.length);
        return new BatchActionResult(completed, t.toString(), remaining, bundleIdentifiers[i]).toCompositeData();
    }

}
