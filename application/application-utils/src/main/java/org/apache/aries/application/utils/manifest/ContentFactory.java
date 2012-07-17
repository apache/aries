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
package org.apache.aries.application.utils.manifest;

import java.util.Map;

import org.apache.aries.application.Content;
import org.apache.aries.application.impl.ContentImpl;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;


public class ContentFactory {
  /**
   * Parse a content object
   * @param bundleSymbolicName bundle symbolic name
   * @param versionRange version range in the String format
   * @return Content object
   */
  public static Content parseContent(String bundleSymbolicName, String versionRange) {
    return new ContentImpl(bundleSymbolicName, ManifestHeaderProcessor.parseVersionRange(versionRange));
  }
  
  /**
   * Parse a content
   * @param contentName The content name
   * @param nameValueMap The map containing the content attributes/directives
   * @return a content object
   */
  public static Content parseContent(String contentName, Map<String, String> nameValueMap) {
    return new ContentImpl(contentName, nameValueMap);
  }
}
