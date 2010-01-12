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
package org.apache.aries.jpa.container.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.aries.jpa.container.parsing.PersistenceDescriptor;
import org.osgi.framework.Bundle;

/**
 * This helper can be used to locate persistence.xml files in a bundle
 */
public class PersistenceBundleHelper
{
  /** The persistence xml location */
  public static final String PERSISTENCE_XML = "META-INF/persistence.xml";
  public static final String PERSISTENCE_UNIT_HEADER = "Meta-Persistence";

  /**
   * This method locates persistence descriptor files based on a combination of
   * the default location "META-INF/persistence.xml" and the Meta-Persistence
   * header.
   * 
   * Note that getEntry is used to ensure we do not alter the state of the bundle
   * 
   * @param bundle
   * @return
   */
  public static Collection<PersistenceDescriptor> findPersistenceXmlFiles(Bundle bundle)
  {
    //The files we have found
    Collection<PersistenceDescriptor> persistenceXmlFiles = new HashSet<PersistenceDescriptor>();
    
    //Always search the default location
    List<String> locations = new ArrayList<String>();
    locations.add(PERSISTENCE_XML);
    
    String header = (String) bundle.getHeaders().get(PERSISTENCE_UNIT_HEADER);
    
    if(header != null) {
      //Split apart the header to get the individual entries
      List<String> headerLocations = Arrays.asList(header.split(","));
      locations.addAll(headerLocations);
    
    
      try {
        for(String location : locations) {
          InputStream file = locateFile(bundle, location.trim());
          if(file != null)
            persistenceXmlFiles.add(new PersistenceDescriptorImpl(location, file));
          }
      } catch (Exception e) {
          //TODO log
        for (PersistenceDescriptor desc : persistenceXmlFiles) {
          try {
            desc.getInputStream().close();
          } catch (IOException ioe) {
            // TODO: log ioe
          }
        }
        persistenceXmlFiles = Collections.emptySet();
      }
    }
   return persistenceXmlFiles;
 }

  /**
   * Locate a persistence descriptor file in a bundle
   * based on a String name.
   * 
   * @param bundle
   * @param persistenceXmlFiles
   * @param jarLocation
   */
  private static InputStream locateFile(Bundle bundle, String location)
  {
    InputStream is = null;
    if(location == "") {
      return null;
    }
      
    int bangIndex = location.indexOf('!');
    
    if(bangIndex == -1) {
      URL url = bundle.getEntry(location);
      
      if(url != null) {
        try {
          is = url.openStream();
        } catch (IOException e) {
          // TODO log this
          e.printStackTrace();
        }
      }
    } else {
      URL url = bundle.getEntry(location.substring(0, bangIndex));
      
      if(url != null) {
        String toLocate = location.substring(bangIndex + 1);
        
        try {
          JarInputStream jis = new JarInputStream(url.openStream());
          JarEntry entry = jis.getNextJarEntry();
          
          while(entry != null) {
            if(entry.getName().equals(toLocate)) {
              is = jis;
              break;
            }
            entry = jis.getNextJarEntry();
          }
        } catch (IOException ioe) {
          //TODO log this
        }
      }
    }
    return is;
  }
}
