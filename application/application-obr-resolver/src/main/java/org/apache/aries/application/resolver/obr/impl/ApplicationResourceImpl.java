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
package org.apache.aries.application.resolver.obr.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import org.apache.aries.application.Content;
import org.apache.aries.application.modelling.ImportedBundle;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

public class ApplicationResourceImpl implements Resource
{
  private String _symbolicName;
  private Version _version;
  private List<Requirement> _requirements = new ArrayList<Requirement>();
  
  private static class FilterWrapper implements Filter
  {
    private Filter delgate;
    
    public FilterWrapper(Filter f)
    {
      delgate = f;
    }
    
    public boolean match(ServiceReference reference)
    {
      return delgate.match(reference);
    }

    public boolean match(Dictionary dictionary)
    {
      boolean result = delgate.match(dictionary);
      return result;
    }

    public boolean matchCase(Dictionary dictionary)
    {
      return delgate.matchCase(dictionary);
    }

    public boolean matches(Map<java.lang.String,?> map) {
        return delgate.matches(map);
    }
    
    public String toString()
    {
      return delgate.toString();
    }
  }
  
  public ApplicationResourceImpl(String appName, Version appVersion, List<Content> appContent)
  {
    _symbolicName = appName;
    _version = appVersion;
    
 
    for (int i = 0; i < appContent.size(); i++) {
      Content c = appContent.get(i);
      
      String comment = "Requires " + Resource.SYMBOLIC_NAME + " " + c.getContentName() + " with attributes " + c.getAttributes();
      
      String resolution = c.getDirective("resolution");

      boolean optional = Boolean.valueOf(resolution);
      
      String f = ManifestHeaderProcessor.generateFilter(Resource.SYMBOLIC_NAME, c.getContentName(), c.getAttributes());
      Filter filter;
      try {
        filter = FrameworkUtil.createFilter(f);
        _requirements.add(new RequirementImpl("bundle", new FilterWrapper(filter), false, optional, false, comment));
      } catch (InvalidSyntaxException e) {
        // TODO work out what to do if this happens. If it does our filter generation code is bust.
      }
    }
  }
  
  public ApplicationResourceImpl(String appName, Version appVersion, Collection<ImportedBundle> inputs)
  {
    _symbolicName = appName;
    _version = appVersion;
    
    for (ImportedBundle match : inputs) {
      _requirements.add(new RequirementImpl(match));
    }
  }
  public Capability[] getCapabilities()
  {
    return null;
  }

  public String[] getCategories()
  {
    return null;
  }

  public String getId()
  {
    return _symbolicName;
  }

  public String getPresentationName()
  {
    return _symbolicName;
  }

  public Map getProperties()
  {
    return null;
  }

  public Requirement[] getRequirements()
  {
    if (_requirements!= null) {
    Requirement[] reqs = new Requirement[_requirements.size()];
    int index =0;
    for (Requirement req: _requirements) {
      reqs[index++] = req;
    }
    return reqs;
    } else {
      return null;
    }
      
  }

  public String getSymbolicName()
  {
    return _symbolicName;
  }

  public java.net.URL getURL()
  {
    return null;
  }

  public Version getVersion()
  {
    return _version;
  }

  public Long getSize()
  {
    return 0l;
  }

  public String getURI()
  {
    return null;
  }

  public boolean isLocal()
  {
    return false;
  }
}