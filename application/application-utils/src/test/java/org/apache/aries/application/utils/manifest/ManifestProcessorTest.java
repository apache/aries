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
package org.apache.aries.application.utils.manifest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.Content;
import org.apache.aries.application.VersionRange;
import org.apache.aries.application.impl.ApplicationMetadataFactoryImpl;
import org.apache.aries.application.utils.manifest.ManifestProcessor;
import org.junit.Test;
import org.osgi.framework.Version;

public class ManifestProcessorTest
{

  private String appName = "Travel Reservation";

  /**
   * Check a simple manifest can be read.
   * @throws Exception
   */
  @Test
  public void testSimpleManifest() throws Exception
  {
    //the values of the manifest
    //intentionally include a couple of long lines
    Map<String, String> pairs = new HashMap<String, String>();
    pairs.put("Manifest-Version", "1.0");
    pairs.put("Application-ManifestVersion", "1.0");
    pairs.put("Application-Name", appName );
    pairs.put("Application-SymbolicName", "com.travel.reservation");
    pairs.put("Application-Version", "1.2");
    pairs.put("Application-Content", "com.travel.reservation.web;version=\"[1.1.0,1.2.0)\",com.travel.reservation.business");
    pairs.put("Export-Package", "com.travel.reservation.api;version=1.2");
    pairs.put("Import-Package", "com.travel.flight.api;version=\"[2.1.1,3.0.0)\",com.travel.rail.api;version=\"[1.0.0,2.0.0)\"");
    pairs.put("Application-Services", "services.xml");

    InputStream in = getClass().getClassLoader().getResourceAsStream("META-INF/APPLICATION.MF");
    Manifest mf = new Manifest(in);
    Map<String, String> map = ManifestProcessor.readManifestIntoMap(mf);
    assertNotNull(map);

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
    ApplicationMetadataFactoryImpl manager = new ApplicationMetadataFactoryImpl();
    InputStream in = getClass().getClassLoader().getResourceAsStream("META-INF/APPLICATION.MF");    
    ApplicationMetadata am = manager.parseApplicationMetadata(in);
    assertNotNull(am);

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
        assertEquals(new Version(0,0,0), vr.getMinimumVersion());
      } else 
        fail("Unexepcted content name " + content.getContentName());
    }
  }

  /**
   * Check metadata can be extracted from a manifest that uses multiple lines
   * for a single manifest attribute.
   */
  @Test
  public void testManifestMetadataWithMultiLineEntries() throws Exception
  {
    ApplicationMetadataFactoryImpl manager = new ApplicationMetadataFactoryImpl();
    
    InputStream in = getClass().getClassLoader().getResourceAsStream("META-INF/APPLICATION2.MF");
    
    ApplicationMetadata am = manager.parseApplicationMetadata(in);
    assertNotNull(am);

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
        assertEquals(new Version(0,0,0), vr.getMinimumVersion());
      } else 
        fail("Unexepcted content name " + content.getContentName());
    }
  }
  
  @Test
  public void testManifestWithoutEndingInNewLine() throws Exception
  {
    ApplicationMetadataFactoryImpl manager = new ApplicationMetadataFactoryImpl();
    
    InputStream in = getClass().getClassLoader().getResourceAsStream("META-INF/APPLICATION3.MF");
    
    ApplicationMetadata am = manager.parseApplicationMetadata(in);
    assertNotNull(am);

    assertEquals("Wrong number of bundles are in the application", 1, am.getApplicationContents().size());
    assertEquals("Wrong bundle name", "org.apache.aries.applications.test.bundle", am.getApplicationContents().get(0).getContentName());
  }
}