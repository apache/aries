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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.resource.Capability;
import org.osgi.service.resolver.HostedCapability;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ResolveContextTest {
    @Test
    public void testInsertHostedCapability() throws Exception {
        Activator prev = getActivator();
        try {
            Activator activator = createActivator();
            setActivator(activator);

            SubsystemResource res = new SubsystemResource(new File("."));
            ResolveContext rc = new ResolveContext(res);

            HostedCapability hc = EasyMock.createNiceMock(HostedCapability.class);

            List<Capability> caps = new ArrayList<Capability>() {
                // Must use add(idx, obj), get the other add() overloads to complain

                @Override
                public boolean add(Capability e) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean addAll(Collection<? extends Capability> c) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean addAll(int index, Collection<? extends Capability> c) {
                    throw new UnsupportedOperationException();
                }
            };
            caps.add(0, EasyMock.createNiceMock(HostedCapability.class));

            assertEquals(1, rc.insertHostedCapability(caps, hc));
            assertSame(hc, caps.get(1));
        } finally {
            setActivator(prev);
        }
    }

    private Activator createActivator() throws Exception {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);

        Activator a = new Activator();

        Field f = Activator.class.getDeclaredField("subsystems");
        f.setAccessible(true);
        f.set(a, new Subsystems());

        Field f2 = Activator.class.getDeclaredField("systemRepositoryManager");
        f2.setAccessible(true);
        f2.set(a, new SystemRepositoryManager(bc));

        return a;
    }

    private Activator getActivator() throws Exception {
        Field f = Activator.class.getDeclaredField("instance");
        f.setAccessible(true);
        return (Activator) f.get(null);
    }

    private void setActivator(Activator a) throws Exception {
        Field f = Activator.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, a);
    }
}
