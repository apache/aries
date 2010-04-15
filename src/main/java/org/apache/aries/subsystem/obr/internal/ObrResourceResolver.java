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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.subsystem.obr.internal;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.SubsystemConstants;
import org.apache.aries.subsystem.SubsystemException;
import org.apache.aries.subsystem.spi.Resource;
import org.apache.aries.subsystem.spi.ResourceResolver;
import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.utils.manifest.Attribute;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.util.tracker.ServiceTracker;

public class ObrResourceResolver implements ResourceResolver
{

    private static final long TIMEOUT = 30000L;

    private final ServiceTracker repositoryAdminTracker;

    public ObrResourceResolver(ServiceTracker repositoryAdminTracker)
    {
        this.repositoryAdminTracker = repositoryAdminTracker;
    }

    protected RepositoryAdmin getRepositoryAdmin()
    {
        try {
            RepositoryAdmin admin = (RepositoryAdmin) repositoryAdminTracker.waitForService(TIMEOUT);
            if (admin == null) {
                throw new SubsystemException("RepositoryAdmin service not available");
            }
            return admin;
        } catch (InterruptedException e) {
            throw new SubsystemException("RepositoryAdmin service not available", e);
        }
    }

    public Resource find(String resource) throws SubsystemException {
        Clause[] clauses = Parser.parseHeader(resource);
        if (clauses.length != 1) {
            throw new SubsystemException("Unsupported resource: " + resource);
        }

        String bsn = clauses[0].getName();
        String ver = clauses[0].getAttribute(Constants.VERSION_ATTRIBUTE);
        String typ = clauses[0].getAttribute(SubsystemConstants.RESOURCE_TYPE_ATTRIBUTE);
        String loc = clauses[0].getAttribute(SubsystemConstants.RESOURCE_LOCATION_ATTRIBUTE);
        Map<String,String> attributes = new HashMap<String,String>();
        for (Attribute a : clauses[0].getAttributes()) {
            String name = a.getName();
            if (!Constants.VERSION_ATTRIBUTE.equals(name)
                    && !SubsystemConstants.RESOURCE_TYPE_ATTRIBUTE.equals(name)
                    && !SubsystemConstants.RESOURCE_LOCATION_ATTRIBUTE.equals(name))
            {
                attributes.put(name, a.getValue());
            }
        }
        if (loc != null) {
            return new ResourceImpl(
                    bsn,
                    ver != null ? new Version(ver) : Version.emptyVersion,
                    typ != null ? typ : SubsystemConstants.RESOURCE_TYPE_BUNDLE,
                    loc,
                    attributes
            );
        } else {
            try {
                RepositoryAdmin repo = getRepositoryAdmin();
                org.apache.felix.bundlerepository.Resource[] resources =
                        repo.discoverResources("(&(symbolicname=" + bsn + ")" + (ver != null ? "(version=" + ver + ")" : "") + ")");
                if (resources.length == 0) {
                    throw new SubsystemException("Unable to find a matching resource: " + resource);
                }
                return new ObrResourceImpl(resources[0],
                                           typ != null ? typ : SubsystemConstants.RESOURCE_TYPE_BUNDLE,
                                           attributes);
            } catch (InvalidSyntaxException e) {
                throw new SubsystemException("Invalid resource definition: " + resource, e);
            }
        }
    }

    public List<Resource> resolve(List<Resource> subsystemContent, List<Resource> subsystemResources) throws SubsystemException {
        RepositoryAdmin admin = getRepositoryAdmin();
        List<org.apache.felix.bundlerepository.Resource> obrResources = new ArrayList<org.apache.felix.bundlerepository.Resource>();
        for (Resource res : subsystemResources)
        {
            if (res.getType().equals(SubsystemConstants.RESOURCE_TYPE_BUNDLE)) {
                try
                {
                    obrResources.add(admin.getHelper().createResource(new URL(res.getLocation())));
                }
                catch (Exception e) {
                    // TODO: log exception
                }
            }
        }
        Repository repository = admin.getHelper().repository(obrResources.toArray(new org.apache.felix.bundlerepository.Resource[obrResources.size()]));
        List<Repository> repos = new ArrayList<Repository>();
        repos.add(admin.getSystemRepository());
        repos.add(admin.getLocalRepository());
        repos.add(repository);
        repos.addAll(Arrays.asList(admin.listRepositories()));
        Resolver resolver = admin.resolver(repos.toArray(new Repository[repos.size()]));

        for (Resource res : subsystemContent)
        {
            if (res.getType().equals(SubsystemConstants.RESOURCE_TYPE_BUNDLE)) {
                resolver.add(admin.getHelper().requirement(Capability.BUNDLE, "(&(symbolicname=" + res.getSymbolicName() + ")" + (res.getVersion() != null ? "(version=" + res.getVersion() + ")" : "") + ")"));
            }
        }
        if (!resolver.resolve(Resolver.NO_OPTIONAL_RESOURCES)) {
            throw new SubsystemException("Can not resolve subsystem");
        }

        List<Resource> resolved = new ArrayList<Resource>();
        for (org.apache.felix.bundlerepository.Resource res : resolver.getRequiredResources()) {
            resolved.add(new ObrResourceImpl(res, SubsystemConstants.RESOURCE_TYPE_BUNDLE, new HashMap<String,String>()));
        }
        return resolved;
    }

}
