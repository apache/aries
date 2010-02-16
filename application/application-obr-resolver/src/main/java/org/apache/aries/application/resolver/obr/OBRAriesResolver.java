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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.aries.application.Content;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationResolver;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.resolver.obr.impl.CapabilityImpl;
import org.apache.aries.application.resolver.obr.impl.OBRBundleInfo;
import org.apache.aries.application.resolver.obr.impl.RequirementImpl;
import org.apache.aries.application.resolver.obr.impl.ResourceImpl;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.service.obr.Capability;
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
    for (BundleInfo bundleInfo: app.getBundleInfo()) {
      Resource resource = toResource(bundleInfo);
      obrResolver.add(resource);
    }
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
      throw new ResolverException("Could not resolve requirements: " + toString(obrResolver.getUnsatisfiedRequirements()));
    }
  }

  public BundleInfo getBundleInfo(String bundleSymbolicName, Version bundleVersion)
  {
    Map<String, String> attribs = new HashMap<String, String>();
    attribs.put("BundleSymbolic-Name", bundleSymbolicName);
    attribs.put("Bundle-Version", bundleVersion.toString());
    String filterString = ManifestHeaderProcessor.generateFilter("bundle", bundleSymbolicName, attribs);
    Resource[] resources = repositoryAdmin.discoverResources(filterString);
    if (resources != null && resources.length > 0) {
      return toBundleInfo(resources[0]);
    } else {
      return null;
    }
  }

  private String toString(Requirement[] unsatisfiedRequirements)
  {
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
            null);
  }

  private Resource toResource(BundleInfo bundleInfo) throws ResolverException
  {
    String id = bundleInfo.getSymbolicName() + "_" + bundleInfo.getVersion();
    Requirement[] requirements = toRequirements(bundleInfo.getImportPackage());
    Capability[] capabilities = toCapabilities(bundleInfo.getExportPackage());
    URL url;
    try {
      url = new URL(bundleInfo.getLocation());
    } catch (MalformedURLException e) {
      throw new ResolverException(e);
    }
    return new ResourceImpl(bundleInfo.getHeaders(),
            bundleInfo.getSymbolicName(),
            bundleInfo.getSymbolicName(),
            bundleInfo.getVersion(),
            id,
            url,
            requirements,
            capabilities,
            null,
            null);
  }

  private Requirement[] toRequirements(Set<Content> importPackage) throws ResolverException
  {
    Requirement[] requirements = new Requirement[importPackage.size()];
    int i = 0;
    for (Content content: importPackage) {
      requirements[i++] = toRequirement(content);
    }
    return requirements;
  }

  private Requirement toRequirement(Content content) throws ResolverException
  {
    Map<String, String> attributes = new HashMap<String, String>();
    for (Map.Entry<String, String> entry: content.getNameValueMap().entrySet()) {
      //leave out resolution:=optional, etc
      if (!entry.getKey().endsWith(":")) {
        attributes.put(entry.getKey(), entry.getValue());
      }
    }
    String filterString = ManifestHeaderProcessor.generateFilter("package", content.getContentName(), attributes);
    Filter filter = null;
    try {
      filter = FrameworkUtil.createFilter(filterString);
    } catch (InvalidSyntaxException e) {
      throw new ResolverException(e);
    }
    boolean multiple = false;
    boolean optional = "optional".equals(content.getNameValueMap().get("resolution:"));
    boolean extend = false;
    return new RequirementImpl("package",
            filter,
            multiple,
            optional,
            extend,
            null);
  }

  private Capability[] toCapabilities(Set<Content> exportPackage)
  {
    Capability[] capabilities = new Capability[exportPackage.size()];
    int i = 0;
    for (Content content: exportPackage) {
      capabilities[i++] = toCapability(content);
    }
    return capabilities;
  }

  private Capability toCapability(Content content)
  {
    return new CapabilityImpl(content.getContentName(), content.getNameValueMap());
  }
}
