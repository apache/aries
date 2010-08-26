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


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.aries.application.VersionRange;
import org.apache.aries.application.management.InvalidAttributeException;
import org.apache.aries.application.modelling.ImportedPackage;
import org.apache.aries.application.modelling.impl.ImportedPackageImpl;
import org.apache.aries.application.modelling.internal.PackageRequirementMerger;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Constants;


public final class PackageRequirementMergerTest
{
  private static boolean isEqual(Collection<ImportedPackage> reqs1, Collection<ImportedPackage> reqs2)
  {
    boolean result = true;
    
    if (reqs1.size() != reqs2.size())
    {
      result = false;
    }
    else
    {
      for (ImportedPackage r1 : reqs1)
      {
        boolean foundMatch = false;
        for (ImportedPackage r2 : reqs2)
        {
          if (!r1.getPackageName().equals(r2.getPackageName()))
          {
            continue;
          }
          
          if (r1.isOptional() != r2.isOptional())
          {
            continue;
          }
          
          Map<String, String> attribs1 = new HashMap<String, String>(r1.getAttributes());
          Map<String, String> attribs2 = new HashMap<String, String>(r2.getAttributes());
          
          VersionRange v1 = ManifestHeaderProcessor.parseVersionRange(attribs1.remove(Constants.VERSION_ATTRIBUTE));
          VersionRange v2 = ManifestHeaderProcessor.parseVersionRange(attribs2.remove(Constants.VERSION_ATTRIBUTE));
          if (!v1.equals(v2))
          {
            continue;
          }
          
          if (!attribs1.equals(attribs2))
          {
            continue;
          }

          foundMatch = true;
          break;
        }
        
        if (!foundMatch)
        {
          result = false;
          break;
        }
      }
    }
    
    return result;
  }
  
  static ImportedPackage newImportedPackage (String name, String version) throws InvalidAttributeException {
    Map<String, String> attrs = new HashMap<String, String>();
    attrs.put(Constants.VERSION_ATTRIBUTE, version);
    return new ImportedPackageImpl (name, attrs);
  }
  
  static ImportedPackage newImportedPackage (String name, String version, boolean optional) throws InvalidAttributeException {
    Map<String, String> attrs = new HashMap<String, String>();
    attrs.put(Constants.VERSION_ATTRIBUTE, version);
    attrs.put(Constants.RESOLUTION_DIRECTIVE + ":", (optional)?Constants.RESOLUTION_OPTIONAL:Constants.RESOLUTION_MANDATORY);
    return new ImportedPackageImpl (name, attrs);
  }
  
  static ImportedPackage newImportedPackage (String name, String version, String attribute) throws InvalidAttributeException {
    Map<String, String> attrs = new HashMap<String, String>();
    attrs.put(Constants.VERSION_ATTRIBUTE, version);
    attrs.put(attribute.split("=")[0], attribute.split("=")[1]); 
    return new ImportedPackageImpl (name, attrs);
  }
  
  @Test
  public void testMergeValid() throws Exception
  {
    Collection<ImportedPackage> reqs = new ArrayList<ImportedPackage>();
    reqs.add(newImportedPackage("a", "1.0.0"));
    reqs.add(newImportedPackage("a", "2.0.0"));
    reqs.add(newImportedPackage("a", "3.0.0"));
    reqs.add(newImportedPackage("b", "1.0.0"));
    reqs.add(newImportedPackage("b", "2.0.0"));
    reqs.add(newImportedPackage("c", "1.0.0"));
    PackageRequirementMerger merger = new PackageRequirementMerger(reqs);
    
    Assert.assertTrue(merger.isMergeSuccessful());
    
    Assert.assertTrue(merger.getInvalidRequirements().isEmpty());
    
    Collection<ImportedPackage> result = merger.getMergedRequirements();
    Collection<ImportedPackage> expected = new ArrayList<ImportedPackage>();
    expected.add(newImportedPackage("a", "3.0.0"));
    expected.add(newImportedPackage("b", "2.0.0"));
    expected.add(newImportedPackage("c", "1.0.0"));
    Assert.assertTrue(result.toString(), isEqual(result, expected));
  }
  
