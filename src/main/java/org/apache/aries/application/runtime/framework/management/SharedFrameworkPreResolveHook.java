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
package org.apache.aries.application.runtime.framework.management;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;

import org.apache.aries.application.Content;
import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.spi.framework.BundleFrameworkManager;
import org.apache.aries.application.management.spi.resolve.PreResolveHook;
import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ModellingManager;
import org.apache.aries.application.utils.manifest.ContentFactory;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class SharedFrameworkPreResolveHook implements PreResolveHook
{
  private BundleFrameworkManager fwMgr;
  private ModellingManager mgr;
  
  private static final class BundleInfoImpl implements BundleInfo
  {
    private final Bundle compositeBundle;
    
    public BundleInfoImpl(Bundle bundle) {
      compositeBundle = bundle;
    }
    
    @Override
	public String getSymbolicName()
    {
      return compositeBundle.getSymbolicName();
    }

    @Override
	public Map<String, String> getBundleDirectives()
    {
      return Collections.emptyMap();
    }

    @Override
	public Map<String, String> getBundleAttributes()
    {
      return Collections.emptyMap();
    }

    @Override
	public Version getVersion()
    {
      return compositeBundle.getVersion();
    }

    @Override
	public String getLocation()
    {
      return compositeBundle.getLocation();
    }

    @Override
	public Set<Content> getImportPackage()
    {
      return Collections.emptySet();
    }

    @Override
	public Set<Content> getRequireBundle()
    {
      return Collections.emptySet();
    }

    @Override
	public Set<Content> getExportPackage()
    {
      String imports = (String) compositeBundle.getHeaders().get(Constants.IMPORT_PACKAGE);
      
      Set<Content> exports = new HashSet<Content>();
      
      Map<String, Map<String, String>> parsedImports = ManifestHeaderProcessor.parseImportString(imports);
      for (Map.Entry<String, Map<String, String>> anImport : parsedImports.entrySet()) {
        exports.add(ContentFactory.parseContent(anImport.getKey(), anImport.getValue()));
      }
      
      return exports;
    }

    @Override
	public Set<Content> getImportService()
    {
      return Collections.emptySet();
    }

    @Override
	public Set<Content> getExportService()
    {
      return Collections.emptySet();
    }

    @Override
	public Map<String, String> getHeaders()
    {
      Map<String, String> result = new HashMap<String, String>();
      @SuppressWarnings("unchecked")
      Dictionary<String, String> headers = compositeBundle.getHeaders();
      Enumeration<String> keys = headers.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement();
        String value = headers.get(key);
//        if (Constants.IMPORT_PACKAGE.equals(key)) {
//          result.put(Constants.EXPORT_PACKAGE, value);
//        } else if (!!!Constants.EXPORT_PACKAGE.equals(key)) {
//          result.put(key, value);
//        }
        result.put(key, value);
      }
      
      return result;
    }

    @Override
	public Attributes getRawAttributes()
    {
      return ManifestProcessor.mapToManifest(getHeaders()).getMainAttributes();
    }
    
  }
  
  @Override
public void collectFakeResources(Collection<ModelledResource> resources)
  {
    Bundle b = fwMgr.getSharedBundleFramework().getIsolatedBundleContext().getBundle(1);
    BundleInfo info = new BundleInfoImpl(b);
    Collection<ImportedService> serviceImports = Collections.emptySet();
    Collection<ExportedService> serviceExports = Collections.emptySet();
    try {
      resources.add(mgr.getModelledResource(info.getLocation(), info, serviceImports, serviceExports));
    } catch (InvalidAttributeException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  public void setBundleFrameworkManager(BundleFrameworkManager bfm)
  {
    fwMgr = bfm;
  }
  
  public void setModellingManager(ModellingManager manager)
  {
    mgr = manager;
  }
}