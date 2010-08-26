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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ResourceType;
import org.apache.aries.application.modelling.WrappedServiceMetadata;
import org.apache.aries.application.modelling.utils.ModellingConstants;
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
    if (o == null) {
      logger.debug(LOG_EXIT, "compareTo", -1);
      return -1;      // shunt nulls to the end of any lists
    }
    int result = this.toString().compareTo(o.toString());
    logger.debug(LOG_EXIT,"compareTo", result);
    return result;
  }

  @Override
  public boolean equals (Object o) { 
    logger.debug(LOG_ENTRY, "equals", o);
    // Doubles as a null check
    if (!(o instanceof WrappedServiceMetadata)) { 
      logger.debug(LOG_EXIT, "equals", false);
      return false;
    }

    if (o==this) { 
      logger.debug(LOG_EXIT, "equals", true);
      return true;
    }
 
    boolean eq = this.toString().equals(o.toString());
    logger.debug(LOG_EXIT, "equals", eq);
    return eq;
  }
  
  
  @Override
  public int hashCode() {
    logger.debug(LOG_ENTRY, "hashCode");
    int result = toString().hashCode();
    logger.debug(LOG_EXIT, "hashCode", result);
    return result;
  }
  
  @Override 
  public String toString() { 
    if (_toString != null) { 
      return _toString;
    }
    
    List<String> interfaces = new ArrayList<String>(_interfaces);
    Collections.sort(interfaces);
    
    List<String> props = new ArrayList<String>();
    for (Map.Entry<String, Object> entry : _serviceProperties.entrySet()) {
      Object entryValue = entry.getValue();
      String entryText;
      if (entryValue.getClass().isArray()) { 
        // Turn arrays into comma separated Strings
        Object [] entryArray = (Object[]) entryValue;
        StringBuilder sb = new StringBuilder();
        for (Object o: entryArray) { 
          sb.append(String.valueOf(o) + ",");
        }
        sb = sb.deleteCharAt(sb.length()-1);
        entryText = sb.toString();
      } else { 
        entryText = String.valueOf(entryValue);
      }
      props.add ("<entry> key=\"" + entry.getKey() + "\" value=\"" + entryText + "\"/>");
    }
    Collections.sort(props);
    
    StringBuffer buf = new StringBuffer("<service>");
    if(_name != null) {
      buf.append("<name>" + _name + "</name>");
    }
    if (_interfaces.size() > 0) { 
      buf.append("<interfaces>");
    }
    for (String i : interfaces) { 
      buf.append("<value>" + i + "</value>");
    }
    if (_interfaces.size() > 0) { 
      buf.append("</interfaces>");
    }
    if (_serviceProperties.size() > 0) { 
      buf.append("<service-properties>");
    }
    for (String p : props) { 
      buf.append(p);
    }
    if (_serviceProperties.size() > 0) { 
      buf.append("</service-properties>");
    }
    buf.append("</service>");
    _toString = buf.toString();
    return _toString;
  }
  

  public boolean identicalOrDiffersOnlyByName(WrappedServiceMetadata wsmi) {
    logger.debug(LOG_ENTRY,"identicalOrDiffersOnlyByName", wsmi);
    
    if (this.equals(wsmi)) { 
      logger.debug(LOG_EXIT, "identicalOrDiffersOnlyByName", true);
      return true;
    }

    Set<String> myInterfaces = new HashSet<String>(_interfaces);
    Set<String> cmpInterfaces = new HashSet<String>(wsmi.getInterfaces());
    if (!myInterfaces.equals(cmpInterfaces)) { 
      logger.debug(LOG_EXIT, "identicalOrDiffersOnlyByName", false);
      return false;
    }
    
    boolean propertiesEqual = _serviceProperties.equals(wsmi.getServiceProperties());
    if (!propertiesEqual) {
      logger.debug(LOG_EXIT, "identicalOrDiffersOnlyByName", false);
      return false;
    }
    logger.debug(LOG_EXIT, "identicalOrDiffersOnlyByName", true);
    return true;
  }

  
}
