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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.modelling.DeployedBundles;
import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedBundle;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.impl.ExportedServiceImpl;
import org.apache.aries.application.modelling.impl.ImportedBundleImpl;
import org.apache.aries.application.modelling.impl.ImportedServiceImpl;
import org.apache.aries.application.modelling.impl.ModelledResourceImpl;
import org.apache.aries.application.modelling.utils.impl.ModellingHelperImpl;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor.NameValueMap;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor.NameValuePair;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Constants;


public final class DeployedBundlesTest
{
  
  private DeployedBundles validDeployedBundles() throws Exception {

    Collection<ImportedBundle> content = new ArrayList<ImportedBundle>();
    Collection<ImportedBundle> uses = new ArrayList<ImportedBundle>();

    content.add(new ImportedBundleImpl("bundle.a", "1.0.0"));
    content.add(new ImportedBundleImpl("bundle.b", "1.0.0"));
    
    uses.add(new ImportedBundleImpl("bundle.c", "1.0.0"));
    uses.add(new ImportedBundleImpl("bundle.d", "1.0.0"));
    
    return new ModellingHelperImpl().createDeployedBundles("test",content, uses, null);
  }
  
  private void basicResolve(DeployedBundles db, boolean cPersistent) throws InvalidAttributeException {
    db.addBundle(createModelledResource("bundle.a", "1.0.0", 
        Arrays.asList("package.b", "package.c"), Arrays.asList("package.a;version=1.0.0")));
    db.addBundle(createModelledResource("bundle.b", "1.0.0", 
        Arrays.asList("package.d;version=1.0.0", "package.e;version=\"[1.0.0,2.0.0)\"", "package.g"),
        Arrays.asList("package.b;version=1.0.0")));
    
    db.addBundle(createModelledResource("bundle.c", "1.0.0", 
        (cPersistent) ? Arrays.asList("package.d;version=\"[1.0.0,2.0.0)\"", "javax.persistence;version=1.1.0") : 
          Arrays.asList("package.d;version=\"[1.0.0,2.0.0)\""), Arrays.asList("package.c;version=1.0.0")));
    db.addBundle(createModelledResource("bundle.d", "1.0.0", 
        Arrays.asList("package.e;version=\"[1.0.0,1.0.0]\""), Arrays.asList("package.d;version=1.0.0")));

  }
  
  private void packagesResolve(DeployedBundles db) throws InvalidAttributeException {
    basicResolve(db, false);
    
    db.addBundle(createModelledResource("bundle.e", "1.0.0",
        new ArrayList<String>(), Arrays.asList("package.e;version=1.0.0")));
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
    return new ModelledResourceImpl(null, att, null, null);
  }
  
  public static ModelledResource createModelledServiceBundle(String bundleName, String bundleVersion,
      Collection<String> importService, Collection<String> exportService) throws InvalidAttributeException 
  {
    Attributes att = new Attributes();
    att.put(new Attributes.Name(Constants.BUNDLE_SYMBOLICNAME), bundleName);
    att.put(new Attributes.Name(Constants.BUNDLE_VERSION), bundleVersion);
    att.put(new Attributes.Name(Constants.BUNDLE_MANIFESTVERSION), "2");
    
    List<ImportedService> importedServices = new ArrayList<ImportedService>();
    for (String s : importService) {
      importedServices.add(new ImportedServiceImpl(false, s, null, null, null, false));
    }
    
    List<ExportedService> exportedServices = new ArrayList<ExportedService>();
    for (String s : exportService) {
      exportedServices.add(new ExportedServiceImpl(null, 0, Collections.singleton(s), Collections.<String,Object>emptyMap()));
    }
    
    return new ModelledResourceImpl(null, att, importedServices, exportedServices);
  }
  
