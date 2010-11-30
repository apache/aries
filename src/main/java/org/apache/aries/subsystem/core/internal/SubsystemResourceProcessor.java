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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.aries.subsystem.SubsystemAdmin;
import org.apache.aries.subsystem.SubsystemConstants;
import org.apache.aries.subsystem.SubsystemException;
import org.apache.aries.subsystem.spi.Resource;
import org.apache.aries.subsystem.spi.ResourceProcessor;
import org.apache.aries.subsystem.spi.ResourceResolver;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.service.composite.CompositeAdmin;
import org.osgi.service.composite.CompositeBundle;
import org.osgi.service.composite.CompositeConstants;
import org.osgi.util.tracker.ServiceTracker;

import static org.apache.aries.subsystem.core.internal.FileUtils.closeQuietly;

import static org.osgi.framework.Constants.*;
import static org.osgi.service.composite.CompositeConstants.*;
import static org.apache.aries.subsystem.SubsystemConstants.*;

public class SubsystemResourceProcessor implements ResourceProcessor {

    private static final Version SUBSYSTEM_MANIFEST_VERSION = new Version("1.0");

    public SubsystemSession createSession(BundleContext context) {
        return new SubsystemSession(context);
    }

    public static class SubsystemSession implements Session {

        private static final long TIMEOUT = 30000;

        private final BundleContext context;
        private final Map<Resource, CompositeBundle> installed = new HashMap<Resource, CompositeBundle>();
        /*
         * Map to keep track of composite bundle headers before the update and
         * the updated composite bundle. This is needed for rollback
         */
        private final Map<Dictionary, CompositeBundle> updated = new HashMap<Dictionary, CompositeBundle>();
        private final Map<Resource, CompositeBundle> removed = new HashMap<Resource, CompositeBundle>();
        private final List<CompositeBundle> stopped = new ArrayList<CompositeBundle>();
        private final Map<String, ServiceTracker> trackers = new HashMap<String, ServiceTracker>();
        private final Map<BundleContext, Map<String, Session>> sessions = new HashMap<BundleContext, Map<String, Session>>();

