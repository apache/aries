/*
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
package org.apache.aries.subsystem.scope.impl;

import java.lang.IllegalArgumentException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.SharePolicy;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

public class ScopeImpl implements Scope {

    private String name;
    private String location;
    private BundleContext context;
    private List<Scope> children = new ArrayList<Scope>();
    private List<Bundle> bundles = new ArrayList<Bundle>();
    private Map<String, List<SharePolicy>> importPolicies = new HashMap<String, List<SharePolicy>>();
    private Map<String, List<SharePolicy>> exportPolicies = new HashMap<String, List<SharePolicy>>();
    private BundleTracker bt;
    private long id;
    private List<String> bundleLocations = new ArrayList<String>();
    
    public ScopeImpl(String name) {
        this.name = name;
        this.id = getId();
    }
    
    public ScopeImpl(String name, String location) {
        this.name = name;
        this.location = location;
        this.id = getId();
    }
    // assume this constructor would be used to construct the root scope
    public ScopeImpl(String name, BundleContext context) {
        this(name);

        this.context = context;
        if (name.equals("root")) {
            bundles.addAll(Arrays.asList(context.getBundles()));
        }

     
        // let's use a bundle tracker to dynamically update the bundle list - need to wait on the resolution of the new rfc 138 bug
        // we cannot really use bundle tracker because the hooks may not work here
        //bt = new BundleTracker(context, Bundle.INSTALLED | Bundle.UNINSTALLED, new ScopeBundleTrackerCustomizer());
        //bt.open();
    }
    
    public void destroy() {
        /*if (bt != null) {
            bt.close();
        }*/
    }
    
    public Collection<Bundle> getBundles() {
        return Collections.unmodifiableCollection(bundles);
    }
    
    protected Collection<String> getToBeInstalledBundleLocation() {
        return bundleLocations;
    }
    
    protected void clearBundleLocations() {
        bundleLocations = new ArrayList<String>();
    }
    
    protected Collection<Bundle> getModifiableBundles() {
        return this.bundles;
    }

    public Collection<Scope> getChildren() {
        return Collections.unmodifiableList(children);
    }
    
    protected Collection<Scope> getModifiableChildren() {
        return this.children;
    }

    public String getName() {
        return this.name;
    }

    public Map<String, List<SharePolicy>> getSharePolicies(String type) {
        if (type.equals(SharePolicy.TYPE_IMPORT)) {
            return Collections.unmodifiableMap(this.importPolicies);
        } else if (type.equals(SharePolicy.TYPE_EXPORT)) {
            return Collections.unmodifiableMap(this.exportPolicies);
        }
        throw new IllegalArgumentException("Valid Types are : " + SharePolicy.TYPE_EXPORT + " & " + 
                SharePolicy.TYPE_IMPORT + " Invalid type: " + type);
        
    }
    
    protected Map<String, List<SharePolicy>> getModifiableSharePolicies(String type) {
        if (type.equals(SharePolicy.TYPE_IMPORT)) {
            return this.importPolicies;
        } else if (type.equals(SharePolicy.TYPE_EXPORT)) {
            return this.exportPolicies;
        }
        throw new IllegalArgumentException("Valid Types are : " + SharePolicy.TYPE_EXPORT + " & " + 
                SharePolicy.TYPE_IMPORT + " Invalid type: " + type);
    }

    private class ScopeBundleTrackerCustomizer implements BundleTrackerCustomizer {

        public Object addingBundle(Bundle bundle, BundleEvent event) {
            if (event.getType() == BundleEvent.INSTALLED) {
                bundles.add(bundle);
            } else if (event.getType() == BundleEvent.UNINSTALLED) {
                bundles.remove(bundle);
            }
            
            return bundle;
        }

        public void modifiedBundle(Bundle bundle, BundleEvent event,
                Object object) {
            if (event.getType() == BundleEvent.INSTALLED) {
                bundles.add(bundle);
            } else if (event.getType() == BundleEvent.UNINSTALLED) {
                bundles.remove(bundle);
            }
            
        }

        public void removedBundle(Bundle bundle, BundleEvent event,
                Object object) {
            if (event.getType() == BundleEvent.INSTALLED) {
                bundles.add(bundle);
            } else if (event.getType() == BundleEvent.UNINSTALLED) {
                bundles.remove(bundle);
            }          
        }
        
    }

    public long getId() {
        if (id == 0) {
            id = IdGenerator.next();
        } 
        
        return id;
    }
    
    private static class IdGenerator {
        static long newId;       
        
        protected static synchronized long next() {
            newId++;
            return newId;
        }
    }

    public String getLocation() {
        return this.location;
    }
    
}
