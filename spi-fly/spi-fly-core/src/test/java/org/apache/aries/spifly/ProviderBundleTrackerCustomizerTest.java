/**
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
package org.apache.aries.spifly;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class ProviderBundleTrackerCustomizerTest {

    private BaseActivator activator = new BaseActivator() {
        @Override
        public void start(BundleContext context) throws Exception {}
    };

    @Test
    public void testAddingRemovedBundle() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42l).anyTimes();
        EasyMock.replay(mediatorBundle);

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, mediatorBundle);

        ServiceRegistration sreg = EasyMock.createMock(ServiceRegistration.class);
        sreg.unregister();
        EasyMock.expectLastCall();
        EasyMock.replay(sreg);

        BundleContext implBC = mockSPIBundleContext(sreg);
        Bundle implBundle = mockSPIBundle(implBC);

        assertEquals("Precondition", 0, activator.findProviderBundles("org.apache.aries.mytest.MySPI").size());
        // Call addingBundle();
        List<ServiceRegistration> registrations = customizer.addingBundle(implBundle, null);
        Collection<Bundle> bundles = activator.findProviderBundles("org.apache.aries.mytest.MySPI");
        assertEquals(1, bundles.size());
        assertSame(implBundle, bundles.iterator().next());

        // The bc.registerService() call should now have been made
        EasyMock.verify(implBC);

        // Call removedBundle();
        customizer.removedBundle(implBundle, null, registrations);
        // sreg.unregister() should have been called.
        EasyMock.verify(sreg);
    }

    @Test
    public void testAddingBundleSPIBundle() throws Exception {
        BundleContext implBC = mockSPIBundleContext(EasyMock.createNiceMock(ServiceRegistration.class));
        Bundle spiBundle = mockSPIBundle(implBC);

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, spiBundle);
        assertNull("The SpiFly bundle itself should be ignored", customizer.addingBundle(spiBundle, null));
    }

    @Test
    public void testAddingNonOptInBundle() throws Exception {
        BundleContext implBC = mockSPIBundleContext(EasyMock.createNiceMock(ServiceRegistration.class));
        Bundle implBundle = mockSPIBundle(implBC, null);

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, null);
        assertEquals("Bundle without providers should remain tracked for late fragments",
                Collections.emptyList(), customizer.addingBundle(implBundle, null));
    }

    @Test
    public void testStandardDiscoveryUsesBundleLocalEntries() throws Exception {
        final String serviceType = "org.apache.aries.mytest.MySPI";
        final URL serviceFile = getClass().getResource(
                "impl1/META-INF/services/" + serviceType);
        assertNotNull("precondition", serviceFile);

        BundleWiring wiring = EasyMock.createNiceMock(BundleWiring.class);
        EasyMock.expect(wiring.findEntries("META-INF/services", serviceType, 0))
                .andReturn(Collections.singletonList(serviceFile)).anyTimes();
        EasyMock.replay(wiring);
        Bundle bundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundle.adapt(BundleWiring.class)).andReturn(wiring).anyTimes();
        EasyMock.expect(bundle.getHeaders())
                .andReturn(new Hashtable<String, String>()).anyTimes();
        EasyMock.replay(bundle);

        ProviderBundleTrackerCustomizer customizer =
                new ProviderBundleTrackerCustomizer(activator, null);

        assertEquals(Collections.singletonList(serviceFile),
                customizer.getServiceFileUrls(bundle,
                        Arrays.asList(serviceType, serviceType)));
    }

    @Test
    public void testStandardDiscoveryRetainsAttachedFragmentRevisionUntilRefresh()
            throws Exception {
        final String serviceType = "org.apache.aries.mytest.MySPI";
        final URL embeddedF1 = getClass().getResource("/embedded.jar");
        final URL embeddedF2 = getClass().getResource("/embedded2.jar");
        assertNotNull("precondition", embeddedF1);
        assertNotNull("precondition", embeddedF2);

        BundleRevision fragmentF1 = EasyMock.createNiceMock(BundleRevision.class);
        BundleRevision fragmentF2 = EasyMock.createNiceMock(BundleRevision.class);
        AtomicReference<BundleRevision> currentFragment =
                new AtomicReference<BundleRevision>(fragmentF1);
        Bundle fragment = EasyMock.createNiceMock(Bundle.class);
        Dictionary<String, String> fragmentHeaders =
                new Hashtable<String, String>();
        fragmentHeaders.put(Constants.BUNDLE_CLASSPATH, "embedded.jar");
        EasyMock.expect(fragment.getHeaders()).andReturn(fragmentHeaders).anyTimes();
        EasyMock.expect(fragment.adapt(BundleRevision.class))
                .andAnswer(() -> currentFragment.get()).anyTimes();
        EasyMock.expect(fragment.getEntry("embedded.jar"))
                .andAnswer(() -> currentFragment.get() == fragmentF1
                        ? embeddedF1 : embeddedF2).anyTimes();
        EasyMock.replay(fragment);
        EasyMock.expect(fragmentF1.getBundle()).andReturn(fragment).anyTimes();
        EasyMock.replay(fragmentF1);
        EasyMock.expect(fragmentF2.getBundle()).andReturn(fragment).anyTimes();
        EasyMock.replay(fragmentF2);

        BundleRevision hostRevision = EasyMock.createNiceMock(BundleRevision.class);
        EasyMock.replay(hostRevision);
        ClassLoader classLoaderF1 = new URLClassLoader(new URL[] {embeddedF1}, null);
        ClassLoader classLoaderF2 = new URLClassLoader(new URL[] {embeddedF2}, null);
        BundleWiring wiringF1 = mockProviderWiring(
                hostRevision, mockHostWire(fragmentF1), classLoaderF1, serviceType);
        BundleWiring wiringF2 = mockProviderWiring(
                hostRevision, mockHostWire(fragmentF2), classLoaderF2, serviceType);
        AtomicReference<BundleWiring> currentWiring =
                new AtomicReference<BundleWiring>(wiringF1);
        Bundle host = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(host.adapt(BundleWiring.class))
                .andAnswer(() -> currentWiring.get()).anyTimes();
        EasyMock.expect(host.adapt(BundleRevision.class))
                .andReturn(hostRevision).anyTimes();
        EasyMock.expect(host.getHeaders())
                .andReturn(new Hashtable<String, String>()).anyTimes();
        EasyMock.replay(host);

        ProviderBundleTrackerCustomizer customizer =
                new ProviderBundleTrackerCustomizer(activator, null);
        List<URL> f1 = customizer.getServiceFileUrls(
                host, Collections.singletonList(serviceType));
        assertEquals(Collections.singletonList(
                new URL("jar:" + embeddedF1 + "!/META-INF/services/" + serviceType)), f1);

        currentFragment.set(fragmentF2);
        assertEquals("The unchanged host wiring must retain F1", f1,
                customizer.getServiceFileUrls(
                        host, Collections.singletonList(serviceType)));

        currentWiring.set(wiringF2);
        assertEquals(Collections.singletonList(
                new URL("jar:" + embeddedF2 + "!/META-INF/services/" + serviceType)),
                customizer.getServiceFileUrls(
                        host, Collections.singletonList(serviceType)));
    }

    @Test
    public void testStandardDiscoveryUsesExactWiringForStaleFragment()
            throws Exception {
        final String serviceType = "org.apache.aries.mytest.MySPI";
        final String resourceName = "META-INF/services/" + serviceType;
        final URL embeddedF1 = getClass().getResource("/embedded.jar");
        assertNotNull("precondition", embeddedF1);

        Bundle fragment = EasyMock.createNiceMock(Bundle.class);
        BundleRevision fragmentF1 = EasyMock.createNiceMock(BundleRevision.class);
        BundleRevision fragmentF2 = EasyMock.createNiceMock(BundleRevision.class);
        EasyMock.expect(fragment.adapt(BundleRevision.class))
                .andReturn(fragmentF2).anyTimes();
        EasyMock.replay(fragment);
        EasyMock.expect(fragmentF1.getBundle()).andReturn(fragment).anyTimes();
        EasyMock.replay(fragmentF1);
        EasyMock.replay(fragmentF2);

        BundleRevision hostRevision = EasyMock.createNiceMock(BundleRevision.class);
        EasyMock.replay(hostRevision);
        ClassLoader exactClassLoader = new URLClassLoader(
                new URL[] {embeddedF1}, null);
        BundleWiring wiring = mockProviderWiring(hostRevision,
                mockHostWire(fragmentF1), exactClassLoader, serviceType);

        Bundle host = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(host.adapt(BundleWiring.class)).andReturn(wiring).anyTimes();
        EasyMock.expect(host.adapt(BundleRevision.class))
                .andReturn(hostRevision).anyTimes();
        EasyMock.expect(host.getHeaders())
                .andReturn(new Hashtable<String, String>()).anyTimes();
        EasyMock.replay(host);

        ProviderBundleTrackerCustomizer customizer =
                new ProviderBundleTrackerCustomizer(activator, null);
        assertEquals(Collections.singletonList(
                new URL("jar:" + embeddedF1 + "!/" + resourceName)),
                customizer.getServiceFileUrls(
                        host, Collections.singletonList(serviceType)));
    }

    private BundleWire mockHostWire(BundleRevision fragmentRevision) {
        BundleRequirement requirement = EasyMock.createNiceMock(BundleRequirement.class);
        EasyMock.expect(requirement.getRevision())
                .andReturn(fragmentRevision).anyTimes();
        EasyMock.replay(requirement);
        BundleWire wire = EasyMock.createNiceMock(BundleWire.class);
        EasyMock.expect(wire.getRequirement()).andReturn(requirement).anyTimes();
        EasyMock.replay(wire);
        return wire;
    }

    private BundleWiring mockProviderWiring(BundleRevision hostRevision,
            BundleWire hostWire, ClassLoader classLoader, String serviceType) {
        BundleWiring wiring = EasyMock.createNiceMock(BundleWiring.class);
        EasyMock.expect(wiring.getRevision()).andReturn(hostRevision).anyTimes();
        EasyMock.expect(wiring.getProvidedWires("osgi.wiring.host"))
                .andReturn(Collections.singletonList(hostWire)).anyTimes();
        EasyMock.expect(wiring.findEntries("META-INF/services", serviceType, 0))
                .andReturn(Collections.emptyList()).anyTimes();
        EasyMock.expect(wiring.getClassLoader()).andReturn(classLoader).anyTimes();
        EasyMock.expect(wiring.getRequiredWires(null))
                .andReturn(Collections.emptyList()).anyTimes();
        EasyMock.expect(wiring.listResources("META-INF/services", serviceType,
                BundleWiring.LISTRESOURCES_LOCAL)).andReturn(classLoader == null
                        ? Collections.<String>emptyList()
                        : Collections.singleton("META-INF/services/" + serviceType))
                .anyTimes();
        EasyMock.replay(wiring);
        return wiring;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAddingBundleWithBundleClassPath() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42l).anyTimes();
        EasyMock.replay(mediatorBundle);

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, mediatorBundle);

        BundleContext implBC = EasyMock.createMock(BundleContext.class);
        EasyMock.<Object>expect(implBC.registerService(
                EasyMock.eq("org.apache.aries.mytest.MySPI"),
                EasyMock.isA(ServiceFactory.class),
                (Dictionary<String,?>) EasyMock.anyObject())).andReturn(EasyMock.createNiceMock(ServiceRegistration.class)).times(3);
        EasyMock.replay(implBC);


        Bundle implBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(implBundle.getBundleContext()).andReturn(implBC).anyTimes();
        EasyMock.expect(implBundle.hasPermission(EasyMock.isA(ServicePermission.class)))
                .andReturn(true).anyTimes();

        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.SPI_PROVIDER_HEADER, "*");
        headers.put(Constants.BUNDLE_CLASSPATH, ".,non-jar.jar,embedded.jar,embedded2.jar");
        EasyMock.expect(implBundle.getHeaders()).andReturn(headers).anyTimes();

        URL embeddedJar = getClass().getResource("/embedded.jar");
        assertNotNull("precondition", embeddedJar);
        EasyMock.expect(implBundle.getEntry("embedded.jar")).andReturn(embeddedJar).anyTimes();
        URL embedded2Jar = getClass().getResource("/embedded2.jar");
        assertNotNull("precondition", embedded2Jar);
        EasyMock.expect(implBundle.getEntry("embedded2.jar")).andReturn(embedded2Jar).anyTimes();
        URL dir = new URL("jar:" + embeddedJar + "!/META-INF/services");
        assertNotNull("precondition", dir);
        EasyMock.expect(implBundle.getResource("/META-INF/services")).andReturn(dir).anyTimes();
        EasyMock.expect(implBundle.findEntries((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), EasyMock.anyBoolean())).
            andReturn(null).anyTimes();

        ClassLoader cl = new URLClassLoader(new URL [] {embeddedJar}, getClass().getClassLoader());
        Class<?> clsA = cl.loadClass("org.apache.aries.spifly.impl2.MySPIImpl2a");
        EasyMock.<Object>expect(implBundle.loadClass("org.apache.aries.spifly.impl2.MySPIImpl2a")).andReturn(clsA).anyTimes();
        Class<?> clsB = cl.loadClass("org.apache.aries.spifly.impl2.MySPIImpl2b");
        EasyMock.<Object>expect(implBundle.loadClass("org.apache.aries.spifly.impl2.MySPIImpl2b")).andReturn(clsB).anyTimes();
        ClassLoader cl2 = new URLClassLoader(new URL [] {embedded2Jar}, getClass().getClassLoader());
        Class<?> clsC = cl2.loadClass("org.apache.aries.spifly.impl3.MySPIImpl3");
        EasyMock.<Object>expect(implBundle.loadClass("org.apache.aries.spifly.impl3.MySPIImpl3")).andReturn(clsC).anyTimes();
        EasyMock.replay(implBundle);

        assertEquals("Precondition", 0, activator.findProviderBundles("org.apache.aries.mytest.MySPI").size());
        // Call addingBundle();
        List<ServiceRegistration> registrations = customizer.addingBundle(implBundle, null);
        Collection<Bundle> bundles = activator.findProviderBundles("org.apache.aries.mytest.MySPI");
        assertEquals(1, bundles.size());
        assertSame(implBundle, bundles.iterator().next());

        // The bc.registerService() call should now have been made
        EasyMock.verify(implBC);
    }

    @Test
    public void testMultipleProviderServices() throws Exception {
        BundleContext implBC = mockSPIBundleContext(EasyMock.createNiceMock(ServiceRegistration.class));
        Bundle implBundle = mockMultiSPIBundle(implBC);
        Bundle spiBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(spiBundle.getBundleId()).andReturn(25l).anyTimes();
        EasyMock.replay(spiBundle);

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, spiBundle);
        assertEquals(2, customizer.addingBundle(implBundle, null).size());
    }

    @SuppressWarnings("unchecked")
    private BundleContext mockSPIBundleContext(ServiceRegistration sreg) {
        BundleContext implBC = EasyMock.createMock(BundleContext.class);
        EasyMock.<Object>expect(implBC.registerService(
                EasyMock.anyString(),
                EasyMock.isA(ServiceFactory.class),
                (Dictionary<String,?>) EasyMock.anyObject())).andReturn(sreg).anyTimes();
        EasyMock.replay(implBC);
        return implBC;
    }

    private Bundle mockSPIBundle(BundleContext implBC) throws ClassNotFoundException {
        return mockSPIBundle(implBC, "*");
    }

    private Bundle mockSPIBundle(BundleContext implBC, String spiProviderHeader) throws ClassNotFoundException {
        Bundle implBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(implBundle.getBundleContext()).andReturn(implBC).anyTimes();
        EasyMock.expect(implBundle.hasPermission(EasyMock.isA(ServicePermission.class)))
                .andReturn(true).anyTimes();

        Dictionary<String, String> headers = new Hashtable<String, String>();
        if (spiProviderHeader != null)
            headers.put(SpiFlyConstants.SPI_PROVIDER_HEADER, spiProviderHeader);
        EasyMock.expect(implBundle.getHeaders()).andReturn(headers).anyTimes();

        // List the resources found at META-INF/services in the test bundle
        URL dir = getClass().getResource("impl1/META-INF/services");
        assertNotNull("precondition", dir);
        EasyMock.expect(implBundle.getResource("/META-INF/services")).andReturn(dir).anyTimes();
        URL res = getClass().getResource("impl1/META-INF/services/org.apache.aries.mytest.MySPI");
        assertNotNull("precondition", res);
        EasyMock.expect(implBundle.findEntries("META-INF/services", "*", false)).andReturn(
                Collections.enumeration(Collections.singleton(res))).anyTimes();
        Class<?> cls = getClass().getClassLoader().loadClass("org.apache.aries.spifly.impl1.MySPIImpl1");
        EasyMock.<Object>expect(implBundle.loadClass("org.apache.aries.spifly.impl1.MySPIImpl1")).andReturn(cls).anyTimes();
        EasyMock.replay(implBundle);
        return implBundle;
    }

    private Bundle mockMultiSPIBundle(BundleContext implBC) throws ClassNotFoundException {
        Bundle implBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(implBundle.getBundleContext()).andReturn(implBC).anyTimes();
        EasyMock.expect(implBundle.hasPermission(EasyMock.isA(ServicePermission.class)))
                .andReturn(true).anyTimes();

        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(
                Constants.REQUIRE_CAPABILITY,
                "osgi.extender;filter:='(osgi.extender=osgi.serviceloader.registrar)'"
        );
        headers.put(
            Constants.PROVIDE_CAPABILITY,
            "osgi.serviceloader;osgi.serviceloader='org.apache.aries.mytest.MySPI2';register:='org.apache.aries.spifly.impl4.MySPIImpl4b';foo='bbb'," +
            "osgi.serviceloader;osgi.serviceloader='org.apache.aries.mytest.MySPI2';register:='org.apache.aries.spifly.impl4.MySPIImpl4c';foo='ccc'"
        );
        EasyMock.expect(implBundle.getHeaders()).andReturn(headers).anyTimes();
        EasyMock.expect(implBundle.adapt(BundleWiring.class)).andReturn(
                mockStandardProviderWiring("org.apache.aries.mytest.MySPI2", 25L)).anyTimes();

        // List the resources found at META-INF/services in the test bundle
        URL dir = getClass().getResource("impl4/META-INF/services");
        assertNotNull("precondition", dir);
        EasyMock.expect(implBundle.getResource("/META-INF/services")).andReturn(dir).anyTimes();
        URL resA = getClass().getResource("impl4/META-INF/services/org.apache.aries.mytest.MySPI");
        assertNotNull("precondition", resA);
        URL resB = getClass().getResource("impl4/META-INF/services/org.apache.aries.mytest.MySPI2");
        assertNotNull("precondition", resB);
        EasyMock.expect(implBundle.findEntries("META-INF/services", "*", false)).andReturn(
                Collections.enumeration(Arrays.asList(resA, resB))).anyTimes();
        Class<?> cls = getClass().getClassLoader().loadClass("org.apache.aries.spifly.impl4.MySPIImpl4b");
        EasyMock.<Object>expect(implBundle.loadClass("org.apache.aries.spifly.impl4.MySPIImpl4b")).andReturn(cls).anyTimes();
        cls = getClass().getClassLoader().loadClass("org.apache.aries.spifly.impl4.MySPIImpl4c");
        EasyMock.<Object>expect(implBundle.loadClass("org.apache.aries.spifly.impl4.MySPIImpl4c")).andReturn(cls).anyTimes();
        EasyMock.replay(implBundle);
        return implBundle;
    }

    private BundleWiring mockStandardProviderWiring(String serviceType, long mediatorBundleId) {
        Map<String, Object> serviceAttributes = new HashMap<String, Object>();
        serviceAttributes.put(SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE, serviceType);
        BundleCapability serviceCapability = EasyMock.createNiceMock(BundleCapability.class);
        EasyMock.expect(serviceCapability.getAttributes()).andReturn(serviceAttributes).anyTimes();
        EasyMock.expect(serviceCapability.getDirectives()).andReturn(
                Collections.<String, String>emptyMap()).anyTimes();
        EasyMock.replay(serviceCapability);

        Map<String, Object> extenderAttributes = new HashMap<String, Object>();
        extenderAttributes.put(SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE,
                SpiFlyConstants.REGISTRAR_EXTENDER_NAME);
        BundleCapability extenderCapability = EasyMock.createNiceMock(BundleCapability.class);
        EasyMock.expect(extenderCapability.getAttributes()).andReturn(extenderAttributes).anyTimes();
        EasyMock.replay(extenderCapability);

        Bundle mediator = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(mediator.getBundleId()).andReturn(mediatorBundleId).anyTimes();
        EasyMock.replay(mediator);
        BundleWiring mediatorWiring = EasyMock.createNiceMock(BundleWiring.class);
        EasyMock.expect(mediatorWiring.getBundle()).andReturn(mediator).anyTimes();
        EasyMock.replay(mediatorWiring);

        BundleWire extenderWire = EasyMock.createNiceMock(BundleWire.class);
        EasyMock.expect(extenderWire.getCapability()).andReturn(extenderCapability).anyTimes();
        EasyMock.expect(extenderWire.getProviderWiring()).andReturn(mediatorWiring).anyTimes();
        EasyMock.replay(extenderWire);

        BundleWiring wiring = EasyMock.createNiceMock(BundleWiring.class);
        EasyMock.expect(wiring.getCapabilities(SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE))
                .andReturn(Collections.singletonList(serviceCapability)).anyTimes();
        EasyMock.expect(wiring.getRequiredWires(SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE))
                .andReturn(Collections.singletonList(extenderWire)).anyTimes();
        EasyMock.replay(wiring);
        return wiring;
    }
}
