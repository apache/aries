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
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;

public class ResolvedWiringTest {
    private static final String SERVICE_TYPE = "org.example.Service";
    private static final String PROCESSED_REQUIRE_CAPABILITY_HEADER =
            "X-SpiFly-Processed-Require-Capability";

    private final BaseActivator activator = new BaseActivator() {
        @Override
        public void start(BundleContext context) throws Exception {}
    };
    private Bundle mediator;

    @Before
    public void setUp() throws Exception {
        mediator = mockBundle(42L);
        BundleContext context = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(context.getBundle()).andReturn(mediator).anyTimes();
        EasyMock.replay(context);

        setBundleContext(context);
    }

    @Test
    public void ignoresProcessorRequirementThatIsNotWired() throws Exception {
        BundleWiring wiring = mockConsumerWiring(
                Collections.<BundleWire>emptyList(),
                Collections.<BundleRequirement>emptyList(),
                Collections.<BundleWire>emptyList());
        Bundle consumer = mockConsumer(wiring);

        activator.addConsumerWeavingData(consumer, SpiFlyConstants.SPI_CONSUMER_HEADER);

        assertNull(activator.getWeavingData(consumer));
    }

    @Test
    public void ignoresProcessorWireToAnotherMediator() throws Exception {
        BundleWiring wiring = mockConsumerWiring(
                Collections.singletonList(mockWire(
                        SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE,
                        SpiFlyConstants.PROCESSOR_EXTENDER_NAME, mockBundle(99L))),
                Collections.<BundleRequirement>emptyList(),
                Collections.<BundleWire>emptyList());
        Bundle consumer = mockConsumer(wiring);

        activator.addConsumerWeavingData(consumer, SpiFlyConstants.SPI_CONSUMER_HEADER);

        assertNull(activator.getWeavingData(consumer));
    }

    @Test
    public void restrictsConsumerToActuallyWiredProvider() throws Exception {
        Bundle selectedProvider = mockBundle(7L);
        BundleRequirement serviceRequirement = EasyMock.createNiceMock(BundleRequirement.class);
        EasyMock.replay(serviceRequirement);
        BundleWiring wiring = mockConsumerWiring(
                Collections.singletonList(mockWire(
                        SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE,
                        SpiFlyConstants.PROCESSOR_EXTENDER_NAME, mediator)),
                Collections.singletonList(serviceRequirement),
                Collections.singletonList(mockWire(
                        SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE,
                        SERVICE_TYPE, selectedProvider)));
        Bundle consumer = mockConsumer(wiring);

        activator.addConsumerWeavingData(consumer, SpiFlyConstants.SPI_CONSUMER_HEADER);

        assertNotNull(activator.getWeavingData(consumer));
        Collection<Bundle> providers = activator.findConsumerRestrictions(
                consumer, ServiceLoader.class.getName(), "load", serviceArguments(SERVICE_TYPE));
        assertEquals(Collections.singleton(selectedProvider), providers);
    }

    @Test
    public void declaredButUnwiredServiceRequirementAllowsNoProviders() throws Exception {
        BundleRequirement serviceRequirement = EasyMock.createNiceMock(BundleRequirement.class);
        EasyMock.replay(serviceRequirement);
        BundleWiring wiring = mockConsumerWiring(
                Collections.singletonList(mockWire(
                        SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE,
                        SpiFlyConstants.PROCESSOR_EXTENDER_NAME, mediator)),
                Collections.singletonList(serviceRequirement),
                Collections.<BundleWire>emptyList());
        Bundle consumer = mockConsumer(wiring);

        activator.addConsumerWeavingData(consumer, SpiFlyConstants.SPI_CONSUMER_HEADER);

        assertEquals(Collections.emptySet(), activator.findConsumerRestrictions(
                consumer, ServiceLoader.class.getName(), "load", serviceArguments(SERVICE_TYPE)));
        assertEquals(Collections.emptySet(), activator.findConsumerRestrictions(
                consumer, ServiceLoader.class.getName(), "loadInstalled",
                serviceArguments(SERVICE_TYPE)));
    }

