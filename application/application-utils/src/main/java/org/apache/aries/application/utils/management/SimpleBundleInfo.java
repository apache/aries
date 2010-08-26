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

package org.apache.aries.application.utils.management;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.jar.Attributes;

import org.apache.aries.application.Content;
import org.apache.aries.application.impl.ContentImpl;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.utils.manifest.BundleManifest;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public final class SimpleBundleInfo implements BundleInfo {
  private Content _contentName;
  private Version _version;
  private Attributes _attributes;
  private Set<Content> _exportPackages = null;
  private Set<Content> _importPackages = null;
  private Set<Content> _exportServices = null;
  private Set<Content> _importServices = null;
  private Set<Content> _requireBundle = null;
  
  private String _location;
  
  public SimpleBundleInfo(BundleManifest bm, String location) { 
    _contentName = new ContentImpl(
        bm.getSymbolicName(), 
        ManifestHeaderProcessor.parseBundleSymbolicName(bm.getSymbolicName()).getValue());
    _version = bm.getVersion();
    _attributes = bm.getRawAttributes();
    _location = location;
  }
    
  public Set<Content> getExportPackage() {
    if (_exportPackages == null) { 
      _exportPackages = getContentSetFromHeader (_attributes, Constants.EXPORT_PACKAGE);
    }
    return _exportPackages;
  }
  
  public Set<Content> getExportService() {
    if (_exportServices == null) {
      _exportServices = getContentSetFromHeader (_attributes, Constants.EXPORT_SERVICE);
    }
    return _exportPackages;
  }

  public Map<String, String> getHeaders() {
    Map<String, String> result = new HashMap<String, String>();
    for (Entry<Object, Object> h: _attributes.entrySet()) {
      Attributes.Name name = (Attributes.Name) h.getKey();
      String value = (String) h.getValue();
      result.put(name.toString(), value);
    }
    return result;
  }

  public Set<Content> getImportPackage() {
    if (_importPackages == null) { 
      _importPackages = getContentSetFromHeader (_attributes, Constants.IMPORT_PACKAGE);
    }
    return _importPackages;
  }

  public Set<Content> getImportService() {
    if (_importServices == null) {
      _importServices = getContentSetFromHeader (_attributes, Constants.IMPORT_SERVICE);
    }
    return _importServices;
  }

  public String getLocation() {
    return _location;
  }

  public String getSymbolicName() {
    return _contentName.getContentName();
  }

  public Version getVersion() {
    return _version;
  }

  private Set<Content> getContentSetFromHeader (Attributes attributes, String key) {
    String header = _attributes.getValue(key);
    List<String> splitHeader = ManifestHeaderProcessor.split(header, ",");
    HashSet<Content> result = new HashSet<Content>();
    for (String s: splitHeader) { 
      Content c = new ContentImpl(s);
      result.add(c);
    }
    return result;
  }

  public Map<String, String> getBundleAttributes()
  {
    return _contentName.getAttributes();
  }

  public Map<String, String> getBundleDirectives()
  {
    return _contentName.getDirectives();
  }

  public Set<Content> getRequireBundle()
  {
    if (_requireBundle == null) {
      _requireBundle = getContentSetFromHeader(_attributes, Constants.REQUIRE_BUNDLE);
    }
    
    return _requireBundle;
  }
  
  /**
   * Equality is just based on the location. If you install a bundle from the same location string
   * you get the same Bundle, even if the underlying bundle had a different symbolic name/version.
   * This seems reasonable and quick.
   */
  public boolean equals(Object other)
  {
    if (other == null) return false;
    if (other == this) return true;
    if (other instanceof SimpleBundleInfo) {
      return _location.equals(((SimpleBundleInfo)other)._location);
    }
    
    return false;
  }
  
  public int hashCode()
  {
    return _location.hashCode();
  }
  
  public String toString()
  {
    return _contentName.getContentName() + "_" + getVersion();
  }
  public Attributes getRawAttributes()
  {
    
    return _attributes;
  }
}