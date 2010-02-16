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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

  private Resource toResource(BundleInfo bundleInfo) throws ResolverException
  {
    String id = bundleInfo.getSymbolicName() + "_" + bundleInfo.getVersion();
    List<Requirement> requirements = new ArrayList<Requirement>();
    requirements.addAll(toRequirements(bundleInfo.getImportPackage(), "package"));
    requirements.addAll(toRequirements(bundleInfo.getImportService(), "service"));
    List<Capability> capabilities = new ArrayList<Capability>();
    capabilities.addAll(toPackageCapabilities(bundleInfo.getExportPackage()));
    capabilities.addAll(toServiceCapabilities(bundleInfo.getExportService()));
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
            requirements.toArray(new Requirement[requirements.size()]),
            capabilities.toArray(new Capability[capabilities.size()]),
            null,
            null);
  }

  private Collection<Requirement> toRequirements(Set<Content> imports, String type) throws ResolverException
  {
    Collection<Requirement> requirements = new ArrayList<Requirement>(imports.size());
    for (Content content: imports) {
      requirements.add(toRequirement(content, type));
    }
    return requirements;
  }

  private Requirement toRequirement(Content content, String type) throws ResolverException
  {
    Map<String, String> attributes = new HashMap<String, String>();
    for (Map.Entry<String, String> entry: content.getNameValueMap().entrySet()) {
      //leave out resolution:=optional, etc
      if (!entry.getKey().endsWith(":")) {
        attributes.put(entry.getKey(), entry.getValue());
      }
    }
    String filterString = ManifestHeaderProcessor.generateFilter(type, content.getContentName(), attributes);
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

  private Collection<Capability> toPackageCapabilities(Set<Content> exportPackage)
  {
    Collection<Capability> capabilities = new ArrayList<Capability>(exportPackage.size());
    for (Content content: exportPackage) {
      capabilities.add(toPackageCapability(content));
    }
    return capabilities;
  }

  private Capability toPackageCapability(Content content)
  {
    Map<String,String> props = new HashMap<String,String>();
    props.put("package", content.getContentName());
    props.put("version", content.getVersion() != null ? content.getVersion().getMinimumVersion().toString() : Version.emptyVersion.toString());
    return new CapabilityImpl("package", props);
  }

  private Collection<Capability> toServiceCapabilities(Set<Content> exportPackage)
  {
    Collection<Capability> capabilities = new ArrayList<Capability>(exportPackage.size());
    for (Content content: exportPackage) {
      capabilities.add(toServiceCapability(content));
    }
    return capabilities;
  }

  private Capability toServiceCapability(Content content)
  {
    Map<String,String> props = new HashMap<String,String>();
    props.put("service", content.getContentName());
    return new CapabilityImpl("service", props);
  }

}
