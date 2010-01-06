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

package org.apache.aries.application.management.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.ApplicationMetadataManager;
import org.apache.aries.application.Content;
import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.DeploymentMetadataFactory;
import org.apache.aries.application.filesystem.IDirectory;
import org.apache.aries.application.impl.ApplicationMetadataManagerImpl;
import org.apache.aries.application.impl.ContentImpl;
import org.apache.aries.application.impl.DeploymentContentImpl;
import org.apache.aries.application.impl.DeploymentMetadataFactoryImpl;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationResolver;
import org.apache.aries.application.management.BundleConverter;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.impl.AriesApplicationManagerImpl;
import org.apache.aries.application.management.impl.BundleInfoImpl;
import org.apache.aries.application.utils.filesystem.FileSystem;
import org.apache.aries.application.utils.filesystem.IOUtils;
import org.apache.aries.application.utils.manifest.BundleManifest;
import org.apache.aries.unittest.utils.EbaUnitTestUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Version;

public class AriesApplicationManagerImplTest {
  
  class DummyResolver implements AriesApplicationResolver {

    Set<BundleInfo> nextResult;
    public Set<BundleInfo> resolve(AriesApplication app) {
      return nextResult;
    } 
    
    void setNextResult (Set<BundleInfo> r) { 
      nextResult = r;
    }
    
  }

  static String _testEba = "./ariesApplicationManagerImplTest/test.eba";
  
  @BeforeClass 
  public static void setup() throws Exception { 
    EbaUnitTestUtils.createEba("../src/test/resources/bundles/test.eba", _testEba);
    File src = new File ("../src/test/resources/bundles/repository/a.handy.persistence.library.jar");
    File dest = new File ("ariesApplicationManagerImplTest/a.handy.persistence.library.jar");
    IOUtils.zipUp(src, dest);
  }
  
  @Test
  public void testCreate() throws Exception { 
    AriesApplicationManagerImpl appMgr = new AriesApplicationManagerImpl ();
    ApplicationMetadataManager appMetaMgr = new ApplicationMetadataManagerImpl ();
    DeploymentMetadataFactory dmf = new DeploymentMetadataFactoryImpl();
    List<BundleConverter> bundleConverters = new ArrayList<BundleConverter>();
    DummyResolver resolver = new DummyResolver();
    
    appMgr.setApplicationMetadataManager(appMetaMgr);
    appMgr.setDeploymentMetadataFactory(dmf);
    appMgr.setBundleConverters(bundleConverters);
    appMgr.setResolver(resolver);
    
    // This next block is a very long winded way of constructing a BundleInfoImpl
    // against the existing (BundleManifest bm, String location) constructor. If we 
    // find we need a String-based BundleInfoImpl constructor for other reasons, 
    // we could change to using it here. 
    Set<BundleInfo> nextResolverResult = new HashSet<BundleInfo>();
    String persistenceLibraryLocation = "../src/test/resources/bundles/repository/a.handy.persistence.library.jar";
    File persistenceLibrary = new File (persistenceLibraryLocation);
    BundleManifest mf = BundleManifest.fromBundle(persistenceLibrary);
    BundleInfo resolvedPersistenceLibrary = new BundleInfoImpl(mf, persistenceLibraryLocation); 
    Field v = BundleInfoImpl.class.getDeclaredField("_version");
    v.setAccessible(true);
    v.set(resolvedPersistenceLibrary, new Version("1.1.0"));
    nextResolverResult.add(resolvedPersistenceLibrary);
    resolver.setNextResult(nextResolverResult);
    
    IDirectory testEba = FileSystem.getFSRoot(new File(_testEba));
    AriesApplication app = appMgr.createApplication(testEba);
    
    ApplicationMetadata appMeta = app.getApplicationMetadata();
    assertEquals (appMeta.getApplicationName(), "Test application");
    assertEquals (appMeta.getApplicationSymbolicName(), "org.apache.aries.application.management.test");
    assertEquals (appMeta.getApplicationVersion(), new Version("1.0"));
    List<Content> appContent = appMeta.getApplicationContents();
    assertEquals (appContent.size(), 2);
    Content fbw = new ContentImpl("foo.bar.widgets;version=1.0.0");
    Content mbl = new ContentImpl("my.business.logic;version=1.0.0");
    assertTrue (appContent.contains(fbw));
    assertTrue (appContent.contains(mbl));
    
    DeploymentMetadata dm = app.getDeploymentMetadata();
    List<DeploymentContent> dcList = dm.getApplicationDeploymentContents();
    
    assertEquals (dcList.size(), 3);
    DeploymentContent dc1 = new DeploymentContentImpl ("foo.bar.widgets;deployed-version=1.0.0");
    DeploymentContent dc2 = new DeploymentContentImpl ("my.business.logic;deployed-version=1.0.0");
    DeploymentContent dc3 = new DeploymentContentImpl ("a.handy.persistence.library;deployed-version=1.1.0");
    assertTrue (dcList.contains(dc1));
    assertTrue (dcList.contains(dc2));
    assertTrue (dcList.contains(dc3));
  
  }
}
