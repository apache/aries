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

import static org.apache.aries.subsystem.SubsystemConstants.SUBSYSTEM_CONTENT;
import static org.apache.aries.subsystem.SubsystemConstants.SUBSYSTEM_DIRECTIVE;
import static org.apache.aries.subsystem.SubsystemConstants.SUBSYSTEM_EXPORTPACKAGE;
import static org.apache.aries.subsystem.SubsystemConstants.SUBSYSTEM_IMPORTPACKAGE;
import static org.apache.aries.subsystem.SubsystemConstants.SUBSYSTEM_MANIFESTVERSION;
import static org.apache.aries.subsystem.SubsystemConstants.SUBSYSTEM_RESOURCES;
import static org.apache.aries.subsystem.SubsystemConstants.SUBSYSTEM_SYMBOLICNAME;
import static org.apache.aries.subsystem.SubsystemConstants.SUBSYSTEM_VERSION;
import static org.apache.aries.subsystem.core.internal.FileUtils.closeQuietly;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
import static org.osgi.framework.Constants.BUNDLE_VERSION;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.apache.aries.subsystem.Subsystem;
import org.apache.aries.subsystem.SubsystemAdmin;
import org.apache.aries.subsystem.SubsystemConstants;
import org.apache.aries.subsystem.SubsystemException;
import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.apache.aries.subsystem.scope.SharePolicy;
import org.apache.aries.subsystem.spi.Resource;
import org.apache.aries.subsystem.spi.ResourceProcessor;
import org.apache.aries.subsystem.spi.ResourceResolver;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.Capability;
import org.osgi.util.tracker.ServiceTracker;

public class SubsystemResourceProcessor implements ResourceProcessor {

    private static final Version SUBSYSTEM_MANIFEST_VERSION = new Version("1.0");

    public SubsystemSession createSession(SubsystemAdmin subsystemAdmin) {
        return new SubsystemSession(subsystemAdmin);
    }

    public static class SubsystemSession implements Session {

        private static final long TIMEOUT = 30000;
        private final SubsystemAdmin subsystemAdmin;

        private final Map<Resource, Subsystem> installed = new HashMap<Resource, Subsystem>();
        /*
         * Map to keep track of composite bundle headers before the update and
         * the updated composite bundle. This is needed for rollback
         */
        private final Map<Map<String, String>, Subsystem> updated = new HashMap<Map<String, String>, Subsystem>();
        private final Map<Resource, Subsystem> removed = new HashMap<Resource, Subsystem>();
        private final List<Subsystem> stopped = new ArrayList<Subsystem>();
        private final Map<String, ServiceTracker> trackers = new HashMap<String, ServiceTracker>();
        private final Map<SubsystemAdmin, Map<String, Session>> sessions = new HashMap<SubsystemAdmin, Map<String, Session>>();
        private final BundleContext context;
        
        public SubsystemSession(SubsystemAdmin subsystemAdmin) {
            this.subsystemAdmin = subsystemAdmin;
            this.context = Activator.getBundleContext();
        }

