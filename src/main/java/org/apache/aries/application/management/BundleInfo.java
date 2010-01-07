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

package org.apache.aries.application.management;

import java.util.Map;
import java.util.Set;
import org.osgi.framework.Version;
import org.apache.aries.application.Content;

/**
 * Information about a bundle
 *
 */
public interface BundleInfo
{
  /** Bundle-SymbolicName */
  public String getSymbolicName();
  
  /** Bundle-Version: */
  public Version getVersion();
  
  /** Returns a String which can be turned into a URL to the bundle binary */
  public String getLocation();
  
  /** Import-Package */
  public Set<Content> getImportPackage();
  
  /** Export-Package */
  public Set<Content> getExportPackage();
  
  /** All the headers in the MANIFEST.MF file */
  public Map<String, String> getHeaders();
  
}
