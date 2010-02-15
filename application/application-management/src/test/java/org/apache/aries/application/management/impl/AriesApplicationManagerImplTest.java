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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.ApplicationMetadataFactory;
import org.apache.aries.application.Content;
import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.DeploymentMetadataFactory;
import org.apache.aries.application.filesystem.IDirectory;
import org.apache.aries.application.filesystem.IFile;
import org.apache.aries.application.impl.ApplicationMetadataFactoryImpl;
import org.apache.aries.application.impl.ContentImpl;
import org.apache.aries.application.impl.DeploymentContentImpl;
import org.apache.aries.application.impl.DeploymentMetadataFactoryImpl;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationResolver;
import org.apache.aries.application.management.BundleConverter;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.LocalPlatform;
import org.apache.aries.application.management.ManagementException;
import org.apache.aries.application.management.impl.AriesApplicationManagerImpl;
import org.apache.aries.application.management.impl.BundleInfoImpl;
import org.apache.aries.application.utils.filesystem.FileSystem;
import org.apache.aries.application.utils.filesystem.IOUtils;
import org.apache.aries.application.utils.manifest.BundleManifest;
import org.apache.aries.unittest.utils.EbaUnitTestUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Version;

/**
 * Initial, simple test that creates and stores an AriesApplication. No
 * BundleConverters are used in this test. 
 */
public class AriesApplicationManagerImplTest {
  
  static class DummyResolver implements AriesApplicationResolver {
    Set<BundleInfo> nextResult;
    public Set<BundleInfo> resolve(AriesApplication app) {
      return nextResult;
    } 
    void setNextResult (Set<BundleInfo> r) { 
      nextResult = r;
    }
    public BundleInfo getBundleInfo(String bundleSymbolicName, Version bundleVersion)
    {
      return null;
    }
  }
  
  static class DummyLocalPlatform implements LocalPlatform {
    public File getTemporaryDirectory() throws IOException {
      File f = File.createTempFile("ebaTmp", null);
      f.delete();
      f.mkdir();
      return f;
    } 
    public File getTemporaryFile () throws IOException { 
      // Not used
      return File.createTempFile("ebaTmp", null);
    }
  }

  static final String TEST_EBA = "./ariesApplicationManagerImplTest/test.eba";
  
  @BeforeClass 
  public static void preTest() throws Exception { 
    new File("ariesApplicationManagerImplTest").mkdir();
    EbaUnitTestUtils.createEba("../src/test/resources/bundles/test.eba", TEST_EBA);
    File src = new File ("../src/test/resources/bundles/repository/a.handy.persistence.library.jar");
    File dest = new File ("ariesApplicationManagerImplTest/a.handy.persistence.library.jar");
    IOUtils.zipUp(src, dest);
  }
  
  AriesApplicationManagerImpl _appMgr;
  ApplicationMetadataFactory _appMetaFactory;
  DummyResolver _resolver;
  
  @Before
  public void setup() { 
    _appMgr = new AriesApplicationManagerImpl ();
    _appMetaFactory = new ApplicationMetadataFactoryImpl ();

    DeploymentMetadataFactory dmf = new DeploymentMetadataFactoryImpl();
    List<BundleConverter> bundleConverters = new ArrayList<BundleConverter>();
    _resolver = new DummyResolver();
    
    _appMgr.setApplicationMetadataFactory(_appMetaFactory);
    _appMgr.setDeploymentMetadataFactory(dmf);
    _appMgr.setBundleConverters(bundleConverters);
    _appMgr.setResolver(_resolver);
    _appMgr.setLocalPlatform(new DummyLocalPlatform());
  }
  
  @Test
  public void testCreate() throws Exception { 
    AriesApplication app = createApplication (TEST_EBA);
    
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
  
  @Test
  public void testStoreAndReload() throws Exception { 
    AriesApplication app = createApplication (TEST_EBA);
    File dest = new File ("ariesApplicationManagerImplTest/stored.eba");
    app.store(dest);
    
    /* Dest should be a zip file with four entries:
     *  /foo.bar.widgets.jar
     *  /my.business.logic.jar
     *  /META-INF/APPLICATION.MF
     *  /META-INF/DEPLOYMENT.MF
     */
    
    IDirectory storedEba = FileSystem.getFSRoot(dest);
    assertNotNull (storedEba);
    assertEquals (storedEba.listFiles().size(), 3);
    IFile ifile = storedEba.getFile("META-INF/APPLICATION.MF");
    assertNotNull (ifile);
    ifile = storedEba.getFile ("META-INF/DEPLOYMENT.MF");
    assertNotNull (ifile);
    ifile = storedEba.getFile ("foo.bar.widgets.jar");
    assertNotNull (ifile);
    ifile = storedEba.getFile ("my.business.logic.jar");
    assertNotNull (ifile);
    
    AriesApplication newApp = _appMgr.createApplication(storedEba);
    DeploymentMetadata dm = newApp.getDeploymentMetadata();
    assertEquals (dm.getApplicationDeploymentContents().size(), 3);
    assertEquals (dm.getApplicationSymbolicName(), app.getApplicationMetadata().getApplicationSymbolicName());
    assertEquals (dm.getApplicationVersion(), app.getApplicationMetadata().getApplicationVersion());
  }
  
  private AriesApplication createApplication (String fileName) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, ManagementException  { 
    // This next block is a very long winded way of constructing a BundleInfoImpl
    // against the existing (BundleManifest bm, String location) constructor. If we 
    // find we need a String-based BundleInfoImpl constructor for other reasons, 
    // we could change to using it here. 
    Set<BundleInfo> nextResolverResult = new HashSet<BundleInfo>();
    String persistenceLibraryLocation = "../src/test/resources/bundles/repository/a.handy.persistence.library.jar";
    File persistenceLibrary = new File (persistenceLibraryLocation);
    BundleManifest mf = BundleManifest.fromBundle(persistenceLibrary);
    BundleInfo resolvedPersistenceLibrary = new BundleInfoImpl(_appMetaFactory, mf, persistenceLibraryLocation); 
    Field v = BundleInfoImpl.class.getDeclaredField("_version");
    v.setAccessible(true);
    v.set(resolvedPersistenceLibrary, new Version("1.1.0"));
    nextResolverResult.add(resolvedPersistenceLibrary);
    _resolver.setNextResult(nextResolverResult);
    
    IDirectory testEba = FileSystem.getFSRoot(new File(TEST_EBA));
    AriesApplication app = _appMgr.createApplication(testEba);
    return app;
  }
}
