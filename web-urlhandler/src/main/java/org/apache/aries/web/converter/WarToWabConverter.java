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
package org.apache.aries.web.converter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.Manifest;

/**
 * Service interface for WAR to WAB conversion
 */
public interface WarToWabConverter {
  /**
   * Support class for WabConverter to allow multiple passes over an input war
   * archive without requiring in-memory buffering.
   */
  public static interface InputStreamProvider {
    InputStream getInputStream() throws IOException;
  }

  public static final String WEB_CONTEXT_PATH = "Web-ContextPath";

  
  /**
   * Generate the new manifest for the converted war file.
   * @param input
   * @param name The name of the war file
   * @param properties Properties to influence the conversion as defined in RFC66 (see also {@link #convert} method)
   * @return
   */
  Manifest generateManifest(InputStreamProvider input, String name, Properties properties) throws IOException;
  
  /**
   * Generate the converter WAB file. This file includes all the files from the input
   * and has the new manifest.
   * @param input
   * @param name The name of the war file
   * @param properties Properties to influence the conversion as defined in RFC66. The following
   * properties are supported
   * <ul>
   *    <li>Bundle-ClassPath</li>
   *    <li>Bundle-ManifestVersion</li>
   *    <li>Bundle-SymbolicName</li>
   *    <li>Bundle-Version</li>
   *    <li>Import-Package</li>
   *    <li>Web-ContextPath</li>
   *    <li>Web-JSPExtractLocation</li>
   * </ul>
   * Except for Bundle-ClassPath and Import-Package any supplied properties will
   * overwrite values specified in an existing bundle manifest. For Bundle-ClassPath and Import-Package 
   * the supplied values will be joined to those specified in a bundle manifest
   * (if it exists) and also the results of the scan of the WAR file.
   * @return
   */
  InputStream convert(InputStreamProvider input, String name, Properties properties) throws IOException;
}
