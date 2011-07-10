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
package org.apache.aries.blueprint.itests;

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Constants;
import org.osgi.service.blueprint.container.BlueprintContainer;

import static org.apache.aries.itest.ExtraOptions.*;

@RunWith(JUnit4TestRunner.class)
public class FragmentTest extends AbstractIntegrationTest
{
  @Test
  public void testFragmentProvidesBlueprintFile() throws Exception
  {
    ZipFixture hostJar = ArchiveFixture.newJar().manifest().attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
    .attribute(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.test.host").end();
    
    ZipFixture fragmentJar = ArchiveFixture.newJar().manifest().attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
      .attribute(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.test.fragment")
      .attribute(Constants.FRAGMENT_HOST, "org.apache.aries.test.host").end()
      .binary("OSGI-INF/blueprint/bp.xml", this.getClass().getResourceAsStream("/bp.xml")).end();
    
    bundleContext.installBundle("fragment", fragmentJar.getInputStream());
    bundleContext.installBundle("host", hostJar.getInputStream()).start();
    
    Runnable r = context().getService(Runnable.class);
    assertNotNull("Could not find blueprint registered service", r);
    BlueprintContainer bc = Helper.getBlueprintContainerForBundle(context(), "org.apache.aries.test.host");
    assertNotNull("Could not find blueprint container for bundle", bc);
  }
  
  @Test
  public void testFragmentWithOverriddenHeader() throws Exception
  {
    ZipFixture hostJar = ArchiveFixture.newJar().manifest().attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
    .attribute(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.test.host")
    .attribute("Bundle-Blueprint", "META-INF/bp/*.xml").end();
    
    ZipFixture fragmentJar = ArchiveFixture.newJar().manifest().attribute(Constants.BUNDLE_MANIFESTVERSION, "2")
      .attribute(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.test.fragment")
      .attribute(Constants.FRAGMENT_HOST, "org.apache.aries.test.host").end()
      .binary("META-INF/bp/bp.xml", this.getClass().getResourceAsStream("/bp.xml")).end();
    
    bundleContext.installBundle("fragment", fragmentJar.getInputStream());
    bundleContext.installBundle("host", hostJar.getInputStream()).start();
    
    Runnable r = context().getService(Runnable.class);
    assertNotNull("Could not find blueprint registered service", r);
    BlueprintContainer bc = Helper.getBlueprintContainerForBundle(context(), "org.apache.aries.test.host");
    assertNotNull("Could not find blueprint container for bundle", bc);
  }
  
  @org.ops4j.pax.exam.junit.Configuration
  public static Option[] configuration() {
      return testOptions(
          paxLogging("DEBUG"),
          Helper.blueprintBundles(),
          
          mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample").noStart(),
          equinox().version("3.5.0")
      );
  }

}