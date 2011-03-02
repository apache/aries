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

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.aries.spifly.api.SpiFlyConstants;
import org.apache.aries.spifly.impl1.MySPIImpl1;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class ProviderBundleTrackerCustomizerTest {
    @Test
    @SuppressWarnings("unchecked")
    public void testAddingRemovedBundle() throws Exception {        
        Bundle spiBundle = EasyMock.createMock(Bundle.class);
        EasyMock.replay(spiBundle);
        Activator a = new Activator();        
        
        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(a, spiBundle);
        
        ServiceRegistration<Object> sreg = EasyMock.createMock(ServiceRegistration.class);
        sreg.unregister();
        EasyMock.expectLastCall();
        EasyMock.replay(sreg);

        // The bundle context for the test SPI bundle
        BundleContext implBC = EasyMock.createMock(BundleContext.class);
        EasyMock.<Object>expect(implBC.registerService(
                EasyMock.eq("org.apache.aries.mytest.MySPI"), 
                EasyMock.isA(MySPIImpl1.class), 
                (Dictionary<String,?>) EasyMock.anyObject())).andReturn(sreg);
        EasyMock.replay(implBC);

        // The test impl bundle
        Bundle implBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(implBundle.getBundleContext()).andReturn(implBC);
        
        Dictionary<String, String> headers = new Hashtable<String, String>();
        // Specify the headers for the test bundle
        headers.put(SpiFlyConstants.SPI_PROVIDER_HEADER, "true");
        EasyMock.expect(implBundle.getHeaders()).andReturn(headers);
        
        // List the resources found at META-INF/services in the test bundle
        URL res = getClass().getResource("impl1/META-INF/services/org.apache.aries.mytest.MySPI");
        Assert.assertNotNull("precondition", res);
        EasyMock.expect(implBundle.findEntries("META-INF/services", "*", false)).andReturn(
                Collections.enumeration(Collections.singleton(res)));
        
        Class<?> cls = getClass().getClassLoader().loadClass("org.apache.aries.spifly.impl1.MySPIImpl1");
        EasyMock.<Object>expect(implBundle.loadClass("org.apache.aries.spifly.impl1.MySPIImpl1")).andReturn(cls);
        
        EasyMock.replay(implBundle);
        
        Assert.assertEquals("Precondition", 0, a.findProviderBundles("org.apache.aries.mytest.MySPI").size());
        // Call addingBundle();
        List<ServiceRegistration<?>> registrations = customizer.addingBundle(implBundle, null);
        Collection<Bundle> bundles = a.findProviderBundles("org.apache.aries.mytest.MySPI");
        Assert.assertEquals(1, bundles.size());
        Assert.assertSame(implBundle, bundles.iterator().next());
        
        // The bc.registerService() call should now have been made
        EasyMock.verify(implBC);
        
        // Call removedBundle();
        customizer.removedBundle(implBundle, null, registrations);
        // sreg.unregister() should have been called.
        EasyMock.verify(sreg);
    }
}
