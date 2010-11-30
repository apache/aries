/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.internal;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.aries.subsystem.SubsystemAdmin;
import org.apache.aries.subsystem.SubsystemConstants;
import org.apache.aries.subsystem.SubsystemException;
import org.apache.aries.subsystem.scope.InstallInfo;
import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.ScopeAdmin;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.apache.aries.subsystem.spi.Resource;
import org.apache.aries.subsystem.spi.ResourceProcessor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class BundleResourceProcessor implements ResourceProcessor {

    public Session createSession(SubsystemAdmin subsystemAdmin) {
        return new BundleSession(subsystemAdmin);
    }

    public static class BundleSession implements Session {

        private final ScopeAdmin scopeAdmin;
        private final List<Bundle> installed = new ArrayList<Bundle>();
        private final Map<Resource, Bundle> updated = new HashMap<Resource, Bundle>();
        private final Map<Resource, Bundle> removed = new HashMap<Resource, Bundle>();
        

        public BundleSession(SubsystemAdmin subsystemAdmin) {
            SubsystemAdminImpl subsystemAdminImpl = (SubsystemAdminImpl)subsystemAdmin;
            this.scopeAdmin = subsystemAdminImpl.getScopeAdmin();
        }

        public void process(Resource resource) throws SubsystemException {
            try {
                // find the bundle
                Bundle bundle = findBundle(resource);
                
                if (bundle == null) {
                    // fresh install 
                    InstallInfo installInfo = new InstallInfo(new URL(resource.getLocation()), resource.getLocation());
                    ScopeUpdate scopeUpdate = scopeAdmin.newScopeUpdate();
                    scopeUpdate.getBundlesToInstall().add(installInfo);
                    scopeUpdate.commit();
                } else {
                    // update only if RESOURCE_UPDATE_ATTRIBUTE is set to true
                    String updateAttribute = resource.getAttributes().get(SubsystemConstants.RESOURCE_UPDATE_ATTRIBUTE);
                    if ("update".equals(updateAttribute)) {
                        bundle.update(resource.open());
                        updated.put(resource, bundle);
                    }
                }
                
                if (bundle == null) {
                    bundle = findBundle(resource);
                    installed.add(bundle);
                }

                String startAttribute = resource.getAttributes().get(SubsystemConstants.RESOURCE_START_ATTRIBUTE);
                
                if (startAttribute == null || startAttribute.length() == 0) {
                    // defaults to true
                    startAttribute = "true";
                }
                if ("true".equals(startAttribute)) {
                    bundle.start();
                }
            } catch (SubsystemException e) {
                throw e;
            } catch (Exception e) {
//                throw new SubsystemException("Unable to process bundle resource", e);
            	e.printStackTrace();
            }
        }

        public void dropped(Resource resource) throws SubsystemException {
            // find the bundle
            Bundle bundle = findBundle(resource);
            
            if (bundle == null) {
                throw new SubsystemException("Unable to find the resource to be dropped");
            } else {
                try {
                    bundle.uninstall();
                    removed.put(resource, bundle);
                } catch (BundleException be) {
                    throw new SubsystemException("Unable to drop resource", be);
                }
            }
        }

        public void prepare() throws SubsystemException {
            // no-op
        }

        public void commit() {
            clearAll();
        }

        public void rollback() {
            // rollback installed bundle
            for (Bundle bundle : installed) {
                try {
                    bundle.uninstall();
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            // rollback updated bundle - not sure what we can do here
            
            // rollback removed bundle
            if (!removed.isEmpty()) {
                Set<Entry<Resource, Bundle>> removedSet = removed.entrySet();
                for (Entry<Resource, Bundle> entry : removedSet) {
                    Bundle bundle = entry.getValue();
                    Resource res = entry.getKey();
                    try {
                        InstallInfo installInfo = new InstallInfo(res.open(), res.getLocation());
                        ScopeUpdate scopeUpdate = scopeAdmin.newScopeUpdate();
                        scopeUpdate.getBundlesToInstall().add(installInfo);
                        scopeUpdate.commit();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
            
            clearAll();
        }
        
        protected Bundle findBundle(Resource resource) {
            Scope scope = scopeAdmin.getScope();
            for (Bundle b : scope.getBundles()) {
                if (resource.getLocation().equals(scope.getLocation())) {
                    return b;
                }
            }
            
            return null;
        }
        
        private void clearAll() {
            installed.clear();
            updated.clear();
            removed.clear();
        }
    }

    public Session createSession(BundleContext arg0) {
        // TODO Auto-generated method stub
        return null;
    }


}
