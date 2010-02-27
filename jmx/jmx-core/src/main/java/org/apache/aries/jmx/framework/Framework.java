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

import javax.management.openmbean.CompositeData;

import org.apache.aries.jmx.codec.BatchActionResult;
import org.apache.aries.jmx.codec.BatchInstallResult;
import org.apache.aries.jmx.util.FrameworkUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
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
     * @see org.osgi.jmx.framework.FrameworkMBean#installBundle(java.lang.String)
     */
    public long installBundle(String location) throws IOException {
        try {
            Bundle bundle = context.installBundle(location);
            return bundle.getBundleId();
        } catch (BundleException e) {
            IOException ioex = new IOException("Can't install bundle with location " + location);
            ioex.initCause(e);
            throw ioex;
        }
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#installBundle(java.lang.String, java.lang.String)
     */
    public long installBundle(String location, String url) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = createStream(url);
            Bundle bundle = context.installBundle(location, inputStream);
            return bundle.getBundleId();
        } catch (BundleException e) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ioe) {

                }
            }
            IOException ioex = new IOException("Can't install bundle with location " + location);
            ioex.initCause(e);
            throw ioex;
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
     * @see org.osgi.jmx.framework.FrameworkMBean#installBundles(java.lang.String[], java.lang.String[])
     */
    public CompositeData installBundles(String[] locations, String[] urls) throws IOException {
        if(locations == null || urls == null){
            return new BatchInstallResult("Failed to install bundles arguments can't be null").toCompositeData(); 
        }
        
        if(locations.length != urls.length){
            return new BatchInstallResult("Failed to install bundles size of arguments should be same").toCompositeData(); 
        }
        long[] ids = new long[locations.length];
        for (int i = 0; i < locations.length; i++) {
            try {
                long id = installBundle(locations[i], urls[i]);
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
     * @see org.osgi.jmx.framework.FrameworkMBean#refreshBundles(long[])
     */
    public void refreshBundles(long[] bundleIdentifiers) throws IOException 
    {
       Bundle[] bundles = null;
       if(bundleIdentifiers != null)
       {
          bundles = new Bundle[bundleIdentifiers.length];
          for (int i = 0; i < bundleIdentifiers.length; i++) 
          {
             bundles[i] = context.getBundle(bundleIdentifiers[i]);
          }
       }
       packageAdmin.refreshPackages(bundles);
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
       if(bundleIdentifiers != null)
       {
          bundles = new Bundle[bundleIdentifiers.length];
          for (int i = 0; i < bundleIdentifiers.length; i++) 
          {
             bundles[i] = context.getBundle(bundleIdentifiers[i]);
          }
       }
       return packageAdmin.resolveBundles(bundles);
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#restartFramework()
     */
    public void restartFramework() throws IOException {
        Bundle bundle = context.getBundle(0);
        try {
            bundle.update();
        } catch (BundleException be) {
            IOException ioex = new IOException("Failed to restart framework");
            ioex.initCause(be);
            throw ioex;
        }

    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#setBundleStartLevel(long, int)
     */
    public void setBundleStartLevel(long bundleIdentifier, int newlevel) throws IOException {
        Bundle bundle = FrameworkUtils.resolveBundle(context, bundleIdentifier);
        startLevel.setBundleStartLevel(bundle, newlevel);

    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#setBundleStartLevels(long[], int[])
     */
    public CompositeData setBundleStartLevels(long[] bundleIdentifiers, int[] newlevels) throws IOException {
        if(bundleIdentifiers == null || newlevels == null){
            return new BatchActionResult("Failed to setBundleStartLevels arguments can't be null").toCompositeData(); 
        }
        
        if(bundleIdentifiers != null && newlevels != null && bundleIdentifiers.length != newlevels.length){
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
        startLevel.setStartLevel(newlevel);
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#setInitialBundleStartLevel(int)
     */
    public void setInitialBundleStartLevel(int newlevel) throws IOException {
        startLevel.setInitialBundleStartLevel(newlevel);
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#shutdownFramework()
     */
    public void shutdownFramework() throws IOException {
        Bundle bundle = context.getBundle(0);
        try {
            bundle.stop();
        } catch (BundleException be) {
            IOException ioex = new IOException("Failed to shutdown framework");
            ioex.initCause(be);
            throw ioex;
        }

    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#startBundle(long)
     */
    public void startBundle(long bundleIdentifier) throws IOException {
        Bundle bundle = FrameworkUtils.resolveBundle(context, bundleIdentifier);
        if (bundle != null) {
            try {
                bundle.start();
            } catch (BundleException be) {
                IOException ioex = new IOException("Failed to start bundle with id " + bundleIdentifier);
                ioex.initCause(be);
                throw ioex;
            }
        }
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#startBundles(long[])
     */
    public CompositeData startBundles(long[] bundleIdentifiers) throws IOException {
        if(bundleIdentifiers == null){
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
        if (bundle != null) {
            try {
                bundle.stop();
            } catch (BundleException e) {
                IOException ioex = new IOException("Failed to stop bundle with id " + bundleIdentifier);
                ioex.initCause(e);
                throw ioex;
            }
        }
    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#stopBundles(long[])
     */
    public CompositeData stopBundles(long[] bundleIdentifiers) throws IOException {
        if(bundleIdentifiers == null){
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
        if (bundle != null) {
            try {
                bundle.uninstall();
            } catch (BundleException be) {
                IOException ioex = new IOException("Failed to uninstall bundle with id " + bundleIdentifier);
                ioex.initCause(be);
                throw ioex;
            }
        }

    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#uninstallBundles(long[])
     */
    public CompositeData uninstallBundles(long[] bundleIdentifiers) throws IOException {
        if(bundleIdentifiers == null){
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
        } catch (BundleException be) {
            IOException ioex = new IOException("Failed to update bundle with id " + bundleIdentifier);
            ioex.initCause(be);
            throw ioex;
        }

    }

    /**
     * @see org.osgi.jmx.framework.FrameworkMBean#updateBundle(long, java.lang.String)
     */
    public void updateBundle(long bundleIdentifier, String url) throws IOException {
        Bundle bundle = FrameworkUtils.resolveBundle(context, bundleIdentifier);;
        InputStream inputStream = null;
        try {
            inputStream = createStream(url);
            bundle.update(inputStream);
        } catch (BundleException be) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ioe) {

                }
            }
            IOException ioex = new IOException("Can't update system bundle");
            ioex.initCause(be);
            throw ioex;
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
     * @see org.osgi.jmx.framework.FrameworkMBean#updateBundles(long[], java.lang.String[])
     */
    public CompositeData updateBundles(long[] bundleIdentifiers, String[] urls) throws IOException {
        if(bundleIdentifiers == null || urls == null){
            return new BatchActionResult("Failed to update bundles arguments can't be null").toCompositeData(); 
        }
        
        if(bundleIdentifiers != null && urls != null && bundleIdentifiers.length != urls.length){
            return new BatchActionResult("Failed to update bundles size of arguments should be same").toCompositeData(); 
        }
        for (int i = 0; i < bundleIdentifiers.length; i++) {
            try {
                updateBundle(bundleIdentifiers[i], urls[i]);
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
        } catch (BundleException be) {
            IOException ioex = new IOException("Failed to update system bundle");
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
