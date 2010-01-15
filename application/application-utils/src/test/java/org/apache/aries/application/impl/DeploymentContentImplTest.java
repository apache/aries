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

package org.apache.aries.application.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.aries.application.VersionRange;
import org.junit.Test;
import org.osgi.framework.Version;

public class DeploymentContentImplTest {
  
  @Test
  public void testDeploymentContent001() throws Exception {
    DeploymentContentImpl dc = new DeploymentContentImpl("com.travel.reservation.web;deployed-version=\"1.1.0\"");
    assertEquals("1.1.0", dc.getAttribute("deployed-version"));
    VersionRange vi = dc.getVersion();
    assertTrue(vi.isExactVersion());
    assertEquals(new Version("1.1.0"), dc.getExactVersion());
    assertEquals("com.travel.reservation.web", dc.getContentName());
    assertEquals("{deployed-version->1.1.0}", dc.getNameValueMap().toString());
  }
  
  @Test
  public void testDeploymentContent002() throws Exception {
    DeploymentContentImpl dc = new DeploymentContentImpl("com.travel.reservation.business;deployed-version=2.0");
    assertEquals("2.0", dc.getAttribute("deployed-version"));
    VersionRange vi = dc.getVersion();
    assertTrue(vi.isExactVersion());
    assertEquals(new Version("2.0"), dc.getExactVersion());
    assertEquals("com.travel.reservation.business", dc.getContentName());
    assertEquals("{deployed-version->2.0}", dc.getNameValueMap().toString());
  }
  
  
  @Test
  public void testDeploymentContent003() throws Exception {
    DeploymentContentImpl dc = new DeploymentContentImpl("com.travel.reservation.data;deployed-version=2.1.1");
    assertEquals("2.1.1", dc.getAttribute("deployed-version"));
    VersionRange vi = dc.getVersion();
    assertTrue(vi.isExactVersion());
    assertEquals(new Version("2.1.1"), dc.getExactVersion());
    assertEquals("com.travel.reservation.data", dc.getContentName());
    assertEquals("{deployed-version->2.1.1}", dc.getNameValueMap().toString());
  }
}