    @Test
    public void optionalDeclaredButDiscardedRequirementAllowsNoProviders()
            throws Exception {
        BundleRequirement declaredRequirement =
                EasyMock.createNiceMock(BundleRequirement.class);
        EasyMock.replay(declaredRequirement);
        BundleRevision hostRevision = mockRevision(
                Collections.singletonList(declaredRequirement));
        BundleWiring wiring = mockConsumerWiring(
                Collections.singletonList(mockWire(
                        SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE,
                        SpiFlyConstants.PROCESSOR_EXTENDER_NAME, mediator)),
                Collections.<BundleRequirement>emptyList(),
                Collections.<BundleWire>emptyList(), hostRevision,
                Collections.<BundleWire>emptyList());
        Bundle consumer = mockConsumer(wiring);

        activator.addConsumerWeavingData(consumer, SpiFlyConstants.SPI_CONSUMER_HEADER);

        assertEquals(Collections.emptySet(), activator.findConsumerRestrictions(
                consumer, ServiceLoader.class.getName(), "load",
                serviceArguments(SERVICE_TYPE)));
    }

    @Test
    public void fragmentDeclaredButDiscardedRequirementAllowsNoProviders()
            throws Exception {
        BundleRequirement declaredRequirement =
                EasyMock.createNiceMock(BundleRequirement.class);
        EasyMock.replay(declaredRequirement);
        BundleRevision fragmentRevision = mockRevision(
                Collections.singletonList(declaredRequirement));
        BundleRequirement hostRequirement = EasyMock.createNiceMock(BundleRequirement.class);
        EasyMock.expect(hostRequirement.getRevision())
                .andReturn(fragmentRevision).anyTimes();
        EasyMock.replay(hostRequirement);
        BundleWire hostWire = EasyMock.createNiceMock(BundleWire.class);
        EasyMock.expect(hostWire.getRequirement()).andReturn(hostRequirement).anyTimes();
        EasyMock.replay(hostWire);

        BundleWiring wiring = mockConsumerWiring(
                Collections.singletonList(mockWire(
                        SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE,
                        SpiFlyConstants.PROCESSOR_EXTENDER_NAME, mediator)),
                Collections.<BundleRequirement>emptyList(),
                Collections.<BundleWire>emptyList(),
                mockRevision(Collections.<BundleRequirement>emptyList()),
                Collections.singletonList(hostWire));
        Bundle consumer = mockConsumer(wiring);

        activator.addConsumerWeavingData(consumer, SpiFlyConstants.SPI_CONSUMER_HEADER);

        assertEquals(Collections.emptySet(), activator.findConsumerRestrictions(
                consumer, ServiceLoader.class.getName(), "loadInstalled",
                serviceArguments(SERVICE_TYPE)));
    }

    @Test
    public void consumerWithoutServiceRequirementCanSeeAllPublishedProviders() throws Exception {
        BundleWiring wiring = mockConsumerWiring(
                Collections.singletonList(mockWire(
                        SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE,
                        SpiFlyConstants.PROCESSOR_EXTENDER_NAME, mediator)),
                Collections.<BundleRequirement>emptyList(),
                Collections.<BundleWire>emptyList());
        Bundle consumer = mockConsumer(wiring);

        activator.addConsumerWeavingData(consumer, SpiFlyConstants.SPI_CONSUMER_HEADER);

        assertNull(activator.findConsumerRestrictions(
                consumer, ServiceLoader.class.getName(), "load", serviceArguments(SERVICE_TYPE)));
        assertNull(activator.findConsumerRestrictions(
                consumer, ServiceLoader.class.getName(), "loadInstalled",
                serviceArguments(SERVICE_TYPE)));
    }