  /**
   * Check the actual results match the expected values, regardless of order of the parts.
   * @param entry the actual manifest entry.
   * @param expected the expected manifest entry.
   * @return true if they match; false otherwise.
   */
  private static boolean isEqual(String actual, String expected)
  {
    Map<NameValuePair<String, NameValueMap<String, String>>, Integer> actualEntries = parseEntries(actual);
    Map<NameValuePair<String, NameValueMap<String, String>>, Integer> expectedEntries = parseEntries(expected);
    return actualEntries.equals(expectedEntries);
  }

  /**
   * Parse manifest entries into a set of values and associated attributes, which can
   * be directly compared for equality regardless of ordering.
   * <p>
   * Example manifest entry format: value1;attrName1=attrValue1;attrName2=attrValue2,value2;attrName1=attrValue1
   * @param entries a manifest header entry.
   * @return a set of parsed entries.
   */
  private static Map<NameValuePair<String, NameValueMap<String, String>>, Integer> parseEntries(String entries)
  {
    Map<NameValuePair<String, NameValueMap<String, String>>, Integer> result = new HashMap<NameValuePair<String, NameValueMap<String, String>>, Integer>();
    for (NameValuePair<String, NameValueMap<String, String>> entry : ManifestHeaderProcessor.parseExportString(entries))
    {
      Integer count = result.get(entry);
      if (count != null)
      {
        // This entry already exists to increment the count.
        count++;
      }
      else
      {
        count = 1;
      }
      result.put(entry, count);
    }
    
    return result;
  }

  @Test
  public void testGetContent_Valid() throws Exception
  {
    // Get a valid set of deployment information.
    DeployedBundles deployedBundles = validDeployedBundles();
    packagesResolve(deployedBundles);
    
    // Check the deployed content entry is correct.
    String contentEntry = deployedBundles.getContent();
    String expectedResult = "bundle.a;deployed-version=1.0.0,bundle.b;deployed-version=1.0.0";
    Assert.assertTrue("Content=" + contentEntry, isEqual(contentEntry, expectedResult));
  }

  @Test
  public void testGetUseBundle_Valid() throws Exception
  {
    // Get a valid set of deployment information.
    DeployedBundles deployedBundles = validDeployedBundles();
    packagesResolve(deployedBundles);
    
    // Check the deployed use bundle entry is correct.
    String useBundleEntry = deployedBundles.getUseBundle();
    String expectedResult = "bundle.c;deployed-version=1.0.0,bundle.d;deployed-version=1.0.0";
    Assert.assertTrue("UseBundle=" + useBundleEntry, isEqual(useBundleEntry, expectedResult));
  }

  @Test
  public void testGetProvisionBundle_Valid() throws Exception
  {
    // Check the provision bundle entry is correct.
    DeployedBundles deployedBundles = validDeployedBundles();
    packagesResolve(deployedBundles);
    String provisionBundleEntry = deployedBundles.getProvisionBundle();
    String expectedResult = "bundle.e;deployed-version=1.0.0";
    Assert.assertTrue("ProvisionBundle=" + provisionBundleEntry, isEqual(provisionBundleEntry, expectedResult));
  }

  @Test
  public void testGetImportPackage_Valid() throws Exception
  {
    // Check the import package entry is correct.
    String importPackageEntry = null;
    try
    {
      DeployedBundles deployedBundles = validDeployedBundles();
      packagesResolve(deployedBundles);
      
      importPackageEntry = deployedBundles.getImportPackage();
    }
    catch (ResolverException e)
    {
      e.printStackTrace();
      Assert.fail(e.toString());
    }
    
    String expectedResult = "package.c;version=\"1.0.0\";bundle-symbolic-name=\"bundle.c\";bundle-version=\"[1.0.0,1.0.0]\","
      + "package.d;version=\"1.0.0\";bundle-symbolic-name=\"bundle.d\";bundle-version=\"[1.0.0,1.0.0]\"," 
      + "package.e;version=\"[1.0.0,2.0.0)\","
      + "package.g;version=\"0.0.0\"";
    
    
    /*
     * String expectedResult = "package.c;bundle-symbolic-name=bundle.c;bundle-version=\"[1.0.0,1.0.0]\""
     
        + ",package.d;version=\"1.0.0\";bundle-symbolic-name=bundle.d;bundle-version=\"[1.0.0,1.0.0]\""
        + ",package.e;version=\"[1.0.0,2.0.0)\""
        + ",package.g";
     */
    Assert.assertTrue("ImportPackage=" + importPackageEntry, isEqual(importPackageEntry, expectedResult));
  }
  
  
  

