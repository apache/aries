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
package org.apache.aries.application.management.repository;

import java.util.Set;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.Content;
import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationResolver;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.BundleRepository;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

public class ApplicationRepository implements BundleRepository
{
  private static final int REPOSITORY_COST = 0;
  public static final String REPOSITORY_SCOPE = "repositoryScope";
  
  AriesApplicationResolver resolver;

  public ApplicationRepository(AriesApplicationResolver resolver)
  {
    this.resolver = resolver;
  }
  
  public int getCost()
  {
    return REPOSITORY_COST;
  }

  public BundleSuggestion suggestBundleToUse(DeploymentContent content)
  {
    return new BundleSuggestionImpl(content);
  }

  private class BundleSuggestionImpl implements BundleSuggestion
  {
    private BundleInfo bundleInfo;
    
    BundleSuggestionImpl(DeploymentContent content)
    {
      this.bundleInfo = resolver.getBundleInfo(content.getContentName(), content.getExactVersion());
    }
    
    public int getCost()
    {
      return REPOSITORY_COST;
    }

    public Set<Content> getExportPackage()
    {
      return bundleInfo.getExportPackage();
    }

    public Set<Content> getImportPackage()
    {
      return bundleInfo.getImportPackage();
    }

    public Version getVersion()
    {
      return bundleInfo.getVersion();
    }

    public Bundle install(BundleContext ctx, AriesApplication app) throws BundleException
    {
      return ctx.installBundle(bundleInfo.getLocation());
    }
    
  }
  
}
