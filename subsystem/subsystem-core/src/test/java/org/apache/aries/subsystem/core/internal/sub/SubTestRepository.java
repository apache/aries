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
package org.apache.aries.subsystem.core.internal.sub;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.internal.TestCapability;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

// It is important for the test that this class it non-public
class SubTestRepository implements Repository {
    @Override
    public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
        Map<Requirement, Collection<Capability>> res = new HashMap<Requirement, Collection<Capability>>();

        for (Requirement req : requirements) {
            if (req.getNamespace().equals("ns1") &&
                    req.getDirectives().equals(Collections.singletonMap("filter", "(x=y)"))) {
                TestCapability cap = new TestCapability("ns1",
                        Collections.<String, Object>singletonMap("x", "y"));
                Collection<Capability> caps = Collections.<Capability>singleton(cap);
                res.put(req, caps);
            }
        }
        return res;
    }
}