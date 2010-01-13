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
package org.apache.aries.application.converters;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.aries.application.filesystem.IDirectory;
import org.apache.aries.application.filesystem.IFile;
import org.apache.aries.application.management.BundleConverter;
import org.apache.aries.web.converter.WarToWabConverter;
import org.apache.aries.web.converter.WarToWabConverter.InputStreamProvider;

public class WabConverterService implements BundleConverter {
  private WarToWabConverter wabConverter;
  
  public WarToWabConverter getWabConverter() {
    return wabConverter;
  }

  public void setWabConverter(WarToWabConverter wabConverter) {
    this.wabConverter = wabConverter;
  }

  public InputStream convert(IDirectory parentEba, final IFile toBeConverted) {
    try {
      return wabConverter.convert(new InputStreamProvider() {
        public InputStream getInputStream() throws IOException {
          return toBeConverted.open();
        }
      }, toBeConverted.getName(), new Properties());
    } catch (IOException e) {
      // TODO what to do with the Exception
      return null;
    }
  }

}
