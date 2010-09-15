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
package org.apache.aries.application.deployment.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.Content;
import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.VersionRange;
import org.apache.aries.application.deployment.management.impl.DeploymentManifestManagerImpl;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.ResolveConstraint;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.management.spi.resolve.AriesApplicationResolver;
import org.apache.aries.application.management.spi.runtime.LocalPlatform;
import org.apache.aries.application.modelling.ExportedPackage;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ModellingManager;
import org.apache.aries.application.modelling.impl.ModellingManagerImpl;
import org.apache.aries.application.modelling.utils.ModellingHelper;
import org.apache.aries.application.modelling.utils.impl.ModellingHelperImpl;
import org.apache.aries.application.utils.AppConstants;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.apache.aries.mocks.BundleContextMock;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;


/**
 * Tests to ensure we generate DEPLOYMENT.MF artifacts correctly. 
 */
public class DeploymentGeneratorTest
{
  private DeploymentManifestManagerImpl sut;
  private AriesApplication app;
  private ApplicationMetadata appMetadata;
  
  private static class MockResolver implements AriesApplicationResolver {
    boolean returnAppContentNextTime = true;
    
    public Collection<ModelledResource> resolve(String appName, String appVersion,
        Collection<ModelledResource> byValueBundles, Collection<Content> inputs)
        throws ResolverException
    {
      if (_nextResults != null && !_nextResults.isEmpty()) { 
        Collection<ModelledResource> result = _nextResults.remove(0);
        return result;
      }
      Collection<ModelledResource> res = new ArrayList<ModelledResource>();
      if (returnAppContentNextTime) { 
        res.add(CAPABILITY_A.getBundle());
        res.add(CAPABILITY_B.getBundle());
      } 
      res.add(CAPABILITY_C.getBundle());
      res.add(CAPABILITY_E.getBundle());
      boolean addD = false;
      for(Content ib : inputs) {
        if(ib.getContentName().equals("aries.test.d"))
          addD = true;
      }
      if(addD) {
        try {
          res.add(createModelledResource("aries.test.d", "1.0.0", new ArrayList<String>(), new ArrayList<String>()));
        } catch (InvalidAttributeException e) {
          fail("Cannot resolve import for d");
        }
      }
      
      //  deployment manifest manager calls resolve() an extra time, providing
      // just the shared bundles. 
      // If we added D, then the next resolve will be one trying to winnow D out: 
      // AppContent should be returned in that one. We should not return app content
      // next time if we did so last time, unless we just added D
      returnAppContentNextTime = !returnAppContentNextTime || addD;
      return res;
    }
    
    List<Collection<ModelledResource>> _nextResults = null;
    
    //  Some tests want to override the default behaviour of the resolve() method
    public void addResult (Collection<ModelledResource> result) { 
      if (_nextResults == null) { 
        _nextResults = new ArrayList<Collection<ModelledResource>>();
      }
      _nextResults.add(result);
    }

    public BundleInfo getBundleInfo(String bundleSymbolicName, Version bundleVersion)
    {
      return null;
    }

    public Set<BundleInfo> resolve(AriesApplication app, ResolveConstraint... constraints)
        throws ResolverException
    {
      return null;
    }

    
  }
  
  static MockResolver _resolver = new MockResolver();

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
  static LocalPlatform localPlatform = new DummyLocalPlatform();
  static ModellingManager modellingManager = new ModellingManagerImpl();
  static ModellingHelper modellingHelper = new ModellingHelperImpl();
  
  @BeforeClass
  public static void classSetup() throws Exception
  {
    BundleContext bc = Skeleton.newMock(BundleContext.class);
    bc.registerService(AriesApplicationResolver.class.getName(), _resolver, new Hashtable<String, String>());
    bc.registerService(ModellingManager.class.getName(), modellingManager, new Hashtable<String, String>());
    bc.registerService(ModellingHelper.class.getName(), modellingHelper, new Hashtable<String, String>());
  }
  
  @AfterClass
  public static void afterClass() throws Exception { 
    BundleContextMock.clear();
  }
  
