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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.Content;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationResolver;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.ResolveConstraint;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.resolver.obr.generator.RepositoryDescriptorGenerator;
import org.apache.aries.application.resolver.obr.impl.ApplicationResourceImpl;
import org.apache.aries.application.resolver.obr.impl.OBRBundleInfo;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.apache.felix.bundlerepository.DataModelHelper;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * @version $Rev$ $Date$
 */
public class OBRAriesResolver implements AriesApplicationResolver
{
  private static Logger log = LoggerFactory.getLogger(OBRAriesResolver.class);

  private final RepositoryAdmin repositoryAdmin;

  public OBRAriesResolver(RepositoryAdmin repositoryAdmin)
  {
    this.repositoryAdmin = repositoryAdmin;
  }

  /**
   * This method is synchronized because it changes the repositories understood by OBR, and we don't
   * want one apps by value content being used to resolve another. I'll ask for an OBR enhancement.
   */
  public Set<BundleInfo> resolve(AriesApplication app, ResolveConstraint... constraints) throws ResolverException
  {
    log.trace("resolving {}", app);
    DataModelHelper helper = repositoryAdmin.getHelper();

    ApplicationMetadata appMeta = app.getApplicationMetadata();

    String appName = appMeta.getApplicationSymbolicName();
    Version appVersion = appMeta.getApplicationVersion();
    List<Content> appContent = appMeta.getApplicationContents();

    Repository appRepo;
    
    try {
      Document doc = RepositoryDescriptorGenerator.generateRepositoryDescriptor(appName + "_" + appVersion, app.getBundleInfo());
      
      ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
      
      TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(bytesOut));
      
      appRepo = helper.readRepository(new InputStreamReader(new ByteArrayInputStream(bytesOut.toByteArray())));
    } catch (Exception e) {
      throw new ResolverException(e);
    } 
    
    Repository[] repos = repositoryAdmin.listRepositories();
    
    List<Repository> resolveRepos = new ArrayList<Repository>();
    resolveRepos.add(appRepo);
    
    for (Repository r : repos) {
      if (!!!Repository.LOCAL.equals(r.getURI())) {
        resolveRepos.add(r);
      }
    }
    
    Resolver obrResolver = repositoryAdmin.resolver(resolveRepos.toArray(new Repository[resolveRepos.size()]));
    // add a resource describing the requirements of the application metadata.
    obrResolver.add(createApplicationResource(helper, appName, appVersion, appContent));
    if (obrResolver.resolve()) {
      Set<BundleInfo> result = new HashSet<BundleInfo>();
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
      throw new ResolverException("Could not resolve requirements: " + getUnsatisfiedRequirements(obrResolver));
    }
  }

  private Resource createApplicationResource(DataModelHelper helper, String appName, Version appVersion,
      List<Content> appContent)
  {
    return new ApplicationResourceImpl(appName, appVersion, appContent);
  }

  public BundleInfo getBundleInfo(String bundleSymbolicName, Version bundleVersion)
  {
    Map<String, String> attribs = new HashMap<String, String>();
    attribs.put(Resource.VERSION, bundleVersion.toString());
    String filterString = ManifestHeaderProcessor.generateFilter(Resource.SYMBOLIC_NAME, bundleSymbolicName, attribs);
    Resource[] resources;
    try {
      resources = repositoryAdmin.discoverResources(filterString);
      if (resources != null && resources.length > 0) {
        return toBundleInfo(resources[0]);
      } else {
        return null;
      }
    } catch (InvalidSyntaxException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }

  private String getUnsatisfiedRequirements(Resolver resolver)
  {
    Reason[] reqs = resolver.getUnsatisfiedRequirements();
    if (reqs != null) {
      StringBuilder sb = new StringBuilder();
      for (int reqIdx = 0; reqIdx < reqs.length; reqIdx++) {
        sb.append("   " + reqs[reqIdx].getRequirement().getFilter()).append("\n");
        Resource resource = reqs[reqIdx].getResource();
        sb.append("      " + resource.getPresentationName()).append("\n");
      }
      return sb.toString();
    }
    return null;
  }

  private BundleInfo toBundleInfo(Resource resource)
  {
    String location = resource.getURI();
    return new OBRBundleInfo(resource.getSymbolicName(),
            resource.getVersion(),
            location,
            null,
            null,
            null,
            null,
            null, 
            null,
            null,
            null);
  }
}