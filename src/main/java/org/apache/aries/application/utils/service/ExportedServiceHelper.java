package org.apache.aries.application.utils.service;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.WrappedServiceMetadata;

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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
public class ExportedServiceHelper {

  public static String generatePortableExportedServiceToString(ExportedService svc) {
    List<String> interfaces = new ArrayList<String>(svc.getInterfaces());
    Collections.sort(interfaces);
    
    List<String> props = new ArrayList<String>();
    for (Map.Entry<String, Object> entry : svc.getServiceProperties().entrySet()) {
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
    if(svc.getName() != null) {
      buf.append("<name>" + svc.getName() + "</name>");
    }
    if (interfaces.size() > 0) { 
      buf.append("<interfaces>");
    }
    for (String i : interfaces) { 
      buf.append("<value>" + i + "</value>");
    }
    if (interfaces.size() > 0) { 
      buf.append("</interfaces>");
    }
    if (svc.getServiceProperties().size() > 0) { 
      buf.append("<service-properties>");
    }
    for (String p : props) { 
      buf.append(p);
    }
    if (svc.getServiceProperties().size() > 0) { 
      buf.append("</service-properties>");
    }
    buf.append("</service>");
    return buf.toString();
  }
  

  public static boolean portableExportedServiceIdenticalOrDiffersOnlyByName(ExportedService svc, WrappedServiceMetadata wsmi) {
    if (svc.equals(wsmi)) { 
      return true;
    }

    Set<String> svcInterfaces = new HashSet<String>(svc.getInterfaces());
    Set<String> cmpInterfaces = new HashSet<String>(wsmi.getInterfaces());
    if (!svcInterfaces.equals(cmpInterfaces)) { 
      return false;
    }
    
    boolean propertiesEqual = svc.getServiceProperties().equals(wsmi.getServiceProperties());
    if (!propertiesEqual) {
      return false;
    }
    return true;
  }
  
  public static boolean portableExportedServiceEquals (ExportedService left, Object right) { 
    // Doubles as a null check
    if (!(right instanceof WrappedServiceMetadata)) { 
      return false;
    }

    if (right == left) { 
      return true;
    }
 
    boolean eq = left.toString().equals(right.toString());
    return eq;
  }
  
  public static int portableExportedServiceHashCode(ExportedService svc) {
    int result = svc.toString().hashCode();
    return result;
  }
  
  public static int portableExportedServiceCompareTo(ExportedService svc, WrappedServiceMetadata o) {
    if (o == null) {
      return -1;      // shunt nulls to the end of any lists
    }
    int result = svc.toString().compareTo(o.toString());
    return result;
  }
  
}
