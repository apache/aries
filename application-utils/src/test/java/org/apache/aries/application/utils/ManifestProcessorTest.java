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
package org.apache.aries.application.utils;

import static org.apache.aries.application.utils.AppConstants.APPLICATION_MF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Version;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.Content;
import org.apache.aries.application.VersionRange;
import org.apache.aries.application.impl.ApplicationMetadataManagerImpl;
import org.apache.aries.application.utils.manifest.ManifestProcessor;

public class ManifestProcessorTest
{

  private static Map<String,String> pairs = null;
  
  @Before
  public void setUp() throws Exception{
    
    //enforce ordering of the keys
    String[] keys = new String[]{
        "Manifest-Version",
        "Application-ManifestVersion",
        "Application-Name",
        "Application-SymbolicName",
        "Application-Version",
        "Application-Content",
        "Export-Package",
        "Import-Package",
        "Application-Services"  
    };
    
    String [] values = new String[]{
        "1.0",
        "1.0",
        "Travel Reservation",
        "com.travel.reservation",
        "1.2",
        "com.travel.reservation.web;version=\"[1.1.0,1.2.0)\",com.travel.reservation.business",
        "com.travel.reservation.api;version=1.2",
        "com.travel.flight.api;version=\"[2.1.1,3.0.0)\",com.travel.rail.api;version=\"[1.0.0,2.0.0)\"",
        "services.xml"
    };
    
    //the values of the manifest
    //intentionally include a couple of long lines
    pairs = new HashMap<String, String>();
    int i = 0;
    for (String key : keys){
      pairs.put(key, values[i]);
      i++;
    }
  }
 
  /**
   * Check a simple manifest can be read.
   * @throws Exception
   */
  @Test
  public void testSimpleManifest() throws Exception
  {
	Manifest mf = new Manifest(getClass().getClassLoader().getResourceAsStream("META-INF/APPLICATION.MF"));
	checkManifest(mf);
  }
  
  /**
   * Check a simple manifest can be parsed.
   * @throws Exception
   */
  @Test
  public void testParseManifest() throws Exception
  {
    Manifest mf = ManifestProcessor.parseManifest(getClass().getClassLoader().getResourceAsStream("META-INF/APPLICATION.MF"));
    checkManifest(mf);
  }
  
  private void checkManifest(Manifest mf) throws Exception 
  {
      Map<String, String> map = ManifestProcessor.readManifestIntoMap(mf);
      assertNotNull(map);

      assertEquals("Unexpected number of manifest entires", pairs.size(), map.size());
      
      //check all the expected keys and values
      for (String key : pairs.keySet()){
        assertTrue("Key: " + key + " was not found",map.containsKey(key));
        String value = map.get(key);
        assertNotNull("Value was not present for key: " + key ,value);
        assertEquals("Value was not correct for key: " + key ,pairs.get(key),value);
      }
      //check there aren't any extra entries in the map that weren't expected
      assertEquals("The maps did not match",pairs,map);  
  }
  /**
   * Check metadata can be extracted from a simple manifest.
   */
  @Test
  public void testManifestMetadata() throws Exception
  {
    ApplicationMetadataManagerImpl manager = new ApplicationMetadataManagerImpl();
    ApplicationMetadata am = manager.parseApplication(getClass().getClassLoader().getResourceAsStream("META-INF/APPLICATION.MF"));
    assertNotNull(am);

    String appName = pairs.get("Application-Name");
    assertEquals(am.getApplicationName(),appName);

    //"com.travel.reservation.web;version=\"[1.1.0,1.2.0)\",com.travel.reservation.business",
    List<Content> contents = am.getApplicationContents();
    for (Content content : contents){
      if ("com.travel.reservation.web".equals(content.getContentName())){
        VersionRange vr = content.getVersion();
        assertEquals(vr.getMinimumVersion(),new Version("1.1.0"));
        assertEquals(vr.getMaximumVersion(),new Version("1.2.0"));
      } else if("com.travel.reservation.business".equals(content.getContentName())){
        VersionRange vr = content.getVersion();
        assertNull(vr);
      } else 
        fail("Unexepcted content name " + content.getContentName());
    }
  }
}