  @Before
  public void setup() throws Exception
  {
    appMetadata = Skeleton.newMock(ApplicationMetadata.class);
    Skeleton.getSkeleton(appMetadata).setReturnValue(
        new MethodCall(ApplicationMetadata.class, "getApplicationSymbolicName"), "aries.test");
    Skeleton.getSkeleton(appMetadata).setReturnValue(
        new MethodCall(ApplicationMetadata.class, "getApplicationVersion"), new Version("1.0.0"));
    
    app = Skeleton.newMock(AriesApplication.class);
    Skeleton.getSkeleton(app).setReturnValue(new MethodCall(AriesApplication.class, "getApplicationMetadata"), appMetadata);
    
    sut = new DeploymentManifestManagerImpl();
    sut.setResolver(_resolver);
    sut.setLocalPlatform(localPlatform);
    sut.setModellingManager(modellingManager);
    sut.setModellingHelper(modellingHelper);
  }
  
  private static ExportedPackage CAPABILITY_A;
  private static ExportedPackage CAPABILITY_B;
  private static ExportedPackage CAPABILITY_C;
  private static ExportedPackage CAPABILITY_E;

  
  // use bundle
  private static Content BUNDLE_C;
  private static Content BUNDLE_D;
  
  

  public static ExportedPackage createExportedPackage (String bundleName, String bundleVersion, 
      String[] exportedPackages, String[] importedPackages ) throws InvalidAttributeException { 
    ModelledResource mb = createModelledResource(bundleName, bundleVersion,
        Arrays.asList(importedPackages) , Arrays.asList(exportedPackages));
    
   return mb.getExportedPackages().iterator().next();

  }
  static {
    try {
      CAPABILITY_A = createExportedPackage ("aries.test.a", "1.0.0", new String[] {"aries.test.a"}, 
          new String[] {"aries.test.c"});
 
      CAPABILITY_B = createExportedPackage("aries.test.b", "1.1.0", new String[] {"aries.test.b"}, new String[] {"aries.test.e"});
      
      BUNDLE_C = ManifestHeaderProcessor.parseContent("aries.test.c","[1.0.0,1.1.0)");
      
      CAPABILITY_C = createExportedPackage("aries.test.c", "1.0.5", new String[] {"aries.test.c"}, new String[] {});
      
      BUNDLE_D = ManifestHeaderProcessor.parseContent("aries.test.d","1.0.0");
      
     // = new ImportedBundleImpl("aries.test.e", "1.0.0");
      
      CAPABILITY_E = createExportedPackage("aries.test.e", "1.0.0", new String[] {"aries.test.e"}, new String[] {});
      
    } catch (InvalidAttributeException iae) {
      throw new RuntimeException(iae);
    }
  }
  
  
  @Test
  public void testResolve() throws Exception
  {
    
    Skeleton.getSkeleton(appMetadata).setReturnValue(new MethodCall(ApplicationMetadata.class, "getApplicationContents"), Arrays.asList(mockContent("aries.test.a", "1.0.0"), mockContent("aries.test.b", "[1.0.0, 2.0.0)" )));
    Manifest man = sut.generateDeploymentManifest(appMetadata.getApplicationSymbolicName(), 
        appMetadata.getApplicationVersion().toString(), appMetadata.getApplicationContents(), 
        new ArrayList<ModelledResource>(), 
        Arrays.asList(BUNDLE_C, BUNDLE_D), Collections.<Content>emptyList(), 
        appMetadata.getApplicationImportServices());
    Attributes attrs = man.getMainAttributes();
    
    assertEquals("aries.test", attrs.getValue(AppConstants.APPLICATION_SYMBOLIC_NAME));
    assertEquals("1.0.0", (String)attrs.getValue(AppConstants.APPLICATION_VERSION));
    
    String content = (String)attrs.getValue(AppConstants.DEPLOYMENT_CONTENT);
    String useBundle = (String) attrs.getValue(AppConstants.DEPLOYMENT_USE_BUNDLE);
    String provisioned =(String)attrs.getValue(AppConstants.DEPLOYMENT_PROVISION_BUNDLE);
    
    assertTrue(content.contains("aries.test.a;deployed-version=1.0.0"));
    assertTrue(content.contains("aries.test.b;deployed-version=1.1.0"));
    
    assertTrue(useBundle.contains("aries.test.c;deployed-version=1.0.5"));
    assertFalse(useBundle.contains("aries.test.d"));
    
    assertTrue(provisioned.contains("aries.test.e;deployed-version=1.0.0"));
  }
  
