package org.apache.aries.application.runtime.framework.management;
import java.util.Arrays;

import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.spi.framework.BundleFramework;
import org.apache.aries.application.management.spi.framework.BundleFrameworkConfiguration;
import org.apache.aries.application.management.spi.framework.BundleFrameworkConfigurationFactory;
import org.apache.aries.application.management.spi.framework.BundleFrameworkFactory;
import org.apache.aries.application.management.spi.repository.BundleRepository.BundleSuggestion;
import org.apache.aries.application.runtime.framework.management.BundleFrameworkManagerImpl;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import static junit.framework.Assert.*;

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

public class BundleFrameworkManagerTest {
  private BundleFrameworkManagerImpl sut;
  private Skeleton frameworkFactory;
  
  @Before
  public void setup() {
    sut = new BundleFrameworkManagerImpl();

    BundleFrameworkConfigurationFactory bfcf = Skeleton.newMock(BundleFrameworkConfigurationFactory.class);
    sut.setBundleFrameworkConfigurationFactory(bfcf);
    
    BundleFrameworkFactory bff = Skeleton.newMock(BundleFrameworkFactory.class);
    sut.setBundleFrameworkFactory(bff); 
    frameworkFactory = Skeleton.getSkeleton(bff);
    
    sut.init();
  }
  
  @Test
  public void testFailedInstall() throws Exception {
    /*
     * Mock up a failing framework install
     */
    BundleFramework fwk = Skeleton.newMock(new Object() {
      public Bundle install(BundleSuggestion suggestion, AriesApplication app) throws BundleException {
        throw new BundleException("Expected failure");
      }
    }, BundleFramework.class);

    frameworkFactory.setReturnValue(
        new MethodCall(BundleFrameworkFactory.class, "createBundleFramework", 
            BundleContext.class, BundleFrameworkConfiguration.class), 
        fwk);    
    
    try {
      sut.installIsolatedBundles(Arrays.asList(Skeleton.newMock(BundleSuggestion.class)), 
          Skeleton.newMock(AriesApplication.class));
      
      fail("Expected a BundleException");
      
    } catch (BundleException be) {
      // when a failure occurred we need to have cleaned up the new framework, otherwise it is
      // left as floating debris
      Skeleton.getSkeleton(fwk).assertCalled(new MethodCall(BundleFramework.class, "close"));
    }
  }
}
