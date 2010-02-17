/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.aries.application.resolver.obr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.Content;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationResolver;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.resolver.obr.impl.ApplicationResourceImpl;
import org.apache.aries.application.resolver.obr.impl.OBRBundleInfo;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.osgi.framework.Version;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;

/**
 * @version $Rev$ $Date$
 */
public class OBRAriesResolver implements AriesApplicationResolver
{

  private final RepositoryAdmin repositoryAdmin;

  public OBRAriesResolver(RepositoryAdmin repositoryAdmin)
  {
    this.repositoryAdmin = repositoryAdmin;
  }

  public Set<BundleInfo> resolve(AriesApplication app) throws ResolverException
  {
    Resolver obrResolver = repositoryAdmin.resolver();
    
    ApplicationMetadata appMeta = app.getApplicationMetadata();
    
    String appName = appMeta.getApplicationSymbolicName();
    Version appVersion = appMeta.getApplicationVersion();
    List<Content> appContent = appMeta.getApplicationContents();

    // add a resource describing the requirements of the application metadata.
    obrResolver.add(new ApplicationResourceImpl(appName, appVersion, appContent));
    
    // TODO we need to resolve against the app content so we need to generate an OBR.xml for the content
    
    if (obrResolver.resolve()) {
      Set<BundleInfo> result = new HashSet<BundleInfo>(app.getBundleInfo());
      for (Resource resource: obrResolver.getRequiredResources()) {
        BundleInfo bundleInfo = toBundleInfo(resource);
        result.add(bundleInfo);
      }
      for (Resource resource: obrResolver.getOptionalResources()) {
        BundleInfo bundleInfo = toBundleInfo(resource);
        result.add(bundleInfo);
      }
      return result;
    } else {
      throw new ResolverException("Could not resolve requirements: " + getUnsatifiedRequirements(obrResolver));
    }
  }

  public BundleInfo getBundleInfo(String bundleSymbolicName, Version bundleVersion)
  {
    Map<String, String> attribs = new HashMap<String, String>();
    attribs.put(Resource.VERSION, bundleVersion.toString());
    String filterString = ManifestHeaderProcessor.generateFilter(Resource.SYMBOLIC_NAME, bundleSymbolicName, attribs);
    Resource[] resources = repositoryAdmin.discoverResources(filterString);
    if (resources != null && resources.length > 0) {
      return toBundleInfo(resources[0]);
    } else {
      return null;
    }
  }

  private String getUnsatifiedRequirements(Resolver resolver)
  {
    Requirement[] reqs = resolver.getUnsatisfiedRequirements();
    if (reqs != null) {
      StringBuilder sb = new StringBuilder();
      for (int reqIdx = 0; reqIdx < reqs.length; reqIdx++) {
        sb.append("   " + reqs[reqIdx].getFilter()).append("\n");
        Resource[] resources = resolver.getResources(reqs[reqIdx]);
        for (int resIdx = 0; resIdx < resources.length; resIdx++) {
          sb.append("      " + resources[resIdx].getPresentationName()).append("\n");
        }
      }
      return sb.toString();
    }
    return null;
  }

  private BundleInfo toBundleInfo(Resource resource)
  {
    String location = resource.getURL().toExternalForm();
    return new OBRBundleInfo(resource.getSymbolicName(),
            resource.getVersion(),
            location,
            null,
            null,
            null,
            null,
            null);
  }
}