    @Test
    public void unrestrictedConsumerOnlySeesTypeSpaceCompatibleProviders() throws Exception {
        BundleWiring wiring = mockConsumerWiring(
                Collections.singletonList(mockWire(
                        SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE,
                        SpiFlyConstants.PROCESSOR_EXTENDER_NAME, mediator)),
                Collections.<BundleRequirement>emptyList(),
                Collections.<BundleWire>emptyList());
        Bundle consumer = mockConsumer(wiring);
        Bundle compatible = mockProvider(7L, TestService.class.getName(), TestService.class);
        Bundle incompatible = mockProvider(8L, TestService.class.getName(), String.class);

        activator.addConsumerWeavingData(consumer, SpiFlyConstants.SPI_CONSUMER_HEADER);

        assertEquals(Collections.singletonList(compatible),
                activator.filterCompatibleProviderBundles(consumer, TestService.class,
                        Arrays.asList(compatible, incompatible)));
    }

    @Test
    public void staticallyWovenConsumerWiredToAnotherMediatorIsDenied() throws Exception {
        BundleWiring wiring = mockConsumerWiring(
                Collections.singletonList(mockWire(
                        SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE,
                        SpiFlyConstants.PROCESSOR_EXTENDER_NAME, mockBundle(99L))),
                Collections.<BundleRequirement>emptyList(),
                Collections.<BundleWire>emptyList());
        Bundle consumer = mockConsumer(wiring, true, true);

        activator.addConsumerWeavingData(
                consumer, SpiFlyConstants.PROCESSED_SPI_CONSUMER_HEADER);

        assertNull(activator.getWeavingData(consumer));
        assertEquals(Collections.emptySet(), activator.findConsumerRestrictions(
                consumer, ServiceLoader.class.getName(), "load", serviceArguments(SERVICE_TYPE)));
    }

    @Test
    public void legacyStaticConsumerWithoutProcessorRequirementRemainsSupported() throws Exception {
        Bundle selectedProvider = mockBundle(7L);
        BundleRequirement serviceRequirement = EasyMock.createNiceMock(BundleRequirement.class);
        EasyMock.replay(serviceRequirement);
        BundleWiring wiring = mockConsumerWiring(
                Collections.<BundleWire>emptyList(),
                Collections.singletonList(serviceRequirement),
                Collections.singletonList(mockWire(
                        SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE,
                        SERVICE_TYPE, selectedProvider)));
        Bundle consumer = mockConsumer(wiring, true, false);

        activator.addConsumerWeavingData(
                consumer, SpiFlyConstants.PROCESSED_SPI_CONSUMER_HEADER);

        assertNotNull(activator.getWeavingData(consumer));
        assertEquals(Collections.singleton(selectedProvider), activator.findConsumerRestrictions(
                consumer, ServiceLoader.class.getName(), "load", serviceArguments(SERVICE_TYPE)));
    }

    @Test
    public void resolvedFragmentTriggersHostRefreshPath() {
        final boolean[] attached = new boolean[1];
        BaseActivator fragmentActivator = new BaseActivator() {
            @Override
            public void start(BundleContext context) throws Exception {}

            @Override
            void fragmentAttached(Bundle fragment, String consumerHeaderName) {
                attached[0] = true;
            }
        };
        BundleRevision revision = EasyMock.createNiceMock(BundleRevision.class);
        EasyMock.expect(revision.getTypes()).andReturn(
                BundleRevision.TYPE_FRAGMENT).anyTimes();
        EasyMock.replay(revision);
        Bundle fragment = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(fragment.adapt(BundleRevision.class))
                .andReturn(revision).anyTimes();
        EasyMock.replay(fragment);

        new ConsumerBundleTrackerCustomizer(fragmentActivator,
                SpiFlyConstants.SPI_CONSUMER_HEADER).addingBundle(fragment, null);

        assertTrue(attached[0]);
    }

