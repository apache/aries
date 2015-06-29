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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

public class RepositoryServiceRepositoryTest {
    @Test
    public void testFindProviders() throws Exception {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        RepositoryServiceRepository rsr = new RepositoryServiceRepository(bc);

        @SuppressWarnings("unchecked")
        ServiceReference<Object> sr = EasyMock.createMock(ServiceReference.class);
        @SuppressWarnings("unchecked")
        ServiceReference<Object> sr2 = EasyMock.createMock(ServiceReference.class);
        EasyMock.expect(bc.getAllServiceReferences("org.osgi.service.repository.Repository", null)).
            andReturn(new ServiceReference[] {sr, sr2}).anyTimes();

        TestRepository tr = new TestRepository();
        EasyMock.expect(bc.getService(sr)).andReturn(tr).anyTimes();

        ToastRepository tr2 = new ToastRepository();
        EasyMock.expect(bc.getService(sr2)).andReturn(tr2).anyTimes();
        EasyMock.replay(bc);


        Map<String, String> dirs = Collections.singletonMap("filter", "(org.foo=bar)");
        Requirement req = new TestRequirement("org.foo", dirs);
        Collection<Capability> res = rsr.findProviders(req);
        assertEquals(1, res.size());
        Capability cap = res.iterator().next();
        assertEquals("org.foo", cap.getNamespace());
        assertEquals(1, cap.getAttributes().size());
        assertEquals("bar", cap.getAttributes().get("org.foo"));

        Map<String, String> dirs2 = Collections.singletonMap("filter", "(org.foo=b)");
        Requirement req2 = new TestRequirement("poing", dirs2);
        Collection<Capability> res2 = rsr.findProviders(req2);
        assertEquals(1, res2.size());
        Capability cap2 = res2.iterator().next();
        assertEquals("poing", cap2.getNamespace());
        assertEquals(1, cap2.getAttributes().size());
        assertEquals("b", cap2.getAttributes().get("org.foo"));
    }

    private static class TestRequirement implements Requirement {
        private final String namespace;
        private final Map<String, String> directives;

        private TestRequirement(String ns, Map<String, String> dirs) {
            namespace = ns;
            directives = dirs;
        }

        @Override
        public String getNamespace() {
            return namespace;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, String> getDirectives() {
            return directives;
        }

        @Override
        public Resource getResource() {
            return null;
        }
    }

    private static class TestCapability implements Capability {
        private final String namespace;
        private final Map<String, Object> attributes;

        private TestCapability(String ns, Map<String, Object> attrs) {
            namespace = ns;
            attributes = attrs;
        }

        @Override
        public String getNamespace() {
            return namespace;
        }

        @Override
        public Map<String, String> getDirectives() {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public Resource getResource() {
            return null;
        }
    }

    private static class TestRepository implements Repository {
        @Override
        public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
            Map<Requirement, Collection<Capability>> res = new HashMap<Requirement, Collection<Capability>>();

            for (Requirement req : requirements) {
                if (req.getNamespace().equals("org.foo") &&
                        req.getDirectives().equals(Collections.singletonMap("filter", "(org.foo=bar)"))) {
                    TestCapability cap = new TestCapability("org.foo",
                            Collections.<String, Object>singletonMap("org.foo", "bar"));
                    Collection<Capability> caps = Collections.<Capability>singleton(cap);
                    res.put(req, caps);
                }
            }

            return res;
        }
    }

    private static class ToastRepository extends TestRepository {
        @Override
        public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
            for (Requirement req : requirements) {
                if (req.getNamespace().equals("poing") &&
                        req.getDirectives().equals(Collections.singletonMap("filter", "(org.foo=b)"))) {
                    TestCapability cap = new TestCapability("poing",
                            Collections.<String, Object>singletonMap("org.foo", "b"));
                    Collection<Capability> caps = Collections.<Capability>singleton(cap);
                    return Collections.singletonMap(req, caps);
                }
            }
            return Collections.emptyMap();
        }
    }
}
