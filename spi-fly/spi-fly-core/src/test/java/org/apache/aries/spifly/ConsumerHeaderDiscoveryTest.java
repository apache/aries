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

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Optional;
import java.util.Set;

import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Verifies that consumer discovery reads raw manifest headers from hosts and
 * fragments without requesting header localization.
 *
 * @see <a href="https://issues.apache.org/jira/browse/ARIES-2243">ARIES-2243</a>
 */
@RunWith(Parameterized.class)
public class ConsumerHeaderDiscoveryTest {
    @Parameters(name = "{0}, fragment={1}")
    public static Iterable<Object[]> parameters() {
        return Arrays.asList(new Object[][] {
            {SpiFlyConstants.SPI_CONSUMER_HEADER, false},
            {SpiFlyConstants.SPI_CONSUMER_HEADER, true},
            {SpiFlyConstants.REQUIRE_CAPABILITY, false},
            {SpiFlyConstants.REQUIRE_CAPABILITY, true}
        });
    }

    private final String headerName;
    private final boolean fragment;

    public ConsumerHeaderDiscoveryTest(String headerName, boolean fragment) {
        this.headerName = headerName;
        this.fragment = fragment;
    }

    @Test
    public void testConsumerDiscoveryUsesRawHeaders() throws Exception {
        Dictionary<String, String> headers = new Hashtable<String, String>();
        if (SpiFlyConstants.REQUIRE_CAPABILITY.equals(headerName)) {
            headers.put(headerName, SpiFlyConstants.CLIENT_REQUIREMENT + ",osgi.serviceloader;"
                    + "filter:=\"(osgi.serviceloader=org.example.Service)\"");
        } else {
            headers.put(headerName, "java.util.ServiceLoader#load(java.lang.Class)");
        }

        Bundle fragmentBundle = null;
        BundleRevision hostRevision = null;
        if (fragment) {
            fragmentBundle = mockBundle(headers, null);
            hostRevision = mockHostRevision(fragmentBundle);
        }
        Bundle host = mockBundle(fragment ? new Hashtable<String, String>() : headers, hostRevision);
        BaseActivator activator = new BaseActivator() {
            @Override
            public void start(BundleContext context) throws Exception {}
        };
        activator.setAutoConsumerInstructions(Optional.empty());

        activator.addConsumerWeavingData(host, SpiFlyConstants.SPI_CONSUMER_HEADER);

        Set<WeavingData> weavingData = activator.getWeavingData(host);
        assertNotNull(weavingData);
        assertEquals(SpiFlyConstants.REQUIRE_CAPABILITY.equals(headerName) ? 2 : 1, weavingData.size());
        for (WeavingData data : weavingData) {
            assertEquals("java.util.ServiceLoader", data.getClassName());
            assertEquals("load", data.getMethodName());
        }
        EasyMock.verify(host);
        if (fragmentBundle != null) {
            EasyMock.verify(fragmentBundle);
        }
    }

    private Bundle mockBundle(Dictionary<String, String> headers, BundleRevision revision) {
        Bundle bundle = EasyMock.createMock(Bundle.class);
        // The no-argument overload must not be called: it requests localized headers.
        EasyMock.expect(bundle.getHeaders("")).andReturn(headers).times(2);
        EasyMock.expect(bundle.adapt(BundleRevision.class)).andReturn(revision).anyTimes();
        EasyMock.replay(bundle);
        return bundle;
    }

    private BundleRevision mockHostRevision(Bundle fragmentBundle) {
        BundleRevision fragmentRevision = EasyMock.createMock(BundleRevision.class);
        EasyMock.expect(fragmentRevision.getBundle()).andReturn(fragmentBundle).anyTimes();
        EasyMock.replay(fragmentRevision);

        BundleRequirement requirement = EasyMock.createMock(BundleRequirement.class);
        EasyMock.expect(requirement.getRevision()).andReturn(fragmentRevision).anyTimes();
        EasyMock.replay(requirement);

        BundleWire wire = EasyMock.createMock(BundleWire.class);
        EasyMock.expect(wire.getRequirement()).andReturn(requirement).anyTimes();
        EasyMock.replay(wire);

        BundleWiring wiring = EasyMock.createMock(BundleWiring.class);
        EasyMock.expect(wiring.getProvidedWires("osgi.wiring.host"))
                .andReturn(Collections.singletonList(wire)).anyTimes();
        EasyMock.replay(wiring);

        BundleRevision revision = EasyMock.createMock(BundleRevision.class);
        EasyMock.expect(revision.getWiring()).andReturn(wiring).anyTimes();
        EasyMock.replay(revision);
        return revision;
    }
}
