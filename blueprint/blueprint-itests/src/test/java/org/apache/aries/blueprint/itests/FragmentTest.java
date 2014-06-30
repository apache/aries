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

import static org.apache.aries.blueprint.itests.Helper.mvnBundle;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;

import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.osgi.service.blueprint.container.BlueprintContainer;

@RunWith(PaxExam.class)
public class FragmentTest extends AbstractBlueprintIntegrationTest
{
    
  @Test
  public void testFragmentProvidesBlueprintFile() throws Exception
  {
    Runnable r = context().getService(Runnable.class);
    Assert.assertNotNull("Could not find blueprint registered service", r);
    BlueprintContainer bc = Helper.getBlueprintContainerForBundle(context(), "org.apache.aries.test.host");
    Assert.assertNotNull("Could not find blueprint container for bundle", bc);
  }
  
  @Configuration
  public Option[] configuration() {
      InputStream hostJar = TinyBundles.bundle()
              .set(Constants.BUNDLE_MANIFESTVERSION, "2")
              .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.test.host").build();
      
      InputStream fragmentJar = TinyBundles.bundle()
              .set(Constants.BUNDLE_MANIFESTVERSION, "2")
              .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.test.fragment")
              .set(Constants.FRAGMENT_HOST, "org.apache.aries.test.host")
              .add("OSGI-INF/blueprint/bp.xml", this.getClass().getResourceAsStream("/bp.xml"))
              .build();
      
      return new Option[] {
          baseOptions(),
          Helper.blueprintBundles(),
          streamBundle(fragmentJar).noStart(),
          streamBundle(hostJar),
          mvnBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample", false)
      };
  }

}