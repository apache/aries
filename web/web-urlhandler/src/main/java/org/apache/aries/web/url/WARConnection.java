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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import org.apache.aries.web.converter.WarToWabConverter.InputStreamProvider;
import org.apache.aries.web.converter.impl.WarToWabConverterImpl;
import org.osgi.framework.Constants;

public class WARConnection extends URLConnection
{
  private WarToWabConverterImpl converter = null;
  private Properties properties;
  
  protected WARConnection(URL url, Properties properties) throws MalformedURLException
  {
    super(url);
    this.properties = properties;
    
    // Validate properties
    
    String bundleManifestVersion = (String) properties.get(Constants.BUNDLE_MANIFESTVERSION);
    if (bundleManifestVersion != null && !bundleManifestVersion.equals("2")) {
      throw new MalformedURLException("Incorrect bundle version supplied in URL : " + bundleManifestVersion);
    }
    
  }

  @Override
  public void connect() throws IOException
  {
    int fileNameIndex = url.getFile().lastIndexOf("/") + 1;
    String warName;
    if (fileNameIndex != 0)
      warName = url.getFile().substring(fileNameIndex);
    else
      warName = url.getFile();

    converter = new WarToWabConverterImpl(new InputStreamProvider() {
      public InputStream getInputStream() throws IOException {
        return url.openStream();
      }
    }, warName, properties);
  }

  @Override
  public InputStream getInputStream() throws IOException
  {
    if (converter == null)
      connect();
    
    return converter.getWAB();
  }

  @Override
  public int getContentLength()
  {
    try {
      if (converter == null)
        connect();
      return converter.getWabLength();
    } catch (IOException e) {
      return -1;
    }
  }
}
