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

package org.apache.aries.application.management;

import java.io.InputStream;

import org.apache.aries.application.filesystem.IDirectory;
import org.apache.aries.application.filesystem.IFile;

/**
 * A BundleConverter turns a .jar that is not an OSGi bundle into a well formed OSGi bundle,
 * or a .war that is not a WAB into a WAB. The first converter to return a non-null result is
 * taken as having fully converted the bundle. 
 */
public interface BundleConverter {
  /**
   * 
   * @param inputBundle Stream to the input bundle
   * @param parentEba The root of the eba containing the artifact being converted - 
   *                  currently a .zip file. In time we may come to support this 
   *                  being an exploded directory.
   * @param pathToArtifact Path to the artifact to be converted
   *                   
   * @return valid input stream or null if the bundle could not be converted. 
   */
  public InputStream convert (IDirectory parentEba, IFile fileInEba);

}
