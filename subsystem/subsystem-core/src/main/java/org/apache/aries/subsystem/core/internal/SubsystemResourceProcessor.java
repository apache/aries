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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.aries.subsystem.Subsystem;
import org.apache.aries.subsystem.SubsystemAdmin;
import org.apache.aries.subsystem.SubsystemConstants;
import org.apache.aries.subsystem.SubsystemException;
import org.apache.aries.subsystem.spi.Resource;
import org.apache.aries.subsystem.spi.ResourceProcessor;
import org.apache.aries.subsystem.spi.ResourceResolver;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.service.composite.CompositeAdmin;
import org.osgi.service.composite.CompositeBundle;
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
        private final Map<Resource, CompositeBundle> removed = new HashMap<Resource, CompositeBundle>();
        private final Map<String, ServiceTracker> trackers = new HashMap<String, ServiceTracker>();
        private final Map<BundleContext, Map<String, Session>> sessions = new HashMap<BundleContext, Map<String, Session>>();


        public SubsystemSession(BundleContext context) {
            this.context = context;
        }

        public void process(Resource res) throws SubsystemException {
            CompositeBundle composite = null;
            boolean success = false;
            try {

                CompositeAdmin admin = getService(CompositeAdmin.class);
                ResourceResolver resolver = getService(ResourceResolver.class);

                // Unpack the subsystem archive into a temporary directory
                File dir = File.createTempFile("subsystem", "", null);
                if (dir == null || !dir.delete() || !dir.mkdir()) {
                    throw new Exception("Unable to create temporary dir");
                }
                FileUtils.unpackArchive(res.open(), dir);

                Manifest manifest = new Manifest();
                InputStream mis = new FileInputStream(new File(dir, JarFile.MANIFEST_NAME));
                try {
                    manifest.read(mis);
                } finally {
                    closeQuietly(mis);
                }

                List<Resource> resource = new ArrayList<Resource>();
                String resourceHeader = manifest.getMainAttributes().getValue(SUBSYSTEM_RESOURCES);
                Clause[] resourceClauses = Parser.parseHeader(resourceHeader);
                for (Clause c : resourceClauses) {
                    Resource r = resolver.find(c.toString());
                    resource.add(r);
                }

                List<Resource> content = new ArrayList<Resource>();
                String contentHeader = manifest.getMainAttributes().getValue(SUBSYSTEM_CONTENT);
                Clause[] contentClauses = Parser.parseHeader(contentHeader);
                for (Clause c : contentClauses) {
                    Resource r = resolver.find(c.toString());
                    content.add(r);
                }

                // TODO: convert resources before calling the resolver?

                List<Resource> additional = resolver.resolve(content, resource);

                // Check subsystem manifest required headers
                String mfv = manifest.getMainAttributes().getValue(SUBSYSTEM_MANIFESTVERSION);
                if (mfv == null || mfv.length() == 0) {
                    throw new SubsystemException("Invalid subsystem manifest version: " + mfv);
                }
                try {
                    Version v = Version.parseVersion(mfv);
                    if (!SUBSYSTEM_MANIFEST_VERSION.equals(v)) {
                        throw new SubsystemException("Unsupported subsystem manifest version: " + mfv);
                    }
                } catch (IllegalArgumentException e) {
                    throw new SubsystemException("Invalid subsystem manifest version: " + mfv, e);
                }
                String ssn = manifest.getMainAttributes().getValue(SUBSYSTEM_SYMBOLICNAME);
                if (ssn == null || ssn.length() == 0) {
                    throw new SubsystemException("Invalid subsystem symbolic name: " + ssn);
                }
                // TODO: check attributes / directives on the subsystem symbolic name ?
                String sv = manifest.getMainAttributes().getValue(SUBSYSTEM_VERSION);
                if (sv == null || sv.length() == 0) {
                    throw new SubsystemException("Invalid subsystem version: " + sv);
                }
                try {
                    new Version(sv);
                } catch (IllegalArgumentException e) {
                    throw new SubsystemException("Invalid subsystem version: " + sv, e);
                }
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
                headers.put(BUNDLE_SYMBOLICNAME, ssn + ";" + COMPOSITE_DIRECTIVE + ":=true;" + SUBSYSTEM_DIRECTIVE + ":=true");
                headers.put(BUNDLE_VERSION, sv);
                // TODO: compute other composite manifest entries
                // TODO: compute list of bundles

                composite = admin.installCompositeBundle(
                                            res.getLocation(),
                                            headers,
                                            Collections.<String, String>emptyMap());
                installed.put(res, composite);
                composite.getSystemBundleContext().registerService(SubsystemAdmin.class.getName(), new Activator.SubsystemAdminFactory(), null);

                for (Resource r : additional) {
                    getSession(context, r.getType()).process(r);
                }
                for (Resource r : content) {
                    getSession(composite.getSystemBundleContext(), r.getType()).process(r);
                }

                success = true;

            } catch (SubsystemException e) {
                throw e;
            } catch (Exception e) {
                throw new SubsystemException("Unable to install subsystem", e);
            } finally {
                if (!success && composite != null) {
                    try {
                        composite.uninstall();
                    } catch (Exception e) {
                        // TODO: log error
                    }
                }
            }
        }

        public void dropped(Resource resource) throws SubsystemException {
            // TODO: find corresponding subsystem
        }

        protected Subsystem findSubsystem(Resource resource) {
            // TODO
            return null;
        }

        public void prepare() throws SubsystemException {
            for (Map<String, Session> sm : sessions.values()) {
                for (Session s : sm.values()) {
                    s.prepare();
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
            closeTrackers();
        }

        public void rollback() {
            for (Map<String, Session> sm : sessions.values()) {
                for (Session s : sm.values()) {
                    s.rollback();
                }
            }
            for (CompositeBundle c : installed.values()) {
                try {
                    c.uninstall();
                } catch (BundleException e) {
                    // Ignore 
                }
            }
            installed.clear();
            closeTrackers();
        }

        protected Session getSession(BundleContext context, String type) throws InvalidSyntaxException, InterruptedException {
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

        protected ResourceProcessor getProcessor(String type) throws InvalidSyntaxException, InterruptedException {
            return getService(ResourceProcessor.class, SubsystemConstants.SERVICE_RESOURCE_TYPE + "=" + type);
        }

        protected <T> T getService(Class<T> clazz) throws InvalidSyntaxException, InterruptedException {
            return getService(clazz, null);
        }

        protected <T> T getService(Class<T> clazz, String filter) throws InvalidSyntaxException, InterruptedException {
            Filter flt;
            if (filter != null) {
                if (!filter.startsWith("(")) {
                    flt = context.createFilter("(&(" + Constants.OBJECTCLASS + "=" + clazz.getName() + ")(" + filter + "))");
                } else {
                    flt = context.createFilter("(&(" + Constants.OBJECTCLASS + "=" + clazz.getName() + ")" + filter + ")");
                }
            } else {
                flt = context.createFilter("(" + Constants.OBJECTCLASS + "=" + clazz.getName() + ")");
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

}