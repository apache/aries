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

public interface WarToWabConverter {
  public static interface InputStreamProvider {
    InputStream getInputStream() throws IOException;
  }

  /**
   * Generate the new manifest for the 
   * @param input
   * @param name The name of the war file
   * @param properties Properties to influence the conversion as defined in RFC66
   * @return
   */
  Manifest generateManifest(InputStreamProvider input, String name, Properties properties) throws IOException;
  
  /**
   * Generate the converter WAB file. This file includes all the files from the input
   * and has the new manifest.
   * @param input
   * @param name The name of the war file
   * @param properties Properties to influence the conversion as defined in RFC66
   * @return
   */
  InputStream convert(InputStreamProvider input, String name, Properties properties) throws IOException;
}
