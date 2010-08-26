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
 * */

package org.apache.aries.application.deployment.management.impl;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.aries.application.deployment.management.internal.BundleBlueprintParser;
import org.apache.aries.application.filesystem.IDirectory;
import org.apache.aries.application.filesystem.IFile;
import org.apache.aries.application.management.InvalidAttributeException;
import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ModelledResourceManager;
import org.apache.aries.application.modelling.ModellerException;
import org.apache.aries.application.modelling.ParsedServiceElements;
import org.apache.aries.application.modelling.ParserProxy;
import org.apache.aries.application.modelling.utils.ModellingManager;
import org.apache.aries.application.utils.manifest.BundleManifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ModelledResourceManagerImpl implements ModelledResourceManager
{
  private Logger _logger = LoggerFactory.getLogger(ModelledResourceManagerImpl.class);

  private ParserProxy parserProxy;

  public ParserProxy getParserProxy()
  {
    return parserProxy;
  }
  public void setParserProxy(ParserProxy parserProxy)
  {
    this.parserProxy = parserProxy;
  }


  /**
   * For a given file, which we know to be a bundle, parse out all the
   * service, reference and reference-list elements. This method will return
   * all such services, including anonymous ones, 
   * but should not return indistinguishable duplicates. 
   * @param archive CommonArchive. The caller is responsible for closing this afterwards. 
   * @return ParsedServiceElementsImpl 
   * @throws OpenFailureException 
   */
  public ParsedServiceElements getServiceElements (IDirectory archive) throws ModellerException { 

    _logger.debug(LOG_ENTRY,"getServiceElements", archive );

    Set<ExportedService> services = new HashSet<ExportedService>();
    Set<ImportedService> references = new HashSet<ImportedService>();
    try { 
      Collection<IFile> blueprints = findBlueprints(archive);
      InputStream is = null;
      for (IFile bpFile : blueprints) {
        URL url = bpFile.toURL();
        URLConnection conn = url.openConnection();
        is = conn.getInputStream();
        //is = this.getClass().getResourceAsStream(bpFile.getName());
        try {
          ParsedServiceElements pse = parserProxy.parseAllServiceElements(is);
          services.addAll(pse.getServices());
          references.addAll(pse.getReferences());

        } finally {
          if (is != null) {
            is.close();
          }
        }
      }
    } catch (URISyntaxException e) {
      ModellerException m = new ModellerException(e);
      _logger.debug(LOG_EXIT, "getServiceElements", m);
      throw m;
    } catch (IOException e) {
      ModellerException m = new ModellerException(e);
      _logger.debug(LOG_EXIT, "getServiceElements", m);
      throw m;
    } catch (Exception e) {
      ModellerException m = new ModellerException(e);
      _logger.debug(LOG_EXIT, "getServiceElements", m);
      throw m;
    } 
    ParsedServiceElements result = ModellingManager.getParsedServiceElements(services, references);
    _logger.debug(LOG_EXIT, "getServiceElements", result);
    return result;
  }


  /**
   * Helper method to pass a single bundle into findBlueprints 
   * @param oneBundle a single bundle
   * @return Files for all the blueprint files within the bundle
   * @throws URISyntaxException
   * @throws IOException
   * @throws OpenFailureException
   */
  private Collection<IFile> findBlueprints (IDirectory oneBundle) 
  throws  IOException
  {
    _logger.debug(LOG_ENTRY, "findBlueprints", oneBundle);
    Set<IDirectory> archiveSet = new HashSet<IDirectory>();
    archiveSet.add(oneBundle);
    Collection<IFile> result = findBlueprints (archiveSet);
    _logger.debug(LOG_EXIT, "findBlueprints", result);
    return result;
  }

  /**
   * Locate all blueprint xml files located within a set of bundles. Typically, call findApplicationBundles()
   * first to determine which bundles within an EBA fall within the range of the Application-Content header. 
   * (See the comment on that method). 
   * @param applicationBundles
   * @return A Collection of blue print files
   * @throws URISyntaxException
   * @throws IOException
   * @throws OpenFailureException
   */
  private Collection<IFile> findBlueprints(Collection<IDirectory> applicationBundles)
  throws IOException
  {
    _logger.debug(LOG_ENTRY, "findBlueprints", applicationBundles);
    Collection<IFile> blueprints = new ArrayList<IFile>();
    for (IDirectory appBundle : applicationBundles) {
      if (appBundle != null) {
        File bundleFile = new File(appBundle.toString());
        BundleManifest bundleMf = BundleManifest.fromBundle(bundleFile);
        BundleBlueprintParser bpParser = new BundleBlueprintParser(bundleMf);
        ZipFile zipFile = new ZipFile(bundleFile);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
          ZipEntry ze = entries.nextElement();

          System.out.println(ze.getName());
        }
        List<IFile> files = appBundle.listAllFiles();
        Iterator<IFile> it = files.iterator();
        while (it.hasNext()) {
          IFile file = (IFile) it.next();         
          String directoryFullPath = file.getName(); 
          String directoryName = "";
          String fileName = "";
          if (directoryFullPath.lastIndexOf("/") != -1) {
            directoryName = directoryFullPath.substring(0, directoryFullPath.lastIndexOf("/"));
            fileName = directoryFullPath.substring(directoryFullPath.lastIndexOf("/") + 1);
          } else {
            if (file.isFile()) {
              directoryName="";
              fileName = directoryFullPath;
            } 

          }
          if (bpParser.isBPFile(directoryName, fileName)) {
            blueprints.add(file);
          }
        }
      }
    }
    _logger.debug(LOG_EXIT, "findBlueprints", blueprints);
    return blueprints;
  }

  public ModelledResource getModelledResource(String uri, IDirectory bundle) throws ModellerException{
    _logger.debug(LOG_ENTRY, "getModelledResource", new Object[]{uri, bundle});
    ParsedServiceElements pse = getServiceElements(bundle);

    BundleManifest bm = BundleManifest.fromBundle(new File(bundle.toString()));
    Attributes attributes = bm.getRawAttributes();
    ModelledResource mbi;
    try {
      mbi = ModellingManager.getModelledResource(uri, attributes, pse.getReferences(), pse.getServices());
    } catch (InvalidAttributeException iae) {
      throw new ModellerException(iae);
    }
    _logger.debug(LOG_EXIT, "getModelledResource", mbi);
    return mbi;
  }

}
