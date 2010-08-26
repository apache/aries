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
package org.apache.aries.application.modelling.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.modelling.ExportedPackage;
import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedBundle;
import org.apache.aries.application.modelling.ImportedPackage;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModelledResource;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;



/* This is an abstract class and should not be instantiated, so we have an ignore
 * annotation to the class.
 */
@Ignore
public abstract class AbstractBundleResourceTest
{
  protected ModelledResource bundleResource;

  @Before
  public void setUp() throws Exception
  {
    bundleResource = instantiateBundleResource();
  }

  /**
   * @return
   * @throws ResolverException 
   * @throws FileNotFoundException 
   * @throws IOException 
   * @throws Exception 
   */
  protected abstract ModelledResource instantiateBundleResource() throws Exception;

  @Test
  public void testBundleResource() throws Exception
  {
    assertEquals("The bundle symbolic name is wrong.", "test.bundle1", bundleResource.getSymbolicName());
    assertEquals("The bundle version is wrong.", "2.0.0.build-121", bundleResource.getVersion().toString());
    assertEquals("The bundle presentation name is wrong.", "Test Bundle", bundleResource.getExportedBundle()
        .getAttributes().get(ModellingConstants.OBR_PRESENTATION_NAME));
    

    
    int count = 0;
  
    for (ImportedPackage ip : bundleResource.getImportedPackages()) {
      
      if (ip.getPackageName().equals("org.osgi.framework")) {
        count++;
        assertEquals("The filter is wrong.", "(&(package=org.osgi.framework)(version>=1.3.0))", 
            ip.getAttributeFilter());
  
      } else if (ip.getPackageName().equals("aries.ws.kernel.file")) {
        count++;
        assertEquals("The filter is wrong.", "(&(package=aries.ws.kernel.file)(version>=0.0.0))", ip.getAttributeFilter());
  
      } else if (ip.getPackageName().equals("aries.wsspi.application.aries")) {
        count++;
        assertEquals("The filter is wrong.",
            "(&(package=aries.wsspi.application.aries)(version>=0.0.0)(company=yang)(mandatory:<*company))", ip
                .getAttributeFilter());
      } else if (ip.getPackageName().equals("aries.ws.ffdc")) {
        count++;
        assertEquals("The filter is wrong.", "(&(package=aries.ws.ffdc)(version>=0.0.0))", ip.getAttributeFilter());
        assertTrue ("Optional import not correctly represented", ip.isOptional());
      } else if (ip.getPackageName().equals("aries.ws.app.framework.plugin")) {
        count++;
        assertEquals(
            "The filter is wrong.",
            "(&(package=aries.ws.app.framework.plugin)(version>=1.0.0)(version<=2.0.0)(!(version=2.0.0)))",
            ip.getAttributeFilter());
      } else if (ip.getPackageName().equals("aries.ejs.ras")) {
        count++;
        assertEquals("The filter is wrong.", "(&(package=aries.ejs.ras)(version>=0.0.0))", ip.getAttributeFilter());
      } else if (ip.getPackageName().equals("aries.ws.event")) {
        count++;
        assertEquals("The filter is wrong.", "(&(package=aries.ws.event)(version>=1.0.0))", ip
            .getAttributeFilter());
      } else if (ip.getPackageName().equals("aries.wsspi.app.container.aries")) {
        count++;
        assertEquals(
            "The filter is wrong.",
            "(&(package=aries.wsspi.app.container.aries)(version>=0.0.0)(bundle-symbolic-name=B)(bundle-version>=1.2.0)(bundle-version<=2.2.0)(!(bundle-version=2.2.0)))",
            ip.getAttributeFilter());
      } else if (ip.getPackageName().equals("aries.ws.eba.bla")) {
  
        count++;
        assertEquals("The filter is wrong.", "(&(package=aries.ws.eba.bla)(version>=0.0.0))", ip.getAttributeFilter());
      } else if (ip.getPackageName().equals("aries.ws.eba.launcher")) {
  
        count++;
        assertEquals("The filter is wrong.",
            "(&(package=aries.ws.eba.launcher)(version>=1.0.0)(version<=2.0.0))", ip.getAttributeFilter());
        assertTrue ("Dynamic-ImportPackage should be optional", ip.isOptional());
  
      } else if (ip.getPackageName().equals("aries.ws.eba.bundle4")) {
        count++;
        assertEquals("The filter is wrong.", "(&(package=aries.ws.eba.bundle4)(version>=3.0.0))",
            ip.getAttributeFilter());
      } else if (ip.getPackageName().equals("aries.ws.eba.bundle5")) {
        count++;
        assertEquals("The filter is wrong.", "(&(package=aries.ws.eba.bundle5)(version>=3.0.0))",
            ip.getAttributeFilter());
      } else if (ip.getPackageName().equals("aries.ws.eba.bundle6")) {
        count++;
        assertEquals("The filter is wrong.", "(&(package=aries.ws.eba.bundle6)(version>=0.0.0))", ip.getAttributeFilter());
      } else if (ip.getPackageName().equals("aries.ws.eba.bundle7")) {
        count++;
        assertEquals("The filter is wrong.", "(&(package=aries.ws.eba.bundle7)(version>=0.0.0))", ip.getAttributeFilter());
      } 
    }
      
    for (ImportedBundle ib : bundleResource.getRequiredBundles()) {
    
      if (ib.getSymbolicName().equals("com.acme.facade")) {
        count++;
        assertEquals("The filter is wrong.", "(&(symbolicname=com.acme.facade)(version>=3.0.0))",
            ib.getAttributeFilter());
      } else if (ib.getSymbolicName().equals("com.acme.bar")) {
        count++;
        assertEquals("The filter is wrong.", "(symbolicname=com.acme.bar)", ib.getAttributeFilter());
      } else if (ib.getSymbolicName().equals("aries.ws.eba.framework")) {
        count++;
        assertEquals("The filter is wrong.",
            "(&(symbolicname=aries.ws.eba.framework)(version>=3.0.0)(version<=4.0.0))", ib
                .getAttributeFilter());
      } else if (ib.getSymbolicName().equals("com.de.ba")) {
        count++;
        assertEquals("The filter is wrong.", "(symbolicname=com.de.ba)", ib.getAttributeFilter());
      } else if (ib.getSymbolicName().equals("com.ab.de")) {
        count++;
        assertEquals("The filter is wrong.", "(symbolicname=com.ab.de)", ib.getAttributeFilter());
      }
    }
    
    for(ImportedService svc : bundleResource.getImportedServices()) {
      if (svc.getInterface().equals("aries.ws.eba.import")) {
        count++;
        assertTrue("objectClass should be aries.ws.eba.import", svc.getAttributeFilter().contains("(objectClass=aries.ws.eba.import)"));
        assertTrue("(service=service) should be present", svc.getAttributeFilter().contains("(service=service)"));
        assertTrue("(mandatory:<*service) should be present", svc.getAttributeFilter().contains("(mandatory:<*service)"));        
      } 
    }
    
    assertEquals("Not all requirements are listed.", bundleResource.getImportedPackages().size() +
        bundleResource.getImportedServices().size() + bundleResource.getRequiredBundles().size() , count);
  
    //verify the capability
  
    int verifiedExport = 0;
    for (ExportedPackage cap : bundleResource.getExportedPackages()) {
 
        if (cap.getPackageName().equals("aries.ws.eba.bundle1")) {
  
          verifiedExport++;
          assertEquals("The export package is not expected.", "2.2.0", cap.getVersion());
          assertEquals("The export package is not expected.", "test.bundle1", cap.getAttributes().get(
              "bundle-symbolic-name"));
          assertEquals("The export package is not expected.", "2.0.0.build-121", cap.getAttributes()
              .get("bundle-version").toString());
        } else if (cap.getPackageName().equals("aries.ws.eba.bundle2")) {
          verifiedExport++;
          assertEquals("The export package is not expected.", "3", cap.getVersion());
        } else if (cap.getPackageName().equals("aries.ws.eba.bundle3")) {
          verifiedExport++;
          assertEquals("The export package is not expected.", "3", cap.getVersion());
        }
    }
    assertEquals("The number of exports are not expected.", bundleResource.getExportedPackages().size()
        , verifiedExport);
    

    // bundle resource
    assertEquals("The bundle resource is wrong.", "Test Bundle", bundleResource.getExportedBundle().
        getAttributes().get(ModellingConstants.OBR_PRESENTATION_NAME));
    assertEquals("The bundle resource is wrong.", "2.0.0.build-121", bundleResource.getExportedBundle().
        getVersion());
    assertEquals("The bundle resource is wrong.", "test.bundle1", bundleResource.getExportedBundle().
        getSymbolicName());
    
    
    for (ExportedService svc : bundleResource.getExportedServices()) {
      assertEquals("The export service is wrong", "aries.ws.eba.export", svc.getInterfaces().
          iterator().next());
    }
  }
}
