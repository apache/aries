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
package org.apache.aries.jndi.url;

import java.util.Enumeration;

import javax.naming.InvalidNameException;
import javax.naming.Name;

/**
 * A composite name for the aries namespace. This provides useful utility methods
 * for accessing the name.
 * 
 * component 0: osgi:service, aries:services, osgi:servicelist
 * component 1: interface
 * component 2: filter
 */
public final class OsgiName extends AbstractName
{
  /** The serial version UID */
  private static final long serialVersionUID = 6617580228852444656L;
  public static final String OSGI_SCHEME = "osgi";
  public static final String ARIES_SCHEME = "aries";
  public static final String SERVICE_PATH = "service";
  public static final String SERVICES_PATH = "services";
  public static final String SERVICE_LIST_PATH = "servicelist";
  public static final String FRAMEWORK_PATH = "framework";
  
  public OsgiName(String name) throws InvalidNameException
  {
    super(name);
  }

  public OsgiName(Name name) throws InvalidNameException
  {
    this(name.toString());
  }

  public boolean hasFilter()
  {
    return size() == 3;
  }
  
  public boolean isServiceNameBased()
  {
    return size() > 3;
  }
  
  public String getInterface()
  {
    return get(1);
  }
  
  public String getFilter()
  {
    return hasFilter() ? get(2) : null;
  }
  
  public String getServiceName()
  {
    Enumeration<String> parts = getAll();
    parts.nextElement();
    
    StringBuilder builder = new StringBuilder();
    
    if (parts.hasMoreElements()) {

      while (parts.hasMoreElements()) {
        builder.append(parts.nextElement());
        builder.append('/');
      }
    
      builder.deleteCharAt(builder.length() - 1);
    }
    
    return builder.toString();
  }

  public boolean hasInterface()
  {
    return size() > 1;
  }
}