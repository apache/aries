/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.ejb.modelling.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.aries.application.modelling.ModellerException;
import org.apache.aries.application.modelling.ParsedServiceElements;
import org.apache.aries.application.modelling.ServiceModeller;
import org.apache.aries.ejb.modelling.EJBLocator;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.manifest.BundleManifest;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EJBModeller implements ServiceModeller {

  private static final Logger logger = LoggerFactory.getLogger(EJBModeller.class);
  
  private EJBLocator locator;
  
  public void setLocator(EJBLocator locator) {
    this.locator = locator;
  }
  
  /**
   * This modeller only searches for EJBs if there is an Export-EJB header with
   * a value other than NONE (the default empty string value means ALL exported).
   */
  public ParsedServiceElements modelServices(BundleManifest manifest, IDirectory bundle) 
    throws ModellerException {
    logger.debug("modelServices", new Object[] {manifest, bundle});
    ParsedEJBServices ejbServices = new ParsedEJBServices();
    
    String header = manifest.getRawAttributes().getValue("Export-EJB");
    logger.debug("Export-EJB header is " + header);
    
   
    if(header == null)
      return ejbServices;
    
    Collection<String> allowedNames = getNames(header.trim());
    
    if(allowedNames.contains("NONE"))
      return ejbServices;

    ejbServices.setAllowedNames(allowedNames);
    locator.findEJBs(manifest, bundle, ejbServices);
    
    logger.debug("modelServices", ejbServices);
    return ejbServices;
  }

  private Collection<String> getNames(String header) {
    Collection<String> names = new ArrayList<String>();
    for(NameValuePair nvp: ManifestHeaderProcessor.parseExportString(header)){
      names.add(nvp.getName().trim());
    }
    return names;
  }

}
