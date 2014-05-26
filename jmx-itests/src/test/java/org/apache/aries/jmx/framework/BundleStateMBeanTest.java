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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.osgi.jmx.framework.BundleStateMBean.OBJECTNAME;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.management.AttributeChangeNotification;
import javax.management.AttributeChangeNotificationFilter;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.apache.aries.jmx.AbstractIntegrationTest;
import org.apache.aries.jmx.codec.BundleData.Header;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.jmx.framework.BundleStateMBean;

/**
 * @version $Rev$ $Date$
 */
@ExamReactorStrategy(PerMethod.class)
public class BundleStateMBeanTest extends AbstractIntegrationTest {

    private ObjectName objectName;
	private BundleStateMBean mbean;
	private Bundle a;
	private Bundle b;
	private Bundle fragc;
	private Bundle d;

	@Configuration
    public Option[] configuration() {
		return options(
				// new VMOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"),
				// new TimeoutOption( 0 ),
				jmxRuntime(), 
				bundlea(),
				bundleb(),
				fragmentc(),
				bundled(),
				bundlee());
    }

    @Before
    public void doSetUp() throws Exception {
    	objectName = waitForMBean(BundleStateMBean.OBJECTNAME);
        mbean = getMBean(BundleStateMBean.OBJECTNAME, BundleStateMBean.class);
        //get bundles
        a = getBundleByName("org.apache.aries.jmx.test.bundlea");
        b = getBundleByName("org.apache.aries.jmx.test.bundleb");
        fragc = getBundleByName("org.apache.aries.jmx.test.fragc");
        d = getBundleByName("org.apache.aries.jmx.test.bundled");
    }

    @Test
    public void testObjectName() throws Exception {
        Set<ObjectName> names = mbeanServer.queryNames(new ObjectName(BundleStateMBean.OBJECTNAME + ",*"), null);
        assertEquals(1, names.size());
        ObjectName name = names.iterator().next();
        Hashtable<String, String> props = name.getKeyPropertyList();
        assertEquals(context().getProperty(Constants.FRAMEWORK_UUID), props.get("uuid"));
        assertEquals(context().getBundle(0).getSymbolicName(), props.get("framework"));
    }

