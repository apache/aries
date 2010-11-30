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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.ScopeAdmin;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.apache.aries.subsystem.scope.SharePolicy;
import org.apache.aries.subsystem.scope.internal.Activator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.bundle.EventHook;
import org.osgi.framework.hooks.bundle.FindHook;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.Capability;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

// the idea is one scopeAdmin per scope. for bundles in the same scope, same
// ScopeAdmin would be returned
public class ScopeAdminServiceFactory implements ServiceFactory {

    // mapping of scope and ScopeAdminImpl
    private final List<ScopeAdmin> admins = new ArrayList<ScopeAdmin>();
    private final Map<ScopeAdmin, Long> references = new HashMap<ScopeAdmin, Long>();
    protected static ScopeAdminImpl defaultScopeAdmin;
    private static BundleContext context;
    private ServiceTracker serviceTracker;
    private List<ServiceRegistration> srs = new ArrayList<ServiceRegistration>();
    public static final String SERVICE_CAPABILITY = "osgi.service";
    private ServiceRegistration rootScopeAdminserviceReg;
    
    public void init() throws InvalidSyntaxException {
        context = Activator.getBundleContext();
        Filter filter = FrameworkUtil.createFilter("(&("
                + Constants.OBJECTCLASS + "=" + ScopeAdmin.class.getName() + "))");
        serviceTracker = new ServiceTracker(context, filter,
                new ServiceTrackerCustomizer() {

                    public Object addingService(ServiceReference reference) {
                        // adding new service, update admins map
                        ScopeAdmin sa = (ScopeAdmin) context
                                .getService(reference);
                        admins.add(sa);

                        return sa;
                    }

                    public void modifiedService(ServiceReference reference,
                            Object service) {
                        // TODO Auto-generated method stub

                    }

                    public void removedService(ServiceReference reference,
                            Object service) {
                        ScopeAdmin sa = (ScopeAdmin) service;
                        admins.remove(sa);
                    }

                });
        defaultScopeAdmin = new ScopeAdminImpl(null, new ScopeImpl("root",
                context));
        rootScopeAdminserviceReg = context.registerService(ScopeAdmin.class.getName(), 
                defaultScopeAdmin, 
                DictionaryBuilder.build("ScopeName", defaultScopeAdmin.getScope().getName(), "ScopeId", defaultScopeAdmin.getScope().getId()));
        admins.add(defaultScopeAdmin);
        references.put(defaultScopeAdmin, new Long(0));
        serviceTracker.open();
        
        ScopeAdminBundleHooks bundleHooks = new ScopeAdminBundleHooks();
        srs.add(context.registerService(new String[]{FindHook.class.getName(), EventHook.class.getName(), ResolverHook.class.getName()}, bundleHooks, null));
        ScopeAdminEventHooks eventHooks = new ScopeAdminEventHooks();
        srs.add(context.registerService(new String[]{org.osgi.framework.hooks.service.FindHook.class.getName(), org.osgi.framework.hooks.service.EventHook.class.getName()}, eventHooks, null));

        
    }

    public void destroy() {
        if (serviceTracker != null) {
            serviceTracker.close();
        }
        
        for (ServiceRegistration sr : srs) {
            sr.unregister();
        }
        
        if (rootScopeAdminserviceReg != null) {
            rootScopeAdminserviceReg.unregister();
        }
    }

    public synchronized Object getService(Bundle bundle, ServiceRegistration registration) {
        // determine bundle in which scope first, then determine the
        // scopeadmin to return
        ScopeAdmin sa = getScopeAdmin(bundle);
        long ref = 0;

        // unable to find scope admin in our admins map
        if (sa == null) {
            // unable to find scope admin, assuming it is in the root scope.
            sa = defaultScopeAdmin;
            
        } else {
            ref = references.get(sa);
        }

        references.put(sa, ref + 1);
        return sa;
    }

    public synchronized void ungetService(Bundle bundle,
            ServiceRegistration registration, Object service) {
        ScopeAdminImpl admin = (ScopeAdminImpl) service;
        long ref = references.get(admin) - 1;
        if (ref == 0) {
            // admin.dispose();
            admins.remove(admin);
            references.remove(admin);
        } else {
            references.put(admin, ref);
        }
    }
    
