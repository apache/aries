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

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

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
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationContextManager;
import org.apache.aries.application.management.AriesApplicationResolver;
import org.apache.aries.application.management.BundleConversion;
import org.apache.aries.application.management.BundleConverter;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.ConversionException;
import org.apache.aries.application.management.DeploymentManifestManager;
import org.apache.aries.application.management.LocalPlatform;
import org.apache.aries.application.management.ManagementException;
import org.apache.aries.application.management.ResolveConstraint;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.management.UpdateException;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.utils.AppConstants;
import org.apache.aries.application.utils.filesystem.FileSystem;
import org.apache.aries.application.utils.filesystem.IOUtils;
import org.apache.aries.application.utils.management.SimpleBundleInfo;
import org.apache.aries.application.utils.manifest.BundleManifest;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.apache.aries.unittest.utils.EbaUnitTestUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Initial, simple test that creates and stores an AriesApplication. No
 * BundleConverters are used in this test. 
 */
public class AriesApplicationManagerImplTest {
  
  static class DummyDMManager implements DeploymentManifestManager {

    private AriesApplicationResolver resolver;
    public Manifest generateDeploymentManifest(AriesApplication app,
        ResolveConstraint... constraints) throws ResolverException
    {
      
      Map<String, String> deploymentMap = new HashMap<String, String>();
      Set<BundleInfo> byValueBundles = app.getBundleInfo();
      StringBuilder deployedContents = new StringBuilder();
      boolean beginning= true;
      for (BundleInfo bundle: byValueBundles) {
        if (!!!beginning) {
          deployedContents.append(",");
        }
        deployedContents.append(bundle.getSymbolicName()+";" + AppConstants.DEPLOYMENT_BUNDLE_VERSION + "=" +  bundle.getVersion());
        beginning = false;
      }
      deploymentMap.put(AppConstants.DEPLOYMENT_CONTENT, deployedContents.toString());
      // fake the provision bundle now.
      String persistenceLibraryLocation = "../src/test/resources/bundles/repository/a.handy.persistence.library.jar";
      File persistenceLibrary = new File (persistenceLibraryLocation);
      BundleManifest mf = BundleManifest.fromBundle(persistenceLibrary);
      
      deploymentMap.put(AppConstants.DEPLOYMENT_PROVISION_BUNDLE, mf.getSymbolicName()+";" + AppConstants.DEPLOYMENT_BUNDLE_VERSION + "=" + mf.getVersion());
      deploymentMap.put(AppConstants.APPLICATION_SYMBOLIC_NAME, app.getApplicationMetadata().getApplicationSymbolicName());
     
      deploymentMap.put(AppConstants.APPLICATION_VERSION, app.getApplicationMetadata().getApplicationVersion().toString());
      
      
      Manifest man = new Manifest();
      Attributes att = man.getMainAttributes();
      att.putValue(Attributes.Name.MANIFEST_VERSION.toString(), AppConstants.MANIFEST_VERSION);
      for (Map.Entry<String, String> entry : deploymentMap.entrySet()) {
        att.putValue(entry.getKey(),  entry.getValue());
      }
      return man;
    }

   

    public Manifest generateDeploymentManifest(AriesApplication app,
        Collection<ModelledResource> byValueBundles, Collection<Content> useBundleSet,
        Collection<Content> otherBundles) throws ResolverException
    {
      
      return null;
    }


    public void setResolver(AriesApplicationResolver resolver) {
      this.resolver = resolver;
    }

    public AriesApplicationResolver getResolver()
    {
      
      return resolver;
    }
    
    
  }