        /**
         * internal used in this class only. When a manifest is passed in, we
         * won't attempt to read the manifest from the resource
         * 
         * @param res
         * @param manifest
         */
        private void process(Resource res, Manifest manifest) {

            try {
                SubsystemAdminImpl adminImpl = (SubsystemAdminImpl)subsystemAdmin;
                Scope admin = adminImpl.getScopeAdmin();
                ResourceResolver resolver = getService(ResourceResolver.class);

                if (manifest == null) {
                    // Unpack the subsystem archive into a temporary directory
                    File dir = File.createTempFile("subsystem", "", null);
                    if (dir == null || !dir.delete() || !dir.mkdir()) {
                        throw new Exception("Unable to create temporary dir");
                    }
                    FileUtils.unpackArchive(res.open(), dir);

                    manifest = new Manifest();
                    InputStream mis = new FileInputStream(new File(dir,
                            JarFile.MANIFEST_NAME));
                    try {
                        manifest.read(mis);
                    } finally {
                        closeQuietly(mis);
                    }

                }

                List<Resource> resource = new ArrayList<Resource>();
                String resourceHeader = manifest.getMainAttributes().getValue(
                        SUBSYSTEM_RESOURCES);
                Clause[] resourceClauses = Parser.parseHeader(resourceHeader);
                for (Clause c : resourceClauses) {
                    Resource r = resolver.find(c.toString());
                    resource.add(r);
                }

                List<Resource> content = new ArrayList<Resource>();
                String contentHeader = manifest.getMainAttributes().getValue(
                        SUBSYSTEM_CONTENT);
                Clause[] contentClauses = Parser.parseHeader(contentHeader);
                for (Clause c : contentClauses) {
                    Resource r = resolver.find(c.toString());
                    content.add(r);
                }

                List<Resource> previous = new ArrayList<Resource>();

                // TODO: convert resources before calling the resolver?

                List<Resource> additional = resolver.resolve(content, resource);

                // check manifest header to see if they are valid
                String ssn = manifest.getMainAttributes().getValue(
                        SUBSYSTEM_SYMBOLICNAME);
                String sv = manifest.getMainAttributes().getValue(
                        SUBSYSTEM_VERSION);
                checkManifestHeaders(manifest, ssn, sv);

                Map<String, String> headers = computeSubsystemHeaders(manifest,
                        ssn, sv);

                // Check existing bundles
                Subsystem subsystem = findSubsystem(res);
                ScopeUpdate scopeUpdate = admin.newScopeUpdate();
                ScopeUpdate childScopeUpdate;
                if (subsystem == null) {
                    // brand new install

                    childScopeUpdate = scopeUpdate.newChild(headers.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + headers.get(Constants.BUNDLE_VERSION), res.getLocation());
                    Map<String, List<SharePolicy>> exportSharePolicies = childScopeUpdate.getSharePolicies(SharePolicy.TYPE_EXPORT);
                    Map<String, List<SharePolicy>> importSharePolicies = childScopeUpdate.getSharePolicies(SharePolicy.TYPE_IMPORT);
                    
                    setupSharePolicies(exportSharePolicies, importSharePolicies, headers);
                    scopeUpdate.commit();
                    
      //              ScopeAdmin childScopeAdmin = getService(ScopeAdmin.class, "ScopeId=" + childScopeUpdate.getScope().getId());
      
                    Scope childScopeAdmin = childScopeUpdate.getScope();
                    
                    subsystem = new SubsystemImpl(childScopeUpdate.getScope(), headers);
                    SubsystemAdmin childSubsystemAdmin = new SubsystemAdminImpl(childScopeAdmin, subsystem, subsystemAdmin.getSubsystem());
                    context.registerService(SubsystemAdmin.class.getName(), childSubsystemAdmin, DictionaryBuilder.build("SubsystemParentId", childSubsystemAdmin.getParentSubsystem().getSubsystemId(), "SubsystemId", subsystem.getSubsystemId()));
                    
                    installed.put(res, subsystem);
                    
                    
                } else {
                    // update
                    // capture data before update
                    Map<String, String> subsystemHeaders = subsystem.getHeaders();
                    String previousContentHeader = (String) subsystemHeaders.get(SUBSYSTEM_CONTENT);
                    Clause[] previousContentClauses = Parser
                            .parseHeader(previousContentHeader);
                    for (Clause c : previousContentClauses) {
                        Resource r = resolver.find(c.toString());
                        previous.add(r);
                    }
                    
                    subsystem.updateHeaders(headers);
                    updated.put(subsystemHeaders, subsystem);
                }

                // content is installed in the scope, so need to find the subsystemAdmin for the scope first
                long scopeId = subsystem.getSubsystemId();
                SubsystemAdmin childAdmin = getService(SubsystemAdmin.class, "SubsystemId=" + scopeId);
                
                for (Resource r : previous) {
                    boolean stillHere = false;
                    for (Resource r2 : content) {
                        if (r2.getSymbolicName().equals(r.getSymbolicName())
                                && r2.getVersion().equals(r.getVersion())) {
                            stillHere = true;
                            break;
                        }
                    }
                    if (!stillHere) {
                        getSession(childAdmin,
                                r.getType()).dropped(r);
                    }
                }
                
                // additional resource is installed outside of the subsystem.
                for (Resource r : additional) {
                    getSession(subsystemAdmin, r.getType()).process(r);
                }
                

                for (Resource r : content) {
                    getSession(childAdmin, r.getType())
                            .process(r);
                }

            } catch (SubsystemException e) {
                throw e;
            } catch (Exception e) {
                throw new SubsystemException("Unable to process subsystem", e);
            }
        }

