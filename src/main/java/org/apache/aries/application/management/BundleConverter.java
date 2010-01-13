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
   * @param parentEba The root of the eba containing the artifact being converted - 
   *                  a zip format file with .eba suffix, or an exploded directory. 
   * @param fileInEba The object within the eba to convert
   * @throws ConversionException if conversion was attempted but failed
   * @return valid input stream or null if this converter does not support conversion of
   *         this artifact type.  
   */
  public InputStream convert (IDirectory parentEba, IFile fileInEba) throws ConversionException;

}