    // assume one bundle only belongs to one subsystem at most
    private ScopeAdmin getScopeAdmin(Bundle bundle) {
        
        // add pax-exam-probe to default scope for now before we could figure out bundles in which scope via some sorta of installhook (bug 1747 in OSGi Aliance bug system)
        if (bundle.getSymbolicName().equals("pax-exam-probe") || bundle.getSymbolicName().indexOf("fileinstall") > -1) {
            ScopeUpdate scopeUpdate = ScopeAdminServiceFactory.defaultScopeAdmin.newScopeUpdate();
            scopeUpdate.getBundles().add(bundle);
        }
        
        // add hard coded value below due to bug 1747
        if (bundle.getSymbolicName().indexOf("helloIsolation") > 0 && bundle.getVersion().equals(new Version("1.0.0"))) {
            ScopeUpdate scopeUpdate = ScopeAdminServiceFactory.defaultScopeAdmin.newScopeUpdate();
            scopeUpdate.getBundles().add(bundle);
        }
        
        for (ScopeAdmin admin : admins) {
            ScopeImpl scope = (ScopeImpl)admin.getScope();
            
            // it is possible the scope is in the tobeinstalled bundle location list, and hasn't fully installed yet.
            Collection<String> bundlesLocations = scope.getToBeInstalledBundleLocation();
            for (String loc : bundlesLocations) {
                if (bundle.getLocation().equals(loc)) {
                    return admin;
                }
            }
            Collection<Bundle> bundles = scope.getBundles();
            
            for (Bundle b : bundles) {
                if (b == bundle) {
                    return admin;
                }
            }
            

        }

        // it is possible there is no scopeAdmin for the bundle as the bundle has not been added to the scope yet.
        return null;
    }


    private class ScopeAdminBundleHooks implements FindHook, EventHook, ResolverHook {

        public void find(BundleContext context, Collection<Bundle> bundles) {
            Bundle b = context.getBundle();
            // obtain bundle associated subsystem
            ScopeAdmin scopeAdmin = getScopeAdmin(b);
            
            if (scopeAdmin != null) {
                // able to obtain the correct scope otherwise, we don't need to do anything
                Collection<Bundle> buns = scopeAdmin.getScope().getBundles();
                trimBundleCollections(bundles, buns);
            } else {
                // should this be an error.  a bundle would have to be in a scope.
            }
            
            // check the scope policy
            // are we allow any package to be exported out of a scope?   yes  if yes, do we want to have bundle that exports the package visible?
            // are we allow any service to be exported out of a scope?   yes
            // go through bundles and remove the bundles that are not part of the scope
        }

        public void event(BundleEvent event, Collection<BundleContext> contexts) {
            Bundle bundle = event.getBundle();
            
            // obtain bundle associated scopeAdmin
            ScopeAdmin scopeAdmin = getScopeAdmin(bundle);
            
            if (scopeAdmin != null) {
                // able to obtain the correct scope otherwise, we don't need to do anything
                Collection<Bundle> buns = scopeAdmin.getScope().getBundles();
                ScopeImpl scopeImpl = (ScopeImpl)scopeAdmin.getScope();
                trimBundleContextCollections(contexts, buns, scopeImpl.getToBeInstalledBundleLocation());
            } 
            
            // figure out where contexts reside in which scope and trim them as needed.
            // should be easy as events should only been seen inside a scope
            
        }

        public void begin() {
            // TODO Auto-generated method stub
            
        }

        public void end() {
            // TODO Auto-generated method stub
            
        }

        public void filterMatches(BundleRevision requirer, Collection<Capability> candidates) {
            // obtain requirer bundle
            Bundle bundle = requirer.getBundle();
            
            // figure out if the requirer bundle in any scope
            // obtain bundle associated scopeAdmin
            ScopeAdmin scopeAdmin = getScopeAdmin(bundle);
            
            if (scopeAdmin != null) {
                // able to obtain the correct scope otherwise, we don't need to do anything
                Collection<Bundle> buns = scopeAdmin.getScope().getBundles();
                
                Collection<Scope> childrenScopes = scopeAdmin.getScope().getChildren();
                List<SharePolicy> exportPackagePolicies = new ArrayList<SharePolicy>();
                for (Scope childScope : childrenScopes) {
                    Map<String, List<SharePolicy>> exportPolicies = childScope.getSharePolicies(SharePolicy.TYPE_EXPORT);
                    if (exportPolicies.get(Capability.PACKAGE_CAPABILITY) != null) {
                        exportPackagePolicies.addAll(exportPolicies.get(Capability.PACKAGE_CAPABILITY));
                    }
                }
                
                List<SharePolicy> importPackagePolicies = new ArrayList<SharePolicy>();
                Map<String, List<SharePolicy>> importPolicies = scopeAdmin.getScope().getSharePolicies(SharePolicy.TYPE_IMPORT);
                if (importPolicies.get(Capability.PACKAGE_CAPABILITY) != null) {
                    importPackagePolicies.addAll(importPolicies.get(Capability.PACKAGE_CAPABILITY));
                }

                trimCapabilityCollections(candidates, buns, exportPackagePolicies, importPackagePolicies);
            }
            // simple filter candidates
          
        }

