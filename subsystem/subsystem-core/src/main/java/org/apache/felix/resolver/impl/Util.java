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
import java.util.List;

import org.apache.aries.subsystem.core.Environment;
import org.apache.aries.subsystem.core.ResourceHelper;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.resource.Wiring;

public class Util
{
    public static String getSymbolicName(Resource resource)
    {
        List<Capability> caps = resource.getCapabilities(null);
        for (Capability cap : caps)
        {
            if (cap.getNamespace().equals(IdentityNamespace.IDENTITY_NAMESPACE))
            {
                return cap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE).toString();
            }
        }
        return null;
    }

    public static Version getVersion(Resource resource)
    {
        List<Capability> caps = resource.getCapabilities(null);
        for (Capability cap : caps)
        {
            if (cap.getNamespace().equals(IdentityNamespace.IDENTITY_NAMESPACE))
            {
                return (Version)
                    cap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
            }
        }
        return null;
    }

    public static boolean isFragment(Resource resource)
    {
        List<Capability> caps = resource.getCapabilities(null);
        for (Capability cap : caps)
        {
            if (cap.getNamespace().equals(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE))
            {
                String type = (String)
                    cap.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
                return (type != null) && type.equals(IdentityNamespace.TYPE_FRAGMENT);
            }
        }
        return false;
    }

    public static boolean isOptional(Requirement req)
    {
        String resolution = req.getDirectives().get(Constants.RESOLUTION_DIRECTIVE);
        return ((resolution == null)
            || resolution.equalsIgnoreCase(Constants.RESOLUTION_OPTIONAL));
    }

    public static List<Requirement> getDynamicRequirements(List<Requirement> reqs)
    {
        List<Requirement> result = new ArrayList<Requirement>();
        if (reqs != null)
        {
            for (Requirement req : reqs)
            {
                String resolution = req.getDirectives().get(Constants.RESOLUTION_DIRECTIVE);
                if ((resolution != null) && resolution.equals("dynamic"))
                {
                    result.add(req);
                }
            }
        }
        return result;
    }

    public static Capability getSatisfyingCapability(
        Environment env, Resource br, Requirement req)
    {
        Wiring wiring = env.getWirings().get(br);
        List<Capability> caps = (wiring != null)
            ? wiring.getResourceCapabilities(null)
            : br.getCapabilities(null);
        if (caps != null)
        {
            for (Capability cap : caps)
            {
                if (cap.getNamespace().equals(req.getNamespace())
                    && ResourceHelper.matches(req, cap))
                {
                    return cap;
                }
            }
        }
        return null;
    }
}