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
package org.apache.aries.web.url;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.apache.aries.web.converter.impl.CaseInsensitiveMap;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

public class WAR_URLServiceHandler extends AbstractURLStreamHandlerService implements
    BundleActivator
{

  private static final String urlScheme = "webbundle";
  
  public WAR_URLServiceHandler()
  {
    super();
  }

  public URLConnection openConnection(URL url) throws IOException
  {
    // Create properties object
    CaseInsensitiveMap properties = new CaseInsensitiveMap();
    if (url.getQuery() != null)
    {
      String propString = url.getQuery();
      StringTokenizer tok = new StringTokenizer(propString);
      boolean firstProperty = true;
      
      // We now parse the property pairs query string.
      // This has the format name=value&name=value...(etc)
      while (tok.hasMoreElements())
      {
        String name = tok.nextToken("=");
        // "name" will now contain the name of the property we are trying to 
        // set. Property pairs are seperated by the '&' symbol. The tokenizer
        // will include this symbol in the token so we need to return it from
        // all property names except the first.
        if (!!!firstProperty)
          name = name.substring(1);
        String value = tok.nextToken("&").substring(1);
        properties.put(name, value);
        firstProperty = false;
      }
    }
        
    return new WARConnection(new URL(url.getPath()), properties);
  }

  @Override
  public void parseURL(URL u, String spec, int start, int limit)
  {
    int propertyStart = spec.lastIndexOf('?') + 1;
    String propertyString = null;
    if (propertyStart > 0) 
    {
      propertyString = spec.substring(propertyStart, spec.length());
      propertyStart--;
    }
    else
      propertyStart = spec.length();

    String warURL = spec.substring(start, propertyStart);
    
    // For our war url, we use the "path" field to specify the full url path to the WAR file,
    // and we use the "query" field to specify the properties for the WAB manifest
    
    setURL(u, urlScheme, null, 0, null, null, warURL, propertyString, null);
  }

  public void start(BundleContext context) throws Exception
  {
    Dictionary<String, Object> properties = new Hashtable<String, Object>();
    properties.put( URLConstants.URL_HANDLER_PROTOCOL, new String[] {urlScheme});
    context.registerService(URLStreamHandlerService.class.getName(), this, properties);
  }

  public void stop(BundleContext arg0) throws Exception
  {
    // TODO Auto-generated method stub
    
  }

}
