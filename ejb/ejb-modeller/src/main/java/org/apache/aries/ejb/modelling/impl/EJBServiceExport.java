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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ModellingConstants;
import org.apache.aries.application.modelling.ResourceType;
import org.apache.aries.application.modelling.WrappedServiceMetadata;
import org.apache.aries.application.utils.service.ExportedServiceHelper;
import org.osgi.framework.Constants;

public class EJBServiceExport implements ExportedService {

  private final String interfaceName;
  private final String ejbName;
  
  private final Map<String, Object> _attributes;
  
  private final Map<String, Object> serviceProperties;
  private String _toString;
  
  public EJBServiceExport(String ejbName, String ejbType, String interfaceName,
      boolean remote) {
    this.interfaceName = interfaceName;
    this.ejbName = ejbName;
    
    serviceProperties = new HashMap<String, Object>();
    serviceProperties.put("ejb.name", ejbName);
    serviceProperties.put("ejb.type", ejbType);
    if(remote)
      serviceProperties.put("service.exported.interfaces", interfaceName);
    
    _attributes = new HashMap<String, Object>(serviceProperties);
    
    _attributes.put(Constants.OBJECTCLASS, interfaceName);
    _attributes.put (Constants.SERVICE_RANKING, "0");
    _attributes.put(ModellingConstants.OBR_SERVICE, ModellingConstants.OBR_SERVICE);
  }

  public Map<String, Object> getAttributes() {
    return _attributes;
  }

  public ResourceType getType() {
    return ResourceType.SERVICE;
  }

  public Collection<String> getInterfaces() {
    
    return Arrays.asList(interfaceName);
  }

  public String getName() {
    return ejbName;
  }

  public int getRanking() {
    return 0;
  }

  public Map<String, Object> getServiceProperties() {
    return serviceProperties;
  }

  public int compareTo(WrappedServiceMetadata o) {
    return ExportedServiceHelper.portableExportedServiceCompareTo(this, o);
  }

  @Override
  public boolean equals (Object o) { 
    return ExportedServiceHelper.portableExportedServiceEquals(this, o);
  }
  
  
  @Override
  public int hashCode() {
    return ExportedServiceHelper.portableExportedServiceHashCode(this);
  }
  
  @Override 
  public String toString() { 
    if (_toString == null) { 
      _toString = ExportedServiceHelper.generatePortableExportedServiceToString(this);
    }
    return _toString;    
  }
  

  public boolean identicalOrDiffersOnlyByName(WrappedServiceMetadata wsmi) {
   return ExportedServiceHelper.
        portableExportedServiceIdenticalOrDiffersOnlyByName(this, wsmi);
  }

}