  private enum ternary { CONTENT,USES,NONE }
  
  private DeployedBundles getSimpleDeployedBundles(ternary a, ternary b, ternary c) throws InvalidAttributeException
  {
    Collection<ImportedBundle> content = new ArrayList<ImportedBundle>();
    Collection<ImportedBundle> uses = new ArrayList<ImportedBundle>();

    if(a == ternary.CONTENT)
      content.add(new ImportedBundleImpl("bundle.a", "1.0.0"));
    else if (a == ternary.USES)
      uses.add(new ImportedBundleImpl("bundle.a", "1.0.0"));
    if (b == ternary.CONTENT)
      content.add(new ImportedBundleImpl("bundle.b", "1.0.0"));
    else if (b == ternary.USES)
      uses.add(new ImportedBundleImpl("bundle.b", "1.0.0"));
    if (c == ternary.CONTENT)
      content.add(new ImportedBundleImpl("bundle.c", "1.0.0"));
    else if (c == ternary.USES)
      uses.add(new ImportedBundleImpl("bundle.c", "1.0.0"));
    
    // In a unit test we could go straight to the static method; choosing not to in this case. 
    return new ModellingHelperImpl().createDeployedBundles("test",content, uses, null);
  }
  
  @Test
  public void testGetImportPackage_ValidDuplicates() throws Exception
  {
    DeployedBundles deployedBundles = getSimpleDeployedBundles(ternary.CONTENT, ternary.CONTENT, ternary.CONTENT);
    
    deployedBundles.addBundle(createModelledResource("bundle.a", "1.0.0",
        Arrays.asList("package.d;version=\"[1.0.0,3.0.0)\""), new ArrayList<String>()));
    deployedBundles.addBundle(createModelledResource("bundle.b", "1.0.0",
        Arrays.asList("package.d;version=\"2.0.0\""), new ArrayList<String>()));
    deployedBundles.addBundle(createModelledResource("bundle.c", "1.0.0",
        Arrays.asList("package.d;version=\"1.0.0\""), new ArrayList<String>()));
    deployedBundles.addBundle(createModelledResource("bundle.d", "1.0.0",
        new ArrayList<String>(), Arrays.asList("package.d;version=2.0.1")));
    
    // Check that package D is not duplicated in Import-Package, and that the version range
    // has been narrowed to the intersection of the original requirements.
    String importPackageEntry = null;
    try
    {
      importPackageEntry = deployedBundles.getImportPackage();
    }
    catch (ResolverException e)
    {
      e.printStackTrace();
      Assert.fail(e.toString());
    }
    String expectedResult = "package.d;version=\"[2.0.0,3.0.0)\"";
    Assert.assertTrue("ImportPackage=" + importPackageEntry, isEqual(importPackageEntry, expectedResult));
  }

