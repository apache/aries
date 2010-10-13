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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.aries.subsystem.scope.InstallInfo;
import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.ScopeAdmin;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.apache.aries.subsystem.scope.SharePolicy;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

public class ScopeUpdateImpl implements ScopeUpdate {

    private ScopeImpl scope;
    private List<InstallInfo> installInfo = new ArrayList<InstallInfo>();
    private List<ScopeUpdate> children = new ArrayList<ScopeUpdate>();
    private List<Scope> tbrChildren = new ArrayList<Scope>();
    private BundleContext bc;
    private static ConcurrentHashMap<Long, ServiceRegistration> srs = new ConcurrentHashMap<Long, ServiceRegistration>();
    
    public ScopeUpdateImpl(ScopeImpl scope, BundleContext bc) {
        this.scope = scope;
        this.bc = bc;
    }
    
    public ScopeUpdateImpl(ScopeImpl scope, List<InstallInfo> installInfo) {
        this.scope = scope;
        this.installInfo = installInfo;
    }
    
    
    public ScopeUpdateImpl(ScopeImpl scope, List<InstallInfo> installInfo, List<ScopeUpdate> children) {
        this.scope = scope;
        this.installInfo = installInfo;
        this.children = children;
    }
    
    public ScopeImpl getScope() {
        return this.scope;
    }
    
    public boolean commit() throws BundleException {
        // process installedBundle
        boolean success = false;
        List<Bundle> installedBundle = new ArrayList<Bundle>();
        for (InstallInfo info : this.installInfo) {
            URL url = info.getContent();
            String loc = info.getLocation();
            Bundle b;
            Bundle oldB = alreadyInstalled(info);
            
            // in case of modify, uninstall the previous bundle first.
            if (oldB != null) {
                oldB.uninstall();
                getBundles().remove(oldB);
            }
            
            try {
                scope.getToBeInstalledBundleLocation().add(loc);
                b = bc.installBundle(loc, url.openStream());
                installedBundle.add(b);
            } catch (IOException e) {
                // clear bundle location off the list.
                scope.getToBeInstalledBundleLocation().remove(loc);
                throw new BundleException("problem when opening url " + e.getCause());
            }
            scope.getToBeInstalledBundleLocation().remove(loc);
        }
        
        // clear bundle location list since all install is finished.
        scope.clearBundleLocations();
        
        // update bundle list for the scope
        getBundles().addAll(installedBundle);

        
        // process child scopes
        Collection<ScopeUpdate> children = getChildren();
        for (ScopeUpdate child : children) {
            
            ScopeUpdateImpl scopeUpdateImpl = (ScopeUpdateImpl)child;
            ServiceRegistration sr = null;
            try {
                // also create a new scopeAdmin as scopeadmin and scope is 1-1 relationship
                ScopeAdminImpl newScopeAdmin = new ScopeAdminImpl(this.scope, scopeUpdateImpl.getScope());
                
                
                sr = this.bc.registerService(ScopeAdmin.class.getName(), 
                        newScopeAdmin, 
                        DictionaryBuilder.build("ScopeName", child.getName(), "ScopeId", scopeUpdateImpl.getScope().getId()));
                srs.put(scopeUpdateImpl.getScope().getId(), sr);
                child.commit();
            } catch (BundleException e) {
                if (sr != null) {
                    sr.unregister();
                    srs.remove(scopeUpdateImpl.getScope().getId());
                }
                throw new BundleException("problem when commiting child scope: " + child.getName() + " " + e.getCause());
            }
            

            // update current scope to specify the children.
            getExistingChildren().add(scopeUpdateImpl.getScope());
            

        }
        // remove any scopes in to be removed children list
        for (Scope scope : tbrChildren) {
            removeChildScope(scope); 
        }
        
        
        return true;
    }
    
    // check if the install info is already installed in the scope
    private Bundle alreadyInstalled(InstallInfo info) {
        String loc = info.getLocation();
        
        Collection<Bundle> bundles = scope.getBundles();
        
        for (Bundle b : bundles) {
            if (b.getLocation().equals(loc)) {
                return b;
            }
        }
        
        return null;
    }

    public Collection<Bundle> getBundles() {
        return scope.getModifiableBundles();
    }

    public List<InstallInfo> getBundlesToInstall() {
        return this.installInfo;
    }
    
    /*public List<InstallInfo> getBundlesToDelete() {
        return this.installInfo;
    }
    
    public List<InstallInfo> getBundlesToModify() {
        return this.installInfo;
    }*/

    public Collection<ScopeUpdate> getChildren() {
        return this.children;
    }
    
    public Collection<Scope> getExistingChildren() {
        return scope.getModifiableChildren();
    }

    public Collection<Scope> getToBeRemovedChildren() {
        return this.tbrChildren;
    }
    
    // this would remove the child off the scope and uninstall the scope.
    private void removeChildScope(Scope sc) {
        removeChildScope(sc.getId());
    }
    // this would remove the child off the scope and uninstall the scope.
    private void removeChildScope(long id) {
        Collection<Scope> scopes =  scope.getModifiableChildren();
        for (Scope scope : scopes) {
            if (scope.getId() == id) {
                for (Bundle b : scope.getBundles()) {
                    try {
                        b.uninstall();
                    } catch (BundleException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                scopes.remove(scope);
                // unregister the associated ScopeAdmin in service registry
                ServiceRegistration sr = srs.get(id);
                if (sr != null) {
                    sr.unregister();
                    srs.remove(id);
                } else {
                    throw new NullPointerException ("Unable to find the ScopeAdmin service Registration in the map");
                }
                return;
            }
        }
    }
    
    public String getName() {
        return scope.getName();
    }

    public Map<String, List<SharePolicy>> getSharePolicies(String type) {
        return scope.getModifiableSharePolicies(type);
    }

    public ScopeUpdate newChild(String name) {
        ScopeImpl newScope = new ScopeImpl(name);

        // create scope update
        ScopeUpdate scopeUpdate = new ScopeUpdateImpl(newScope, this.bc);
        this.children.add(scopeUpdate);
        return scopeUpdate;
    }

    public ScopeUpdate newChild(String name, String location) {
        ScopeImpl newScope = new ScopeImpl(name, location);

        // create scope update
        ScopeUpdate scopeUpdate = new ScopeUpdateImpl(newScope, this.bc);
        this.children.add(scopeUpdate);
        return scopeUpdate;
    }
}
