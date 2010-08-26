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
package org.apache.aries.application.modelling.impl;

import java.util.Map;

import org.apache.aries.application.modelling.ExportedBundle;
import org.apache.aries.application.modelling.ImportedBundle;
import org.apache.aries.application.modelling.ResourceType;
import org.apache.aries.application.modelling.utils.ModellingConstants;
import org.apache.aries.application.utils.AppConstants;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

 abstract class AbstractExportedBundle implements ExportedBundle
{


  /**
   * Get the exported bundle or composite's manifest attributes
   * @return attributes extracted from the object's manifest 
   */
  public abstract Map<String, Object> getAttributes(); 

  /**
   * This is always BUNDLE, even for composites. Their compositey-ness is detected
   * from ModelledResource.getType()
   */

  public final ResourceType getType() { 
    
    return ResourceType.BUNDLE;
  }
  
  /**
   * Get the bundle's symbolic name
   * @return symbolic name
   */
  public String getSymbolicName() {
    
    String result = String.valueOf(getAttributes().get(ModellingConstants.OBR_SYMBOLIC_NAME));
    
    return result;
  }

  /**
   * Get the bundle or composite's version
   * @return version
   */
  public String getVersion () { 
    
    String result = String.valueOf(getAttributes().get(Constants.VERSION_ATTRIBUTE));
    
    return Version.parseVersion(result).toString();
  }


  
  public String toDeploymentString() {
    
    String result = getSymbolicName() + ";" + AppConstants.DEPLOYMENT_BUNDLE_VERSION + "=" + getVersion();
    return result;
  }
  
  /**
   * Return true if this is a fragment
   * @return true if this is a fragment
   */
  public abstract boolean isFragment();
  
  /**
   * If this bundle is a fragment, this method will return the bundle to which it attaches 
   * @return fragment host
   */
  public abstract ImportedBundle getFragmentHost();

}
