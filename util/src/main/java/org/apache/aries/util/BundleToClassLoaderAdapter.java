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
package org.apache.aries.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import org.osgi.framework.Bundle;

public class BundleToClassLoaderAdapter extends ClassLoader
{
  private Bundle b;
  
  public BundleToClassLoaderAdapter(Bundle bundle)
  {
    b = bundle;
  }
  
  @Override
  public URL getResource(String name)
  {
    return b.getResource(name);
  }

  @Override
  public InputStream getResourceAsStream(String name)
  {
    URL url = getResource(name);
    
    InputStream result = null;
    
    if (url != null) {
      try {
        result = url.openStream();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    
    return result;
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException
  {
    @SuppressWarnings("unchecked")
    Enumeration<URL> urls = b.getResources(name);
    
    if (urls == null) {
      urls = Collections.enumeration(new ArrayList<URL>());
    }
    
    return urls;
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException
  {
    return b.loadClass(name);
  }
}