        private void setupSharePolicies(
                Map<String, List<SharePolicy>> exportSharePolicies,
                Map<String, List<SharePolicy>> importSharePolicies,
                Map<String, String> headers) {
            String importPackage = headers.get(SubsystemConstants.SUBSYSTEM_IMPORTPACKAGE);
            String exportPackage = headers.get(SubsystemConstants.SUBSYSTEM_EXPORTPACKAGE);
            String importService = headers.get(SubsystemConstants.SUBSYSTEM_IMPORTSERVICE);
            String exportService = headers.get(SubsystemConstants.SUBSYSTEM_EXPORTSERVICE);
            if (importPackage != null && !importPackage.trim().isEmpty()) {
                List<SharePolicy> importPackagePolicies = importSharePolicies.get(Capability.PACKAGE_CAPABILITY);
                if (importPackagePolicies == null) {
                    importPackagePolicies = new ArrayList<SharePolicy>();
                    importSharePolicies.put(Capability.PACKAGE_CAPABILITY,importPackagePolicies);
                }
                
                importPackagePolicies.add(new SharePolicy(SharePolicy.TYPE_IMPORT, Capability.PACKAGE_CAPABILITY, createFilter(importPackage, Capability.PACKAGE_CAPABILITY)));
            }
            
            if (importService != null && !importService.trim().isEmpty()) {
                List<SharePolicy> importServicePolicies = importSharePolicies.get("osgi.service");
                if (importServicePolicies == null) {
                    importServicePolicies = new ArrayList<SharePolicy>();
                    importSharePolicies.put("osgi.service",importServicePolicies);
                }
                
                importServicePolicies.add(new SharePolicy(SharePolicy.TYPE_IMPORT, "osgi.service", createFilter(importService, "osgi.service")));
            }
            
        }
        