  @Test
  public void testGetImportPackage_ValidDuplicatesWithAttributes() throws Exception
  {
    DeployedBundles deployedBundles = getSimpleDeployedBundles(ternary.CONTENT, ternary.CONTENT, ternary.NONE);
    
    deployedBundles.addBundle(createModelledResource("bundle.a", "1.0.0",
        Arrays.asList("package.c;version=1.0.0;was_internal=true"), new ArrayList<String>()));
    deployedBundles.addBundle(createModelledResource("bundle.b", "1.0.0",
        Arrays.asList("package.c;version=2.0.0;was_internal=true"), new ArrayList<String>()));
    deployedBundles.addBundle(createModelledResource("bundle.c", "1.0.0",
        new ArrayList<String>(), Arrays.asList("package.c;version=2.0.0;was_internal=true")));

    // Check that package C is not duplicated in Import-Package, and that the version range
    // has been narrowed to the intersection of the original requirements.
    String importPackageEntry = null;
    try
    {
      importPackageEntry = deployedBundles.getImportPackage();
    }
    catch (ResolverException e)
    {
      e.printStackTrace();
      Assert.fail(e.toString());
    }
    String expectedResult = "package.c;was_internal=\"true\";version=\"2.0.0\"";
    Assert.assertTrue("ImportPackage=" + importPackageEntry, isEqual(importPackageEntry, expectedResult));
  }

  @Test
  public void testGetImportPackage_InvalidDuplicates() throws Exception
  {
    DeployedBundles deployedBundles = getSimpleDeployedBundles(ternary.CONTENT, ternary.CONTENT, ternary.NONE);
    
    deployedBundles.addBundle(createModelledResource("bundle.a", "1.0.0",
        Arrays.asList("package.c;version=\"[1.0.0,2.0.0)\""), new ArrayList<String>()));
    deployedBundles.addBundle(createModelledResource("bundle.b", "1.0.0",
        Arrays.asList("package.c;version=2.0.0"), new ArrayList<String>()));
    deployedBundles.addBundle(createModelledResource("bundle.c", "1.0.0",
        new ArrayList<String>(), Arrays.asList("package.c;version=2.0.0;was_internal=true")));
    
    // Check that the incompatible version requirements cannot be resolved.
    String importPackageEntry = null;
    try
    {
      importPackageEntry = deployedBundles.getImportPackage();
      Assert.fail("Expected exception. ImportPackage=" + importPackageEntry);
    }
    catch (ResolverException e)
    {
      // We expect to reach this point if the test passes.
    }
  }

  @Test
  public void testGetImportPackage_InvalidDuplicatesWithAttributes() throws Exception
  {
    DeployedBundles deployedBundles = getSimpleDeployedBundles(ternary.CONTENT, ternary.CONTENT, ternary.NONE);
    
    deployedBundles.addBundle(createModelledResource("bundle.a", "1.0.0",
        Arrays.asList("package.c;version=1.0.0;was_internal=true"), new ArrayList<String>()));
    deployedBundles.addBundle(createModelledResource("bundle.b", "1.0.0",
        Arrays.asList("package.c;version=2.0.0"), new ArrayList<String>()));
    deployedBundles.addBundle(createModelledResource("bundle.c", "1.0.0",
        new ArrayList<String>(), Arrays.asList("package.c;version=2.0.0;was_internal=true")));

    // Check that the incompatible package requirement attributes cause an exception.
    String importPackageEntry = null;
    try
    {
      importPackageEntry = deployedBundles.getImportPackage();
      Assert.fail("Expected exception. ImportPackage=" + importPackageEntry);
    }
    catch (ResolverException e)
    {
      // We expect to reach this point if the test passes.
    }
  }

  
  @Test
  public void testGetImportPackage_bundleSymbolicNameOK() throws Exception
  {
    DeployedBundles deployedBundles = getSimpleDeployedBundles(ternary.CONTENT, ternary.CONTENT, ternary.NONE);
    
    deployedBundles.addBundle(createModelledResource("bundle.a", "1.0.0",
        Arrays.asList("package.b;version=1.0.0;bundle-symbolic-name=bundle.b;bundle-version=\"[1.0.0,2.0.0)\""), new ArrayList<String>()));
    deployedBundles.addBundle(createModelledResource("bundle.b", "1.0.0",
        new ArrayList<String>(), Arrays.asList("package.b;version=2.0.0")));
    
    // Check that the bundle-symbolic-name attribute for a bundle within deployed-content is ok. 
    String importPackageEntry = null; 
    try
    {
      importPackageEntry = deployedBundles.getImportPackage();      
    }
    catch (ResolverException e)
    {
      e.printStackTrace();
      Assert.fail(e.toString());
    }
    String expectedResult = "";  // All packages are satisfied internally 
    Assert.assertTrue("ImportPackage=" + importPackageEntry, isEqual(importPackageEntry, expectedResult));

  }
  