  @Test
  public void testMergeInvalid() throws Exception
  {
    Collection<ImportedPackage> reqs = new ArrayList<ImportedPackage>();
    reqs.add(newImportedPackage("a", "[1.0.0,2.0.0]"));
    reqs.add(newImportedPackage("a", "[3.0.0,3.0.0]"));
    reqs.add(newImportedPackage("b", "1.0.0"));
    reqs.add(newImportedPackage("b", "2.0.0"));
    reqs.add(newImportedPackage("c", "[1.0.0,2.0.0)"));
    reqs.add(newImportedPackage("c", "2.0.0"));
    PackageRequirementMerger merger = new PackageRequirementMerger(reqs);
    
    Assert.assertFalse(merger.isMergeSuccessful());
    
    try
    {
      merger.getMergedRequirements();
      Assert.fail("getMergedRequirements should throw IllegalStateException.");
    }
    catch (IllegalStateException e) { }
    
    Set<String> result = merger.getInvalidRequirements();
    Set<String> expected = new HashSet<String>();
    expected.add("a");
    expected.add("c");
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testMergeOptionalResolution() throws Exception
  {
    Collection<ImportedPackage> reqs = new ArrayList<ImportedPackage>();
    reqs.add(newImportedPackage("a", "1.0.0", true));
    reqs.add(newImportedPackage("a", "2.0.0", true));
    PackageRequirementMerger merger = new PackageRequirementMerger(reqs);
    
    Assert.assertTrue(merger.isMergeSuccessful());
    
    Assert.assertTrue(merger.getInvalidRequirements().isEmpty());
    
    Collection<ImportedPackage> result = merger.getMergedRequirements();
    Collection<ImportedPackage> expected = new ArrayList<ImportedPackage>();
    expected.add(newImportedPackage("a", "2.0.0", true));
    Assert.assertTrue(result.toString(), isEqual(result, expected));
  }
  
  @Test
  public void testMergeMandatoryResolution() throws Exception 
  {
    Collection<ImportedPackage> reqs = new ArrayList<ImportedPackage>();
    reqs.add(newImportedPackage("a", "1.0.0", true));
    reqs.add(newImportedPackage("a", "2.0.0", false));
    PackageRequirementMerger merger = new PackageRequirementMerger(reqs);
    
    Assert.assertTrue(merger.isMergeSuccessful());
    
    Assert.assertTrue(merger.getInvalidRequirements().isEmpty());
    
    Collection<ImportedPackage> result = merger.getMergedRequirements();
    Collection<ImportedPackage> expected = new ArrayList<ImportedPackage>();
    expected.add(newImportedPackage("a", "2.0.0"));
    Assert.assertTrue(result.toString(), isEqual(result, expected));
  }
  
  @Test
  public void testMergeValidAdditionalAttributes()  throws Exception 
  {
    Collection<ImportedPackage> reqs = new ArrayList<ImportedPackage>();
    reqs.add(newImportedPackage("a", "1.0.0", "foo=bar"));
    reqs.add(newImportedPackage("a", "2.0.0", "foo=bar"));
    PackageRequirementMerger merger = new PackageRequirementMerger(reqs);
    
    Assert.assertTrue(merger.isMergeSuccessful());
    
    Assert.assertTrue(merger.getInvalidRequirements().isEmpty());
    
    Collection<ImportedPackage> result = merger.getMergedRequirements();
    Collection<ImportedPackage> expected = new ArrayList<ImportedPackage>();
    expected.add(newImportedPackage("a", "2.0.0", "foo=bar"));
    Assert.assertTrue(result.toString(), isEqual(result, expected));
  }
  
  @Test
  public void testMergeInvalidAdditionalAttributes() throws Exception
  {
    Collection<ImportedPackage> reqs = new ArrayList<ImportedPackage>();
    reqs.add(newImportedPackage("a", "1.0.0", "foo=bar"));
    reqs.add(newImportedPackage("a", "2.0.0", "foo=blah"));
    reqs.add(newImportedPackage("b", "1.0.0"));
    PackageRequirementMerger merger = new PackageRequirementMerger(reqs);
    
    Assert.assertFalse(merger.isMergeSuccessful());
    
    try
    {
      merger.getMergedRequirements();
      Assert.fail("getMergedRequirements should throw IllegalStateException.");
    }
    catch (IllegalStateException e) { }
    
    Set<String> result = merger.getInvalidRequirements();
    Set<String> expected = new HashSet<String>();
    expected.add("a");
    Assert.assertEquals(expected, result);
  }
  
}
