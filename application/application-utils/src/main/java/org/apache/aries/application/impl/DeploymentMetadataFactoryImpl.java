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

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.DeploymentMetadataFactory;
import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.filesystem.IFile;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.utils.manifest.ManifestProcessor;

public class DeploymentMetadataFactoryImpl implements DeploymentMetadataFactory
{

  public DeploymentMetadata createDeploymentMetadata(AriesApplication app,
      Set<BundleInfo> additionalBundlesRequired) throws ResolverException
  {
    return new DeploymentMetadataImpl(app, additionalBundlesRequired);
  }

  public DeploymentMetadata parseDeploymentMetadata(IFile src) throws IOException
  {
    InputStream is = src.open();
    try {
      return parseDeploymentMetadata(is);
    } finally {
      is.close();
    }
  }

  public DeploymentMetadata parseDeploymentMetadata(InputStream in) throws IOException
  {
    return createDeploymentMetadata(ManifestProcessor.parseManifest(in));
  }

  public DeploymentMetadata createDeploymentMetadata(Manifest manifest) throws IOException
  {
    try {
      return new DeploymentMetadataImpl(manifest);
    } catch (InvalidAttributeException iae) {
      throw new IOException(iae);
    }
  }

  public DeploymentMetadata createDeploymentMetadata(IFile src) throws IOException
  {
    return parseDeploymentMetadata(src);
  }

  public DeploymentMetadata createDeploymentMetadata(InputStream in) throws IOException
  {
    return parseDeploymentMetadata(in);
  }
}
