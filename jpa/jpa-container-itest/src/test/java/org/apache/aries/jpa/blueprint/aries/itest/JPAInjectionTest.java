/*  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.jpa.blueprint.aries.itest;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import org.apache.aries.jpa.blueprint.itest.JPATestBean;
import org.apache.aries.jpa.itest.AbstractJPAItest;
import org.junit.Ignore;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

public class JPAInjectionTest extends AbstractJPAItest {

  @Test
  @Ignore
  public void findResources() throws Exception {
    JPATestBean bean = context().getService(JPATestBean.class, "(version=1.0.0)");
    
    assertTrue("No persistence unit injection", bean.pUnitAvailable());
    assertTrue("No persistence context injection", bean.pContextAvailable());
  }
  
  @Test
  @Ignore
  public void findResources_110() throws Exception {
    JPATestBean bean = context().getService(JPATestBean.class, "(version=1.1.0)");
    
    assertTrue("No constructor unit injection", bean.constructorPUnitAvailable());
    assertTrue("No constructor context injection", bean.constructorPContextAvailable());
    
    assertTrue("No persistence unit injection", bean.pUnitAvailable());
    assertTrue("No persistence context injection", bean.pContextAvailable());
  }
  
  @Test
  @Ignore
  public void testLifecycle() throws Exception {
    JPATestBean bean = context().getService(JPATestBean.class, "(lifecycle=true)");
    assertTrue("No persistence context injection", bean.pContextAvailable());
    
    context().getBundleByName(TEST_BUNDLE_NAME).update();
    assertTrue("No persistence context injection", bean.pContextAvailable());
  }

  @Configuration
  public Option[] configuration() {
    return options(
    		baseOptions(),
    		ariesJpa(),
    		openJpa(),
    		testDs(),
    		testBundleBlueprint(),
    		//	For lifecycle testing
    		testBundle()
    );
      
  }
}