  static class DummyResolver implements AriesApplicationResolver {
    Set<BundleInfo> nextResult;
    public Set<BundleInfo> resolve(AriesApplication app, ResolveConstraint... constraints) {
      Set<BundleInfo> info = new HashSet<BundleInfo>(nextResult);
      
      info.addAll(app.getBundleInfo());
      
      return info;
    } 
    void setNextResult (Set<BundleInfo> r) { 
      nextResult = r;
    }
    public BundleInfo getBundleInfo(String bundleSymbolicName, Version bundleVersion)
    {
      return null;
    }
    public Collection<ModelledResource> resolve(String appName, String appVersion,
        Collection<ModelledResource> byValueBundles, Collection<Content> inputs)
        throws ResolverException
    {
      
      return byValueBundles;
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
  
  static class DummyConverter implements BundleConverter {

	public BundleConversion convert(IDirectory parentEba, IFile toBeConverted)
			throws ConversionException {
		if (toBeConverted.getName().equals("helloWorld.war")) {
			InputStream is = null;
            try {
            	is = new FileInputStream(new File("../src/test/resources/conversion/MANIFEST.MF"));
            	Manifest warManifest = new Manifest(is);           
            	final File convertedFile = new File("./ariesApplicationManagerImplTest/conversion/helloWorld.war");
            	IOUtils.jarUp(new File("../src/test/resources/conversion/conversion.eba/helloWorld.war"), convertedFile, warManifest);            
                final String location = toBeConverted.toString();                
            	return new BundleConversion() {

					public BundleInfo getBundleInfo() throws IOException {
						return new SimpleBundleInfo(BundleManifest.fromBundle(convertedFile), location);
					}

					public InputStream getInputStream() throws IOException {
						return new FileInputStream(convertedFile);
					}
                	
                };
            } catch (IOException e) {
            	e.printStackTrace();                
            } finally {
            	try {
            	if (is != null)
            		is.close();
            	} catch (Exception e) {
            		e.printStackTrace();
            	}
            }
        }

        return null;
    }
	
	  
  }
  


  static final String TEST_EBA = "./ariesApplicationManagerImplTest/test.eba";
  static final String CONVERSION_EBA = "./ariesApplicationManagerImplTest/conversion.eba";
  @BeforeClass 
  public static void preTest() throws Exception { 
    new File("ariesApplicationManagerImplTest/conversion").mkdirs();
    EbaUnitTestUtils.createEba("../src/test/resources/bundles/test.eba", TEST_EBA);
    File src = new File ("../src/test/resources/bundles/repository/a.handy.persistence.library.jar");
    File dest = new File ("ariesApplicationManagerImplTest/a.handy.persistence.library.jar");
    IOUtils.zipUp(src, dest);
    EbaUnitTestUtils.createEba("../src/test/resources/conversion/conversion.eba", CONVERSION_EBA);
  }
  
  AriesApplicationManagerImpl _appMgr;
  ApplicationMetadataFactory _appMetaFactory;
  DummyResolver _resolver;
  DummyConverter _converter;
  DummyDMManager _dmMgr;
  @Before
  public void setup() { 
    _appMgr = new AriesApplicationManagerImpl ();
    _appMetaFactory = new ApplicationMetadataFactoryImpl ();

    DeploymentMetadataFactory dmf = new DeploymentMetadataFactoryImpl();
    _converter = new DummyConverter();
    List<BundleConverter> bundleConverters = new ArrayList<BundleConverter>();
    bundleConverters.add(_converter);
    _resolver = new DummyResolver(); 
    _dmMgr = new DummyDMManager();
    _dmMgr.setResolver(_resolver);
    _appMgr.setApplicationMetadataFactory(_appMetaFactory);
    _appMgr.setDeploymentMetadataFactory(dmf);
    _appMgr.setBundleConverters(bundleConverters);
    _appMgr.setDeploymentManifestManager(_dmMgr);
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

    assertEquals (2, dcList.size());
    DeploymentContent dc1 = new DeploymentContentImpl ("foo.bar.widgets;deployed-version=1.1.0");
    DeploymentContent dc2 = new DeploymentContentImpl ("my.business.logic;deployed-version=1.1.0");
    DeploymentContent dc3 = new DeploymentContentImpl ("a.handy.persistence.library;deployed-version=1.1.0");
    assertTrue (dcList.contains(dc1));
    assertTrue (dcList.contains(dc2));
    
    dcList = dm.getApplicationProvisionBundles();
    
    assertEquals(1, dcList.size());
    assertTrue (dcList.contains(dc3));

  }
  
  @Test
  public void testCreateAndConversion() throws Exception {
	  	AriesApplication app = createApplication (CONVERSION_EBA);	    
	    ApplicationMetadata appMeta = app.getApplicationMetadata();	    
	    assertEquals (appMeta.getApplicationName(), "conversion.eba");	   
	    assertEquals (appMeta.getApplicationSymbolicName(), "conversion.eba");	    
	    assertEquals (appMeta.getApplicationVersion(), new Version("0.0"));	    
	    List<Content> appContent = appMeta.getApplicationContents();
	    assertEquals (2, appContent.size());
	    Content fbw = new ContentImpl("hello.world.jar;version=\"[1.1.0, 1.1.0]\"");
	    Content mbl = new ContentImpl("helloWorld.war;version=\"[0.0.0, 0.0.0]\"");
	    assertTrue (appContent.contains(fbw));
	    assertTrue (appContent.contains(mbl));
	    
	    DeploymentMetadata dm = app.getDeploymentMetadata();
	    List<DeploymentContent> dcList = dm.getApplicationDeploymentContents();

	    assertEquals (2, dcList.size());
	    DeploymentContent dc1 = new DeploymentContentImpl ("hello.world.jar;deployed-version=1.1.0");
	    DeploymentContent dc2 = new DeploymentContentImpl ("helloWorld.war;deployed-version=0.0.0");
	    DeploymentContent dc3 = new DeploymentContentImpl ("a.handy.persistence.library;deployed-version=1.1.0");
	    assertTrue (dcList.contains(dc1));
	    assertTrue (dcList.contains(dc2));
	    
	    dcList = dm.getApplicationProvisionBundles();
	    
	    assertEquals(1, dcList.size());
	    assertTrue (dcList.contains(dc3));
	    
	    assertEquals(2, app.getBundleInfo().size());
	    BundleInfo info;
	    info = findBundleInfo(app.getBundleInfo(), "hello.world.jar");
	    assertNotNull(info);
	    assertEquals("HelloWorldJar", info.getHeaders().get(Constants.BUNDLE_NAME));
	    
	    info = findBundleInfo(app.getBundleInfo(), "helloWorld.war");
        assertNotNull(info);
        assertEquals("helloWorld.war", info.getHeaders().get(Constants.BUNDLE_NAME));
        assertEquals("/test", info.getHeaders().get("Bundle-ContextPath"));
  }
  
  private BundleInfo findBundleInfo(Set<BundleInfo> infos, String symbolicName) {
      for (BundleInfo info : infos) {
          if (symbolicName.equals(info.getSymbolicName())) {
              return info;
          }
      }
      return null;
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
    assertEquals (2, dm.getApplicationDeploymentContents().size());
    assertEquals(1, dm.getApplicationProvisionBundles().size());
    assertEquals (dm.getApplicationSymbolicName(), app.getApplicationMetadata().getApplicationSymbolicName());
    assertEquals (dm.getApplicationVersion(), app.getApplicationMetadata().getApplicationVersion());
  }
  
  @Test
  public void testUpdate() throws Exception {
    AriesApplication app = createApplication(TEST_EBA);

    DeploymentMetadata depMf = createUpdateDepMf();
    
    AriesApplicationContextManager ctxMgr = Skeleton.newMock(AriesApplicationContextManager.class);
    _appMgr.setApplicationContextManager(ctxMgr);

    _appMgr.update(app, depMf);
    
    assertTrue("Deployment.mf should have been updated", app.getDeploymentMetadata() == depMf);
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testUpdateWithIncorrectDepMf() throws Exception
  {
    AriesApplication app = createApplication(TEST_EBA);

    DeploymentMetadata depMf = Skeleton.newMock(DeploymentMetadata.class);
    Skeleton.getSkeleton(depMf).setReturnValue(new MethodCall(DeploymentMetadata.class, "getApplicationSymbolicName"), "random.app");
    Skeleton.getSkeleton(depMf).setReturnValue(new MethodCall(DeploymentMetadata.class, "getApplicationVersion"), new Version("1.0.0"));
    
    AriesApplicationContextManager ctxMgr = Skeleton.newMock(AriesApplicationContextManager.class);
    _appMgr.setApplicationContextManager(ctxMgr);

    _appMgr.update(app, depMf);    
  }
  
  @Test
  public void testFailedUpdate() throws Exception {
    AriesApplication app = createApplication(TEST_EBA);

    DeploymentMetadata depMf = createUpdateDepMf();
    
    AriesApplicationContext ctx = Skeleton.newMock(AriesApplicationContext.class);
    Skeleton.getSkeleton(ctx).setReturnValue(new MethodCall(AriesApplicationContext.class, "getApplication"), app);
    
    AriesApplicationContextManager ctxMgr = Skeleton.newMock(AriesApplicationContextManager.class);
    Skeleton.getSkeleton(ctxMgr).setReturnValue(
        new MethodCall(AriesApplicationContextManager.class, "getApplicationContexts"), 
        Collections.singleton(ctx));    
    
    Skeleton.getSkeleton(ctxMgr).setThrows(
        new MethodCall(AriesApplicationContextManager.class, "update", AriesApplication.class, DeploymentMetadata.class), 
        new UpdateException("", null, false, null));
    
    _appMgr.setApplicationContextManager(ctxMgr);

    try {
      _appMgr.update(app, depMf);
      fail("Update should have failed.");
    } catch (UpdateException e) {
      assertTrue("Deployment.mf should have been updated", app.getDeploymentMetadata() == depMf);
    }
  }
  
  @Test
  public void testRolledbackUpdate() throws Exception {
    AriesApplication app = createApplication(TEST_EBA);

    DeploymentMetadata depMf = createUpdateDepMf();
    DeploymentMetadata oldMf = app.getDeploymentMetadata();
    
    AriesApplicationContext ctx = Skeleton.newMock(AriesApplicationContext.class);
    Skeleton.getSkeleton(ctx).setReturnValue(new MethodCall(AriesApplicationContext.class, "getApplication"), app);
    
    AriesApplicationContextManager ctxMgr = Skeleton.newMock(AriesApplicationContextManager.class);
    Skeleton.getSkeleton(ctxMgr).setReturnValue(
        new MethodCall(AriesApplicationContextManager.class, "getApplicationContexts"), 
        Collections.singleton(ctx));    
    
    Skeleton.getSkeleton(ctxMgr).setThrows(
        new MethodCall(AriesApplicationContextManager.class, "update", AriesApplication.class, DeploymentMetadata.class), 
        new UpdateException("", null, true, null));
    
    _appMgr.setApplicationContextManager(ctxMgr);

    try {
      _appMgr.update(app, depMf);
      fail("Update should have failed.");
    } catch (UpdateException e) {
      assertTrue("Deployment.mf should have been rolled back to the old", app.getDeploymentMetadata() == oldMf);
    }
  }
  
  private DeploymentMetadata createUpdateDepMf()
  {
    DeploymentMetadata depMf = Skeleton.newMock(DeploymentMetadata.class);
    Skeleton.getSkeleton(depMf).setReturnValue(new MethodCall(DeploymentMetadata.class, "getApplicationSymbolicName"), "org.apache.aries.application.management.test");
    Skeleton.getSkeleton(depMf).setReturnValue(new MethodCall(DeploymentMetadata.class, "getApplicationVersion"), new Version("1.0.0"));

    return depMf;
  }
  
  private AriesApplication createApplication (String fileName) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, ManagementException, ResolverException {
    // This next block is a very long winded way of constructing a BundleInfoImpl
    // against the existing (BundleManifest bm, String location) constructor. If we 
    // find we need a String-based BundleInfoImpl constructor for other reasons, 
    // we could change to using it here. 
    Set<BundleInfo> nextResolverResult = new HashSet<BundleInfo>();
    String persistenceLibraryLocation = "../src/test/resources/bundles/repository/a.handy.persistence.library.jar";
    File persistenceLibrary = new File (persistenceLibraryLocation);
    BundleManifest mf = BundleManifest.fromBundle(persistenceLibrary);
    BundleInfo resolvedPersistenceLibrary = new SimpleBundleInfo(mf, persistenceLibraryLocation); 
    Field v = SimpleBundleInfo.class.getDeclaredField("_version");
    v.setAccessible(true);
    v.set(resolvedPersistenceLibrary, new Version("1.1.0"));
    nextResolverResult.add(resolvedPersistenceLibrary);
    _resolver.setNextResult(nextResolverResult);
    
    IDirectory testEba = FileSystem.getFSRoot(new File(fileName));    
    AriesApplication app = _appMgr.createApplication(testEba);
    app = _appMgr.resolve(app);
    return app;
  }
}
