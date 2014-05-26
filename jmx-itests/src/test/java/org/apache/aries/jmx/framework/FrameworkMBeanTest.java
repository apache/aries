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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.apache.aries.jmx.AbstractIntegrationTest;
import org.apache.aries.jmx.codec.BatchActionResult;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.jmx.framework.FrameworkMBean;

/**
 * @version $Rev$ $Date$
 */
@ExamReactorStrategy(PerMethod.class)
public class FrameworkMBeanTest extends AbstractIntegrationTest {

    private FrameworkMBean framework;

	@Configuration
    public Option[] configuration() {
        return CoreOptions.options(
            // new VMOption( "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000" ),
            // new TimeoutOption( 0 ),

            jmxRuntime(),
            bundlea1(),
            bundleb1()
        );
    }
    
	protected Option bundlea1() {
		return provision(bundle()
		        .add(org.apache.aries.jmx.test.bundlea.api.InterfaceA.class)
		        .add(org.apache.aries.jmx.test.bundlea.impl.A2.class)
		        .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.jmx.test.bundlea")
		        .set(Constants.BUNDLE_VERSION, "1")
		        .set(Constants.EXPORT_PACKAGE, "org.apache.aries.jmx.test.bundlea.api")
		        .build(withBnd()));
	}
	
	protected Option bundleb1() {
		return provision(bundle()
		        .add(org.apache.aries.jmx.test.bundleb.api.InterfaceB.class)
		        .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.jmx.test.bundleb")
		        .set(Constants.IMPORT_PACKAGE, "org.apache.aries.jmx.test.bundlea.api," +
		                "org.apache.aries.jmx.test.bundlea.impl;resolution:=optional")
		        .build(withBnd()));
	}

    @Before
    public void doSetUp() throws BundleException {
    	for (Bundle bundle : context().getBundles()) {
			System.out.println(bundle.getBundleId() + " " + bundle.getSymbolicName() + " " + bundle.getState());
		};
        waitForMBean(FrameworkMBean.OBJECTNAME);
        framework = getMBean(FrameworkMBean.OBJECTNAME, FrameworkMBean.class);
    }

    @Test
    public void testObjectName() throws Exception {
        Set<ObjectName> names = mbeanServer.queryNames(new ObjectName(FrameworkMBean.OBJECTNAME + ",*"), null);
        assertEquals(1, names.size());
        ObjectName name = names.iterator().next();
        Hashtable<String, String> props = name.getKeyPropertyList();
        assertEquals(context().getProperty(Constants.FRAMEWORK_UUID), props.get("uuid"));
        assertEquals(context().getBundle(0).getSymbolicName(), props.get("framework"));
    }

    @Test
    public void testGetProperty() throws Exception {
        String expectedVer = context().getProperty(Constants.FRAMEWORK_VERSION);
        String actualVer = framework.getProperty(Constants.FRAMEWORK_VERSION);
        assertEquals(expectedVer, actualVer);

        String expectedTmp = context().getProperty("java.io.tmpdir");
        String actualTmp = framework.getProperty("java.io.tmpdir");
        assertEquals(expectedTmp, actualTmp);
    }

    @Test
    public void testGetDependencyClosure() throws Exception {
        Bundle bundleA = getBundleByName("org.apache.aries.jmx.test.bundlea");
        Bundle bundleB = getBundleByName("org.apache.aries.jmx.test.bundleb");

        BundleWiring bw = bundleB.adapt(BundleWiring.class);

        List<BundleWire> initialRequiredWires = bw.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
        assertEquals(1, initialRequiredWires.size());
        BundleWire wire = initialRequiredWires.get(0);
        Map<String, Object> capabilityAttributes = wire.getCapability().getAttributes();
        assertEquals("Precondition", bundleA.getSymbolicName(), capabilityAttributes.get(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE));
        assertEquals("Precondition", new Version("1.0"), capabilityAttributes.get(Constants.BUNDLE_VERSION_ATTRIBUTE));
        assertEquals("Precondition", "org.apache.aries.jmx.test.bundlea.api", capabilityAttributes.get(BundleRevision.PACKAGE_NAMESPACE));

        Collection<Bundle> expectedDC = context().getBundle(0).adapt(FrameworkWiring.class).getDependencyClosure(Collections.singleton(bundleA));
        Set<Long> expectedClosure = new TreeSet<Long>();
        for (Bundle b : expectedDC) {
            expectedClosure.add(b.getBundleId());
        }

        long[] actualDC = framework.getDependencyClosure(new long [] {bundleA.getBundleId()});
        Set<Long> actualClosure = new TreeSet<Long>();
        for (long l : actualDC) {
            actualClosure.add(l);
        }

        assertEquals(expectedClosure, actualClosure);
    }