    @Test
    public void lateProcessorFragmentRefreshesConsumerHostOnce() throws Exception {
        BundleWiring wiring = mockConsumerWiring(
                Collections.singletonList(mockWire(
                        SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE,
                        SpiFlyConstants.PROCESSOR_EXTENDER_NAME, mediator)),
                Collections.<BundleRequirement>emptyList(),
                Collections.<BundleWire>emptyList());
        Bundle consumer = mockConsumer(wiring);
        BundleRevision fragmentRevision = EasyMock.createNiceMock(BundleRevision.class);
        EasyMock.replay(fragmentRevision);

        FrameworkWiring frameworkWiring = EasyMock.createMock(FrameworkWiring.class);
        frameworkWiring.refreshBundles(
                EasyMock.eq(Collections.singleton(consumer)),
                EasyMock.<FrameworkListener[]>anyObject());
        EasyMock.expectLastCall().once();
        EasyMock.replay(frameworkWiring);

        Bundle systemBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(systemBundle.adapt(FrameworkWiring.class))
                .andReturn(frameworkWiring).anyTimes();
        EasyMock.replay(systemBundle);
        BundleContext context = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(context.getBundle()).andReturn(mediator).anyTimes();
        EasyMock.expect(context.getBundle(0)).andReturn(systemBundle).anyTimes();
        EasyMock.replay(context);
        setBundleContext(context);

        activator.reprocessConsumerHost(
                consumer, fragmentRevision, SpiFlyConstants.SPI_CONSUMER_HEADER);
        assertTrue(activator.isStandardConsumer(consumer));

        // A refresh can make the tracker rebuild the host metadata and observe the same
        // attached fragment revision again. It must not start a refresh loop.
        activator.removeWeavingData(consumer);
        activator.reprocessConsumerHost(
                consumer, fragmentRevision, SpiFlyConstants.SPI_CONSUMER_HEADER);

        EasyMock.verify(frameworkWiring);
    }

    @Test
    public void lateProcessorFragmentDoesNotRefreshStaticConsumer() throws Exception {
        FrameworkWiring frameworkWiring = EasyMock.createMock(FrameworkWiring.class);
        EasyMock.replay(frameworkWiring);
        Bundle systemBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(systemBundle.adapt(FrameworkWiring.class))
                .andReturn(frameworkWiring).anyTimes();
        EasyMock.replay(systemBundle);
        BundleContext context = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(context.getBundle()).andReturn(mediator).anyTimes();
        EasyMock.expect(context.getBundle(0)).andReturn(systemBundle).anyTimes();
        EasyMock.replay(context);
        setBundleContext(context);

        BundleWiring wiring = mockConsumerWiring(
                Collections.singletonList(mockWire(
                        SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE,
                        SpiFlyConstants.PROCESSOR_EXTENDER_NAME, mediator)),
                Collections.<BundleRequirement>emptyList(),
                Collections.<BundleWire>emptyList());
        Bundle consumer = mockConsumer(wiring, true, true);
        BundleRevision fragmentRevision = EasyMock.createNiceMock(BundleRevision.class);
        EasyMock.replay(fragmentRevision);

        activator.reprocessConsumerHost(consumer, fragmentRevision,
                SpiFlyConstants.PROCESSED_SPI_CONSUMER_HEADER);

        assertTrue(activator.isStandardConsumer(consumer));
        EasyMock.verify(frameworkWiring);
    }

    private BundleWiring mockConsumerWiring(List<BundleWire> extenderWires,
            List<BundleRequirement> serviceRequirements, List<BundleWire> serviceWires) {
        return mockConsumerWiring(extenderWires, serviceRequirements, serviceWires,
                null, Collections.<BundleWire>emptyList());
    }

