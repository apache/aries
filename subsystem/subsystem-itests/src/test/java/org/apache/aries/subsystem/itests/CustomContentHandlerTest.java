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
package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;

import org.apache.aries.subsystem.ContentHandler;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.subsystem.Subsystem;

public class CustomContentHandlerTest extends SubsystemTest {

    @Override
    protected void createApplications() throws Exception {
        createApplication("customContent", "custom1.sausages", "customContentBundleA.jar");
        createApplication("customContent1", "custom2.sausages", "customContentBundleB.jar");
        createApplication("customContent2", "custom3.sausages", "customContentBundleC.jar");
        createApplication("customContent3", "custom4.sausages", "customContentBundleD.jar");
    }

    @Test
    public void testCustomContentHandler() throws Exception {
        for (Bundle b : bundleContext.getBundles()) {
            if ("org.apache.aries.subsystem.itests.customcontent.bundleA".equals(b.getSymbolicName())) {
                fail("Precondition");
            }
        }

        SausagesContentHandler handler = new SausagesContentHandler();
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(ContentHandler.CONTENT_TYPE_PROPERTY, "foo.sausages");
        ServiceRegistration<ContentHandler> reg = bundleContext.registerService(ContentHandler.class, handler, props);

        try {
            assertEquals("Precondition", 0, handler.calls.size());
            Subsystem subsystem = installSubsystemFromFile("customContent.esa");
            try {
                assertEquals(Arrays.asList("install:customContent1 sausages = 1"), handler.calls);

                Collection<Resource> constituents = subsystem.getConstituents();
                assertEquals("The custom content should not show up as a subsystem constituent",
                        1, constituents.size());

                boolean foundBundle = false;
                for (Bundle b : bundleContext.getBundles()) {
                    if ("org.apache.aries.subsystem.itests.customcontent.bundleA".equals(b.getSymbolicName())) {
                        foundBundle = true;
                    }
                }
                assertTrue(foundBundle);

                boolean foundBundleInConstituents = false;
                for (Resource c : constituents) {
                    for(Capability idCap : c.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE)) {
                        Object name = idCap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
                        if ("org.apache.aries.subsystem.itests.customcontent.bundleA".equals(name))
                            foundBundleInConstituents = true;
                    }
                }
                assertTrue(foundBundleInConstituents);

                handler.calls.clear();
                assertEquals(Subsystem.State.INSTALLED, subsystem.getState());

                subsystem.start();
                assertEquals(Arrays.asList("start:customContent1"), handler.calls);

                handler.calls.clear();
                assertEquals(Subsystem.State.ACTIVE, subsystem.getState());

                subsystem.stop();
                assertEquals(Arrays.asList("stop:customContent1"), handler.calls);

                assertEquals(Subsystem.State.RESOLVED, subsystem.getState());
            } finally {
                handler.calls.clear();
                subsystem.uninstall();
                assertEquals(Arrays.asList("uninstall:customContent1"), handler.calls);
                assertEquals(Subsystem.State.UNINSTALLED, subsystem.getState());
            }
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testCustomContentInstallationException() throws Exception {
        for (Bundle b : bundleContext.getBundles()) {
            if ("org.apache.aries.subsystem.itests.customcontent.bundleB".equals(b.getSymbolicName())) {
                fail("Precondition");
            }
        }

        SausagesContentHandler handler = new SausagesContentHandler(true, "install");
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(ContentHandler.CONTENT_TYPE_PROPERTY, "foo.sausages");
        ServiceRegistration<ContentHandler> reg = bundleContext.registerService(ContentHandler.class, handler, props);

        assertEquals("Precondition", 0, handler.calls.size());

        try {
            installSubsystemFromFile("customContent1.esa");
        } catch (Exception ex) {
            // ignore
        }
        try {
            for (Bundle b : bundleContext.getBundles()) {
                if ("org.apache.aries.subsystem.itests.customcontent.bundleB".equals(b.getSymbolicName())) {
                    fail("Should not have installed the bundle");
                }
            }
        } finally {
            reg.unregister();
        }
    }

    @Test @Ignore("This test exposes a problem that needs to be fixed, namely that the previous test leaves stuff behind and that "
            + "customContent1.esa cannot be installed again. Currently ignored until someone finds the time to fix it.")
    public void testCustomContentInstallationSecondTime() throws Exception {
        for (Bundle b : bundleContext.getBundles()) {
            if ("org.apache.aries.subsystem.itests.customcontent.bundleB".equals(b.getSymbolicName())) {
                fail("Precondition");
            }
        }

        SausagesContentHandler handler = new SausagesContentHandler();
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(ContentHandler.CONTENT_TYPE_PROPERTY, "foo.sausages");
        ServiceRegistration<ContentHandler> reg = bundleContext.registerService(ContentHandler.class, handler, props);

        try {
            Subsystem subsystem = installSubsystemFromFile("customContent1.esa");
            subsystem.uninstall();
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testCustomContentInstallationCoordinationFails() throws Exception {
        for (Bundle b : bundleContext.getBundles()) {
            if ("org.apache.aries.subsystem.itests.customcontent.bundleC".equals(b.getSymbolicName())) {
                fail("Precondition");
            }
        }

        SausagesContentHandler handler = new SausagesContentHandler(false, "install");
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(ContentHandler.CONTENT_TYPE_PROPERTY, "foo.sausages");
        ServiceRegistration<ContentHandler> reg = bundleContext.registerService(ContentHandler.class, handler, props);

        assertEquals("Precondition", 0, handler.calls.size());

        try {
            installSubsystemFromFile("customContent2.esa");
        } catch (Exception ex) {
            // ignore
        }
        try {
            for (Bundle b : bundleContext.getBundles()) {
                if ("org.apache.aries.subsystem.itests.customcontent.bundleC".equals(b.getSymbolicName())) {
                    fail("Should not have installed the bundle");
                }
            }
        } finally {
            reg.unregister();
        }
    }



    @Test @Ignore("This test currently doesn't pass, the bundle moves to the active state, while it shouldn't")
    public void testCustomContentStartException() throws Exception {
        for (Bundle b : bundleContext.getBundles()) {
            if ("org.apache.aries.subsystem.itests.customcontent.bundleC".equals(b.getSymbolicName())) {
                fail("Precondition");
            }
        }

        SausagesContentHandler handler = new SausagesContentHandler(true, "start");
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(ContentHandler.CONTENT_TYPE_PROPERTY, "foo.sausages");
        ServiceRegistration<ContentHandler> reg = bundleContext.registerService(ContentHandler.class, handler, props);

        assertEquals("Precondition", 0, handler.calls.size());
        Subsystem subsystem = installSubsystemFromFile("customContent2.esa");

        try {
            assertEquals(Arrays.asList("install:customContent3 sausages = 3"), handler.calls);

            try {
                Bundle theBundle = null;
                for (Bundle b : bundleContext.getBundles()) {
                    if ("org.apache.aries.subsystem.itests.customcontent.bundleC".equals(b.getSymbolicName())) {
                        assertEquals(Bundle.INSTALLED, b.getState());
                        theBundle = b;
                    }
                }
                assertNotNull(theBundle);

                try {
                    subsystem.start();
                } catch (Exception ex) {
                    // good
                }
                assertEquals("There was an exception during start, so the bundle should not be started",
                        Bundle.INSTALLED, theBundle.getState());
            } finally {
                subsystem.uninstall();
            }
        } finally {
            reg.unregister();
        }
    }

    @Test @Ignore("This test currently doesn't pass, the bundle moves to the active state, while it shouldn't")
    public void testCustomContentStartFailCoordination() throws Exception {
        for (Bundle b : bundleContext.getBundles()) {
            if ("org.apache.aries.subsystem.itests.customcontent.bundleD".equals(b.getSymbolicName())) {
                fail("Precondition");
            }
        }

        SausagesContentHandler handler = new SausagesContentHandler(false, "start");
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(ContentHandler.CONTENT_TYPE_PROPERTY, "foo.sausages");
        ServiceRegistration<ContentHandler> reg = bundleContext.registerService(ContentHandler.class, handler, props);

        assertEquals("Precondition", 0, handler.calls.size());
        Subsystem subsystem = installSubsystemFromFile("customContent3.esa");

        try {
            assertEquals(Arrays.asList("install:customContent4 sausages = 4"), handler.calls);

            try {
                Bundle theBundle = null;
                for (Bundle b : bundleContext.getBundles()) {
                    if ("org.apache.aries.subsystem.itests.customcontent.bundleD".equals(b.getSymbolicName())) {
                        assertEquals(Bundle.INSTALLED, b.getState());
                        theBundle = b;
                    }
                }
                assertNotNull(theBundle);

                try {
                    subsystem.start();
                } catch (Exception ex) {
                    // good
                }
                assertEquals("The coordination failued during start, so the bundle should not be started",
                        Bundle.INSTALLED, theBundle.getState());
            } finally {
                subsystem.uninstall();
            }
        } finally {
            reg.unregister();
        }
    }

    private static String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    static class SausagesContentHandler implements ContentHandler {
        List<String> calls = new ArrayList<String>();
        private final boolean exception;
        private final String state;

        public SausagesContentHandler() {
            this(false, null);
        }

        public SausagesContentHandler(boolean exception, String state) {
            this.exception = exception;
            this.state = state;
        }

        @Override
        public void install(InputStream is, String symbolicName, String type, Subsystem subsystem,
                Coordination coordination) {
            if ("install".equals(state)) {
                if (exception) {
                    throw new RuntimeException(state);
                } else {
                    coordination.fail(new RuntimeException(state));
                }
            }

            String content = convertStreamToString(is);
            calls.add(("install:" + symbolicName + " " + content).trim());
        }

        @Override
        public void start(String symbolicName, String type, Subsystem subsystem, Coordination coordination) {
            if ("start".equals(state)) {
                if (exception) {
                    throw new RuntimeException(state);
                } else {
                    coordination.fail(new RuntimeException(state));
                }
            }

            calls.add("start:" + symbolicName);
        }

        @Override
        public void stop(String symbolicName, String type, Subsystem subsystem) {
            calls.add("stop:" + symbolicName);
        }

        @Override
        public void uninstall(String symbolicName, String type, Subsystem subsystem) {
            calls.add("uninstall:" + symbolicName);
        }
    }
}