    @Test
    public void testRefreshBundleAndWait() throws Exception {
        FrameworkWiring frameworkWiring = context().getBundle(0).adapt(FrameworkWiring.class);

        Bundle bundleA = getBundleByName("org.apache.aries.jmx.test.bundlea");
        Bundle bundleB = getBundleByName("org.apache.aries.jmx.test.bundleb");

        BundleWiring bw = bundleB.adapt(BundleWiring.class);

        List<BundleWire> initialRequiredWires = bw.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
        assertEquals(1, initialRequiredWires.size());
        BundleWire wire = initialRequiredWires.get(0);
        Map<String, Object> capabilityAttributes = wire.getCapability().getAttributes();
        assertEquals("Precondition", bundleA.getSymbolicName(), capabilityAttributes.get(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE));
        assertEquals("Precondition", new Version("1.0"), capabilityAttributes.get(Constants.BUNDLE_VERSION_ATTRIBUTE));
        assertEquals("Precondition", "org.apache.aries.jmx.test.bundlea.api", capabilityAttributes.get(BundleRevision.PACKAGE_NAMESPACE));

        // Create an updated version of Bundle A, which an extra export and version 1.1
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.jmx.test.bundlea");
        manifest.getMainAttributes().putValue(Constants.BUNDLE_VERSION, "1.1");
        manifest.getMainAttributes().putValue(Constants.EXPORT_PACKAGE, "org.apache.aries.jmx.test.bundlea.api,org.apache.aries.jmx.test.bundlea.impl");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jos = new JarOutputStream(baos, manifest);
        addResourceToJar("org/apache/aries/jmx/test/bundlea/api/InterfaceA.class", jos, bundleA);
        addResourceToJar("org/apache/aries/jmx/test/bundlea/impl/A2.class", jos, bundleA);
        jos.close();

        assertEquals("Precondition", 0, frameworkWiring.getRemovalPendingBundles().size());
        assertEquals(0, framework.getRemovalPendingBundles().length);

        assertEquals("Precondition", 1, bundleA.adapt(BundleRevisions.class).getRevisions().size());
        bundleA.update(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals("There should be 2 revisions now", 2, bundleA.adapt(BundleRevisions.class).getRevisions().size());
        assertEquals("No refresh called, the bundle wiring for B should still be the old one",
                bw, bundleB.adapt(BundleWiring.class));

        assertEquals("Precondition", 1, frameworkWiring.getRemovalPendingBundles().size());
        assertEquals(1, framework.getRemovalPendingBundles().length);
        assertEquals(frameworkWiring.getRemovalPendingBundles().iterator().next().getBundleId(),
                framework.getRemovalPendingBundles()[0]);

        assertTrue(framework.refreshBundleAndWait(bundleB.getBundleId()));

        List<BundleWire> requiredWires = bundleB.adapt(BundleWiring.class).getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
        assertEquals(2, requiredWires.size());
        List<String> imported = new ArrayList<String>();
        for (BundleWire w : requiredWires) {
            Map<String, Object> ca = w.getCapability().getAttributes();
            assertEquals(bundleA.getSymbolicName(), ca.get(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE));
            imported.add(ca.get(BundleRevision.PACKAGE_NAMESPACE).toString());

            if ("org.apache.aries.jmx.test.bundlea.impl".equals(ca.get(BundleRevision.PACKAGE_NAMESPACE))) {
                // Came across an issue where equinox was reporting the other package as still coming from from the 1.0 bundle
                // not sure if this is a bug or not...
                assertEquals(new Version("1.1"), ca.get(Constants.BUNDLE_VERSION_ATTRIBUTE));
            }
        }
        assertEquals(Arrays.asList("org.apache.aries.jmx.test.bundlea.api", "org.apache.aries.jmx.test.bundlea.impl"), imported);
    }

    @Test
    public void testRefreshBundlesAndWait() throws Exception {
        Bundle bundleA = getBundleByName("org.apache.aries.jmx.test.bundlea");
        Bundle bundleB = getBundleByName("org.apache.aries.jmx.test.bundleb");

        BundleWiring bw = bundleB.adapt(BundleWiring.class);

        List<BundleWire> initialRequiredWires = bw.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
        assertEquals(1, initialRequiredWires.size());
        BundleWire wire = initialRequiredWires.get(0);
        Map<String, Object> capabilityAttributes = wire.getCapability().getAttributes();
        assertEquals("Precondition", bundleA.getSymbolicName(), capabilityAttributes.get(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE));
        assertEquals("Precondition", new Version("1.0"), capabilityAttributes.get(Constants.BUNDLE_VERSION_ATTRIBUTE));
        assertEquals("Precondition", "org.apache.aries.jmx.test.bundlea.api", capabilityAttributes.get(BundleRevision.PACKAGE_NAMESPACE));

        // Create an updated version of Bundle A, which an extra export and version 1.1
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.jmx.test.bundlea");
        manifest.getMainAttributes().putValue(Constants.BUNDLE_VERSION, "1.1");
        manifest.getMainAttributes().putValue(Constants.EXPORT_PACKAGE, "org.apache.aries.jmx.test.bundlea.api,org.apache.aries.jmx.test.bundlea.impl");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jos = new JarOutputStream(baos, manifest);
        addResourceToJar("org/apache/aries/jmx/test/bundlea/api/InterfaceA.class", jos, bundleA);
        addResourceToJar("org/apache/aries/jmx/test/bundlea/impl/A2.class", jos, bundleA);
        jos.close();

        assertEquals("Precondition", 1, bundleA.adapt(BundleRevisions.class).getRevisions().size());
        bundleA.update(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals("There should be 2 revisions now", 2, bundleA.adapt(BundleRevisions.class).getRevisions().size());
        assertEquals("No refresh called, the bundle wiring for B should still be the old one",
                bw, bundleB.adapt(BundleWiring.class));

        FrameworkMBean framework = getMBean(FrameworkMBean.OBJECTNAME, FrameworkMBean.class);
        CompositeData result = framework.refreshBundlesAndWait(new long[] {bundleB.getBundleId()});
        assertTrue((Boolean) result.get(FrameworkMBean.SUCCESS));
        assertTrue(Arrays.equals(new Long[] {bundleB.getBundleId()}, (Long []) result.get(FrameworkMBean.COMPLETED)));

        List<BundleWire> requiredWires = bundleB.adapt(BundleWiring.class).getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
        assertEquals(2, requiredWires.size());
        List<String> imported = new ArrayList<String>();
        for (BundleWire w : requiredWires) {
            Map<String, Object> ca = w.getCapability().getAttributes();
            assertEquals(bundleA.getSymbolicName(), ca.get(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE));
            imported.add(ca.get(BundleRevision.PACKAGE_NAMESPACE).toString());

            if ("org.apache.aries.jmx.test.bundlea.impl".equals(ca.get(BundleRevision.PACKAGE_NAMESPACE))) {
                // Came across an issue where equinox was reporting the other package as still coming from from the 1.0 bundle
                // not sure if this is a bug or not...
                assertEquals(new Version("1.1"), ca.get(Constants.BUNDLE_VERSION_ATTRIBUTE));
            }
        }
        assertEquals(Arrays.asList("org.apache.aries.jmx.test.bundlea.api", "org.apache.aries.jmx.test.bundlea.impl"), imported);
    }

    @Test
    public void testRefreshBundlesAndWait2() throws Exception {
        Bundle bundleA = getBundleByName("org.apache.aries.jmx.test.bundlea");
        Bundle bundleB = getBundleByName("org.apache.aries.jmx.test.bundleb");

        BundleWiring bw = bundleB.adapt(BundleWiring.class);

        List<BundleWire> initialRequiredWires = bw.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
        assertEquals(1, initialRequiredWires.size());
        BundleWire wire = initialRequiredWires.get(0);
        Map<String, Object> capabilityAttributes = wire.getCapability().getAttributes();
        assertEquals("Precondition", bundleA.getSymbolicName(), capabilityAttributes.get(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE));
        assertEquals("Precondition", new Version("1.0"), capabilityAttributes.get(Constants.BUNDLE_VERSION_ATTRIBUTE));
        assertEquals("Precondition", "org.apache.aries.jmx.test.bundlea.api", capabilityAttributes.get(BundleRevision.PACKAGE_NAMESPACE));

        // Create an updated version of Bundle A, which an extra export and version 1.1
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.jmx.test.bundlea");
        manifest.getMainAttributes().putValue(Constants.BUNDLE_VERSION, "1.1");
        manifest.getMainAttributes().putValue(Constants.EXPORT_PACKAGE, "org.apache.aries.jmx.test.bundlea.api,org.apache.aries.jmx.test.bundlea.impl");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jos = new JarOutputStream(baos, manifest);
        addResourceToJar("org/apache/aries/jmx/test/bundlea/api/InterfaceA.class", jos, bundleA);
        addResourceToJar("org/apache/aries/jmx/test/bundlea/impl/A2.class", jos, bundleA);
        jos.close();

        assertEquals("Precondition", 1, bundleA.adapt(BundleRevisions.class).getRevisions().size());
        bundleA.update(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals("There should be 2 revisions now", 2, bundleA.adapt(BundleRevisions.class).getRevisions().size());
        assertEquals("No refresh called, the bundle wiring for B should still be the old one",
                bw, bundleB.adapt(BundleWiring.class));

        FrameworkMBean framework = getMBean(FrameworkMBean.OBJECTNAME, FrameworkMBean.class);
        CompositeData result = framework.refreshBundlesAndWait(null);
        Set<Long> completed = new HashSet<Long>(Arrays.asList((Long []) result.get(FrameworkMBean.COMPLETED)));
        assertTrue(completed.contains(bundleA.getBundleId()));
        assertTrue(completed.contains(bundleB.getBundleId()));

        List<BundleWire> requiredWires = bundleB.adapt(BundleWiring.class).getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
        assertEquals(2, requiredWires.size());
        List<String> imported = new ArrayList<String>();
        for (BundleWire w : requiredWires) {
            Map<String, Object> ca = w.getCapability().getAttributes();
            assertEquals(bundleA.getSymbolicName(), ca.get(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE));
            imported.add(ca.get(BundleRevision.PACKAGE_NAMESPACE).toString());

            if ("org.apache.aries.jmx.test.bundlea.impl".equals(ca.get(BundleRevision.PACKAGE_NAMESPACE))) {
                // Came across an issue where equinox was reporting the other package as still coming from from the 1.0 bundle
                // not sure if this is a bug or not...
                assertEquals(new Version("1.1"), ca.get(Constants.BUNDLE_VERSION_ATTRIBUTE));
            }
        }
        assertEquals(Arrays.asList("org.apache.aries.jmx.test.bundlea.api", "org.apache.aries.jmx.test.bundlea.impl"), imported);
    }

    private void addResourceToJar(String resourceName, JarOutputStream jos, Bundle bundle) throws IOException {
        InputStream intfIs = bundle.getResource("/" + resourceName).openStream();
        JarEntry entry = new JarEntry(resourceName);
        jos.putNextEntry(entry);
        try {
            Streams.pump(intfIs, jos);
        } finally {
            jos.closeEntry();
        }
    }

    @Test
    public void testMBeanInterface() throws IOException {
        long[] bundleIds = new long[]{1,2};
        int[] newlevels = new int[]{1,1};
        CompositeData compData = framework.setBundleStartLevels(bundleIds, newlevels);
        assertNotNull(compData);

        BatchActionResult batch2 = BatchActionResult.from(compData);
        assertNotNull(batch2.getCompleted());
        assertTrue(batch2.isSuccess());
        assertNull(batch2.getError());
        assertNull(batch2.getRemainingItems());

        File file = File.createTempFile("bundletest", ".jar");
        file.deleteOnExit();
        Manifest man = new Manifest();
        man.getMainAttributes().putValue("Manifest-Version", "1.0");
        JarOutputStream jaros = new JarOutputStream(new FileOutputStream(file), man);
        jaros.flush();
        jaros.close();

        long bundleId = 0;
        try {
            bundleId = framework.installBundleFromURL(file.getAbsolutePath(), file.toURI().toString());
        } catch (Exception e) {
            fail("Installation of test bundle shouldn't fail");
        }

        try{
            framework.uninstallBundle(bundleId);
        } catch (Exception e) {
            fail("Uninstallation of test bundle shouldn't fail");
        }
    }
}