    private BundleWiring mockConsumerWiring(List<BundleWire> extenderWires,
            List<BundleRequirement> serviceRequirements, List<BundleWire> serviceWires,
            BundleRevision revision, List<BundleWire> hostWires) {
        BundleWiring wiring = EasyMock.createNiceMock(BundleWiring.class);
        EasyMock.expect(wiring.getRequiredWires(SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE))
                .andReturn(extenderWires).anyTimes();
        EasyMock.expect(wiring.getRequirements(SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE))
                .andReturn(serviceRequirements).anyTimes();
        EasyMock.expect(wiring.getRequiredWires(SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE))
                .andReturn(serviceWires).anyTimes();
        EasyMock.expect(wiring.getRevision()).andReturn(revision).anyTimes();
        EasyMock.expect(wiring.getProvidedWires(HostNamespace.HOST_NAMESPACE))
                .andReturn(hostWires).anyTimes();
        EasyMock.replay(wiring);
        return wiring;
    }

    private BundleRevision mockRevision(List<BundleRequirement> serviceRequirements) {
        BundleRevision revision = EasyMock.createNiceMock(BundleRevision.class);
        EasyMock.expect(revision.getDeclaredRequirements(
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE))
                .andReturn(serviceRequirements).anyTimes();
        EasyMock.replay(revision);
        return revision;
    }

    private BundleWire mockWire(String namespace, String value, Bundle provider) {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(namespace, value);
        BundleCapability capability = EasyMock.createNiceMock(BundleCapability.class);
        EasyMock.expect(capability.getAttributes()).andReturn(attributes).anyTimes();
        EasyMock.replay(capability);

        BundleWiring providerWiring = EasyMock.createNiceMock(BundleWiring.class);
        EasyMock.expect(providerWiring.getBundle()).andReturn(provider).anyTimes();
        EasyMock.replay(providerWiring);

        BundleWire wire = EasyMock.createNiceMock(BundleWire.class);
        EasyMock.expect(wire.getCapability()).andReturn(capability).anyTimes();
        EasyMock.expect(wire.getProviderWiring()).andReturn(providerWiring).anyTimes();
        EasyMock.replay(wire);
        return wire;
    }

    private Bundle mockConsumer(BundleWiring wiring) {
        return mockConsumer(wiring, false, true);
    }

    private Bundle mockConsumer(BundleWiring wiring, boolean processed, boolean processorRequirement) {
        Dictionary<String, String> headers = new Hashtable<String, String>();
        String serviceRequirement = SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE
                + ";filter:=\"(" + SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE
                + "=" + SERVICE_TYPE + ")\"";
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY,
                processorRequirement
                        ? SpiFlyConstants.CLIENT_REQUIREMENT + "," + serviceRequirement
                        : serviceRequirement);
        if (processed) {
            headers.put(PROCESSED_REQUIRE_CAPABILITY_HEADER,
                    SpiFlyConstants.CLIENT_REQUIREMENT + "," + serviceRequirement);
        }
        Bundle consumer = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(consumer.getHeaders()).andReturn(headers).anyTimes();
        EasyMock.expect(consumer.adapt(BundleWiring.class)).andReturn(wiring).anyTimes();
        EasyMock.replay(consumer);
        return consumer;
    }

    private void setBundleContext(BundleContext context) throws Exception {
        Field contextField = BaseActivator.class.getDeclaredField("bundleContext");
        contextField.setAccessible(true);
        contextField.set(activator, context);
    }

    private Bundle mockBundle(long id) {
        Bundle bundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundle.getBundleId()).andReturn(id).anyTimes();
        EasyMock.replay(bundle);
        return bundle;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Bundle mockProvider(long id, String serviceType, Class<?> providerServiceType)
            throws ClassNotFoundException {
        Bundle bundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundle.getBundleId()).andReturn(id).anyTimes();
        EasyMock.expect(bundle.loadClass(serviceType)).andReturn((Class) providerServiceType).anyTimes();
        EasyMock.replay(bundle);
        return bundle;
    }

    private Map<Pair<Integer, String>, String> serviceArguments(String serviceType) {
        Map<Pair<Integer, String>, String> arguments = new HashMap<Pair<Integer, String>, String>();
        arguments.put(new Pair<Integer, String>(0, Class.class.getName()), serviceType);
        return arguments;
    }

    private interface TestService {
    }
}