  @Test
  public void testGetImportPackage_rfc138PreventsBundleSymbolicNameWorking() throws Exception
  {
    DeployedBundles deployedBundles = getSimpleDeployedBundles(ternary.CONTENT, ternary.USES, ternary.NONE);
    
    deployedBundles.addBundle(createModelledResource("bundle.a", "1.0.0",
        Arrays.asList("package.b;version=1.0.0;bundle-symbolic-name=bundle.b"), new ArrayList<String>()));
    deployedBundles.addBundle(createModelledResource("bundle.b", "1.0.0",
        new ArrayList<String>(), Arrays.asList("package.b;version=2.0.0")));

    
    // Check that the bundle-symbolic-name attribute for a bundle outside use-bundle causes an exception.
    String importPackageEntry = null;
    try
    {
      importPackageEntry = deployedBundles.getImportPackage();
      Assert.fail("Expected exception. ImportPackage=" + importPackageEntry);
    }
    catch (ResolverException e)
    {
      // We expect to reach this point if the test passes.
    }
  }
  
  @Test
  public void testGetImportPackage_rfc138PreventsBundleVersionWorking() throws Exception
  {
    DeployedBundles deployedBundles = getSimpleDeployedBundles(ternary.CONTENT, ternary.NONE, ternary.NONE);
    
    deployedBundles.addBundle(createModelledResource("bundle.a", "1.0.0",
        Arrays.asList("package.b;version=1.0.0;bundle-version=1.0.0"), new ArrayList<String>()));
    deployedBundles.addBundle(createModelledResource("bundle.b", "1.0.0",
        new ArrayList<String>(), Arrays.asList("package.b;version=2.0.0")));

    
    // Check that the bundle-symbolic-name attribute for a bundle outside use-bundle causes an exception.
    String importPackageEntry = null;
    try
    {
      importPackageEntry = deployedBundles.getImportPackage();
      Assert.fail("Expected exception. ImportPackage=" + importPackageEntry);
    }
    catch (ResolverException e)
    {
      // We expect to reach this point if the test passes.
    }
  }
  
  @Test
  public void testGetImportPackage_ValidResolutionAttribute() throws Exception
  {
    DeployedBundles deployedBundles = getSimpleDeployedBundles(ternary.CONTENT, ternary.CONTENT, ternary.NONE);
    
    deployedBundles.addBundle(createModelledResource("bundle.a", "1.0.0",
        Arrays.asList("package.c;version=1.0.0;resolution:=optional"), new ArrayList<String>()));
    deployedBundles.addBundle(createModelledResource("bundle.b", "1.0.0",
         Arrays.asList("package.c;version=1.0.0"), new ArrayList<String>()));
    deployedBundles.addBundle(createModelledResource("bundle.c", "1.0.0",
        new ArrayList<String>(), Arrays.asList("package.c;version=1.0.0")));
    
    // Check that the resulting resolution directive is not optional.
    String importPackageEntry = null;
    try
    {
      importPackageEntry = deployedBundles.getImportPackage();
    }
    catch (ResolverException e)
    {
      e.printStackTrace();
      Assert.fail(e.toString());
    }
    String expectedResult = "package.c;version=1.0.0";
    Assert.assertTrue("ImportPackage=" + importPackageEntry, isEqual(importPackageEntry, expectedResult));
  }