  @Test
  public void checkBasicCircularDependenciesDetected() throws Exception { 
    // Override Resolver behaviour. 
    //ImportedBundle isolated = new ImportedBundleImpl ("test.isolated" , "1.0.0"); 
    
    // When we resolve isolated, we're going to get another bundle which has a dependency on isolated. 
    Collection<ModelledResource> cmr = new ArrayList<ModelledResource>();
    ExportedPackage testIsolatedPkg = createExportedPackage ("test.isolated", "1.0.0", 
        new String[] {"test.shared"}, new String[] {"test.isolated.pkg"});
    cmr.add (testIsolatedPkg.getBundle());
    
    ExportedPackage testSharedPkg = createExportedPackage ("test.shared", "1.0.0", 
        new String[] {"test.isolated.pkg"}, new String[] {"test.shared"});
    cmr.add (testSharedPkg.getBundle());
    _resolver.addResult(cmr);
    
    // The second time DeploymentGenerator calls the Resolver, it will provide just 
    // test.shared. The resolver will return test.shared _plus_ test.isolated. 
    _resolver.addResult(cmr);
    Skeleton.getSkeleton(appMetadata).setReturnValue(new MethodCall(ApplicationMetadata.class, "getApplicationContents"), Arrays.asList(mockContent("test.isolated" , "1.0.0")));
    
    
    try { 
      
      sut.generateDeploymentManifest(appMetadata.getApplicationSymbolicName(), 
          appMetadata.getApplicationVersion().toString(), appMetadata.getApplicationContents(), 
          new ArrayList<ModelledResource>(), new ArrayList<Content>(), 
          Collections.<Content>emptyList(), 
          appMetadata.getApplicationImportServices());
    } catch (ResolverException rx) { 
      List<String> usr = rx.getUnsatisfiedRequirements();
      assertEquals ("One unsatisfied requirement expected, not " + usr.size(), usr.size(), 1);
      String chkMsg = "Shared bundle test.shared_1.0.0 has a dependency for package " +
      		"test.shared which is exported from isolated bundle [test.isolated_1.0.0]";
      assertTrue (chkMsg + " expected, not " + usr, usr.contains(chkMsg));
      return;
    }
    fail ("ResolverException expected");
  }
  
