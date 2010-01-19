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

package org.apache.aries.application.management.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.jar.Attributes;

import org.apache.aries.application.ApplicationMetadataFactory;
import org.apache.aries.application.Content;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.utils.manifest.BundleManifest;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class BundleInfoImpl implements BundleInfo {
  private String _symbolicName;
  private Version _version;
  private Attributes _attributes;
  private Set<Content> _exportPackages = null;
  private Set<Content> _importPackages = null;
  private String _location;
  private ApplicationMetadataFactory _applicationMetadataFactory;
  
  public BundleInfoImpl (ApplicationMetadataFactory amf, BundleManifest bm, String location) { 
    _symbolicName = bm.getSymbolicName();
    _version = bm.getVersion();
    _attributes = bm.getRawAttributes();
    _location = location;
    _applicationMetadataFactory = amf;
  }
  
  public Set<Content> getExportPackage() {
    if (_exportPackages == null) { 
      _exportPackages = getContentSetFromHeader (_attributes, Constants.EXPORT_PACKAGE);
    
    }
    return _exportPackages;
  }
  
  public Map<String, String> getHeaders() {
    Set<Entry<Object, Object>> headers = _attributes.entrySet();
    Map<String, String> result = new HashMap<String, String>();
    for (Entry<Object, Object> h: headers) { 
      result.put((String)h.getKey(), (String)h.getValue());
    }
    return result;
  }

  public Set<Content> getImportPackage() {
    if (_importPackages == null) { 
      _importPackages = getContentSetFromHeader (_attributes, Constants.IMPORT_PACKAGE);
    
    }
    return _importPackages;
  }

  public String getLocation() {
    return _location;
  }

  public String getSymbolicName() {
    return _symbolicName;
  }

  public Version getVersion() {
    return _version;
  }

  private Set<Content> getContentSetFromHeader (Attributes attributes, String key) { 
    String header = _attributes.getValue(key);
    List<String> splitHeader = ManifestHeaderProcessor.split(header, ",");
    HashSet<Content> result = new HashSet<Content>();
    for (String s: splitHeader) { 
      Content c = _applicationMetadataFactory.parseContent(s);
      result.add(c);
    }
    return result;
  }
}