  @Test
  public void testGetRequiredUseBundle_RedundantEntry() throws Exception
  {
    // Bundle A requires package B from bundle B with no version requirement.
    // Bundle B requires package C from bundle C with no version requirement.
    // Bundle C requires package B from bundle B with explicit version requirement.
    
    DeployedBundles deployedBundles = getSimpleDeployedBundles(ternary.CONTENT, ternary.USES, ternary.USES);
    
    deployedBundles.addBundle(createModelledResource("bundle.a", "1.0.0",
        Arrays.asList("package.b"), new ArrayList<String>()));
    deployedBundles.addBundle(createModelledResource("bundle.b", "1.0.0",
         Arrays.asList("package.c"), Arrays.asList("package.b;version=1.0.0")));
    deployedBundles.addBundle(createModelledResource("bundle.c", "1.0.0",
        Arrays.asList("package.b;version=1.0.0"), Arrays.asList("package.c;version=1.0.0")));
      
    // Check the redundant use-bundle entry is identified.
    // Bundle C is not required by app content, although it is specified in use-bundle.
    Collection<ModelledResource> requiredUseBundle = null;
    try
    {
      requiredUseBundle = deployedBundles.getRequiredUseBundle();
    }
    catch (ResolverException e)
    {
      e.printStackTrace();
      Assert.fail(e.toString());
    }
    Assert.assertTrue("RequiredUseBundle=" + requiredUseBundle, requiredUseBundle.size() == 1);
  }

  @Test
  public void testGetRequiredUseBundle_Valid() throws Exception
  {
    // Get a valid set of deployment information.
    DeployedBundles deployedBundles = validDeployedBundles();
    packagesResolve(deployedBundles);
    
    // Check all the use-bundle entries are required.
    Collection<ModelledResource> requiredUseBundle = null;
    try
    {
      requiredUseBundle = deployedBundles.getRequiredUseBundle();
    }
    catch (ResolverException e)
    {
      e.printStackTrace();
      Assert.fail(e.toString());
    }
    Assert.assertTrue("RequiredUseBundle=" + requiredUseBundle, requiredUseBundle.size() == 2);
  }
  
  //Inside cannot bundle-symbolic-name an outside bundle until the new RFC 138!
  @Test
  public void testGetImportPackage_InvalidBundleVersion() throws Exception
  {
    DeployedBundles deployedBundles = getSimpleDeployedBundles(ternary.CONTENT, ternary.USES, ternary.NONE);
    
    deployedBundles.addBundle(createModelledResource("bundle.a", "1.0.0",
        Arrays.asList("package.b;version=\"[1.0.0,1.0.0]\";bundle-symbolic-name=bundle.b;bundle-version=\"[0.0.0,1.0.0)\"")
        , new ArrayList<String>()));
    deployedBundles.addBundle(createModelledResource("bundle.b", "1.0.0",
         new ArrayList<String>(), Arrays.asList("package.b;version=1.0.0")));

    // Check that the bundle version requirement generates an error because it doesn't match the a bundle in use-bundle.
    String importPackageEntry = null;
    try
    {
      importPackageEntry = deployedBundles.getImportPackage();
      Assert.fail("Expected exception. ImportPackage=" + importPackageEntry);
    }
    catch (ResolverException e)
    {
      // We expect to reach this point if the test passes.
    }
  }
  
  
  @Test
  public void testImportedService() throws Exception
  {
    DeployedBundles deployedBundles = getSimpleDeployedBundles(ternary.CONTENT, ternary.NONE, ternary.NONE);

    deployedBundles.addBundle(createModelledServiceBundle("bundle.a", "1.0.0", 
        Collections.singleton("java.util.List"), Collections.<String>emptyList()));

    deployedBundles.addBundle(createModelledServiceBundle("bundle.b", "1.0.0", 
        Collections.singleton("java.util.Set"), Collections.singleton("java.util.List")));

    deployedBundles.addBundle(createModelledServiceBundle("bundle.c", "1.0.0", 
        Collections.<String>emptyList(), Collections.singleton("java.util.Set")));
    
    assertEquals("(objectClass=java.util.List)", deployedBundles.getDeployedImportService());
  }

}
