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

package org.apache.aries.application.modelling.impl;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ModelledResourceManager;
import org.apache.aries.application.modelling.ModellerException;
import org.apache.aries.application.modelling.ModellingManager;
import org.apache.aries.application.modelling.ParsedServiceElements;
import org.apache.aries.application.modelling.ParserProxy;
import org.apache.aries.application.modelling.ServiceModeller;
import org.apache.aries.application.modelling.internal.BundleBlueprintParser;
import org.apache.aries.application.modelling.internal.MessageUtil;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.ICloseableDirectory;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;
import org.apache.aries.util.io.IOUtils;
import org.apache.aries.util.manifest.BundleManifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ModelledResourceManagerImpl implements ModelledResourceManager
{
  private final Logger _logger = LoggerFactory.getLogger(ModelledResourceManagerImpl.class);
  private ParserProxy _parserProxy;
  private ModellingManager _modellingManager;
  private Collection<ServiceModeller> modellingPlugins;

  public void setModellingPlugins(Collection<ServiceModeller> modellingPlugins) {
    this.modellingPlugins = modellingPlugins;
  }

  public void setModellingManager (ModellingManager m) { 
    _modellingManager = m;
  }

  public void setParserProxy (ParserProxy p) { 
    _parserProxy = p;
  }
  
  public ParserProxy getParserProxy() {
    return _parserProxy;
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
      BundleManifest bm = BundleManifest.fromBundle(archive);
      return getServiceElements(bm, archive);
  }
  
  public ParsedServiceElements getServiceElements(InputStreamProvider archive) throws ModellerException {
      ICloseableDirectory dir = null;
      try {
          dir = FileSystem.getFSRoot(archive.open());
          BundleManifest bm = BundleManifest.fromBundle(dir);
          return getServiceElements(bm, dir);
      } catch (IOException e) {
          throw new ModellerException(e);
      } finally {
          IOUtils.close(dir);
      }
  }
  
  private ParsedServiceElements getServiceElements (BundleManifest bundleMf, IDirectory archive) throws ModellerException { 
      
      Set<ExportedService> services = new HashSet<ExportedService>();
      Set<ImportedService> references = new HashSet<ImportedService>();
    
      try {
          ParsedServiceElements pse = getBlueprintServiceElements(bundleMf, 
                      findBlueprints(bundleMf, archive));
          services.addAll(pse.getServices());
          references.addAll(pse.getReferences());
          for (ServiceModeller sm : modellingPlugins) {
            pse = sm.modelServices(bundleMf, archive);
            services.addAll(pse.getServices());
            references.addAll(pse.getReferences());
          }
          return new ParsedServiceElementsImpl(services, references);
      } catch (Exception e) {
          throw new ModellerException(e);
      }
  }
  
  private ParsedServiceElements getBlueprintServiceElements (BundleManifest bundleMf, Iterable<InputStream> blueprints) throws ModellerException { 
    _logger.debug(LOG_ENTRY,"getServiceElements", new Object[] {bundleMf, blueprints} );

    Set<ExportedService> services = new HashSet<ExportedService>();
    Set<ImportedService> references = new HashSet<ImportedService>();
    try { 
      for (InputStream is : blueprints) {
        try {
          ParsedServiceElements pse = getParserProxy().parseAllServiceElements(is);
          services.addAll(pse.getServices());
          references.addAll(pse.getReferences());
        } finally {
          IOUtils.close(is);
        }
      }
    } catch (Exception e) {
      ModellerException m = new ModellerException(e);
      _logger.debug(LOG_EXIT, "getServiceElements", m);
      throw m;
    } 
    ParsedServiceElements result = _modellingManager.getParsedServiceElements(services, references);
    _logger.debug(LOG_EXIT, "getServiceElements", result);
    return result;
  }

  public ModelledResource getModelledResource(IDirectory bundle) throws ModellerException {
      try {
          return getModelledResource(bundle.toURL().toURI().toString(), bundle);
      } catch (MalformedURLException mue) {
          throw new ModellerException(mue);
      } catch (URISyntaxException use) {
          throw new ModellerException(use);
      }
  }

  public ModelledResource getModelledResource(String uri, InputStreamProvider bundle) throws ModellerException {
      ICloseableDirectory dir = null;
      try {
          dir = FileSystem.getFSRoot(bundle.open());
          return getModelledResource(uri, dir);
      } catch (IOException e) {
          throw new ModellerException(e);
      } finally {
          IOUtils.close(dir);
      }
  }
  
  public ModelledResource getModelledResource(String uri, IDirectory bundle) throws ModellerException{
    _logger.debug(LOG_ENTRY, "getModelledResource", new Object[]{uri, bundle});

    if (bundle != null) {
        BundleManifest bm = BundleManifest.fromBundle(bundle);
        ParsedServiceElements pse = getServiceElements(bm, bundle);
        return model(uri, bm, pse);
    } else {
      // The bundle does not exist
      ModellerException me = new ModellerException(MessageUtil.getMessage("INVALID_BUNDLE_LOCATION", bundle));
      _logger.debug(LOG_EXIT, "getModelledResource", me);
      throw me;
    }

  }
  
  private ModelledResource model(String uri, BundleManifest bm, ParsedServiceElements pse) throws ModellerException {
      Attributes attributes = bm.getRawAttributes();
      ModelledResource mbi = null;
      try {
        mbi = _modellingManager.getModelledResource(uri, attributes, pse.getReferences(), pse.getServices());
      } catch (InvalidAttributeException iae) {
        ModellerException me = new ModellerException(iae);
        _logger.debug(LOG_EXIT, "getModelledResource", me);
        throw me;
      }
      _logger.debug(LOG_EXIT, "getModelledResource", mbi);
      return mbi;      
  }


  
  /**
   * Helper method to pass a single bundle into findBlueprints 
   * @param bundleMf The bundle manifest 
   * @param oneBundle a single bundle
   * @return Files for all the blueprint files within the bundle
   * @throws URISyntaxException
   * @throws IOException
   */
  private Iterable<InputStream> findBlueprints(BundleManifest bundleMf, IDirectory bundle) throws IOException
  {
    _logger.debug(LOG_ENTRY, "findBlueprints", bundle);

    Collection<IFile> blueprints = new ArrayList<IFile>();
    BundleBlueprintParser bpParser = new BundleBlueprintParser(bundleMf);
    
    /* OSGi R5 Spec, section 121.3.4: "If the Bundle-Blueprint header is specified but empty, then the Blueprint 
     * bundle must not be managed. This can be used to temporarily disable a Blueprint bundle."
     */
    if (bpParser.mightContainBlueprint()) { 
	    List<IFile> files = bundle.listAllFiles();
	    Iterator<IFile> it = files.iterator();
	    while (it.hasNext()) {
	        IFile file = it.next();         
	        String directoryFullPath = file.getName(); 
	        String directoryName = "";
	        String fileName = "";
	        if (directoryFullPath.lastIndexOf("/") != -1) {
	        	// This bundle may be nested within another archive. In that case, we need to trim
	        	// /bundleFileName.jar from the front of the directory. 
	        	int bundleNameLength = bundle.getName().length();
	            directoryName = directoryFullPath.substring(bundleNameLength, directoryFullPath.lastIndexOf("/"));
	            if (directoryName.startsWith("/") && directoryName.length() > 1) { 
	            	directoryName = directoryName.substring(1);
	            }
	            fileName = directoryFullPath.substring(directoryFullPath.lastIndexOf("/") + 1);
	        } else {
	            if (file.isFile()) {
	                directoryName="";
	                fileName = directoryFullPath;
	            } 
	
	        }
	        if (!file.isDirectory() && bpParser.isBPFile(directoryName, fileName)) {
	            blueprints.add(file);
	        }
	    }
    }
    
    Collection<InputStream> result = new ArrayList<InputStream>();
    try {
        for (IFile bp : blueprints) result.add(bp.open());
    } catch (IOException e) {
        // if something went wrong, make sure we still clean up
        for (InputStream is : result) IOUtils.close(is);
        
        throw e;
    }
    
    _logger.debug(LOG_EXIT, "findBlueprints", result);
    return result;
  }

  private class ZipBlueprintIterator implements Iterator<InputStream> {
      private final ZipInputStream zip;
      private final BundleBlueprintParser bpParser;
      private boolean valid;
      
      public ZipBlueprintIterator(ZipInputStream zip, BundleBlueprintParser bpParser) {
          this.zip = zip;
          this.bpParser = bpParser;
      }

      public boolean hasNext() {
          valid = false;
          ZipEntry entry;
          
          try {
              while (!valid && (entry = zip.getNextEntry()) != null) {
                  if (!entry.isDirectory()) {
                      String name = entry.getName();
                      String directory = "";
                      int index = name.lastIndexOf('/');
                      if (index != -1) {
                          directory = name.substring(0, index);
                          name = name.substring(index+1);
                      }
                      
                      if (bpParser.isBPFile(directory, name)) {
                          valid = true;
                      }
                      
                  }
              }
          } catch (IOException e) {
              _logger.error("Could not open next zip entry", e);
          }
          
          return valid;
      }

      public InputStream next() {
          if (!valid) throw new IllegalStateException();
          
          return new InputStream() {
            public int read() throws IOException {
                return zip.read();
            }
              
            @Override
            public void close() {
                // intercept close so that the zipinputstream stays open
            }
          };
      }

      public void remove() {
          throw new UnsupportedOperationException();
      }
      
  }
  
  /**
   * Internal use only. Different to the general Iterable interface this can return an Iterator only once.
   */
  private Iterable<InputStream> findBlueprints(BundleManifest bundleMf, InputStream stream) {
      final BundleBlueprintParser bpParser = new BundleBlueprintParser(bundleMf);
      final ZipInputStream zip = new ZipInputStream(stream);
      
      return new Iterable<InputStream>() {
        public Iterator<InputStream> iterator() {
            return new ZipBlueprintIterator(zip, bpParser);
        }
    };
  }

}
