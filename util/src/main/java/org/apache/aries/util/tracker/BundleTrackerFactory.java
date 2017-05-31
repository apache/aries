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
package org.apache.aries.util.tracker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Version;
import org.osgi.util.tracker.BundleTracker;

/**
 * This is the factory for BundleTracker
 */
public class BundleTrackerFactory {
    private static ConcurrentHashMap<String, List<BundleTracker>> btMap = new ConcurrentHashMap<String, List<BundleTracker>>();

    /**
     * get bundle tracker based on bundle name and version
     * 
     * @param bundleScope
     *            composite bundle's - SymbolicName_Version
     * @return the list of bundle tracker associated with the bundle scope
     */
    public static List<BundleTracker> getBundleTrackerList(String bundleScope) {
        return (List<BundleTracker>) btMap.get(bundleScope);
    }

    /**
     * get bundle tracker based on composite bundle's symbolicName and version
     * 
     * @param symbolicName
     *            composite bundle's symbolicName
     * @param version
     *            composite bundle's version
     * @return the list of bundle tracker associated with the bundle scope
     */
    public static List<BundleTracker> getBundleTrackerList(String symbolicName,
            Version version) {
        return (List<BundleTracker>) btMap.get(symbolicName + "_"
                + version.toString());
    }

    /**
     * get all bundle tracker registered in this factory
     * 
     * @return all the trackers registered. The collection contains a list of BundleTracker for each bundle scope.
     */
    public static Collection<List<BundleTracker>> getAllBundleTracker() {
        return btMap.values();
    }

    /**
     * register the bundle tracker
     * 
     * @param bundleScope
     *            composite bundle's SymbolicName_Version
     * @param bt
     *            the bundle tracker to be registered
     */
    public static void registerBundleTracker(String bundleScope,
            BundleTracker bt) {
        List<BundleTracker> list = btMap.get(bundleScope);
        if (list == null) {
            list = new ArrayList<BundleTracker>();
        }
        list.add(bt);
        btMap.putIfAbsent(bundleScope, list);
    }

    /**
     * unregister and close the bundle tracker(s) associated with composite
     * bundle's - SymbolicName_Version
     * 
     * @param bundleScope
     *            composite bundle's - SymbolicName_Version
     */
    public static void unregisterAndCloseBundleTracker(String bundleScope) {
        List<BundleTracker> list = btMap.get(bundleScope);
        if (list == null) {
            return;
        } else {
            for (BundleTracker bt : list) {
                bt.close();
            }
        }
        btMap.remove(bundleScope);
    }
}