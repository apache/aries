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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.apache.aries.application.management.LocalPlatform;
import org.apache.aries.application.management.ManagementException;
import org.apache.aries.application.management.ResolveConstraint;
import org.apache.aries.application.utils.AppConstants;
import org.apache.aries.application.utils.filesystem.FileSystem;
import org.apache.aries.application.utils.filesystem.IOUtils;
import org.apache.aries.application.utils.manifest.BundleManifest;
import org.apache.aries.application.utils.manifest.ManifestDefaultsInjector;
import org.apache.aries.application.utils.manifest.ManifestProcessor;
import org.osgi.framework.ServiceException;

public class AriesApplicationManagerImpl implements AriesApplicationManager {

  private ApplicationMetadataManager _applicationMetadataManager;
  private DeploymentMetadataFactory _deploymentMetadataFactory;
  private List<BundleConverter> _bundleConverters;
  private AriesApplicationResolver _resolver;
  private LocalPlatform _localPlatform;

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

  public void setLocalPlatform (LocalPlatform lp) { 
    _localPlatform = lp;
  }
  
  
  /**
   * Create an AriesApplication from a .eba file: a zip file with a '.eba' extension
   * as per http://incubator.apache.org/aries/applications.html 
   */
  public AriesApplication createApplication(IDirectory ebaFile) throws ManagementException {
    ApplicationMetadata applicationMetadata = null;
    DeploymentMetadata deploymentMetadata = null;
    Map<String, InputStream> modifiedBundles = new HashMap<String, InputStream>();
    AriesApplicationImpl application = null;
    
    try { 
      Manifest applicationManifest = parseApplicationManifest (ebaFile);
      ManifestDefaultsInjector.updateManifest(applicationManifest, ebaFile.getName(), ebaFile); 
      applicationMetadata = _applicationMetadataManager.createApplicationMetadata(applicationManifest);

      IFile deploymentManifest = ebaFile.getFile(AppConstants.DEPLOYMENT_MF);
      if (deploymentManifest != null) { 
        deploymentMetadata = _deploymentMetadataFactory.createDeploymentMetadata(deploymentManifest);
      }
      /* We require that all other .jar and .war files included by-value be valid bundles
       * because a DEPLOYMENT.MF has been provided. If no DEPLOYMENT.MF, migrate 
       * wars to wabs, plain jars to bundles
       */
        
      Set<BundleInfo> extraBundlesInfo = new HashSet<BundleInfo>();
      for (IFile f : ebaFile) { 
        if (f.isDirectory()) { 
          continue;
        }
        
        BundleManifest bm = getBundleManifest (f);
        if (bm != null) {
          if (bm.isValid()) {
            extraBundlesInfo.add(new BundleInfoImpl(bm, f.toURL().toExternalForm()));
          } else if (deploymentMetadata != null) {
            throw new ManagementException ("Invalid bundle " + f.getName() + " found when DEPLOYMENT.MF present");
          } else { 
            // We have a jar that needs converting to a bundle, or a war to migrate to a WAB
            InputStream convertedBinary = null;
            Iterator<BundleConverter> converters = _bundleConverters.iterator();
            while (converters.hasNext() && convertedBinary == null) { 
              try { 
                // WarToWabConverter can extract application.xml via
                // eba.getFile(AppConstants.APPLICATION_XML);
                convertedBinary = converters.next().convert(ebaFile, f);
              } catch (ServiceException sx) {
                // We'll get this if our optional BundleConverter has not been injected. 
              }
            }
            if (convertedBinary != null) { 
              modifiedBundles.put (f.getName(), convertedBinary);
              bm = BundleManifest.fromBundle(f);
              extraBundlesInfo.add(new BundleInfoImpl(bm, f.getName()));
            }
          }
        } 
        if (deploymentMetadata != null) { 
          application = new AriesApplicationImpl (applicationMetadata, deploymentMetadata, extraBundlesInfo, _localPlatform);
        } else { 
          application = new AriesApplicationImpl (applicationMetadata, extraBundlesInfo, _localPlatform);
          Set<BundleInfo> additionalBundlesRequired = _resolver.resolve(application);
          deploymentMetadata = _deploymentMetadataFactory.createDeploymentMetadata(application, additionalBundlesRequired);
          application.setDeploymentMetadata(deploymentMetadata);
        
          // Store a reference to any modified bundles
          application.setModifiedBundles (modifiedBundles);
        }
        
      }
    } catch (IOException iox) { 
      // Log an error
      throw new ManagementException(iox);
    }
    
    return application;
  }

  /**
   * Create an application from a URL. 
   * The first version of this method isn't smart enough to check whether
   * the input URL is file://
   */
  public AriesApplication createApplication(URL url) throws ManagementException {
    OutputStream os = null;
    AriesApplication app = null;
    try { 
      File tempFile = _localPlatform.getTemporaryFile();
      InputStream is = url.openStream();
      os = new FileOutputStream (tempFile);
      IOUtils.copy(is, os);
      IDirectory downloadedSource = FileSystem.getFSRoot(tempFile);
      app = createApplication (downloadedSource);
    } catch (IOException iox) {
      throw new ManagementException (iox);
    }
      finally { 
      IOUtils.close(os);
    }
    return app;
  }

  public AriesApplication resolve(AriesApplication originalApp,
      ResolveConstraint... constraints) {
    // TODO Auto-generated method stub
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
   * Locate and parse an application.mf in an eba
   * @param source An aries application file
   * @return parsed manifest, or null
   * @throws IOException
   */
  private Manifest parseApplicationManifest (IDirectory source) throws IOException {
    Manifest result = null;
    IFile f = source.getFile(AppConstants.APPLICATION_MF);
    if (f != null) { 
      InputStream is = null;
      try { 
        is = f.open();
        result = ManifestProcessor.parseManifest(is);
      } catch (IOException iox) { 
        // TODO: log error
        throw iox;
      } finally { 
        IOUtils.close(is);
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
      IOUtils.close(in);
    }    
    return mf;
  }

}