        private Filter createFilter(String packages, String namespace) {
            if (namespace.equals(Capability.PACKAGE_CAPABILITY)) {
                // split packages
                List<String> pkgs = ManifestHeaderProcessor.split(packages, ",");
                StringBuffer sb = new StringBuffer();
                sb.append("(|");
                for (String pkg : pkgs) {
                    sb.append("(" + Capability.PACKAGE_CAPABILITY + "=" + pkg + ")");
                }
                sb.append(")");
                try {
                    return FrameworkUtil.createFilter(sb.toString());
                } catch (InvalidSyntaxException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
            }
            if (namespace.equals("osgi.service")) {
                // split packages
                List<String> pkgs = ManifestHeaderProcessor.split(packages, ",");
                StringBuffer sb = new StringBuffer();
                sb.append("(|");
                for (String pkg : pkgs) {
                    sb.append("(osgi.service=" + pkg + ")");
                }
                sb.append(")");
                try {
                    return FrameworkUtil.createFilter(sb.toString());
                } catch (InvalidSyntaxException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
            
            return null;
        }

        public void process(Resource res) throws SubsystemException {
            process(res, null);

        }

        public void dropped(Resource res) throws SubsystemException {
            Subsystem subsystem = findSubsystem(res);
            if (subsystem == null) {
                throw new SubsystemException(
                        "Unable to find matching subsystem to uninstall");
            }
            // TODO: iterate through all resources and ask for a removal on
            // each one
            subsystemAdmin.uninstall(subsystem);
            removed.put(res, subsystem);

        }

        protected Subsystem findSubsystem(Resource resource) {
            for (Subsystem subsystem : this.subsystemAdmin.getSubsystems()) {
                if (resource.getLocation().equals(subsystem.getLocation())) {
                    return subsystem;
                }
            }
            return null;
        }

        public void prepare() throws SubsystemException {
            for (Map<String, Session> sm : sessions.values()) {
                for (Session s : sm.values()) {
                    s.prepare();
                }
            }
            for (Subsystem subsystem : stopped) {
                subsystem.start();  
            }
        }

        public void commit() {
            for (Map<String, Session> sm : sessions.values()) {
                for (Session s : sm.values()) {
                    s.commit();
                }
            }
            installed.clear();
            updated.clear();
            removed.clear();
            closeTrackers();
        }

        public void rollback() {

            for (Subsystem subsystem : installed.values()) {
                subsystemAdmin.uninstall(subsystem);
            }
            installed.clear();

            // Handle updated subsystems
            Set<Map.Entry<Map<String, String>, Subsystem>> updatedSet = updated
                    .entrySet();
            for (Entry<Map<String, String>, Subsystem> entry : updatedSet) {
                Map<String, String> oldHeader = entry.getKey();
                Subsystem subsystem = entry.getValue();

                // let's build a Manifest from oldDict
                Manifest manifest = new Manifest();
                Attributes attributes = manifest.getMainAttributes();
                attributes.putAll(oldHeader);
                String symbolicName = attributes
                        .getValue(Constants.BUNDLE_SYMBOLICNAME);
                Version v = Version.parseVersion(attributes
                        .getValue(Constants.BUNDLE_VERSION));
                Resource subsystemResource = new ResourceImpl(symbolicName, v,
                        SubsystemConstants.RESOURCE_TYPE_SUBSYSTEM, subsystem
                                .getLocation(), Collections
                                .<String, String> emptyMap());
                try {
                    Session session = getSession(subsystemAdmin, subsystemResource
                            .getType());
                    if (session instanceof SubsystemSession) {
                        ((SubsystemSession) session).process(subsystemResource,
                                manifest);
                    } else {
                        throw new SubsystemException(
                                "Invalid subsystem session - Unable to rollback subsystem");
                    }
                } catch (SubsystemException e) {
                    throw e;
                } catch (Exception e) {
                    throw new SubsystemException(
                            "Unable to rollback subsystem", e);
                }

            }

            // handle uninstalled subsystem
            Set<Map.Entry<Resource, Subsystem>> set = removed.entrySet();
            for (Map.Entry<Resource, Subsystem> entry : set) {
                Resource res = entry.getKey();
                try {
                    getSession(subsystemAdmin, res.getType()).process(res);
                } catch (SubsystemException e) {
                    throw e;
                } catch (Exception e) {
                    throw new SubsystemException(
                            "Unable to rollback subsystem", e);
                }
            }

            removed.clear();

            for (Map<String, Session> sm : sessions.values()) {
                for (Session s : sm.values()) {
                    s.rollback();
                }
            }

            closeTrackers();
        }

        protected Session getSession(SubsystemAdmin admin, String type)
                throws InvalidSyntaxException, InterruptedException {
            Map<String, Session> sm = this.sessions.get(admin);
            if (sm == null) {
                sm = new HashMap<String, Session>();
                this.sessions.put(admin, sm);
            }
            Session session = sm.get(type);
            if (session == null) {
                session = getProcessor(type).createSession(admin);
                sm.put(type, session);
            }
            return session;
        }

        protected ResourceProcessor getProcessor(String type)
                throws InvalidSyntaxException, InterruptedException {
            return getService(ResourceProcessor.class,
                    SubsystemConstants.SERVICE_RESOURCE_TYPE + "=" + type);
        }

        protected <T> T getService(Class<T> clazz)
                throws InvalidSyntaxException, InterruptedException {
            return getService(clazz, null);
        }

        protected <T> T getService(Class<T> clazz, String filter)
                throws InvalidSyntaxException, InterruptedException {
            Filter flt;
            if (filter != null) {
                if (!filter.startsWith("(")) {
                    flt = context.createFilter("(&(" + Constants.OBJECTCLASS
                            + "=" + clazz.getName() + ")(" + filter + "))");
                } else {
                    flt = context.createFilter("(&(" + Constants.OBJECTCLASS
                            + "=" + clazz.getName() + ")" + filter + ")");
                }
            } else {
                flt = context.createFilter("(" + Constants.OBJECTCLASS + "="
                        + clazz.getName() + ")");
            }
            ServiceTracker tracker = trackers.get(flt.toString());
            if (tracker == null) {
                tracker = new ServiceTracker(context, flt, null);
                tracker.open();
                trackers.put(flt.toString(), tracker);
            }
            T t = (T) tracker.waitForService(TIMEOUT);
            if (t == null) {
                throw new SubsystemException("No service available: " + flt);
            }
            return t;
        }

        private void closeTrackers() {
            for (ServiceTracker t : trackers.values()) {
                t.close();
            }
            trackers.clear();
        }
    }

    private static void checkManifestHeaders(Manifest manifest, String ssn,
            String sv) {
        // Check subsystem manifest required headers
        String mfv = manifest.getMainAttributes().getValue(
                SUBSYSTEM_MANIFESTVERSION);
        if (mfv == null || mfv.length() == 0) {
            throw new SubsystemException("Invalid subsystem manifest version: "
                    + mfv);
        }
        try {
            Version v = Version.parseVersion(mfv);
            if (!SUBSYSTEM_MANIFEST_VERSION.equals(v)) {
                throw new SubsystemException(
                        "Unsupported subsystem manifest version: " + mfv
                                + ". Supported "
                                + SubsystemConstants.SUBSYSTEM_MANIFESTVERSION
                                + " is " + SUBSYSTEM_MANIFEST_VERSION);
            }
        } catch (IllegalArgumentException e) {
            throw new SubsystemException("Invalid subsystem manifest version: "
                    + mfv, e);
        }

        if (ssn == null || ssn.length() == 0) {
            throw new SubsystemException("Invalid subsystem symbolic name: "
                    + ssn);
        }
        // check attributes / directives on the subsystem symbolic name
        // TODO add any other symbolic name to check
        Clause[] ssnClauses = Parser.parseHeader(ssn);
        String ssDirective = ssnClauses[0].getDirective(SUBSYSTEM_DIRECTIVE);
        if (ssDirective != null && ssDirective.equalsIgnoreCase("false")) {
            throw new SubsystemException("Invalid " + SUBSYSTEM_DIRECTIVE
                    + " directive in " + SUBSYSTEM_SYMBOLICNAME + ": "
                    + ssDirective);
        }

        if (sv == null || sv.length() == 0) {
            throw new SubsystemException("Invalid subsystem version: " + sv);
        }
        try {
            new Version(sv);
        } catch (IllegalArgumentException e) {
            throw new SubsystemException("Invalid subsystem version: " + sv, e);
        }

        // TODO: do we want to check other headers such as
        // subsystem-importpackage, subsystem-exportpackage, etc.

    }

    // if the ssn already contains COMPOSITE_DIRECTIVE or SUBSYSTEM_DIRECTIVE
    // directive
    // let's not add them again
    private static String getSubsystemSymbolicName(String ssn) {
        Clause[] ssnClauses = Parser.parseHeader(ssn);
        String ssDirective = ssnClauses[0].getDirective(SUBSYSTEM_DIRECTIVE);

        if (ssDirective == null ) {
            ssn = ssn + ";"
                    + SUBSYSTEM_DIRECTIVE + ":=true";
        } 

        return ssn;
    }

    private static Map<String, String> computeSubsystemHeaders(
            Manifest manifest, String ssn, String sv) {
        // Grab all headers
        Map<String, String> headers = new HashMap<String, String>();
        Iterator it = manifest.getMainAttributes().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            String name = e.getKey().toString();
            String value = e.getValue().toString();
            headers.put(name, value);
        }

        // Create the required composite headers
        headers.put(BUNDLE_SYMBOLICNAME, getSubsystemSymbolicName(ssn));
        headers.put(BUNDLE_VERSION, sv);

        String subImportPkg = headers.get(SUBSYSTEM_IMPORTPACKAGE);
        String subExportPkg = headers.get(SUBSYSTEM_EXPORTPACKAGE);


        // TODO: compute other composite manifest entries
        // TODO: compute list of bundles

        return headers;
    }

    public Session createSession(BundleContext arg0) {
        // TODO Auto-generated method stub
        return null;
    }

}