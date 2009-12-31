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

package org.apache.aries.application.management.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.ApplicationMetadataManager;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.DeploymentMetadataFactory;
import org.apache.aries.application.filesystem.IDirectory;
import org.apache.aries.application.filesystem.IFile;
import org.apache.aries.application.management.ApplicationContext;
import org.apache.aries.application.management.ApplicationListener;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.application.management.AriesApplicationResolver;
import org.apache.aries.application.management.BundleConverter;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.ManagementException;
import org.apache.aries.application.utils.AppConstants;
import org.apache.aries.application.utils.filesystem.FileSystem;
import org.apache.aries.application.utils.manifest.BundleManifest;
import org.apache.aries.application.utils.manifest.ManifestDefaultsInjector;
import org.apache.aries.application.utils.manifest.ManifestProcessor;
import org.osgi.framework.ServiceException;

public class AriesApplicationManagerImpl implements AriesApplicationManager {

  private ApplicationMetadataManager _applicationMetadataManager;
  private DeploymentMetadataFactory _deploymentMetadataFactory;
  private List<BundleConverter> _bundleConverters;
  private AriesApplicationResolver _resolver;

  public void setApplicationMetadataManager (ApplicationMetadataManager amm) { 
    _applicationMetadataManager = amm;
  }
  
  public void setDeploymentMetadataFactory (DeploymentMetadataFactory dmf) { 
    _deploymentMetadataFactory = dmf;
  }
  
  public void setBundleConverters (List<BundleConverter> bcs) { 
    _bundleConverters = bcs;
  }
  
  public void setResolver (AriesApplicationResolver resolver) { 
    _resolver = resolver;
  }
  
  
  
  /**
   * Create an AriesApplication from a .eba file
   */
  public AriesApplication createApplication(File ebaFile) throws ManagementException {
    /* 
     * ebaFile should be a zip file with a '.eba' extension 
     * as per http://incubator.apache.org/aries/applications.html
     */    

    ApplicationMetadata applicationMetadata;
    DeploymentMetadata deploymentMetadata;
    Map<String, InputStream> modifiedBundles = new HashMap<String, InputStream>();
    AriesApplicationImpl application = null;
    
    try { 
      if (!ebaFile.isFile()) { 
        // TODO: NLS this, and implement create-from-directory
        throw new ManagementException ("Cannot create .eba from directory yet");
      }
      
      // Locate META-INF/APPLICATION.MF and ensure that the 
      // manifest has the necessary fields set 
      Manifest applicationManifest = parseManifest (ebaFile, AppConstants.APPLICATION_MF);
      boolean manifestChanged = ManifestDefaultsInjector.updateManifest(applicationManifest, ebaFile.getName(), ebaFile); 
      applicationMetadata = _applicationMetadataManager.createApplicationMetadata(applicationManifest);

      Manifest deploymentManifest = parseManifest (ebaFile, AppConstants.DEPLOYMENT_MF);
      if (deploymentManifest != null) {
        // If there's a deployment.mf present, check it matches applicationManifest, and if so, use it
      } else { 
        //   -- Look for application.xml to support .war file migration
        //   -- Process any other files in the .eba, i.e. migrate wars to wabs, plain jars to bundles
        IDirectory eba = FileSystem.getFSRoot(ebaFile);
        IFile appXml = eba.getFile(AppConstants.APPLICATION_XML);
        Set<BundleInfo> bundleInfo = new HashSet<BundleInfo>();
        for (IFile f : eba) { 
          BundleManifest bm = getBundleManifest (f);
          if (bm != null) {
            Attributes bundleAttributes;
            if (bm.isValid()) {
              bundleInfo.add(new BundleInfoImpl(bm, null));
            } else { 
              // We have a jar that needs converting to a bundle, or a war to migrate to a WAB
              InputStream is = null;
              try { 
                is = f.open();
                InputStream convertedBinary = null;
                Iterator<BundleConverter> converters = _bundleConverters.iterator();
                while (converters.hasNext() && convertedBinary == null) { 
                  try { 
                    convertedBinary = converters.next().convert(is, f.getName());
                  } catch (ServiceException sx) {
                    // We'll get this if our optional BundleConverter has not been injected. 
                  }
                }
                if (convertedBinary != null) { 
                  modifiedBundles.put (f.getName(), convertedBinary); // I expect I'll need the filename in a bit
                  bm = BundleManifest.fromBundle(is);
                  bundleInfo.add(new BundleInfoImpl(bm, null));
                }
              } finally { 
                try { 
                  if (is != null) is.close();
                } catch (IOException iox) {}
              }
            }
          }
        }
        application = new AriesApplicationImpl (applicationMetadata, bundleInfo);
        Set<BundleInfo> resolvedBundles = _resolver.resolve(application);
        deploymentMetadata = _deploymentMetadataFactory.createDeploymentMetadata(application, resolvedBundles);
        application.setDeploymentMetadata(deploymentMetadata);
      }
      
      // We may have changed parts of its content. The application's store()
      // method needs to be able to work. Do something with modifiedBundles 
      // and manifestChanged
      
    } catch (IOException iox) { 
      throw new ManagementException(iox);
    }
    
    return application;
  }

  public AriesApplication createApplication(URL url) throws ManagementException {
    return null;
  }

  public ApplicationContext getApplicationContext(AriesApplication app) {
    // TODO Auto-generated method stub
    return null;
  }

  public Set<ApplicationContext> getApplicationContexts() {
    // TODO Auto-generated method stub
    return null;
  }

  public ApplicationContext install(AriesApplication app) {
    // TODO Auto-generated method stub
    return null;
  }
  
  public void uninstall(ApplicationContext app) {
    // TODO Auto-generated method stub

  }

  public void addApplicationListener(ApplicationListener l) {
    // Need application listener lifecycle support
  }

  public void removeApplicationListener(ApplicationListener l) {
    // TODO Auto-generated method stub

  }



  /**
   * Locate and parse an application or deployment.mf in an eba
   * @param ebaFile An aries application file
   * @param fileName META-INF/APPLICATION.MF or META-INF/DEPLOYMENT.MF
   * @return parsed manifest, or null
   * @throws IOException
   */
  private Manifest parseManifest (File ebaFile, String fileName) throws IOException {
    Manifest result = null;
    IDirectory eba = FileSystem.getFSRoot(ebaFile);
    List<IFile> files = eba.listFiles();
    for (IFile f : files) { 
      if (f.getName().equalsIgnoreCase(fileName)) {
        InputStream is = null;
        try { 
          is = f.open();
          result = ManifestProcessor.parseManifest(is);
          is.close();
        } catch (IOException iox) { 
          // TODO: log error
          throw iox;
        } finally { 
          try { 
            if (is != null) is.close();
          } catch (IOException iox) {}
        }
        break;
      }
    }
    return result;
  }
  
  /**
   * Extract a bundle manifest from an IFile representing a bundle
   * @param file The bundle to extract the manifest from
   * @return bundle manifest
   */
  private BundleManifest getBundleManifest(IFile file) throws IOException {
    BundleManifest mf = null;
    InputStream in = null;
    try { 
      in = file.open();
      mf = BundleManifest.fromBundle(in);
    } finally { 
      try { 
        if (in != null) in.close();
      } catch (IOException iox) {}
    }    
    return mf;
  } 
  
  
}
