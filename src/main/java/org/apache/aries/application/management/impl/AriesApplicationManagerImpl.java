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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.ApplicationMetadataFactory;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.DeploymentMetadataFactory;
import org.apache.aries.application.filesystem.IDirectory;
import org.apache.aries.application.filesystem.IFile;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationContextManager;
import org.apache.aries.application.management.AriesApplicationListener;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.application.management.AriesApplicationResolver;
import org.apache.aries.application.management.BundleConversion;
import org.apache.aries.application.management.BundleConverter;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.ConversionException;
import org.apache.aries.application.management.LocalPlatform;
import org.apache.aries.application.management.ManagementException;
import org.apache.aries.application.management.ResolveConstraint;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.management.internal.MessageUtil;
import org.apache.aries.application.utils.AppConstants;
import org.apache.aries.application.utils.filesystem.FileSystem;
import org.apache.aries.application.utils.filesystem.IOUtils;
import org.apache.aries.application.utils.management.SimpleBundleInfo;
import org.apache.aries.application.utils.manifest.BundleManifest;
import org.apache.aries.application.utils.manifest.ManifestDefaultsInjector;
import org.apache.aries.application.utils.manifest.ManifestProcessor;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AriesApplicationManagerImpl implements AriesApplicationManager {

  private ApplicationMetadataFactory _applicationMetadataFactory;
  private DeploymentMetadataFactory _deploymentMetadataFactory;
  private List<BundleConverter> _bundleConverters;
  private AriesApplicationResolver _resolver;
  private LocalPlatform _localPlatform;
  private AriesApplicationContextManager _applicationContextManager;

  private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.application.management.impl");

  public void setApplicationMetadataFactory (ApplicationMetadataFactory amf) { 
    _applicationMetadataFactory = amf;
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
  
  public void setApplicationContextManager (AriesApplicationContextManager acm) { 
    _applicationContextManager = acm;
  }
  
  
  /**
   * Create an AriesApplication from a .eba file: a zip file with a '.eba' extension
   * as per http://incubator.apache.org/aries/applications.html 
   */
  public AriesApplication createApplication(IDirectory ebaFile) throws ManagementException {
    ApplicationMetadata applicationMetadata = null;
    DeploymentMetadata deploymentMetadata = null;
    Map<String, BundleConversion> modifiedBundles = new HashMap<String, BundleConversion>();
    AriesApplicationImpl application = null;
    
    try { 
      Manifest applicationManifest = parseApplicationManifest (ebaFile);
      ManifestDefaultsInjector.updateManifest(applicationManifest, ebaFile.getName(), ebaFile); 
      applicationMetadata = _applicationMetadataFactory.createApplicationMetadata(applicationManifest);

      IFile deploymentManifest = ebaFile.getFile(AppConstants.DEPLOYMENT_MF);
      if (deploymentManifest != null) { 
        deploymentMetadata = _deploymentMetadataFactory.createDeploymentMetadata(deploymentManifest);
        
        // Validate: symbolic names must match
        String appSymbolicName = applicationMetadata.getApplicationSymbolicName();
        String depSymbolicName = deploymentMetadata.getApplicationSymbolicName();
        if (!appSymbolicName.equals(depSymbolicName)) {
          throw new ManagementException (MessageUtil.getMessage("APPMANAGEMENT0002E", ebaFile.getName(), appSymbolicName, depSymbolicName));
        }
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
            extraBundlesInfo.add(new SimpleBundleInfo(_applicationMetadataFactory, bm, f.toURL().toExternalForm()));
          } else if (deploymentMetadata != null) {
            throw new ManagementException (MessageUtil.getMessage("APPMANAGEMENT0003E", f.getName(), ebaFile.getName()));
          } else { 
            // We have a jar that needs converting to a bundle, or a war to migrate to a WAB
            BundleConversion convertedBinary = null;
            Iterator<BundleConverter> converters = _bundleConverters.iterator();
            List<ConversionException> conversionExceptions = Collections.emptyList();
            while (converters.hasNext() && convertedBinary == null) { 
              try { 
                convertedBinary = converters.next().convert(ebaFile, f);
              } catch (ServiceException sx) {
                // We'll get this if our optional BundleConverter has not been injected. 
              } catch (ConversionException cx) { 
                conversionExceptions.add(cx);
              }
            }
            if (conversionExceptions.size() > 0) {
              for (ConversionException cx : conversionExceptions) { 
                _logger.error("APPMANAGEMENT0004E", new Object[]{f.getName(), ebaFile.getName(), cx});
              }
              throw new ManagementException (MessageUtil.getMessage("APPMANAGEMENT0005E", ebaFile.getName()));
            }
            if (convertedBinary != null) { 
              modifiedBundles.put (f.getName(), convertedBinary);
              bm = BundleManifest.fromBundle(f);
              extraBundlesInfo.add(new SimpleBundleInfo(_applicationMetadataFactory, bm, f.getName()));
            }
          }
        } 
      }

      application = new AriesApplicationImpl (applicationMetadata, extraBundlesInfo, _localPlatform);
      application.setDeploymentMetadata(deploymentMetadata);
      // Store a reference to any modified bundles
      application.setModifiedBundles (modifiedBundles);
    } catch (IOException iox) {
      _logger.error ("APPMANAGEMENT0006E", new Object []{ebaFile.getName(), iox});
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

  public AriesApplication resolve(AriesApplication originalApp, ResolveConstraint... constraints) throws ResolverException {
    AriesApplicationImpl application = new AriesApplicationImpl(originalApp.getApplicationMetadata(), originalApp.getBundleInfo(), _localPlatform);
    Set<BundleInfo> additionalBundlesRequired = _resolver.resolve(application, constraints);
    DeploymentMetadata deploymentMetadata = _deploymentMetadataFactory.createDeploymentMetadata(application, additionalBundlesRequired);
    application.setDeploymentMetadata(deploymentMetadata);
    // Store a reference to any modified bundles
    if (originalApp instanceof AriesApplicationImpl) {
        // TODO: are we really passing streams around ?
        application.setModifiedBundles(((AriesApplicationImpl) originalApp).getModifiedBundles());
    }
    return application;
  } 

  public AriesApplicationContext install(AriesApplication app) throws BundleException, ManagementException, ResolverException {
    if (!app.isResolved()) {
        app = resolve(app);
    }
    AriesApplicationContext result = _applicationContextManager.getApplicationContext(app);
    return result;
  }
  
  public void uninstall(AriesApplicationContext app) throws BundleException 
  {
    _applicationContextManager.remove(app);
  }

  public void addApplicationListener(AriesApplicationListener l) {
    // Need application listener lifecycle support
  }

  public void removeApplicationListener(AriesApplicationListener l) {
    // TODO Auto-generated method stub

  }



  /**
   * Locate and parse an application.mf in an eba
   * @param source An aries application file
   * @return parsed manifest, or an empty Manifest
   * @throws IOException
   */
  private Manifest parseApplicationManifest (IDirectory source) throws IOException {
    Manifest result = new Manifest();
    IFile f = source.getFile(AppConstants.APPLICATION_MF);
    if (f != null) { 
      InputStream is = null;
      try { 
        is = f.open();
        result = ManifestProcessor.parseManifest(is);
      } catch (IOException iox) { 
        _logger.error ("APPMANAGEMENT0007E", new Object[]{source.getName(), iox});
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
