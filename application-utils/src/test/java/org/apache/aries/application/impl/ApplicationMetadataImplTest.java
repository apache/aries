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
package org.apache.aries.application.impl;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import junit.framework.Assert;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.ApplicationMetadataFactory;
import org.apache.aries.application.Content;
import org.apache.aries.application.ServiceDeclaration;
import org.apache.aries.application.impl.ApplicationMetadataFactoryImpl;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor.NameValueMap;
import org.junit.Test;
import org.osgi.framework.Version;

public class ApplicationMetadataImplTest
{
  @Test
  public void testBasicMetadataCreation() throws IOException
  {
    ApplicationMetadataFactory manager = new ApplicationMetadataFactoryImpl();
    ApplicationMetadata app = manager.parseApplicationMetadata(getClass().getResourceAsStream("/META-INF/APPLICATION.MF"));
    
    Assert.assertEquals("Travel Reservation", app.getApplicationName());
  }
  @Test
  public void testMetadataCreation() throws Exception
  {
    ApplicationMetadataFactory manager = new ApplicationMetadataFactoryImpl();
    ApplicationMetadata app = manager.parseApplicationMetadata(getClass().getResourceAsStream("/META-INF/APPLICATION4.MF"));
    assertEquals("Travel Reservation", app.getApplicationName());
    assertEquals("com.travel.reservation", app.getApplicationSymbolicName());
    assertEquals(Version.parseVersion("1.2.0"), app.getApplicationVersion());
    List<Content> appContents = app.getApplicationContents();
    assertEquals(2, appContents.size());
    Content appContent1 = new ContentImpl("com.travel.reservation.business");
    NameValueMap<String, String> attrs = new NameValueMap<String, String>();
    attrs.addToCollection("version", "\"[1.1.0,1.2.0)\"");
    Content appContent2 = new ContentImpl("com.travel.reservation.web", attrs);
    assertTrue(appContents.contains(appContent2));
    assertTrue(appContents.contains(appContent1));
    List<ServiceDeclaration> importedService = app.getApplicationImportServices();
    assertEquals(2, importedService.size());
    assertTrue(importedService.contains(new ServiceDeclarationImpl("com.travel.flight.api")));
    assertTrue(importedService.contains(new ServiceDeclarationImpl("com.travel.rail.api")));
    List<ServiceDeclaration> exportedService = app.getApplicationExportServices();
    assertTrue(exportedService.contains(new ServiceDeclarationImpl("com.travel.reservation")));
    
    
  }
}
