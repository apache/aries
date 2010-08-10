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

package org.apache.aries.application.runtime.framework;

import java.util.Properties;

import org.apache.aries.application.management.BundleFramework;
import org.apache.aries.application.management.BundleFrameworkFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.framework.CompositeBundle;
import org.osgi.service.framework.CompositeBundleFactory;

public class BundleFrameworkFactoryImpl implements BundleFrameworkFactory
{  
  public BundleFramework createBundleFramework(BundleContext bc, String frameworkId,
      Properties frameworkConfig, Properties frameworkManifest) throws BundleException
  {
    BundleFramework framework = null;
    ServiceReference sr = bc.getServiceReference(CompositeBundleFactory.class.getName());

    if (sr != null) {
      CompositeBundleFactory cbf = (CompositeBundleFactory) bc.getService(sr);

      CompositeBundle compositeBundle = cbf.installCompositeBundle(
          frameworkConfig, 
          frameworkId,
          frameworkManifest);

      framework = new BundleFrameworkImpl(compositeBundle);
    } else throw new BundleException("Failed to obtain framework factory service");

    return framework;
  }
}
