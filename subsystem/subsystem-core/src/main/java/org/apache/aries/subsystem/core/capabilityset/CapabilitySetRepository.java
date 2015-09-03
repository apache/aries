/*
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
package org.apache.aries.subsystem.core.capabilityset;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.aries.subsystem.core.repository.Repository;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class CapabilitySetRepository implements Repository {
    private final Map<String, CapabilitySet> namespace2capabilitySet;

    public CapabilitySetRepository() {
        namespace2capabilitySet = Collections.synchronizedMap(new HashMap<String, CapabilitySet>());
        namespace2capabilitySet.put(
                IdentityNamespace.IDENTITY_NAMESPACE, 
                new CapabilitySet(Arrays.asList(IdentityNamespace.IDENTITY_NAMESPACE), true));
        namespace2capabilitySet.put(
                NativeNamespace.NATIVE_NAMESPACE, 
                new CapabilitySet(Arrays.asList(NativeNamespace.NATIVE_NAMESPACE), true));
        namespace2capabilitySet.put(
                ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, 
                new CapabilitySet(Arrays.asList(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE), true));
        namespace2capabilitySet.put(
                BundleNamespace.BUNDLE_NAMESPACE, 
                new CapabilitySet(Arrays.asList(BundleNamespace.BUNDLE_NAMESPACE), true));
        namespace2capabilitySet.put(
                HostNamespace.HOST_NAMESPACE, 
                new CapabilitySet(Arrays.asList(HostNamespace.HOST_NAMESPACE), true));
        namespace2capabilitySet.put(
                PackageNamespace.PACKAGE_NAMESPACE, 
                new CapabilitySet(Arrays.asList(PackageNamespace.PACKAGE_NAMESPACE), true));
        namespace2capabilitySet.put(
                ServiceNamespace.SERVICE_NAMESPACE, 
                new CapabilitySet(Arrays.asList(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE), true));
    }
    
    public void addResource(Resource resource) {
        for (Capability capability : resource.getCapabilities(null)) {
            String namespace = capability.getNamespace();
            CapabilitySet capabilitySet;
            synchronized (namespace2capabilitySet) {
                capabilitySet = namespace2capabilitySet.get(namespace);
                if (capabilitySet == null) {
                    capabilitySet = new CapabilitySet(Arrays.asList(namespace), true);
                    namespace2capabilitySet.put(namespace, capabilitySet);
                }
            }
            // TODO Examine CapabilitySet for thread safety.
            capabilitySet.addCapability(capability);
        }
    }

    @Override
    public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
        Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>(requirements.size());
        for (Requirement requirement : requirements) {
            String filterDirective = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
            SimpleFilter simpleFilter;
            if (filterDirective == null) {
                simpleFilter = new SimpleFilter(null, null, SimpleFilter.MATCH_ALL);
            }
            else {
                simpleFilter = SimpleFilter.parse(filterDirective);
            }
            String namespace = requirement.getNamespace();
            CapabilitySet capabilitySet = namespace2capabilitySet.get(namespace);
            if (capabilitySet != null) {
                Set<Capability> capabilities = capabilitySet.match(
                        simpleFilter, 
                        PackageNamespace.PACKAGE_NAMESPACE.equals(namespace)
                                || BundleNamespace.BUNDLE_NAMESPACE.equals(namespace)
                                || HostNamespace.HOST_NAMESPACE.equals(namespace));
                result.put(requirement, capabilities);
            }
            else {
                result.put(requirement, Collections.<Capability>emptyList());
            }
        }
        return result;
    }
    
    public void removeResource(Resource resource) {
        for (Capability capability : resource.getCapabilities(null)) {
            CapabilitySet capabilitySet = namespace2capabilitySet.get(capability.getNamespace());
            if (capabilitySet == null) {
                continue;
            }
            // TODO Examine CapabilitySet for thread safety.
            capabilitySet.removeCapability(capability);
        }
    }
}