    @Test
    public void testMBeanInterface() throws Exception {
        // exportedPackages
        String[] exports = mbean.getExportedPackages(a.getBundleId());
        assertEquals(2, exports.length);
        List<String> packages = Arrays.asList(exports);
        assertTrue(packages.contains("org.apache.aries.jmx.test.bundlea.api;2.0.0"));
        assertTrue(packages.contains("org.apache.aries.jmx.test.fragmentc;0.0.0"));

        // fragments
        long[] fragments = mbean.getFragments(a.getBundleId());
        assertEquals(1, fragments.length);
        assertEquals(fragc.getBundleId() , fragments[0]);

        // headers
        TabularData headers = mbean.getHeaders(b.getBundleId());
        assertNotNull(headers);
        assertEquals(BundleStateMBean.HEADERS_TYPE, headers.getTabularType());
        assertTrue(headers.values().size() >= 4 );
        assertEquals("org.apache.aries.jmx.test.bundleb", Header.from(headers.get(new Object[] {Constants.BUNDLE_SYMBOLICNAME})).getValue());

        // hosts
        long[] hosts = mbean.getHosts(fragc.getBundleId());
        assertEquals(1, hosts.length);
        assertEquals(a.getBundleId() , hosts[0]);

        //imported packages
        String[] imports = mbean.getImportedPackages(a.getBundleId());
        assertTrue(imports.length >= 3);
        List<String> importedPackages = Arrays.asList(imports);
        Version version = getPackageVersion("org.osgi.framework");
        assertTrue(importedPackages.contains("org.osgi.framework;" + version.toString()));
        assertTrue(importedPackages.contains("org.apache.aries.jmx.test.bundleb.api;1.1.0"));

        //last modified
        assertTrue(mbean.getLastModified(b.getBundleId()) > 0);

        //location
        assertEquals(b.getLocation(), mbean.getLocation(b.getBundleId()));

        //registered services
        long[] serviceIds = mbean.getRegisteredServices(a.getBundleId());
        assertEquals(1, serviceIds.length);

        //required bundles
        long[] required = mbean.getRequiredBundles(d.getBundleId());
        assertEquals(1, required.length);
        assertEquals(a.getBundleId(), required[0]);

        //requiring bundles
        long[] requiring = mbean.getRequiringBundles(a.getBundleId());
        assertEquals(2, requiring.length);
        assertTrue(fragc.getSymbolicName(), arrayContains(fragc.getBundleId(), requiring));
        assertTrue(d.getSymbolicName(), arrayContains(d.getBundleId(), requiring));

        //services in use
        long[] servicesInUse = mbean.getServicesInUse(a.getBundleId());
        assertEquals(1, servicesInUse.length);

        //start level
        long startLevel = mbean.getStartLevel(b.getBundleId());
        assertTrue(startLevel >= 0);

        //state
        assertEquals("ACTIVE", mbean.getState(b.getBundleId()));

        //isFragment
        assertFalse(mbean.isFragment(b.getBundleId()));
        assertTrue(mbean.isFragment(fragc.getBundleId()));

        //isRemovalPending
        assertFalse(mbean.isRemovalPending(b.getBundleId()));

        // isRequired
        assertTrue(mbean.isRequired(a.getBundleId()));
        assertTrue(mbean.isRequired(b.getBundleId()));

        // listBundles
        TabularData bundlesTable = mbean.listBundles();
        assertNotNull(bundlesTable);
        assertEquals(BundleStateMBean.BUNDLES_TYPE, bundlesTable.getTabularType());
        assertEquals(bundleContext.getBundles().length, bundlesTable.values().size());

        // notifications
        final List<Notification> received = new ArrayList<Notification>();

        mbeanServer.addNotificationListener(objectName, new NotificationListener() {
            public void handleNotification(Notification notification, Object handback) {
               received.add(notification);
            }
        }, null, null);

        assertEquals(Bundle.ACTIVE, b.getState());
        b.stop();
        assertEquals(Bundle.RESOLVED, b.getState());
        b.start();
        assertEquals(Bundle.ACTIVE, b.getState());

        int i = 0;
        while (received.size() < 2 && i < 3) {
            Thread.sleep(1000);
            i++;
        }

        assertEquals(2, received.size());
    }

    @Test
    public void testAttributeChangeNotifications() throws Exception {
        final List<AttributeChangeNotification> attributeChanges = new ArrayList<AttributeChangeNotification>();
        AttributeChangeNotificationFilter filter = new AttributeChangeNotificationFilter();
        filter.disableAllAttributes();
        filter.enableAttribute("BundleIds");

        mbeanServer.addNotificationListener(objectName, new NotificationListener() {
            public void handleNotification(Notification notification, Object handback) {
                attributeChanges.add((AttributeChangeNotification) notification);
            }
        }, filter, null);

        long[] idsWithout = mbean.getBundleIds();

        assertEquals("Precondition", 0, attributeChanges.size());

        Manifest mf = new Manifest();
        mf.getMainAttributes().putValue("Bundle-ManifestVersion", "2");
        mf.getMainAttributes().putValue("Bundle-SymbolicName", "empty-test-bundle");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jos = new JarOutputStream(baos, mf);
        jos.closeEntry();
        jos.close();

        InputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Bundle bundle = bundleContext.installBundle("http://somelocation", bais);

        long[] idsWith = new long[idsWithout.length + 1];
        System.arraycopy(idsWithout, 0, idsWith, 0, idsWithout.length);
        idsWith[idsWith.length - 1] = bundle.getBundleId();
        Arrays.sort(idsWith);

        waitForListToReachSize(attributeChanges, 1);

        assertEquals(1, attributeChanges.size());
        AttributeChangeNotification ac = attributeChanges.get(0);
        assertEquals("BundleIds", ac.getAttributeName());
        long oldSequence = ac.getSequenceNumber();
        assertTrue(Arrays.equals(idsWithout, (long []) ac.getOldValue()));
        assertTrue(Arrays.equals(idsWith, (long []) ac.getNewValue()));

        bundle.uninstall();

        waitForListToReachSize(attributeChanges, 2);
        AttributeChangeNotification ac2 = attributeChanges.get(1);
        assertEquals("BundleIds", ac2.getAttributeName());
        assertEquals(oldSequence +1, ac2.getSequenceNumber());
        assertTrue(Arrays.equals(idsWith, (long []) ac2.getOldValue()));
        assertTrue(Arrays.equals(idsWithout, (long []) ac2.getNewValue()));
    }

