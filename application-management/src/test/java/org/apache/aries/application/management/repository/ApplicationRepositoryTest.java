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
package org.apache.aries.application.management.repository;

import java.util.HashSet;
import java.util.Map;

import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.VersionRange;
import org.apache.aries.application.impl.DeploymentContentImpl;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.spi.repository.BundleRepository.BundleSuggestion;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Test;
import org.osgi.framework.Version;

import static org.junit.Assert.*;

public class ApplicationRepositoryTest {
  @Test
  public void testBundleNotInApp() {
    AriesApplication app = Skeleton.newMock(AriesApplication.class);
    
    BundleInfo bi = Skeleton.newMock(BundleInfo.class);
    Skeleton.getSkeleton(bi).setReturnValue(new MethodCall(BundleInfo.class, "getSymbolicName"), "test.bundle");
    Skeleton.getSkeleton(bi).setReturnValue(new MethodCall(BundleInfo.class, "getVersion"), new Version("1.0.0"));
    
    Skeleton.getSkeleton(app).setReturnValue(
        new MethodCall(AriesApplication.class, "getBundleInfo"), 
        new HashSet<BundleInfo>());
    
    ApplicationRepository rep = new ApplicationRepository(app);
    BundleSuggestion sug = rep.suggestBundleToUse(new DeploymentContentImpl("test.bundle", new Version("2.0.0")));
    
    assertNull("We have apparently found a bundle that is not in the application in the ApplicationRepository", sug);
  }
}
