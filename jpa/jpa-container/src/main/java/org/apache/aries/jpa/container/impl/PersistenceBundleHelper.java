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
package org.apache.aries.jpa.container.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.aries.jpa.container.parsing.PersistenceDescriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This helper can be used to locate persistence.xml files in a bundle
 */
public class PersistenceBundleHelper
{
  /** Logger */
  private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.jpa.container");
  
  /** The persistence xml location */
  public static final String PERSISTENCE_XML = "META-INF/persistence.xml";
  /** The Meta-Persistence header */
  public static final String PERSISTENCE_UNIT_HEADER = "Meta-Persistence";
  /** The Web-ContextPath header (as defined in the web application bundle spec) */
  public static final String WEB_CONTEXT_PATH_HEADER = "Web-ContextPath";

  /**
   * This method locates persistence descriptor files based on a combination of
   * the default location "META-INF/persistence.xml" and the Meta-Persistence
   * header.
   * 
   * Note that getEntry is used to ensure we do not alter the state of the bundle
   * Note also that web application bundles will never return persistence descriptors
   * 
   * @param bundle The bundle to search
   * @return
   */
  public static Collection<PersistenceDescriptor> findPersistenceXmlFiles(Bundle bundle)
  { 
    Dictionary<String, String> headers = bundle.getHeaders();
    String metaPersistence = headers.get(PERSISTENCE_UNIT_HEADER);
    String webContextPath = headers.get(WEB_CONTEXT_PATH_HEADER);
    
    Collection<String> locations;
    
    if (metaPersistence == null) {
      if(webContextPath == null) {
        return Collections.emptySet();
      } else {
        // WABs behave a bit differently to normal bundles. We process them even if they don't have a Meta-Persistence
       
        if(_logger.isInfoEnabled())
          _logger.info("The bundle " + bundle.getSymbolicName() + " specifies both the " + 
                     WEB_CONTEXT_PATH_HEADER + " header, but it does not specify the " + PERSISTENCE_UNIT_HEADER + " header." +
                     " This bundle will be scanned for persistence descriptors in any locations defined by the JPA specification" +
                     "that are on the Classpath.");
        
        String bundleClassPath = headers.get(Constants.BUNDLE_CLASSPATH);
        
        locations = findWABClassPathLocations(bundleClassPath);
      }
    } else {

      //Always search the default location, and use a set so we don't search the same
      //location twice!
      locations = new HashSet<String>();
      locations.add(PERSISTENCE_XML);
      
      if(!!!metaPersistence.isEmpty()) {
        //Split apart the header to get the individual entries
        for (String s : metaPersistence.split(",")) {
          locations.add(s.trim());
        }
      }
    }

    //The files we have found
    Collection<PersistenceDescriptor> persistenceXmlFiles = new ArrayList<PersistenceDescriptor>();
    
    //Find the file and add it to our list
    for (String location : locations) {
      try {
          InputStream file = locateFile(bundle, location);
          if (file != null) {
            persistenceXmlFiles.add(new PersistenceDescriptorImpl(location, file));
          }
      } catch (Exception e) {
          _logger.error("There was an exception while locating the persistence descriptor at location "
              + location + " in bundle " + bundle.getSymbolicName() + "_" + bundle.getVersion()
          		+ ". No persistence descriptors will be processed for this bundle.", e);
        //If we get an exception, then go through closing all of our streams.
        //It is better to fail completely than half succeed.
        for (PersistenceDescriptor desc : persistenceXmlFiles) {
          try {
            desc.getInputStream().close();
          } catch (IOException ioe) {
            //We don't care about this exception, so swallow it
          }
        }
        persistenceXmlFiles = Collections.emptySet();
        //Exit the for loop
        break;
      }     
    }
    
    if (persistenceXmlFiles.isEmpty()) {
      _logger.warn("The bundle "+bundle.getSymbolicName() + "_" + bundle.getVersion() + " specified the Meta-Persistence header. However, no persistence descriptors " + 
        "could be located. The following locations were searched: " + locations.toString());
    }

    return persistenceXmlFiles;
  }

  private static Collection<String> findWABClassPathLocations(String bundleClassPath) {
    
    Collection<String> locations = new HashSet<String>();
    
    if(bundleClassPath == null || bundleClassPath.isEmpty()) {
      locations.add(PERSISTENCE_XML); 
    } else {
      //Remove quoted parameters (that may have , or ; in them)
      bundleClassPath = bundleClassPath.replaceAll(";[^;,]*?=\\s*\".*?\"", "");
      //Remove any other parameters
      bundleClassPath = bundleClassPath.replaceAll(";[^;,]*?=[^;,]*", ",");
      //Remove any ";" left
      bundleClassPath = bundleClassPath.replace(';', ',');
      
      //Tidy up any duplicate "," we have ended up with
      bundleClassPath = bundleClassPath.replaceAll(",+", ",");
      
      //Finally we have the entries we want
      String[] entries = bundleClassPath.split(",");
      
      for(String entry : entries) {
        entry = entry.trim();
        if(entry.isEmpty())
          continue;
        else if(".".equals(entry)) {
          locations.add(PERSISTENCE_XML);
        } else if(entry.endsWith(".jar")) {
          locations.add(entry + "!/" + PERSISTENCE_XML);
        }  else {
          if(!!!entry.endsWith("/"))
            entry = entry + "/";
          
          locations.add(entry + PERSISTENCE_XML);
        }
      }
    }
    return locations;
  }

  /**
   * Locate a persistence descriptor file in a bundle
   * based on a String name.
   * 
   * @param bundle
   * @param persistenceXmlFiles
   * @param jarLocation
   * @throws IOException 
   */
  private static InputStream locateFile(Bundle bundle, String location) throws IOException
  {
    //There is nothing for an empty location
    InputStream is = null;
    if("".equals(location)) {
      return null;
    }
    
    //If there is a '!' then we have to look in a jar
    int bangIndex = location.indexOf('!');
    //No '!', getEntry will do
    if(bangIndex == -1) {
      URL url = bundle.getEntry(location);
      
      if(url != null) 
        is = url.openStream();
      
    } else {
      //There was a '!', find the jar
      URL url = bundle.getEntry(location.substring(0, bangIndex));
      
      if(url != null) {
        //Remember to trim off the "!/"
        String toLocate = location.substring(bangIndex + 2);
      
        JarInputStream jis = new JarInputStream(url.openStream());
        JarEntry entry = jis.getNextJarEntry();
        
        while(entry != null) {
          if(entry.getName().equals(toLocate)) {
            is = jis;
            break;
          }
          entry = jis.getNextJarEntry();
        }
      }
    }
    return is;
  }
}
