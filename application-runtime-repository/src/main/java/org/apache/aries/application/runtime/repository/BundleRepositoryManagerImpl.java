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
package org.apache.aries.application.runtime.repository;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;
import static org.apache.aries.application.utils.AppConstants.LOG_EXCEPTION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.management.BundleRepository;
import org.apache.aries.application.management.BundleRepositoryManager;
import org.apache.aries.application.management.ContextException;
import org.apache.aries.application.management.BundleRepository.BundleSuggestion;
import org.apache.aries.application.utils.service.ArrayServiceList;
import org.apache.aries.application.utils.service.ServiceCollection;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleRepositoryManagerImpl implements BundleRepositoryManager
{    
  private static final Logger LOGGER = LoggerFactory.getLogger(BundleRepositoryManagerImpl.class);
 
  private BundleContext bc;
    
  public void setBundleContext(BundleContext bc)
  {
    LOGGER.debug(LOG_ENTRY, "setBundleContext");
    this.bc = bc;
    LOGGER.debug(LOG_EXIT, "setBundleContext");
  }
  
  public Collection<BundleRepository> getAllBundleRepositories()
  {
    LOGGER.debug(LOG_ENTRY, "getAllBundleRepositories");
    
    ServiceCollection<BundleRepository> providers = 
      new ArrayServiceList<BundleRepository>(bc);

    try {
      ServiceReference[] refs = bc.getServiceReferences(
          BundleRepository.class.getName(), null);

      if (refs != null) {
        for (ServiceReference ref : refs) {
          providers.addService(ref);
        }
      }

    } catch (InvalidSyntaxException e) {
      LOGGER.error(LOG_EXCEPTION, e);
    }

    LOGGER.debug(LOG_EXIT, "getAllBundleRepositories");
    
    return providers;
  }

  public Collection<BundleRepository> getBundleRepositoryCollection(String appName, String appVersion)
  {
    LOGGER.debug(LOG_ENTRY, "getBundleRepositoryCollection", new Object[] {appName, appVersion});
    
    ServiceCollection<BundleRepository> providers = 
      new ArrayServiceList<BundleRepository>(bc);

    String appScope = appName + "_" + appVersion;
    
    String filter = "(|(" + BundleRepository.REPOSITORY_SCOPE + "=" + BundleRepository.GLOBAL_SCOPE + ")(" + BundleRepository.REPOSITORY_SCOPE + "="
        + appScope + "))";
    try {
      ServiceReference[] refs = bc.getServiceReferences(
          BundleRepository.class.getName(), filter);

      if (refs != null) {
        for (ServiceReference ref : refs) {
          providers.addService(ref);
        }
      }

    } catch (InvalidSyntaxException e) {
      LOGGER.error(LOG_EXCEPTION, e);
    }

    LOGGER.debug(LOG_EXIT, "getBundleRepositoryCollection");
    
    return providers;
  }

  public Map<DeploymentContent, BundleSuggestion> getBundleSuggestions(Collection<BundleRepository> providers, Collection<DeploymentContent> content)
    throws ContextException
  {
    LOGGER.debug(LOG_ENTRY, "getBundleSuggestions", new Object[] {content, providers});
    
    Map<DeploymentContent, BundleSuggestion> urlToBeInstalled = new HashMap<DeploymentContent, BundleSuggestion>();
    Iterator<DeploymentContent> it = content.iterator();
    
    while (it.hasNext()) {
      DeploymentContent bundleToFind = it.next();

      Map<Version, List<BundleSuggestion>> bundlesuggestions = new HashMap<Version, List<BundleSuggestion>>();

      for (BundleRepository obj : providers) {
        BundleSuggestion suggestion = obj.suggestBundleToUse(bundleToFind);

        if (suggestion != null) {
          List<BundleSuggestion> suggestions = bundlesuggestions.get(suggestion.getVersion());

          if (suggestions == null) {
            suggestions = new ArrayList<BundleSuggestion>();
            bundlesuggestions.put(suggestion.getVersion(), suggestions);
          }

          suggestions.add(suggestion);
        }
      }

      BundleSuggestion suggestion = null;

      if (!!!bundlesuggestions.isEmpty()) {

        List<BundleSuggestion> thoughts = bundlesuggestions.get(bundleToFind.getExactVersion());

        if (thoughts != null) {
          Collections.sort(thoughts, new Comparator<BundleSuggestion>() {
            public int compare(BundleSuggestion o1, BundleSuggestion o2)
            {
              return o1.getCost() - o2.getCost();
            }
          });
  
          suggestion = thoughts.get(0);
        }
      }

      // add the suggestion to the list
      if (suggestion != null) {
        urlToBeInstalled.put(bundleToFind, suggestion);
      } else {
        throw new ContextException("Unable to find bundle "+bundleToFind.getContentName() + "/" + bundleToFind.getExactVersion());
      }
    }
    
    LOGGER.debug(LOG_EXIT, "getBundleSuggestions", new Object[] { urlToBeInstalled });

    return urlToBeInstalled;
  }

  public Map<DeploymentContent, BundleSuggestion> getBundleSuggestions(String applicationName,
      String applicationVersion, Collection<DeploymentContent> content) throws ContextException
  {
    return getBundleSuggestions(getBundleRepositoryCollection(applicationName, applicationVersion), content);
  }

  public Map<DeploymentContent, BundleSuggestion> getBundleSuggestions(
      Collection<DeploymentContent> content) throws ContextException
  {
    return getBundleSuggestions(getAllBundleRepositories(), content);
  }  
}
