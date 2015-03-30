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
package org.apache.aries.versioning.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.aries.util.manifest.BundleManifest;
import org.apache.aries.versioning.check.BundleCompatibility;
import org.apache.aries.versioning.check.BundleInfo;
import org.junit.Test;


/**
 * Test that results can be excluded.
 */
public class FilterResultsTest {

  /**
   * Test an error is excluded when required. This test uses two bundles each containing the same
   * class, where the later versioned class has had a method removed.
   */
    @Test
    public void testApiMethodErrorExcluded() {
   
      try {
        File oldBundleFile = new File("../src/test/resources/api_1.0.0.jar");
        BundleInfo oldBundle = new BundleInfo(BundleManifest.fromBundle(oldBundleFile), oldBundleFile);
  
        File newBundleFile = new File("../src/test/resources/api_1.0.1.jar");
        BundleInfo newBundle = new BundleInfo(BundleManifest.fromBundle(newBundleFile), newBundleFile);
        
        String bundleSymbolicName = newBundle.getBundleManifest().getSymbolicName();
        URLClassLoader oldClassLoader = new URLClassLoader(new URL[] {oldBundle.getBundle().toURI()
            .toURL()});
        URLClassLoader newClassLoader = new URLClassLoader(new URL[] {newBundle.getBundle().toURI()
            .toURL()});
  
        List<String> excludes = new ArrayList<String>();
        excludes.add("method void methodToBeExcludedFrom() has been deleted");
        
        BundleCompatibility bundleCompatibility = new BundleCompatibility(bundleSymbolicName,
            newBundle, oldBundle,
            oldClassLoader,
            newClassLoader,
            excludes);
        
        bundleCompatibility.invoke();
        String bundleElement = bundleCompatibility.getBundleElement();
        String pkgElement = bundleCompatibility.getPkgElements().toString();

        assertTrue("Unexpected bundle versioning issue", bundleElement==null);
        assertTrue("Unexpected package versioning issue", pkgElement.trim().length() == 0);
      
      } catch (IOException e) {
        fail("Unexpected IOException " + e);
      }
    }
}
