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

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ModellingConstants;
import org.apache.aries.application.modelling.ResourceType;
import org.apache.aries.application.modelling.WrappedServiceMetadata;
import org.apache.aries.application.utils.service.ExportedServiceHelper;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service exported by a bundle. Provides an entry to DEPLOYMENT.MF
 */
public class ExportedServiceImpl implements ExportedService
{
  private final Logger logger = LoggerFactory.getLogger(ExportedServiceImpl.class);
  private final Map<String, Object> _attributes;
  private final Collection<String> _interfaces;
  private final Map<String, Object> _serviceProperties;
  private final String _name;
  private final int _ranking;
  private String _toString = null;

  /**
   * Constructor. 
   * @param name              "" will be changed to null
   * @param ranking           Service ranking
   * @param ifaces            Interfaces offered by the service
   * @param serviceProperties Service properties. 
   *                          We expect that osgi.service.blueprint.compname has been set if possible
   */
  public ExportedServiceImpl (String name, int ranking, Collection<String> ifaces, 
      Map<String, Object> serviceProperties ) 
  { 
     
    logger.debug(LOG_ENTRY,"ExportedServiceImpl", new Object[]{name, ranking, ifaces});
    _interfaces = new TreeSet<String>(ifaces);
    if (!"".equals(name)) { 
      _name = name;
    } else { 
      _name = null;
    }
    _ranking = ranking;
    if (serviceProperties == null) { 
      _serviceProperties = new HashMap<String, Object>();
    } else { 
      _serviceProperties = new HashMap<String, Object>(serviceProperties);
    }
    
    // Construct _attributes
    _attributes = new HashMap<String, Object>(_serviceProperties);
    
    // Turn interfaces into a comma separated String
    StringBuilder sb = new StringBuilder();
    for (String i : _interfaces) { 
      sb.append(i + ",");
    }
    sb = sb.deleteCharAt(sb.length()-1);
    _attributes.put(Constants.OBJECTCLASS, sb.toString());
    _attributes.put (Constants.SERVICE_RANKING, String.valueOf(_ranking));
    _attributes.put(ModellingConstants.OBR_SERVICE, ModellingConstants.OBR_SERVICE);
    logger.debug(LOG_EXIT,"ExportedServiceImpl");
  }
  
  /**
   * This constructor is for building ExportedServices from Export-Service manifest headers, 
   * which are deprecated in OSGi. 
   * @param ifaceName
   * @param attrs
   */
  @Deprecated 
  public ExportedServiceImpl (String ifaceName, Map<String, String> attrs) { 
    logger.debug(LOG_ENTRY,"ExportedServiceImpl", new Object[]{ ifaceName, attrs});
    _interfaces = new TreeSet<String> (Arrays.asList(ifaceName));
    _ranking = 0;
    _attributes = new HashMap<String, Object> (attrs);
    _attributes.put(Constants.OBJECTCLASS, ifaceName);
    _attributes.put (Constants.SERVICE_RANKING, String.valueOf(_ranking));
    _attributes.put(ModellingConstants.OBR_SERVICE, ModellingConstants.OBR_SERVICE);
    _serviceProperties = new HashMap<String, Object>();
    _name = null;
    logger.debug(LOG_EXIT,"ExportedServiceImpl");
   }
  

  public Map<String, Object> getAttributes() {    
    logger.debug(LOG_ENTRY,"getAttributes");
    logger.debug(LOG_EXIT, "getAttributes", _attributes);
    return Collections.unmodifiableMap(_attributes);
  }


  public ResourceType getType() {
    logger.debug(LOG_ENTRY,"getType");
    logger.debug(LOG_EXIT, "getType", ResourceType.SERVICE);
    return ResourceType.SERVICE;
  }


  public Collection<String> getInterfaces() {
    logger.debug(LOG_ENTRY,"getInterfaces");
    logger.debug(LOG_EXIT, "getInterfaces", _interfaces);
    return Collections.unmodifiableCollection(_interfaces);
  }


  public String getName() {
    logger.debug(LOG_ENTRY,"getName");
    logger.debug(LOG_EXIT, "getName", _name);
    return _name;
  }


  public int getRanking() {
    logger.debug(LOG_ENTRY,"getRanking");
    logger.debug(LOG_EXIT, "getRanking", _ranking);
    return _ranking;
  }


  public Map<String, Object> getServiceProperties() {
    logger.debug(LOG_ENTRY,"getServiceProperties");
    logger.debug(LOG_EXIT, "getServiceProperties", _serviceProperties);
    return Collections.unmodifiableMap(_serviceProperties);
  }


  public int compareTo(WrappedServiceMetadata o) {
    logger.debug(LOG_ENTRY, "compareTo", o);
    int result = ExportedServiceHelper.portableExportedServiceCompareTo(this, o);
    logger.debug(LOG_EXIT,"compareTo", result);
    return result;
  }

  @Override
  public boolean equals (Object o) { 
    logger.debug(LOG_ENTRY, "equals", o);
    boolean eq = ExportedServiceHelper.portableExportedServiceEquals(this, o);
    logger.debug(LOG_EXIT, "equals", eq);
    return eq;
  }
  
  
  @Override
  public int hashCode() {
    logger.debug(LOG_ENTRY, "hashCode");
    int result = ExportedServiceHelper.portableExportedServiceHashCode(this);
    logger.debug(LOG_EXIT, "hashCode", result);
    return result;
  }
  
  @Override 
  public String toString() { 
    if (_toString == null) { 
      _toString = ExportedServiceHelper.generatePortableExportedServiceToString(this);
    }
    return _toString;    
  }
  

  public boolean identicalOrDiffersOnlyByName(WrappedServiceMetadata wsmi) {
    logger.debug(LOG_ENTRY,"identicalOrDiffersOnlyByName", wsmi);
    
    boolean result = ExportedServiceHelper.
        portableExportedServiceIdenticalOrDiffersOnlyByName(this, wsmi);
    logger.debug(LOG_EXIT, "identicalOrDiffersOnlyByName", result);
    return result;
  }
}