  /**
   * This method checks that the a more complicated circular dependency issues the correct error message
   * and checks that the details listed in the exception are correct. 
   * @throws Exception
   */
  @Test
  public void checkMultipleCircularDependenciesDetected() throws Exception { 
    
    Collection<ModelledResource> cmr = new ArrayList<ModelledResource>();
    ExportedPackage testIsolated1 = createExportedPackage ("test.isolated1", "1.0.0", 
        new String[] {"test.isolated1","test.isolated2"}, new String[] {"test.shared1", "test.shared2"});
    cmr.add (testIsolated1.getBundle());
    
    ExportedPackage testIsolated2 = createExportedPackage ("test.isolated2", "1.0.0", 
        new String[] {"test.isolated1","test.isolated2"}, new String[] {"test.shared1", "test.shared2"});
    cmr.add (testIsolated2.getBundle());
    
    ExportedPackage testShared1 = createExportedPackage ("test.shared1", "1.0.0", 
        new String[] {"test.shared1", "test.shared2"}, new String[] {"test.isolated1","test.isolated2"});
    cmr.add (testShared1.getBundle());
    
    ExportedPackage testShared2 = createExportedPackage ("test.shared2", "1.0.0", 
        new String[] {"test.shared1", "test.shared2"}, new String[] {"test.isolated1","test.isolated2"});
    cmr.add (testShared2.getBundle());
    
    _resolver.addResult(cmr);
    
    // The second time DeploymentGenerator calls the Resolver, it will provide just 
    // test.shared. The resolver will return test.shared _plus_ test.isolated. 
    _resolver.addResult(cmr);
    Skeleton.getSkeleton(appMetadata).setReturnValue(new MethodCall(ApplicationMetadata.class, "getApplicationContents"), Arrays.asList(mockContent("test.isolated1" , "1.0.0"), mockContent("test.isolated2" , "1.0.0")));
    
    app = Skeleton.newMock(AriesApplication.class);
    Skeleton.getSkeleton(app).setReturnValue(new MethodCall(AriesApplication.class, "getApplicationMetadata"), appMetadata);
    
    try {
           sut.generateDeploymentManifest(appMetadata.getApplicationSymbolicName(), 
               appMetadata.getApplicationVersion().toString(), 
               appMetadata.getApplicationContents(), 
               Arrays.asList(new ModelledResource[] {testIsolated1.getBundle(), testIsolated2.getBundle()}), new ArrayList<Content>(), Collections.<Content>emptyList(), 
               appMetadata.getApplicationImportServices());

    } catch (ResolverException rx) { 
      // Get the unsatisfied Requirements
      List<String> unsatisfiedReqs = rx.getUnsatisfiedRequirements();
      // Ensure we've got 4 unsatisfied Requirements
      assertEquals ("4 unsatisfied requirements expected, not " + unsatisfiedReqs.size(), unsatisfiedReqs.size(), 4);
      List<String> checkMessages = new ArrayList<String>();
      // Now load an array with the expected messages.
      checkMessages.add("Shared bundle test.shared1_1.0.0 has a dependency for package test.isolated1 which " +
      "is exported from isolated bundles [test.isolated1_1.0.0, test.isolated2_1.0.0]");
      checkMessages.add("Shared bundle test.shared1_1.0.0 has a dependency for package test.isolated2 which " +
      "is exported from isolated bundles [test.isolated1_1.0.0, test.isolated2_1.0.0]");
      checkMessages.add("Shared bundle test.shared2_1.0.0 has a dependency for package test.isolated1 which " +
      "is exported from isolated bundles [test.isolated1_1.0.0, test.isolated2_1.0.0]");
      checkMessages.add("Shared bundle test.shared2_1.0.0 has a dependency for package test.isolated2 which " +
      "is exported from isolated bundles [test.isolated1_1.0.0, test.isolated2_1.0.0]");
      
      // Loop through the unsatisfied Requirements and compare them to the expected msgs. We trim the strings
      // because some unsatisfied reqs have spaces at the end of the string.
      for (String unsatisfiedReq : unsatisfiedReqs) {
        assertTrue(unsatisfiedReq + " is not an expected msg", checkMessages.contains(unsatisfiedReq.trim()));
      }
    }
  }
  
  public static ModelledResource createModelledResource(String bundleName, String bundleVersion, 
      Collection<String> importedPackages, Collection<String> exportedPackages) throws InvalidAttributeException {
    Attributes att = new Attributes();
    att.put(new Attributes.Name(Constants.BUNDLE_SYMBOLICNAME), bundleName);
    att.put(new Attributes.Name(Constants.BUNDLE_VERSION), bundleVersion);
    att.put(new Attributes.Name(Constants.BUNDLE_MANIFESTVERSION), "2");
    
    StringBuilder builder = new StringBuilder();
    for(String iPackage : importedPackages) {
      builder.append(iPackage).append(",");
    }
    if(builder.length() > 0) {
      builder.deleteCharAt(builder.length() - 1);
      att.put(new Attributes.Name(Constants.IMPORT_PACKAGE), builder.toString());
    }
    
    builder = new StringBuilder();
    for(String ePackage : exportedPackages) {
      builder.append(ePackage).append(",");
    }
    if(builder.length() > 0) {
      builder.deleteCharAt(builder.length() - 1);
      att.put(new Attributes.Name(Constants.EXPORT_PACKAGE), builder.toString());
    }
    return new ModellingManagerImpl().getModelledResource(null, att, null, null);
  }
  
  private Content mockContent(String symbolicName, String version) {
    Content bundle = Skeleton.newMock(Content.class);
    VersionRange vr = Skeleton.newMock(VersionRange.class);
    Skeleton.getSkeleton(vr).setReturnValue(new MethodCall(VersionRange.class, "toString"), version);
    Skeleton.getSkeleton(bundle).setReturnValue(new MethodCall(Content.class, "getContentName"), symbolicName);
    Skeleton.getSkeleton(bundle).setReturnValue(new MethodCall(Content.class, "getVersion"), vr);
    
       return bundle;
  }
}
