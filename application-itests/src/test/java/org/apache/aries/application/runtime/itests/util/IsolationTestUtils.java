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
package org.apache.aries.application.runtime.itests.util;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Set;
import java.util.jar.Attributes;

import org.apache.aries.application.Content;
import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.spi.repository.BundleRepository;
import org.apache.aries.application.management.spi.repository.RepositoryGenerator;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ModellingManager;
import org.apache.aries.isolated.sample.HelloWorld;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.service.framework.CompositeBundle;
import org.osgi.util.tracker.ServiceTracker;

public class IsolationTestUtils {
  public static final long DEFAULT_TIMEOUT = 10000;
  
  /**
   * Retrieve the bundle context for an isolated application framework
   */
  public static BundleContext findIsolatedAppBundleContext(BundleContext runtimeCtx, String appName)
  {
    for (Bundle sharedBundle : runtimeCtx.getBundles())
    {
      if (sharedBundle.getSymbolicName().equals("shared.bundle.framework"))
      {
        BundleContext sharedContext = ((CompositeBundle)sharedBundle).getCompositeFramework().getBundleContext();
        for (Bundle appBundle : sharedContext.getBundles())
        {
          if (appBundle.getSymbolicName().equals(appName))
          {
            return ((CompositeBundle)appBundle).getCompositeFramework().getBundleContext();
          }
        }
        break;
      }
    }    
    
    return null;
  }
  
  /**
   * Set up the necessary resources for installing version 2 of the org.apache.aries.isolated.sample sample bundle, 
   * which returns the message "hello brave new world" rather than "hello world"
   * 
   * This means setting up a global bundle repository as well as a global OBR repository
   */
  public static void prepareSampleBundleV2(BundleContext runtimeCtx, 
      RepositoryGenerator repoGen, RepositoryAdmin repoAdmin, 
      ModellingManager modellingManager)
    throws Exception
  {
    BundleRepository repo = new BundleRepository() {
      public int getCost() {
        return 1;
      }

      public BundleSuggestion suggestBundleToUse(DeploymentContent content) {
        if (content.getContentName().equals("org.apache.aries.isolated.sample")) {
          return new BundleSuggestion() {

            public Bundle install(BundleContext ctx, AriesApplication app) throws BundleException {
              File f = new File("sample_2.0.0.jar");
              try {
                return ctx.installBundle(f.toURL().toString());                
              } catch (MalformedURLException mue) {
                throw new RuntimeException(mue);
              }
            }

            public Version getVersion() {
              return new Version("2.0.0");
            }

            public Set<Content> getImportPackage() {
              return Collections.emptySet();
            }

            public Set<Content> getExportPackage() {
              return Collections.emptySet();
            }

            public int getCost() {
              return 1;
            }
          };
        } else {
          return null;
        }
      }        
    };
    
    Hashtable<String, String> props = new Hashtable<String,String>();
    props.put(BundleRepository.REPOSITORY_SCOPE, BundleRepository.GLOBAL_SCOPE);
    
    runtimeCtx.registerService(BundleRepository.class.getName(), repo, props);

    Attributes attrs = new Attributes();
    attrs.putValue("Bundle-ManifestVersion", "2");
    attrs.putValue("Bundle-Version", "2.0.0");
    attrs.putValue("Bundle-SymbolicName", "org.apache.aries.isolated.sample");
    attrs.putValue("Manifest-Version", "1");

    ModelledResource res = modellingManager.getModelledResource(
        new File("sample_2.0.0.jar").toURI().toString(), 
        attrs,
        Collections.EMPTY_LIST, Collections.EMPTY_LIST);

    repoGen.generateRepository("repo.xml", Arrays.asList(res), new FileOutputStream("repo.xml"));
    repoAdmin.addRepository(new File("repo.xml").toURI().toString());
  }
  
  /**
   * Find the {@link HelloWorld} service for the isolated app
   * @return the service object, suitably proxied so that it can be actually used, or null if the service is not present
   * @throws IllegalStateException if the isolated app is not installed
   */
  public static HelloWorld findHelloWorldService(BundleContext runtimeCtx, String appName) throws Exception
  {
    BundleContext appContext = IsolationTestUtils.findIsolatedAppBundleContext(runtimeCtx, appName);
    
    if (appContext != null) {  
      // Dive into the context and pull out the composite bundle for the app
      Filter osgiFilter = FrameworkUtil.createFilter("(" + Constants.OBJECTCLASS + "=" + HelloWorld.class.getName() + ")");
      ServiceTracker tracker = new ServiceTracker(appContext, 
          osgiFilter,
          null);
      
      tracker.open();
      final Object hw = tracker.waitForService(DEFAULT_TIMEOUT);
      tracker.close();

      if (hw != null) {
        // proxy because the class space between the sample app and the test bundle is not consistent
        return new HelloWorld() {
          public String getMessage() {
            try {
              Method m = hw.getClass().getMethod("getMessage");
              return (String) m.invoke(hw);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        };
      } else {
        return null;
      }

    } else {
      throw new IllegalStateException("Expected to find isolated app ctx, but didn't");
    }
  }
}
