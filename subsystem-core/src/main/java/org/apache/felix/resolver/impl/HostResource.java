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
package org.apache.felix.resolver.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.resource.ResourceConstants;

class HostResource implements Resource
{
    private final Resource m_host;
    private final List<Resource> m_fragments;
    private List<Capability> m_cachedCapabilities = null;
    private List<Requirement> m_cachedRequirements = null;

    public HostResource(Resource host, List<Resource> fragments)
    {
        m_host = host;
        m_fragments = fragments;
    }

    public Resource getHost()
    {
        return m_host;
    }

    public List<Resource> getFragments()
    {
        return m_fragments;
    }

    public List<Capability> getCapabilities(String namespace)
    {
        if (m_cachedCapabilities == null)
        {
            List<Capability> caps = new ArrayList<Capability>();

            // Wrap host capabilities.
            for (Capability cap : m_host.getCapabilities(null))
            {
                caps.add(new HostedCapability(this, cap));
            }

            // Wrap fragment capabilities.
            if (m_fragments != null)
            {
                for (Resource fragment : m_fragments)
                {
                    for (Capability cap : fragment.getCapabilities(null))
                    {
// TODO: OSGi R4.4 - OSGi R4.4 may introduce an identity capability, if so
//       that will need to be excluded from here.
                        caps.add(new HostedCapability(this, cap));
                    }
                }
            }
            m_cachedCapabilities = Collections.unmodifiableList(caps);
        }
        return m_cachedCapabilities;
    }

    public List<Requirement> getRequirements(String namespace)
    {
        if (m_cachedRequirements == null)
        {
            List<Requirement> reqs = new ArrayList<Requirement>();

            // Wrap host requirements.
            for (Requirement req : m_host.getRequirements(null))
            {
                reqs.add(new HostedRequirement(this, req));
            }

            // Wrap fragment requirements.
            if (m_fragments != null)
            {
                for (Resource fragment : m_fragments)
                {
                    for (Requirement req : fragment.getRequirements(null))
                    {
                        if (!req.getNamespace().equals(ResourceConstants.WIRING_HOST_NAMESPACE))
                        {
                            reqs.add(new HostedRequirement(this, req));
                        }
                    }
                }
            }
            m_cachedRequirements = Collections.unmodifiableList(reqs);
        }
        return m_cachedRequirements;
    }

    public String toString()
    {
        return m_host.toString();
    }
}