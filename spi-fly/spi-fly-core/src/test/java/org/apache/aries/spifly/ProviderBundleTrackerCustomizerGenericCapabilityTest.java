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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.aries.mytest.MySPI;
import org.apache.aries.mytest.MySPI2;
import org.apache.aries.spifly.impl4.MySPIImpl4a;
import org.apache.aries.spifly.impl4.MySPIImpl4b;
import org.apache.aries.spifly.impl4.MySPIImpl4c;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import aQute.bnd.header.Parameters;

public class ProviderBundleTrackerCustomizerGenericCapabilityTest {
    @Test
    public void testAddingRemovedBundle() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42l).anyTimes();
        EasyMock.replay(mediatorBundle);
        BaseActivator activator = new BaseActivator() {
            @Override
            public void start(BundleContext context) throws Exception {}
        };

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, mediatorBundle);

        @SuppressWarnings("rawtypes")
        ServiceRegistration sreg = EasyMock.createMock(ServiceRegistration.class);
        sreg.unregister();
        EasyMock.expectLastCall();
        EasyMock.replay(sreg);

        BundleContext implBC = mockSPIBundleContext(sreg);
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.PROVIDER_REQUIREMENT);
        headers.put(SpiFlyConstants.PROVIDE_CAPABILITY, SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "; " +
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "=org.apache.aries.mytest.MySPI");
        Bundle implBundle = mockSPIBundle(implBC, headers);

        assertEquals("Precondition", 0, activator.findProviderBundles("org.apache.aries.mytest.MySPI").size());
        // Call addingBundle();
        @SuppressWarnings("rawtypes")
        List<ServiceRegistration> registrations = customizer.addingBundle(implBundle, null);
        Collection<Bundle> bundles = activator.findProviderBundles("org.apache.aries.mytest.MySPI");
        assertEquals(1, bundles.size());
        assertSame(implBundle, bundles.iterator().next());

        // The bc.registerService() call should now have been made
        EasyMock.verify(implBC);

        // Call removedBundle();
        customizer.removedBundle(implBundle, null, registrations);

        Collection<Bundle> bundles2 = activator.findProviderBundles("org.apache.aries.mytest.MySPI");
        assertEquals(0, bundles2.size());

        // sreg.unregister() should have been called.
        EasyMock.verify(sreg);
    }

    @Test
    public void testCapReqHeadersInFragmentWhenHostAlreadyProvidesCapability() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42l).anyTimes();
        EasyMock.replay(mediatorBundle);
        BaseActivator activator = new BaseActivator() {
            @Override
            public void start(BundleContext context) throws Exception {}
        };

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, mediatorBundle);

        BundleContext implBC = mockSPIBundleContext4();
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.PROVIDER_REQUIREMENT);
        headers.put(SpiFlyConstants.PROVIDE_CAPABILITY, SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "; " +
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "=org.apache.aries.mytest.MySPI");

        Dictionary<String, String> fheaders = new Hashtable<String, String>();
        fheaders.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.PROVIDER_REQUIREMENT);
        fheaders.put(SpiFlyConstants.PROVIDE_CAPABILITY, SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "; " +
              SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "=org.apache.aries.mytest.MySPI2");
        Bundle fragment = mockFragment(fheaders);
        Bundle implBundle = mockSPIBundle4(implBC, headers, mockHostRevision(fragment));

        assertEquals("Precondition", 0, activator.findProviderBundles("org.apache.aries.mytest.MySPI").size());
        assertEquals("Precondition", 0, activator.findProviderBundles("org.apache.aries.mytest.MySPI2").size());
        @SuppressWarnings("rawtypes")
        List<ServiceRegistration> registrations = customizer.addingBundle(implBundle, null);
        assertEquals(3, registrations.size());
        assertProviderBundle(activator, "org.apache.aries.mytest.MySPI", implBundle);
        assertProviderBundle(activator, "org.apache.aries.mytest.MySPI2", implBundle);
    }

    @Test
    public void testCapReqHeadersInMultipleFragments() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42l).anyTimes();
        EasyMock.replay(mediatorBundle);
        BaseActivator activator = new BaseActivator() {
            @Override
            public void start(BundleContext context) throws Exception {}
        };

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, mediatorBundle);
        BundleContext implBC = mockSPIBundleContext4();

        Dictionary<String, String> headers = new Hashtable<String, String>();

        Dictionary<String, String> fheaders1 = new Hashtable<String, String>();
        fheaders1.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.PROVIDER_REQUIREMENT);
        fheaders1.put(SpiFlyConstants.PROVIDE_CAPABILITY, SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "; " +
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "=org.apache.aries.mytest.MySPI");

        Dictionary<String, String> fheaders2 = new Hashtable<String, String>();
        fheaders2.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.PROVIDER_REQUIREMENT);
        fheaders2.put(SpiFlyConstants.PROVIDE_CAPABILITY, SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "; " +
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "=org.apache.aries.mytest.MySPI2");

        Bundle implBundle = mockSPIBundle4(implBC, headers,
                mockHostRevision(mockFragment(fheaders1), mockFragment(fheaders2)));

        @SuppressWarnings("rawtypes")
        List<ServiceRegistration> registrations = customizer.addingBundle(implBundle, null);
        assertEquals(3, registrations.size());
        assertProviderBundle(activator, "org.apache.aries.mytest.MySPI", implBundle);
        assertProviderBundle(activator, "org.apache.aries.mytest.MySPI2", implBundle);
    }

    @Test
    public void testCustomAttributesBundle() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42l).anyTimes();
        EasyMock.replay(mediatorBundle);
        BaseActivator activator = new BaseActivator() {
            @Override
            public void start(BundleContext context) throws Exception {}
        };

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, mediatorBundle);

        @SuppressWarnings("rawtypes")
        ServiceRegistration sreg = EasyMock.createMock(ServiceRegistration.class);
        EasyMock.replay(sreg);

        BundleContext implBC = mockSPIBundleContext(sreg);
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.PROVIDER_REQUIREMENT);
        headers.put(SpiFlyConstants.PROVIDE_CAPABILITY, SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "; " +
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "=org.apache.aries.mytest.MySPI; approval=yeah; ");
        Bundle implBundle = mockSPIBundle(implBC, headers);

        @SuppressWarnings("rawtypes")
        List<ServiceRegistration> registrations = customizer.addingBundle(implBundle, null);
        assertEquals(1, registrations.size());
        Collection<Bundle> bundles = activator.findProviderBundles("org.apache.aries.mytest.MySPI");
        assertEquals(1, bundles.size());
        assertSame(implBundle, bundles.iterator().next());

        Map<String, Object> attrs = activator.getCustomBundleAttributes("org.apache.aries.mytest.MySPI", implBundle);
        assertEquals(4, attrs.size());
        assertEquals("yeah", attrs.get("approval"));
    }

    @Test
    public void testAutoProviderSystemProperty() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42l).anyTimes();
        EasyMock.replay(mediatorBundle);
        BaseActivator activator = new BaseActivator() {
            @Override
            public void start(BundleContext context) throws Exception {}
        };

        activator.setAutoProviderInstructions(Optional.of(new Parameters("*")));
        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, mediatorBundle);

        @SuppressWarnings("rawtypes")
        ServiceRegistration sreg = EasyMock.createMock(ServiceRegistration.class);
        EasyMock.replay(sreg);

        BundleContext implBC = mockSPIBundleContext(sreg);
        Dictionary<String, String> headers = new Hashtable<String, String>();
        Bundle implBundle = mockSPIBundle(implBC, headers);

        @SuppressWarnings("rawtypes")
        List<ServiceRegistration> registrations = customizer.addingBundle(implBundle, null);
        assertEquals(1, registrations.size());
        Collection<Bundle> bundles = activator.findProviderBundles("org.apache.aries.mytest.MySPI");
        assertEquals(1, bundles.size());
        assertSame(implBundle, bundles.iterator().next());

        Map<String, Object> attrs = activator.getCustomBundleAttributes("org.apache.aries.mytest.MySPI", implBundle);
        assertEquals(3, attrs.size());
        assertNull(attrs.get("approval"));
    }

    @Test
    public void testAutoProviderSystemPropertyPlusProperty() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42l).anyTimes();
        EasyMock.replay(mediatorBundle);
        BaseActivator activator = new BaseActivator() {
            @Override
            public void start(BundleContext context) throws Exception {}
        };

        activator.setAutoProviderInstructions(Optional.of(new Parameters("*;approval=yeah")));
        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, mediatorBundle);

        @SuppressWarnings("rawtypes")
        ServiceRegistration sreg = EasyMock.createMock(ServiceRegistration.class);
        EasyMock.replay(sreg);

        BundleContext implBC = mockSPIBundleContext(sreg);
        Dictionary<String, String> headers = new Hashtable<String, String>();
        Bundle implBundle = mockSPIBundle(implBC, headers);

        @SuppressWarnings("rawtypes")
        List<ServiceRegistration> registrations = customizer.addingBundle(implBundle, null);
        assertEquals(1, registrations.size());
        Collection<Bundle> bundles = activator.findProviderBundles("org.apache.aries.mytest.MySPI");
        assertEquals(1, bundles.size());
        assertSame(implBundle, bundles.iterator().next());

        Map<String, Object> attrs = activator.getCustomBundleAttributes("org.apache.aries.mytest.MySPI", implBundle);
        assertEquals(4, attrs.size());
        assertEquals("yeah", attrs.get("approval"));
    }

    @Test
    public void testAutoProviderSystemPropertyTargetBundle() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42l).anyTimes();
        EasyMock.replay(mediatorBundle);
        BaseActivator activator = new BaseActivator() {
            @Override
            public void start(BundleContext context) throws Exception {}
        };

        activator.setAutoProviderInstructions(Optional.of(new Parameters("bsn")));
        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, mediatorBundle);

        @SuppressWarnings("rawtypes")
        ServiceRegistration sreg = EasyMock.createMock(ServiceRegistration.class);
        EasyMock.replay(sreg);

        BundleContext implBC = mockSPIBundleContext(sreg);
        Dictionary<String, String> headers = new Hashtable<String, String>();
        Bundle implBundle = mockSPIBundle(implBC, headers);

        @SuppressWarnings("rawtypes")
        List<ServiceRegistration> registrations = customizer.addingBundle(implBundle, null);
        assertEquals(1, registrations.size());
        Collection<Bundle> bundles = activator.findProviderBundles("org.apache.aries.mytest.MySPI");
        assertEquals(1, bundles.size());
        assertSame(implBundle, bundles.iterator().next());

        Map<String, Object> attrs = activator.getCustomBundleAttributes("org.apache.aries.mytest.MySPI", implBundle);
        assertEquals(3, attrs.size());
    }

    @Test
    public void testNonServiceRegistryBundle() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42l).anyTimes();
        EasyMock.replay(mediatorBundle);
        BaseActivator activator = new BaseActivator() {
            @Override
            public void start(BundleContext context) throws Exception {}
        };

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, mediatorBundle);

        @SuppressWarnings("rawtypes")
        ServiceRegistration sreg = EasyMock.createMock(ServiceRegistration.class);
        EasyMock.replay(sreg);

        BundleContext implBC = mockSPIBundleContext(sreg);
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.PROVIDER_REQUIREMENT);
        headers.put(SpiFlyConstants.PROVIDE_CAPABILITY, SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "; " +
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "=org.apache.aries.mytest.MySPI; approval=yeah;" +
                SpiFlyConstants.REGISTER_DIRECTIVE + "=\"\"");
        Bundle implBundle = mockSPIBundle(implBC, headers);

        @SuppressWarnings("rawtypes")
        List<ServiceRegistration> registrations = customizer.addingBundle(implBundle, null);
        assertEquals(0, registrations.size());
        Collection<Bundle> bundles = activator.findProviderBundles("org.apache.aries.mytest.MySPI");
        assertEquals(1, bundles.size());
        assertSame(implBundle, bundles.iterator().next());

        Map<String, Object> attrs = activator.getCustomBundleAttributes("org.apache.aries.mytest.MySPI", implBundle);
        assertTrue(attrs.isEmpty());
    }

    @Test
    public void testRegisterAltAttributeDatatype() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42L).anyTimes();
        EasyMock.replay(mediatorBundle);
        BaseActivator activator = new BaseActivator() {
            @Override
            public void start(BundleContext context) throws Exception {}
        };

        ProviderBundleTrackerCustomizer customizer =
                new ProviderBundleTrackerCustomizer(activator, mediatorBundle);
        BundleContext implBC = mockSPIBundleContext4();

        Map<String, Object> firstAttributes = new HashMap<String, Object>();
        firstAttributes.put(SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE,
                "org.apache.aries.mytest.MySPI");
        firstAttributes.put("decorator", "first");
        firstAttributes.put("longValue", Long.valueOf(7));
        firstAttributes.put("doubleValue", Double.valueOf(2.5));
        firstAttributes.put("versionValue", new Version("1.2.3"));
        firstAttributes.put("listValue", Arrays.asList(Long.valueOf(1), Long.valueOf(2)));
        firstAttributes.put(Constants.SERVICE_SCOPE, Constants.SCOPE_PROTOTYPE);
        firstAttributes.put(".private", "hidden");
        firstAttributes.put(SpiFlyConstants.SERVICELOADER_MEDIATOR_PROPERTY, Long.valueOf(99));
        Map<String, String> firstDirectives = new HashMap<String, String>();
        firstDirectives.put("register",
                "org.apache.aries.spifly.impl1.MySPIImpl1");
        firstDirectives.put("effective", "active");

        Map<String, Object> secondAttributes = new HashMap<String, Object>();
        secondAttributes.put(SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE,
                "org.apache.aries.mytest.MySPI");
        secondAttributes.put("decorator", "second");
        Map<String, String> secondDirectives = Collections.singletonMap(
                "register",
                "org.apache.aries.spifly.impl1.MySPIImpl1");

        BundleWiring wiring = mockProviderWiring(Arrays.asList(
                mockCapability(firstAttributes, firstDirectives),
                mockCapability(secondAttributes, secondDirectives)), true, 42L);
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.PROVIDER_REQUIREMENT);
        headers.put(SpiFlyConstants.PROVIDE_CAPABILITY,
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + ";"
                + SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE
                + "=org.apache.aries.mytest.MySPI");
        Bundle implBundle = mockSPIBundle(implBC, headers, null, wiring);

        @SuppressWarnings("rawtypes")
        List<ServiceRegistration> registrations = customizer.addingBundle(implBundle, null);

        assertEquals(2, registrations.size());
        Set<Object> decorators = new HashSet<Object>();
        for (@SuppressWarnings("rawtypes") ServiceRegistration registration : registrations) {
            Object serviceObject = ((ServiceRegistrationImpl) registration).getServiceObject();
            assertTrue(serviceObject instanceof ProviderServiceFactory);
            assertFalse(serviceObject instanceof ProviderPrototypeServiceFactory);
            ServiceReference<?> reference = registration.getReference();
            decorators.add(reference.getProperty("decorator"));
            assertEquals(Long.valueOf(42), reference.getProperty(
                    SpiFlyConstants.SERVICELOADER_MEDIATOR_PROPERTY));
            assertNull(reference.getProperty(".private"));
            assertNull(reference.getProperty("effective"));

            if ("first".equals(reference.getProperty("decorator"))) {
                assertEquals(Long.valueOf(7), reference.getProperty("longValue"));
                assertEquals(Double.valueOf(2.5), reference.getProperty("doubleValue"));
                assertEquals(new Version("1.2.3"), reference.getProperty("versionValue"));
                assertEquals(Arrays.asList(Long.valueOf(1), Long.valueOf(2)),
                        reference.getProperty("listValue"));
            }
        }
        assertEquals(new HashSet<Object>(Arrays.<Object>asList("first", "second")), decorators);
    }

    @Test
    public void testServiceSubsetSelectionAndRegistrationProperties() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42l).anyTimes();
        EasyMock.replay(mediatorBundle);

        BaseActivator activator = new BaseActivator() {
            @Override
            public void start(BundleContext context) throws Exception {}
        };

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, mediatorBundle);

        BundleContext implBC = mockSPIBundleContext4();
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.PROVIDER_REQUIREMENT);
        headers.put(SpiFlyConstants.PROVIDE_CAPABILITY, SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "; " +
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "=org.apache.aries.mytest.MySPI2; approval=yeah; " +
                SpiFlyConstants.REGISTER_DIRECTIVE + "=\"org.apache.aries.spifly.impl4.MySPIImpl4b\"");
        Bundle implBundle = mockSPIBundle4(implBC, headers);

        @SuppressWarnings("rawtypes")
        List<ServiceRegistration> registrations = customizer.addingBundle(implBundle, null);
        assertEquals(1, registrations.size());

        String[] objectClassProp = (String [])registrations.iterator().next().getReference().getProperty(Constants.OBJECTCLASS);
        assertEquals(1, objectClassProp.length);
        assertEquals("org.apache.aries.mytest.MySPI2", objectClassProp[0]);
        assertNotNull(registrations.iterator().next().getReference().getProperty(SpiFlyConstants.SERVICELOADER_MEDIATOR_PROPERTY));
        assertEquals("yeah", registrations.iterator().next().getReference().getProperty("approval"));
    }

    @Test
    public void testProvidedSPIDirective() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42l).anyTimes();
        EasyMock.replay(mediatorBundle);

        BaseActivator activator = new BaseActivator() {
            @Override
            public void start(BundleContext context) throws Exception {}
        };

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, mediatorBundle);

        BundleContext implBC = mockSPIBundleContext4();
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.PROVIDER_REQUIREMENT);
        headers.put(SpiFlyConstants.PROVIDE_CAPABILITY,
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "; " +
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "=org.apache.aries.mytest.MySPI; approval=yeah, " +
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "; " +
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "=org.apache.aries.mytest.MySPI2");
        Bundle implBundle = mockSPIBundle4(implBC, headers);

        @SuppressWarnings("rawtypes")
        List<ServiceRegistration> registrations = customizer.addingBundle(implBundle, null);
        assertEquals("Expected 3 registrations, one for MySPI and 2 for MySPI2", 3, registrations.size());
        Set<String> expectedObjectClasses = new HashSet<String>(Arrays.asList("org.apache.aries.mytest.MySPI", "org.apache.aries.mytest.MySPI2"));
        Set<String> actualObjectClasses = new HashSet<String>();

        boolean foundMySPI = false;
        boolean foundMySPI2 = false;
        for (@SuppressWarnings("rawtypes") ServiceRegistration sr : registrations) {
            List<String> objectClasses = Arrays.asList((String[]) sr.getReference().getProperty(Constants.OBJECTCLASS));
            actualObjectClasses.addAll(objectClasses);
            assertNotNull(sr.getReference().getProperty(SpiFlyConstants.SERVICELOADER_MEDIATOR_PROPERTY));
            if (objectClasses.contains("org.apache.aries.mytest.MySPI")) {
                assertEquals("yeah", sr.getReference().getProperty("approval"));
                foundMySPI = true;
            } else if (objectClasses.contains("org.apache.aries.mytest.MySPI2")) {
                assertNull(sr.getReference().getProperty("approval"));
                foundMySPI2 = true;
            }
        }
        assertTrue(foundMySPI);
        assertTrue(foundMySPI2);

        assertEquals(expectedObjectClasses, actualObjectClasses);
    }

    @Test
    public void testMultipleServiceInstancesAndTypes() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42l).anyTimes();
        EasyMock.replay(mediatorBundle);

        BaseActivator activator = new BaseActivator() {
            @Override
            public void start(BundleContext context) throws Exception {}
        };

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, mediatorBundle);

        BundleContext implBC = mockSPIBundleContext4();
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.PROVIDER_REQUIREMENT);
        headers.put(SpiFlyConstants.PROVIDE_CAPABILITY,
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "; " +
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "=org.apache.aries.mytest.MySPI," +
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "; " +
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "=org.apache.aries.mytest.MySPI2");
        Bundle implBundle = mockSPIBundle4(implBC, headers);

        @SuppressWarnings("rawtypes")
        List<ServiceRegistration> registrations = customizer.addingBundle(implBundle, null);
        assertEquals(3, registrations.size());

        boolean foundA = false, foundB = false, foundC = false;
        for (@SuppressWarnings("rawtypes") ServiceRegistration sreg : registrations) {
            @SuppressWarnings("rawtypes")
            ServiceReference sref = sreg.getReference();
            String objectClassName = ((String [])sref.getProperty(Constants.OBJECTCLASS))[0];
            String serviceImplClassName = (String) sref.getProperty(SpiFlyConstants.PROVIDER_IMPLCLASS_PROPERTY);
            if (MySPIImpl4a.class.getName().equals(serviceImplClassName)) {
                assertEquals("org.apache.aries.mytest.MySPI", objectClassName);

                @SuppressWarnings("unchecked")
                MySPI svc = (MySPI) implBC.getService(sreg.getReference());
                assertEquals("impl4a", svc.someMethod(""));

                foundA = true;
            } else if (MySPIImpl4b.class.getName().equals(serviceImplClassName)) {
                assertEquals("org.apache.aries.mytest.MySPI2", objectClassName);

                @SuppressWarnings("unchecked")
                MySPI2 svc = (MySPI2) implBC.getService(sreg.getReference());
                assertEquals("impl4b", svc.someMethod(""));

                foundB = true;
            } else if (MySPIImpl4c.class.getName().equals(serviceImplClassName)) {
                assertEquals("org.apache.aries.mytest.MySPI2", objectClassName);

                @SuppressWarnings("unchecked")
                MySPI2 svc = (MySPI2) implBC.getService(sreg.getReference());
                assertEquals("impl4c", svc.someMethod(""));

                foundC = true;
            }
        }

        assertTrue(foundA);
        assertTrue(foundB);
        assertTrue(foundC);
    }

    @Test
    public void testPublishedProviderWithoutRegistrarWireIsNotRegisteredAsService() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42l).anyTimes();
        EasyMock.replay(mediatorBundle);
        BaseActivator activator = new BaseActivator() {
            @Override
            public void start(BundleContext context) throws Exception {}
        };

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, mediatorBundle);

        @SuppressWarnings("rawtypes")
        ServiceRegistration sreg = EasyMock.createMock(ServiceRegistration.class);
        EasyMock.replay(sreg);

        BundleContext implBC = mockSPIBundleContext(sreg);
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.PROVIDE_CAPABILITY,
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "; " +
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "=org.apache.aries.mytest.MySPI");
        Bundle implBundle = mockSPIBundle(implBC, headers);

        @SuppressWarnings("rawtypes")
        List<ServiceRegistration> registrations = customizer.addingBundle(implBundle, null);
        assertEquals(0, registrations.size());
        Collection<Bundle> bundles = activator.findProviderBundles("org.apache.aries.mytest.MySPI");
        assertEquals(1, bundles.size());
        assertSame(implBundle, bundles.iterator().next());
    }

    @Test
    public void testRegistrarWireToAnotherMediatorDoesNotRegisterServices() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42L).anyTimes();
        EasyMock.replay(mediatorBundle);
        BaseActivator activator = new BaseActivator() {
            @Override
            public void start(BundleContext context) throws Exception {}
        };
        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(
                activator, mediatorBundle);

        @SuppressWarnings("rawtypes")
        ServiceRegistration registration = EasyMock.createNiceMock(ServiceRegistration.class);
        BundleContext implBC = mockSPIBundleContext(registration);
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.PROVIDER_REQUIREMENT);
        headers.put(SpiFlyConstants.PROVIDE_CAPABILITY,
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "; " +
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "=org.apache.aries.mytest.MySPI");
        Bundle implBundle = mockSPIBundle(implBC, headers, null,
                mockProviderWiring(headers, null, 99L));

        @SuppressWarnings("rawtypes")
        List<ServiceRegistration> registrations = customizer.addingBundle(implBundle, null);

        assertTrue(registrations.isEmpty());
        assertProviderBundle(activator, "org.apache.aries.mytest.MySPI", implBundle);
    }

    @Test
    public void testProviderWithoutRegisterPermissionRemainsCandidate() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42L).anyTimes();
        EasyMock.replay(mediatorBundle);
        BaseActivator activator = new BaseActivator() {
            @Override
            public void start(BundleContext context) throws Exception {}
        };
        ProviderBundleTrackerCustomizer customizer =
                new ProviderBundleTrackerCustomizer(activator, mediatorBundle);

        BundleContext implBC = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(implBC);
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.PROVIDER_REQUIREMENT);
        headers.put(SpiFlyConstants.PROVIDE_CAPABILITY,
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "; "
                + SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE
                + "=org.apache.aries.mytest.MySPI");
        Bundle implBundle = mockSPIBundle(implBC, headers, null,
                mockProviderWiring(headers, null), false);

        @SuppressWarnings("rawtypes")
        List<ServiceRegistration> registrations = customizer.addingBundle(implBundle, null);

        assertTrue(registrations.isEmpty());
        assertProviderBundle(activator,
                "org.apache.aries.mytest.MySPI", implBundle);
        EasyMock.verify(implBC);
    }

    @SuppressWarnings({ "resource", "unchecked" })
    @Test
    public void testAddingBundleWithBundleClassPath() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42l).anyTimes();
        EasyMock.replay(mediatorBundle);
        BaseActivator activator = new BaseActivator() {
            @Override
            public void start(BundleContext context) throws Exception {}
        };

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
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.PROVIDER_REQUIREMENT);
        headers.put(SpiFlyConstants.PROVIDE_CAPABILITY,
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "; " +
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "=org.apache.aries.mytest.MySPI");
        headers.put(Constants.BUNDLE_CLASSPATH, ".,non-jar.jar,embedded.jar,embedded2.jar");
        EasyMock.expect(implBundle.getHeaders()).andReturn(headers).anyTimes();
        EasyMock.expect(implBundle.adapt(BundleWiring.class)).andReturn(
                mockProviderWiring(headers, null)).anyTimes();

        URL embeddedJar = getClass().getResource("/embedded.jar");
        assertNotNull("precondition", embeddedJar);
        EasyMock.expect(implBundle.getResource("embedded.jar")).andReturn(embeddedJar).anyTimes();
        URL embedded2Jar = getClass().getResource("/embedded2.jar");
        assertNotNull("precondition", embedded2Jar);
        EasyMock.expect(implBundle.getResource("embedded2.jar")).andReturn(embedded2Jar).anyTimes();
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
        customizer.addingBundle(implBundle, null);
        Collection<Bundle> bundles = activator.findProviderBundles("org.apache.aries.mytest.MySPI");
        assertEquals(1, bundles.size());
        assertSame(implBundle, bundles.iterator().next());

        // The bc.registerService() call should now have been made
        EasyMock.verify(implBC);
    }

    @SuppressWarnings("unchecked")
    private BundleContext mockSPIBundleContext(@SuppressWarnings("rawtypes") ServiceRegistration sreg) {
        BundleContext implBC = EasyMock.createMock(BundleContext.class);
        EasyMock.<Object>expect(implBC.registerService(
                EasyMock.eq("org.apache.aries.mytest.MySPI"),
                EasyMock.isA(ServiceFactory.class),
                (Dictionary<String,?>) EasyMock.anyObject())).andReturn(sreg);
        EasyMock.replay(implBC);
        return implBC;
    }

    private Bundle mockSPIBundle(BundleContext implBC, String spiProviderHeader) throws ClassNotFoundException {
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, spiProviderHeader);
        return mockSPIBundle(implBC, headers);
    }

    private Bundle mockSPIBundle(BundleContext implBC, Dictionary<String, String> headers) throws ClassNotFoundException {
        return mockSPIBundle(implBC, headers, null);
    }

    private Bundle mockSPIBundle(BundleContext implBC, Dictionary<String, String> headers, BundleRevision rev) throws ClassNotFoundException {
        return mockSPIBundle(implBC, headers, rev, mockProviderWiring(headers, rev));
    }

    private Bundle mockSPIBundle(BundleContext implBC, Dictionary<String, String> headers,
            BundleRevision rev, BundleWiring providerWiring) throws ClassNotFoundException {
        return mockSPIBundle(implBC, headers, rev, providerWiring, true);
    }

    private Bundle mockSPIBundle(BundleContext implBC, Dictionary<String, String> headers,
            BundleRevision rev, BundleWiring providerWiring, boolean registerPermission)
            throws ClassNotFoundException {
        if (headers == null)
            headers = new Hashtable<String, String>();

        Bundle implBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(implBundle.getBundleContext()).andReturn(implBC).anyTimes();
        EasyMock.expect(implBundle.hasPermission(EasyMock.isA(ServicePermission.class)))
                .andReturn(registerPermission).anyTimes();
        EasyMock.expect(implBundle.getHeaders()).andReturn(headers).anyTimes();
        EasyMock.expect(implBundle.getSymbolicName()).andReturn("bsn").anyTimes();

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

        if (rev != null)
            EasyMock.expect(implBundle.adapt(BundleRevision.class)).andReturn(rev).anyTimes();
        EasyMock.expect(implBundle.adapt(BundleWiring.class)).andReturn(providerWiring).anyTimes();

        EasyMock.replay(implBundle);
        return implBundle;
    }

    @SuppressWarnings("unchecked")
    private BundleContext mockSPIBundleContext4() {
        BundleContext implBC = EasyMock.createNiceMock(BundleContext.class);

        implBC.registerService(EasyMock.anyString(),
                               EasyMock.anyObject(),
                               (Dictionary<String,?>)EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<ServiceRegistration<Object>>() {
            @Override
            public ServiceRegistration<Object> answer() throws Throwable {
                final String className = (String) EasyMock.getCurrentArguments()[0];
                final Object serviceObject = EasyMock.getCurrentArguments()[1];
                final Dictionary<String, Object> registrationProps =
                    (Dictionary<String, Object>) EasyMock.getCurrentArguments()[2];
                return new ServiceRegistrationImpl(className, serviceObject, registrationProps);
            }
        }).anyTimes();
        implBC.getService(EasyMock.anyObject(ServiceReference.class));
        EasyMock.expectLastCall().
            andAnswer(new IAnswer<Object>() {
                @SuppressWarnings("rawtypes")
                @Override
                public Object answer() throws Throwable {
                    ServiceRegistrationImpl reg = (ServiceRegistrationImpl) EasyMock.getCurrentArguments()[0];
                    Object svc = reg.getServiceObject();
                    if (svc instanceof ServiceFactory) {
                        return ((ServiceFactory) svc).getService(null, reg);
                    } else {
                        return svc;
                    }
                }
            }).anyTimes();

        EasyMock.replay(implBC);
        return implBC;
    }

    private Bundle mockSPIBundle4(BundleContext implBC, Dictionary<String, String> headers) throws ClassNotFoundException {
        return mockSPIBundle4(implBC, headers, null);
    }

    private Bundle mockSPIBundle4(BundleContext implBC, Dictionary<String, String> headers, BundleRevision rev) throws ClassNotFoundException {
        return mockSPIBundle4(implBC, headers, rev, mockProviderWiring(headers, rev));
    }

    private Bundle mockSPIBundle4(BundleContext implBC, Dictionary<String, String> headers,
            BundleRevision rev, BundleWiring providerWiring) throws ClassNotFoundException {
        Bundle implBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(implBundle.getBundleContext()).andReturn(implBC).anyTimes();
        EasyMock.expect(implBundle.hasPermission(EasyMock.isA(ServicePermission.class)))
                .andReturn(true).anyTimes();
        EasyMock.expect(implBundle.getHeaders()).andReturn(headers).anyTimes();
        if (rev != null)
            EasyMock.expect(implBundle.adapt(BundleRevision.class)).andReturn(rev).anyTimes();
        EasyMock.expect(implBundle.adapt(BundleWiring.class)).andReturn(providerWiring).anyTimes();

        // List the resources found at META-INF/services in the test bundle
        URL dir = getClass().getResource("impl4/META-INF/services");
        assertNotNull("precondition", dir);
        EasyMock.expect(implBundle.getResource("/META-INF/services")).andReturn(dir).anyTimes();
        URL res = getClass().getResource("impl4/META-INF/services/org.apache.aries.mytest.MySPI");
        assertNotNull("precondition", res);
        URL res2 = getClass().getResource("impl4/META-INF/services/org.apache.aries.mytest.MySPI2");
        assertNotNull("precondition", res2);

        EasyMock.expect(implBundle.findEntries("META-INF/services", "*", false)).andReturn(
                Collections.enumeration(Arrays.asList(res, res2))).anyTimes();

        Class<?> clsa = getClass().getClassLoader().loadClass("org.apache.aries.spifly.impl4.MySPIImpl4a");
        EasyMock.<Object>expect(implBundle.loadClass("org.apache.aries.spifly.impl4.MySPIImpl4a")).andReturn(clsa).anyTimes();
        Class<?> clsb = getClass().getClassLoader().loadClass("org.apache.aries.spifly.impl4.MySPIImpl4b");
        EasyMock.<Object>expect(implBundle.loadClass("org.apache.aries.spifly.impl4.MySPIImpl4b")).andReturn(clsb).anyTimes();
        Class<?> clsc = getClass().getClassLoader().loadClass("org.apache.aries.spifly.impl4.MySPIImpl4c");
        EasyMock.<Object>expect(implBundle.loadClass("org.apache.aries.spifly.impl4.MySPIImpl4c")).andReturn(clsc).anyTimes();


        EasyMock.replay(implBundle);
        return implBundle;
    }

    private Bundle mockFragment(Dictionary<String, String> headers) {
        Bundle fragment = EasyMock.createMock(Bundle.class);
        EasyMock.expect(fragment.getHeaders()).andReturn(headers).anyTimes();
        EasyMock.replay(fragment);
        return fragment;
    }

    private BundleRevision mockHostRevision(Bundle... fragments) {
        List<BundleWire> wires = new ArrayList<BundleWire>();
        for (Bundle fragment : fragments) {
            BundleRevision fragmentRevision = EasyMock.createMock(BundleRevision.class);
            EasyMock.expect(fragmentRevision.getBundle()).andReturn(fragment).anyTimes();
            EasyMock.replay(fragmentRevision);

            BundleRequirement requirement = EasyMock.createMock(BundleRequirement.class);
            EasyMock.expect(requirement.getRevision()).andReturn(fragmentRevision).anyTimes();
            EasyMock.replay(requirement);

            BundleWire wire = EasyMock.createMock(BundleWire.class);
            EasyMock.expect(wire.getRequirement()).andReturn(requirement).anyTimes();
            EasyMock.replay(wire);
            wires.add(wire);
        }

        BundleWiring wiring = EasyMock.createMock(BundleWiring.class);
        EasyMock.expect(wiring.getProvidedWires("osgi.wiring.host")).andReturn(wires).anyTimes();
        EasyMock.replay(wiring);

        BundleRevision revision = EasyMock.createMock(BundleRevision.class);
        EasyMock.expect(revision.getWiring()).andReturn(wiring).anyTimes();
        EasyMock.expect(revision.getTypes()).andReturn(0).anyTimes();
        EasyMock.replay(revision);
        return revision;
    }

    private BundleWiring mockProviderWiring(Dictionary<String, String> hostHeaders, BundleRevision hostRevision) {
        return mockProviderWiring(hostHeaders, hostRevision, 42L);
    }

    private BundleWiring mockProviderWiring(Dictionary<String, String> hostHeaders,
            BundleRevision hostRevision, long mediatorBundleId) {
        if (hostHeaders == null) {
            hostHeaders = new Hashtable<String, String>();
        }
        List<Dictionary<String, String>> headerSources = new ArrayList<Dictionary<String, String>>();
        headerSources.add(hostHeaders);
        if (hostRevision != null) {
            for (BundleWire hostWire : hostRevision.getWiring().getProvidedWires("osgi.wiring.host")) {
                @SuppressWarnings("unchecked")
                Dictionary<String, String> fragmentHeaders =
                        hostWire.getRequirement().getRevision().getBundle().getHeaders();
                headerSources.add(fragmentHeaders);
            }
        }

        List<BundleCapability> capabilities = new ArrayList<BundleCapability>();
        boolean registrarRequired = false;
        for (Dictionary<String, String> headers : headerSources) {
            String requireCapability = headers.get(SpiFlyConstants.REQUIRE_CAPABILITY);
            registrarRequired |= requireCapability != null
                    && requireCapability.contains(SpiFlyConstants.REGISTRAR_EXTENDER_NAME);

            String provideCapability = headers.get(SpiFlyConstants.PROVIDE_CAPABILITY);
            if (provideCapability == null) {
                continue;
            }
            Parameters parameters = new Parameters(provideCapability);
            for (Map.Entry<String, aQute.bnd.header.Attrs> entry : parameters.entrySet()) {
                if (!SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE.equals(
                        ConsumerHeaderProcessor.removeDuplicateMarker(entry.getKey()))) {
                    continue;
                }
                Map<String, Object> attributes = new HashMap<String, Object>();
                Map<String, String> directives = new HashMap<String, String>();
                for (String name : entry.getValue().keySet()) {
                    if (aQute.bnd.header.Attrs.isDirective(name)) {
                        directives.put(name.substring(0, name.length() - 1),
                                entry.getValue().get(name));
                    }
                    else {
                        attributes.put(name, entry.getValue().getTyped(name));
                    }
                }
                capabilities.add(mockCapability(attributes, directives));
            }
        }

        return mockProviderWiring(capabilities, registrarRequired, mediatorBundleId);
    }

    private BundleWiring mockProviderWiring(List<BundleCapability> capabilities,
            boolean registrarRequired, long mediatorBundleId) {
        List<BundleWire> extenderWires = registrarRequired
                ? Collections.singletonList(mockExtenderWire(
                        SpiFlyConstants.REGISTRAR_EXTENDER_NAME, mediatorBundleId))
                : Collections.<BundleWire>emptyList();
        BundleWiring wiring = EasyMock.createNiceMock(BundleWiring.class);
        EasyMock.expect(wiring.getCapabilities(SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE))
                .andReturn(capabilities).anyTimes();
        EasyMock.expect(wiring.getRequiredWires(SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE))
                .andReturn(extenderWires).anyTimes();
        EasyMock.replay(wiring);
        return wiring;
    }

    private BundleCapability mockCapability(Map<String, Object> attributes,
            Map<String, String> directives) {
        BundleCapability capability = EasyMock.createNiceMock(BundleCapability.class);
        EasyMock.expect(capability.getAttributes()).andReturn(attributes).anyTimes();
        EasyMock.expect(capability.getDirectives()).andReturn(directives).anyTimes();
        EasyMock.replay(capability);
        return capability;
    }

    private BundleWire mockExtenderWire(String extenderName, long mediatorBundleId) {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE, extenderName);
        BundleCapability capability = EasyMock.createNiceMock(BundleCapability.class);
        EasyMock.expect(capability.getAttributes()).andReturn(attributes).anyTimes();
        EasyMock.replay(capability);

        Bundle provider = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(provider.getBundleId()).andReturn(mediatorBundleId).anyTimes();
        EasyMock.replay(provider);
        BundleWiring providerWiring = EasyMock.createNiceMock(BundleWiring.class);
        EasyMock.expect(providerWiring.getBundle()).andReturn(provider).anyTimes();
        EasyMock.replay(providerWiring);

        BundleWire wire = EasyMock.createNiceMock(BundleWire.class);
        EasyMock.expect(wire.getCapability()).andReturn(capability).anyTimes();
        EasyMock.expect(wire.getProviderWiring()).andReturn(providerWiring).anyTimes();
        EasyMock.replay(wire);
        return wire;
    }

    private void assertProviderBundle(BaseActivator activator, String serviceName, Bundle implBundle) {
        Collection<Bundle> bundles = activator.findProviderBundles(serviceName);
        assertEquals(1, bundles.size());
        assertSame(implBundle, bundles.iterator().next());
    }

    @SuppressWarnings("rawtypes")
    private static class ServiceRegistrationImpl implements ServiceRegistration<Object>, ServiceReference {
        private final Object serviceObject;
        private final Dictionary<String, Object> properties;

        public ServiceRegistrationImpl(String className, Object serviceObject, Dictionary<String, Object> properties) {
            this.serviceObject = serviceObject;
            this.properties = properties;
            this.properties.put(Constants.OBJECTCLASS, new String[] {className});
        }

        Object getServiceObject() {
            return serviceObject;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ServiceReference<Object> getReference() {
            return this;
        }

        @Override
        public void setProperties(Dictionary<String, ?> properties) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unregister() {
        }

        @Override
        public Object getProperty(String key) {
            return properties.get(key);
        }

        @Override
        public String[] getPropertyKeys() {
            return Collections.list(properties.keys()).toArray(new String [] {});
        }

        @Override
        public Bundle getBundle() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle[] getUsingBundles() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAssignableTo(Bundle bundle, String className) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int compareTo(Object reference) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Dictionary<String, Object> getProperties() {
            Hashtable<String, Object> copy = new Hashtable<>();
            Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                copy.put(key, properties.get(key));
            }
            return copy;
        }

        @Override
        public Object adapt(Class aClass) {
            return null;
        }
    }
}