        public void filterResolvable(Collection candidates) {
            // TODO Auto-generated method stub
            
        }

        public void filterSingletonCollisions(Capability singleton, Collection collisionCandidates) {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    // based on event hooks
    private class ScopeAdminEventHooks implements org.osgi.framework.hooks.service.FindHook, org.osgi.framework.hooks.service.EventHook {

        // modifies getServiceReference
        public void find(BundleContext context, String name, String filter,
                boolean allServices, Collection references) {
            Bundle bundle = context.getBundle();
            
            // figure out if the bundle in any scope
            // obtain bundle associated scopeAdmin
            ScopeAdmin scopeAdmin = getScopeAdmin(bundle);
            
            if (scopeAdmin != null) {
                // able to obtain the correct scope otherwise, we don't need to do anything
                Collection<Bundle> buns = scopeAdmin.getScope().getBundles();
                
                Collection<Scope> childrenScopes = scopeAdmin.getScope().getChildren();
                List<SharePolicy> exportServicePolicies = new ArrayList<SharePolicy>();
                for (Scope childScope : childrenScopes) {
                    Map<String, List<SharePolicy>> exportPolicies = childScope.getSharePolicies(SharePolicy.TYPE_EXPORT);
                    if (exportPolicies.get(SERVICE_CAPABILITY) != null) {
                        exportServicePolicies.addAll(exportPolicies.get(SERVICE_CAPABILITY));
                    }
                }
                
                List<SharePolicy> importServicePolicies = new ArrayList<SharePolicy>();
                Map<String, List<SharePolicy>> importPolicies = scopeAdmin.getScope().getSharePolicies(SharePolicy.TYPE_IMPORT);
                if (importPolicies.get(SERVICE_CAPABILITY) != null) {
                    importServicePolicies.addAll(importPolicies.get(SERVICE_CAPABILITY));
                }
                trimServiceReferenceCollections(references, buns, exportServicePolicies, importServicePolicies);       
            }
            
            
        }

        // modifies service events - assume events are only seen in the scope.
        public void event(ServiceEvent event, Collection contexts) {
            Bundle bundle = event.getServiceReference().getBundle();
            
            // obtain bundle associated scopeAdmin
            ScopeAdmin scopeAdmin = getScopeAdmin(bundle);
            
            if (scopeAdmin != null) {
                // able to obtain the correct scope otherwise, we don't need to do anything
                Collection<Bundle> buns = scopeAdmin.getScope().getBundles();
                trimBundleContextCollections(contexts, buns, null);
            }
            
        }

        
    }
    
    // trim the references candidate to only the ones in scopeBundles
    private synchronized void trimServiceReferenceCollections(
            Collection<ServiceReference> references,
            Collection<Bundle> scopeBundles, 
            List<SharePolicy> exportServicePolicies, 
            List<SharePolicy> importServicePolicies) {
        Collection<ServiceReference> toBeRemoved = new ArrayList<ServiceReference>();
        for (ServiceReference reference : references) {
            Bundle b = reference.getBundle();
            boolean exist = false;

            for (Bundle bundle : scopeBundles) {
                if (b == bundle) {
                    exist = true;
                    break;
                }
            }

            if (!exist) {
                // double check toBeRemoved.  If it exists in toBeRemoved but are part of exportServicePolicies of the child scope
                // then we still allow the services to be used
                // if it exists in toBeRemoved but are part of importServicePolicies of the scope the bundle is in, we still allow
                // the services to be used.
                boolean matchPolicy = false;
                // go through export package policies
                for (SharePolicy sp : exportServicePolicies) {         
                    if (sp.getFilter().match(reference)) {
                        matchPolicy = true;
                        continue;
                    }
                    
                }
                
                if (!matchPolicy) {
                    // go through import package policies
                    for (SharePolicy sp : importServicePolicies) {
                        if (sp.getFilter().match(reference)) {
                            matchPolicy = true;
                            continue;
                        }
                        
                    }
                    if (!matchPolicy) {
                        toBeRemoved.add(reference);
                    }
                }
            }
        }

        if (!toBeRemoved.isEmpty()) {
            references.removeAll(toBeRemoved);
        }
    }
    
    // trim the bundles candidate to only the ones in subsystemBundles
    private synchronized void trimBundleCollections(Collection<Bundle> bundles, Collection<Bundle> scopeBundles) {
        Collection<Bundle> toBeRemoved = new ArrayList<Bundle>();
        for (Bundle b : bundles) {
            boolean exist = false;
            
            for (Bundle bundle : scopeBundles) {
                if (b == bundle) {
                    exist = true;
                    break;
                }
            }
            
            if (!exist) {
                toBeRemoved.add(b);
            }
        }

        if (!toBeRemoved.isEmpty()) {
            bundles.removeAll(toBeRemoved);
        }
    }
    
    // trim the bundles candidate to only the ones in scopeBundles
    private synchronized void trimBundleContextCollections(Collection<BundleContext> bundleContexts, 
            Collection<Bundle> scopeBundles, Collection<String> toBeInstalledBundleLocations) {
        Collection<BundleContext> toBeRemoved = new ArrayList<BundleContext>();

        for (BundleContext bc : bundleContexts) {
            boolean exist = false;
            
            for (Bundle bundle : scopeBundles) {
                if (bc.getBundle() == bundle || existsInList(bc.getBundle().getLocation(), toBeInstalledBundleLocations)) {
                    exist = true;
                    break;
                }
            }
            
            if (!exist) {
                toBeRemoved.add(bc);
            } 
            
        }
        
        if (!toBeRemoved.isEmpty()) {
            bundleContexts.removeAll(toBeRemoved);
        }

    }

    private boolean existsInList(String bundleLoc, Collection<String> toBeInstalledBundleLocations) {
        if (toBeInstalledBundleLocations == null) {
            return false;
        }
        
        for (String loc : toBeInstalledBundleLocations) {
            if (bundleLoc.equals(loc)) {
                return true;
            }
        }
        return false;
    }
    // trim the bundles candidate to only the ones in subsystemBundles
    private synchronized void trimCapabilityCollections(Collection<Capability> capabilities, 
            Collection<Bundle> scopeBundles, 
            List<SharePolicy> exportPackagePolicies, 
            List<SharePolicy> importPackagePolicies) {
        Collection<Capability> toBeRemoved = new ArrayList<Capability>();

        for (Capability cap : capabilities) {
            Bundle b = cap.getProviderRevision().getBundle();

            if (b.getBundleId() == 0) {
                continue;
            }
            boolean exist = false;
            
            for (Bundle bundle : scopeBundles) {
                if (b == bundle) {
                    exist = true;
                    break;
                }
            }
            
            if (!exist) {
                // double check toBeRemoved.  If it exists in toBeRemoved but are part of exportPackagePolicies of the child scope
                // then we still allow the capabilities to be used
                // if it exists in toBeRemoved but are part of importPackagePolicies of the scope the bundle is in, we still allow
                // the capabilities to be used.
                boolean matchPolicy = false;
                // go through export package policies
                for (SharePolicy sp : exportPackagePolicies) {
                    if (sp.getFilter().match(new UnmodifiableDictionary(cap.getAttributes()))) {
                        matchPolicy = true;
                        break;
                    }
                    
                }
                
                if (!matchPolicy) {
                    // go through import package policies
                    for (SharePolicy sp : importPackagePolicies) {
                        // append the scope to the capability to do scope affinity
                        Map<String, Object> capabilityAttributes = new HashMap<String, Object>();
                        capabilityAttributes.putAll(cap.getAttributes());
                        Scope scope = getScopeAdmin(b).getScope();
                        capabilityAttributes.put("scopeName", scope.getName());
                        
                        if (sp.getFilter().match(new UnmodifiableDictionary(capabilityAttributes))) {
                            matchPolicy = true;
                            break;
                        }
                        
                    }
                    if (!matchPolicy) {
                        toBeRemoved.add(cap);
                    }
                }
            }
        }
         
        if (!toBeRemoved.isEmpty()) {
            capabilities.removeAll(toBeRemoved);
        }
    }
    
    static class UnmodifiableDictionary extends Dictionary {
        private final Map   wrapped;
        UnmodifiableDictionary(Map wrapped) {
            this.wrapped = wrapped;
        }
        public Enumeration elements() {
            return Collections.enumeration(wrapped.values());
        }
        public Object get(Object key) {
            return wrapped.get(key);
        }
        public boolean isEmpty() {
            return wrapped.isEmpty();
        }
        public Enumeration keys() {
            return Collections.enumeration(wrapped.keySet());
        }
        public Object put(Object key, Object value) {
            throw new UnsupportedOperationException();
        }
        public Object remove(Object key) {
            throw new UnsupportedOperationException();
        }
        public int size() {
            return wrapped.size();
        }
    }
}

