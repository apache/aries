/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.ejb.modelling.impl;

import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.aries.application.modelling.ModellerException;
import org.apache.aries.ejb.modelling.EJBLocator;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.manifest.BundleManifest;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;

public class EJBModellerTest {

  private EJBModeller modeller;
  private Skeleton ejbLocator; 
  
  private IDirectory bundleLocation;
  
  @Before
  public void setup() {
    modeller = new EJBModeller();

    EJBLocator locator = Skeleton.newMock(EJBLocator.class);
    modeller.setLocator(locator);
    
    ejbLocator = Skeleton.getSkeleton(locator);
    
    bundleLocation = Skeleton.newMock(IDirectory.class);
  }
  
  @Test
  public void testModelServicesNoExportEJB() throws ModellerException {
    Manifest man = new Manifest();
    setBasicHeaders(man);
    modeller.modelServices(new BundleManifest(man), bundleLocation);
    ejbLocator.assertSkeletonNotCalled();
  }
  
  @Test
  public void testModelServicesEmptyExportEJB() throws ModellerException {
    Manifest man = new Manifest();
    setBasicHeaders(man);
    man.getMainAttributes().putValue("Export-EJB", "");
    modeller.modelServices(new BundleManifest(man), bundleLocation);
    ejbLocator.assertSkeletonNotCalled();
  }
  
  @Test
  public void testModelServicesNoneExportEJB() throws ModellerException {
    Manifest man = new Manifest();
    setBasicHeaders(man);
    man.getMainAttributes().putValue("Export-EJB", "NONE,anEJB , another");
    modeller.modelServices(new BundleManifest(man), bundleLocation);
    ejbLocator.assertSkeletonNotCalled();
  }
  
  @Test
  public void testModelServicesExportEJB() throws ModellerException {
    Manifest man = new Manifest();
    setBasicHeaders(man);
    man.getMainAttributes().putValue("Export-EJB", "anEJB , another");
    modeller.modelServices(new BundleManifest(man), bundleLocation);
    ejbLocator.assertCalled(new MethodCall(EJBLocator.class, "findEJBs", BundleManifest.class,
        bundleLocation, ParsedEJBServices.class));
  }

  private void setBasicHeaders(Manifest man) {
    Attributes att = man.getMainAttributes();
    att.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
    att.putValue(Constants.BUNDLE_SYMBOLICNAME, "testBundle");
  }

}
