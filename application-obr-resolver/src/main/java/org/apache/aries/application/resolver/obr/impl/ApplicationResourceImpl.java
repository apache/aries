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

import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import org.apache.aries.application.Content;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.obr.Capability;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resource;

public class ApplicationResourceImpl implements Resource
{
  private String _symbolicName;
  private Version _version;
  private Requirement[] _requirements;
  
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
    
    public String toString()
    {
      return delgate.toString();
    }
  }
  
  public ApplicationResourceImpl(String appName, Version appVersion, List<Content> appContent)
  {
    _symbolicName = appName;
    _version = appVersion;
    
    _requirements = new Requirement[appContent.size()];
    for (int i = 0; i < _requirements.length; i++) {
      Content c = appContent.get(i);
      
      String comment = "Requires " + Resource.SYMBOLIC_NAME + " " + c.getContentName() + " with attributes " + c.getAttributes();
      
      String resolution = c.getDirective("resolution");

      boolean optional = Boolean.valueOf(resolution);
      
      String f = ManifestHeaderProcessor.generateFilter(Resource.SYMBOLIC_NAME, c.getContentName(), c.getAttributes());
      Filter filter;
      try {
        filter = FrameworkUtil.createFilter(f);
        _requirements[i] = new RequirementImpl("bundle", new FilterWrapper(filter), false, optional, false, comment);
      } catch (InvalidSyntaxException e) {
        // TODO work out what to do if his happens. If it does our filter generation code is bust.
      }
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

  public Repository getRepository()
  {
    return null;
  }

  public Requirement[] getRequirements()
  {
    return _requirements;
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
}