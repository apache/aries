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


package org.apache.aries.application.resolver.obr.impl;

import org.apache.aries.application.Content;
import org.apache.aries.application.management.BundleInfo;
import org.osgi.framework.Version;

import java.util.Map;
import java.util.Set;

/**
 * @version $Rev$ $Date$
 */
public class OBRBundleInfo implements BundleInfo
{

  private final String symbolicName;
  private final Version version;
  private final String location;
  private final Set<Content> importPackage;
  private final Set<Content> exportPackage;
  private final Map<String, String> headers;

  public OBRBundleInfo(String symbolicName, Version version, String location, Set<Content> importPackage, Set<Content> exportPackage, Map<String, String> headers)
  {
    this.symbolicName = symbolicName;
    this.version = version;
    this.location = location;
    this.importPackage = importPackage;
    this.exportPackage = exportPackage;
    this.headers = headers;
  }

  public String getSymbolicName()
  {
    return symbolicName;
  }

  public Version getVersion()
  {
    return version;
  }

  public String getLocation()
  {
    return location;
  }

  public Set<Content> getImportPackage()
  {
    return importPackage;
  }

  public Set<Content> getExportPackage()
  {
    return exportPackage;
  }

  public Set<Content> getImportService()
  {
    //TODO NYI
    return null;
  }

  public Set<Content> getExportService()
  {
    //TODO NYI
    return null;
  }

  public Map<String, String> getHeaders()
  {
    return headers;
  }
}
