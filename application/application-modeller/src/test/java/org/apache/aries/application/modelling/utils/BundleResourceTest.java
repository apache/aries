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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.jar.Manifest;

import org.apache.aries.application.management.InvalidAttributeException;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ResourceType;
import org.apache.aries.application.modelling.impl.ModelledResourceImpl;
import org.junit.Test;
import org.osgi.framework.Constants;

public class BundleResourceTest extends AbstractBundleResourceTest
{
  /**
   * 
   */
  private static final String MANIFEST_MF = "MANIFEST.MF";
  /**
   * 
   */
  private static final String TEST_APP_MANIFEST_PATH = "../src/test/resources/bundles/test.bundle1.jar/META-INF";

  /**
   * @return
   * @throws IOException
   * @throws FileNotFoundException
   * @throws ResolverException
   * @throws InvalidAttributeException 
   */
  protected ModelledResource instantiateBundleResource() throws Exception
  {
    File file = new File(TEST_APP_MANIFEST_PATH, MANIFEST_MF);
    Manifest man = new Manifest(new FileInputStream(file));

    ModelledResource br = new ModelledResourceImpl(null, man.getMainAttributes(), null, null);
    return br;
  }

  @Test
  public void testBundleResourceIsBundle() throws Exception
  {
    assertEquals(ResourceType.BUNDLE, bundleResource.getType());
  }
  
  @Test
  public void testFragmentCapability()
  {
    assertEquals("The bundle resource is wrong.", Constants.FRAGMENT_ATTACHMENT_ALWAYS,
        bundleResource.getExportedBundle().getAttributes().get(Constants.FRAGMENT_ATTACHMENT_DIRECTIVE + ":"));
  }
}
