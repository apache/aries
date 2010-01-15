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

package org.apache.aries.application.utils.filesystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.aries.application.filesystem.IDirectory;
import org.apache.aries.application.utils.filesystem.impl.DirectoryImpl;
import org.apache.aries.application.utils.filesystem.impl.ZipDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstraction of a file system. A file system can be a zip, or a directory.
 */
public class FileSystem {

  private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.application.utils");

  /**
   * This method gets the IDirectory that represents the root of a virtual file
   * system. The provided file can either identify a directory, or a zip file.
   * 
   * @param fs the zip file.
   * @return   the root of the virtual FS.
   */
  public static IDirectory getFSRoot(File fs)
  {
    IDirectory dir = null;
    
    if (fs.exists()) {
      if (fs.isDirectory()) {
        dir = new DirectoryImpl(fs, fs);
      } else if (fs.isFile()) {
        try {
          dir = new ZipDirectory(fs, fs);
        } catch (IOException e) {
          _logger.error ("IOException in IDirectory.getFSRoot", e);
        }
      }
    }
    else {
      // since this method does not throw an exception but just returns null, make sure we do not lose the error
      _logger.error("File not found in IDirectory.getFSRoot", new FileNotFoundException(fs.getPath()));
    }
    return dir;
  }
}
