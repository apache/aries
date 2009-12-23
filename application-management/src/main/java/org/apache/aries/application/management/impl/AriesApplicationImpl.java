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

package org.apache.aries.application.management.impl;

import java.io.File;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.BundleInfo;
import org.osgi.framework.Bundle;

public class AriesApplicationImpl implements AriesApplication {

  public AriesApplicationImpl(ApplicationMetadata meta, List<Bundle> bundles) {}
  public AriesApplicationImpl () {}
  
  public ApplicationMetadata getApplicationMetadata() {
    // TODO Auto-generated method stub
    return null;
  }

  public Set<BundleInfo> getBundles() {
    // TODO Auto-generated method stub
    return null;
  }

  public DeploymentMetadata getDeploymentMetadata() {
    // TODO Auto-generated method stub
    return null;
  }

  public void store(File f) {
    // TODO Auto-generated method stub

  }

  public void store(OutputStream in) {
    // TODO Auto-generated method stub

  }

}
