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

import static org.junit.Assert.assertEquals;
import static org.easymock.EasyMock.*;

import java.lang.reflect.Field;
import java.util.Collections;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public class BundleRevisionResourceTest {
    Activator storedActivator;

    @Before
    public void setUp() throws Exception {
        Field field = Activator.class.getDeclaredField("instance");
        field.setAccessible(true);
        storedActivator = (Activator) field.get(null);
        field.set(null, new Activator());
    }

    @After
    public void tearDown() throws Exception {
        Field field = Activator.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, storedActivator);
        storedActivator = null;
    }

    @Test
    public void testNoModellerServiceCapabilities() {
        BundleRevision br = createNiceMock(BundleRevision.class);
        expect(br.getCapabilities(anyObject(String.class))).andReturn(Collections.<Capability>emptyList());
        expect(br.getRequirements(anyObject(String.class))).andReturn(Collections.<Requirement>emptyList());
        replay(br);
        BundleRevisionResource brr = new BundleRevisionResource(br);
        assertEquals(0, brr.getCapabilities("osgi.service").size());
    }

    @Test
    public void testNoModellerServiceRequirements() {
        BundleRevision br = EasyMock.createNiceMock(BundleRevision.class);
        expect(br.getRequirements(anyObject(String.class))).andReturn(Collections.<Requirement>emptyList());
        expect(br.getCapabilities(anyObject(String.class))).andReturn(Collections.<Capability>emptyList());
        replay(br);
        BundleRevisionResource brr = new BundleRevisionResource(br);
        assertEquals(0, brr.getRequirements("osgi.service").size());
    }
}
