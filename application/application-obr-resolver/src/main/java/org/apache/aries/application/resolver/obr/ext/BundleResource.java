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

package org.apache.aries.application.resolver.obr.ext;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.aries.application.modelling.ExportedPackage;
import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedBundle;
import org.apache.aries.application.modelling.ImportedPackage;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ModellingConstants;
import org.apache.aries.application.resolver.obr.impl.OBRCapability;
import org.apache.aries.application.resolver.obr.impl.RequirementImpl;
import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleResource implements Resource
{
  
  private final ModelledResource _modelledBundle;
  private final Collection<Capability> _capabilities;
  private final Collection<Requirement> _requirements;
  private final String _displayName;
  private Logger logger = LoggerFactory.getLogger(BundleResource.class);
  
  /**
   * Build a BundleResource from another BundleResource and some optional extra capabilities and requirements
   * @param br
   * @param extraCapabilities can be null
   * @param extraRequirements can be null
   */
  public BundleResource (BundleResource br, Collection<Capability> extraCapabilities, Collection<Requirement> extraRequirements) { 
    _modelledBundle = br._modelledBundle;
    _capabilities = new ArrayList<Capability> (br._capabilities);
    _requirements = new ArrayList<Requirement> (br._requirements);
    _displayName = new String (br._displayName);
    if (extraCapabilities != null) _capabilities.addAll(extraCapabilities);
    if (extraRequirements != null) _requirements.addAll(extraRequirements);
  }
  
  public BundleResource (ModelledResource mb, RepositoryAdmin repositoryAdmin) { 
    logger.debug(LOG_ENTRY,"BundleResource", mb);
    _modelledBundle = mb;
    
    _capabilities = new ArrayList<Capability>();
    _capabilities.add(new OBRCapability(_modelledBundle.getExportedBundle(), repositoryAdmin));

    for (ExportedPackage pkg : _modelledBundle.getExportedPackages()) {
      _capabilities.add(new OBRCapability(pkg, repositoryAdmin));
    }

    for (ExportedService svc : _modelledBundle.getExportedServices()) {
      _capabilities.add(new OBRCapability(svc, repositoryAdmin));
    }

    _requirements = new ArrayList<Requirement>();
    for (ImportedPackage pkg : _modelledBundle.getImportedPackages()) {
      _requirements.add(new RequirementImpl(pkg));
    }
    
    for (ImportedService svc : _modelledBundle.getImportedServices()) { 
      _requirements.add(new RequirementImpl(svc));
    }
    
    for (ImportedBundle requiredBundle : _modelledBundle.getRequiredBundles()) { 
      _requirements.add(new RequirementImpl(requiredBundle));
    }
    
    if(mb.isFragment())
      _requirements.add(new RequirementImpl(mb.getFragmentHost()));

    String possibleDisplayName = (String) mb.getExportedBundle().getAttributes().get(
        ModellingConstants.OBR_PRESENTATION_NAME);
    if (possibleDisplayName == null) {
      _displayName = mb.getSymbolicName();
    } else {
      _displayName = possibleDisplayName;
    }
    
    
    
    logger.debug(LOG_EXIT,"BundleResource");
    
  }

  public ModelledResource getModelledResource() { 
    return _modelledBundle;
  }
  
  public Capability[] getCapabilities() {
   
    logger.debug(LOG_ENTRY,"getCapabilities");
    Capability [] result = _capabilities.toArray(new Capability[_capabilities.size()]);
    logger.debug(LOG_EXIT,"getCapabilities", result);
    return result;
  }


  public String[] getCategories() {
    logger.debug(LOG_ENTRY,"getCategories");
    logger.debug(LOG_EXIT,"getCategories", null);
    return null;
  }


  public String getId() {   
    logger.debug(LOG_ENTRY,"getId");
    String id = _modelledBundle.getSymbolicName() + '/' + _modelledBundle.getVersion();
    logger.debug(LOG_EXIT,"getId", id);
    return id;
  }


  public String getPresentationName() {
    
    logger.debug(LOG_ENTRY,"getPresentationName");
    logger.debug(LOG_EXIT,"getPresentationName", _displayName);
    return _displayName;
  }


  @SuppressWarnings("unchecked")
  public Map getProperties() {
    logger.debug(LOG_ENTRY,"getProperties");
    logger.debug(LOG_EXIT,"getProperties", null);
    
    return null;
  }




  public Requirement[] getRequirements() {
    logger.debug(LOG_ENTRY,"getRequirements");
    Requirement [] result = _requirements.toArray(new Requirement[_requirements.size()]);
    logger.debug(LOG_EXIT,"getRequirements", result);
    return result;
  }


  public String getSymbolicName() {
    logger.debug(LOG_ENTRY,"getSymbolicName");
    String result = _modelledBundle.getSymbolicName();
    logger.debug(LOG_EXIT,"getSymbolicName", result);
    return result;
  }


  public URL getURL() {
    
    logger.debug(LOG_ENTRY,"getURL");
    URL url = null;
    try {
      URI uri = new URI(_modelledBundle.getLocation());
      url = uri.toURL();
    } catch (URISyntaxException e) {
      logger.error(e.getMessage());
    } catch (MalformedURLException e) {
      logger.error(e.getMessage());
    }
    logger.debug(LOG_EXIT,"getURL", url);
    return url;
  }


  public Version getVersion() {
    logger.debug(LOG_ENTRY,"getVersion");
    Version v = new Version(_modelledBundle.getVersion()); 
    logger.debug(LOG_EXIT,"getVersion", v);
    return v;
  }

  public Long getSize()
  {
    logger.debug(LOG_ENTRY,"getSize");
    logger.debug(LOG_EXIT,"getSize", 5l);
    return 5l;
  }

  public String getURI()
  {
    logger.debug(LOG_ENTRY,"getURI");
    String uri = _modelledBundle.getLocation();
    logger.debug(LOG_EXIT,"getURI", uri);
    return uri;
  }

  public boolean isLocal()
  {
    logger.debug(LOG_ENTRY,"isLocal");
    logger.debug(LOG_EXIT,"isLocal", false);
    return false;
  }

}