    @Test
    public void testBundleIDsAttribute() throws Exception{
        Set<Long> expectedIDs = new HashSet<Long>();
        for (Bundle b : context().getBundles()) {
            expectedIDs.add(b.getBundleId());
        }

        BundleStateMBean mbean = getMBean(OBJECTNAME, BundleStateMBean.class);
        long[] actual = mbean.getBundleIds();
        Set<Long> actualIDs = new HashSet<Long>();
        for (long id : actual) {
            actualIDs.add(id);
        }

        assertEquals(expectedIDs, actualIDs);
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void testHeaderLocalization() throws Exception {
        Bundle bundleE = context().getBundleByName("org.apache.aries.jmx.test.bundlee");

        CompositeData cd = mbean.getBundle(bundleE.getBundleId());
        long id = (Long) cd.get(BundleStateMBean.IDENTIFIER);
        assertEquals("Description", mbean.getHeader(id, Constants.BUNDLE_DESCRIPTION));
        assertEquals("Description", mbean.getHeader(id, Constants.BUNDLE_DESCRIPTION, "en"));
        assertEquals("Omschrijving", mbean.getHeader(id, Constants.BUNDLE_DESCRIPTION, "nl"));

        TabularData td = mbean.getHeaders(id);
        boolean found = false;
        for (CompositeData d : (Collection<CompositeData>) td.values()) {
            if (Constants.BUNDLE_DESCRIPTION.equals(d.get(BundleStateMBean.KEY))) {
                assertEquals("Description", d.get(BundleStateMBean.VALUE));
                found = true;
                break;
            }
        }
        assertTrue(found);

        TabularData tdNL = mbean.getHeaders(id, "nl");
        boolean foundNL = false;
        for (CompositeData d : (Collection<CompositeData>) tdNL.values()) {
            if (Constants.BUNDLE_DESCRIPTION.equals(d.get(BundleStateMBean.KEY))) {
                assertEquals("Omschrijving", d.get(BundleStateMBean.VALUE));
                foundNL = true;
                break;
            }
        }
        assertTrue(foundNL);
    }

    private Version getPackageVersion(String packageName) {
        Bundle systemBundle = context().getBundle(0);
        BundleWiring wiring = systemBundle.adapt(BundleWiring.class);
        List<BundleCapability> packages = wiring.getCapabilities(BundleRevision.PACKAGE_NAMESPACE);
        for (BundleCapability pkg : packages) {
            Map<String, Object> attrs = pkg.getAttributes();
            if (attrs.get(BundleRevision.PACKAGE_NAMESPACE).equals(packageName)) {
                return (Version) attrs.get(Constants.VERSION_ATTRIBUTE);
            }
        }
        throw new IllegalStateException("Package version not found for " + packageName);
    }

    private static boolean arrayContains(long value, long[] values) {
        for (long i : values) {
            if (i == value) {
                return true;
            }
        }
        return false;
    }

    private void waitForListToReachSize(List<?> list, int targetSize) throws InterruptedException {
        int i = 0;
        while (list.size() < targetSize && i < 3) {
            Thread.sleep(1000);
            i++;
        }
    }
}