        public SubsystemSession(BundleContext context) {
            this.context = context;
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

                CompositeAdmin admin = getService(CompositeAdmin.class);
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

                Map<String, String> headers = computeCompositeHeaders(manifest,
                        ssn, sv);

                // Check existing bundles
                CompositeBundle composite = findSubsystemComposite(res);
                if (composite == null) {
                    // brand new install
                    composite = admin.installCompositeBundle(res.getLocation(),
                            headers, Collections.<String, String> emptyMap());
                    installed.put(res, composite);
                } else {
                    // update
                    // capture composite headers before update
                    Dictionary dictionary = composite.getHeaders();

                    String previousContentHeader = (String) composite
                            .getHeaders().get(SUBSYSTEM_CONTENT);
                    Clause[] previousContentClauses = Parser
                            .parseHeader(previousContentHeader);
                    for (Clause c : previousContentClauses) {
                        Resource r = resolver.find(c.toString());
                        previous.add(r);
                    }
                    if (composite.getState() == Bundle.ACTIVE) {
                        composite.stop();
                        stopped.add(composite);
                    }
                    composite.update(headers);
                    updated.put(dictionary, composite);
                }
                composite.getSystemBundleContext().registerService(
                        SubsystemAdmin.class.getName(),
                        new Activator.SubsystemAdminFactory(), null);

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
                        getSession(composite.getSystemBundleContext(),
                                r.getType()).dropped(r);
                    }
                }
                for (Resource r : additional) {
                    getSession(context, r.getType()).process(r);
                }
                for (Resource r : content) {
                    getSession(composite.getSystemBundleContext(), r.getType())
                            .process(r);
                }

            } catch (SubsystemException e) {
                throw e;
            } catch (Exception e) {
                throw new SubsystemException("Unable to process subsystem", e);
            }
        }

        public void process(Resource res) throws SubsystemException {
            process(res, null);

        }

        public void dropped(Resource res) throws SubsystemException {
            CompositeBundle composite = findSubsystemComposite(res);
            if (composite == null) {
                throw new SubsystemException(
                        "Unable to find matching subsystem to uninstall");
            }
            try {
                // TODO: iterate through all resources and ask for a removal on
                // each one
                composite.uninstall();
                removed.put(res, composite);
            } catch (BundleException e) {
                throw new SubsystemException("Unable to uninstall subsystem", e);
            }
        }

        protected CompositeBundle findSubsystemComposite(Resource resource) {
            for (Bundle bundle : context.getBundles()) {
                if (resource.getLocation().equals(bundle.getLocation())) {
                    if (bundle instanceof CompositeBundle) {
                        CompositeBundle composite = (CompositeBundle) bundle;
                        String bsn = (String) bundle.getHeaders().get(
                                Constants.BUNDLE_SYMBOLICNAME);
                        Clause[] bsnClauses = Parser.parseHeader(bsn);
                        if ("true"
                                .equals(bsnClauses[0]
                                        .getDirective(SubsystemConstants.SUBSYSTEM_DIRECTIVE))) {
                            return composite;
                        } else {
                            throw new SubsystemException(
                                    "A bundle with the same location already exists!");
                        }

                    }
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
            for (CompositeBundle composite : stopped) {
                try {
                    composite.start();
                } catch (BundleException e) {
                    throw new SubsystemException(e);
                }
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

            for (CompositeBundle c : installed.values()) {
                try {
                    c.uninstall();
                } catch (BundleException e) {
                    // Ignore
                }
            }
            installed.clear();

            // Handle updated subsystems
            Set<Map.Entry<Dictionary, CompositeBundle>> updatedSet = updated
                    .entrySet();
            for (Entry<Dictionary, CompositeBundle> entry : updatedSet) {
                Dictionary oldDic = entry.getKey();
                CompositeBundle cb = entry.getValue();

                // let's build a Manifest from oldDict
                Manifest manifest = new Manifest();
                Attributes attributes = manifest.getMainAttributes();
                Map<String, String> headerMap = new DictionaryAsMap(oldDic);

                attributes.putAll(headerMap);
                String symbolicName = attributes
                        .getValue(Constants.BUNDLE_SYMBOLICNAME);
                Version v = Version.parseVersion(attributes
                        .getValue(Constants.BUNDLE_VERSION));
                Resource subsystemResource = new ResourceImpl(symbolicName, v,
                        SubsystemConstants.RESOURCE_TYPE_SUBSYSTEM, cb
                                .getLocation(), Collections
                                .<String, String> emptyMap());
                try {
                    Session session = getSession(context, subsystemResource
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
            Set<Map.Entry<Resource, CompositeBundle>> set = removed.entrySet();
            for (Map.Entry<Resource, CompositeBundle> entry : set) {
                Resource res = entry.getKey();
                try {
                    getSession(context, res.getType()).process(res);
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

        protected Session getSession(BundleContext context, String type)
                throws InvalidSyntaxException, InterruptedException {
            Map<String, Session> sm = this.sessions.get(context);
            if (sm == null) {
                sm = new HashMap<String, Session>();
                this.sessions.put(context, sm);
            }
            Session session = sm.get(type);
            if (session == null) {
                session = getProcessor(type).createSession(context);
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
        String comDirective = ssnClauses[0].getDirective(COMPOSITE_DIRECTIVE);
        if (ssDirective != null && ssDirective.equalsIgnoreCase("false")) {
            throw new SubsystemException("Invalid " + SUBSYSTEM_DIRECTIVE
                    + " directive in " + SUBSYSTEM_SYMBOLICNAME + ": "
                    + ssDirective);
        }

        if (ssDirective != null && comDirective.equalsIgnoreCase("false")) {
            throw new SubsystemException("Invalid " + COMPOSITE_DIRECTIVE
                    + " directive in " + SUBSYSTEM_SYMBOLICNAME + ": "
                    + comDirective);
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
    private static String getCompositeSymbolicName(String ssn) {
        Clause[] ssnClauses = Parser.parseHeader(ssn);
        String ssDirective = ssnClauses[0].getDirective(SUBSYSTEM_DIRECTIVE);
        String comDirective = ssnClauses[0].getDirective(COMPOSITE_DIRECTIVE);

        if (ssDirective == null && comDirective == null) {
            ssn = ssn + ";" + COMPOSITE_DIRECTIVE + ":=true;"
                    + SUBSYSTEM_DIRECTIVE + ":=true";
        } else if (ssDirective == null) {
            ssn = ssn + ";" + COMPOSITE_DIRECTIVE + ":=true;";
        } else if (comDirective == null) {
            ssn = ssn + ";" + SUBSYSTEM_DIRECTIVE + ":=true;";
        }

        return ssn;
    }

    private static Map<String, String> computeCompositeHeaders(
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
        headers.put(BUNDLE_SYMBOLICNAME, getCompositeSymbolicName(ssn));
        headers.put(BUNDLE_VERSION, sv);

        String subImportPkg = headers.get(SUBSYSTEM_IMPORTPACKAGE);
        String subExportPkg = headers.get(SUBSYSTEM_EXPORTPACKAGE);
        if (subImportPkg != null && subImportPkg.length() > 0) {
            // use subsystem-importpackage for composite-importpackage
            headers.put(CompositeConstants.COMPOSITE_PACKAGE_IMPORT_POLICY,
                    subImportPkg);
        } else {
            // TODO: let's compute the import package for the subsystem
        }
        if (subExportPkg != null && subExportPkg.length() > 0) {
            // use subsystem-importpackage for composite-importpackage
            headers.put(CompositeConstants.COMPOSITE_PACKAGE_EXPORT_POLICY,
                    subExportPkg);
        }

        // TODO: compute other composite manifest entries
        // TODO: compute list of bundles

        return headers;
    }

}