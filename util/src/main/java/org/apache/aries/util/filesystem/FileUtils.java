/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.util.filesystem;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.aries.util.manifest.BundleManifest;

public class FileUtils {

  /**
   * Check whether a file is a bundle.
   * @param file the file path
   * @return true if the file is a bundle, false else
   */
  public static boolean isBundle(File file) {
    BundleManifest bm = BundleManifest.fromBundle(file);
    return ((bm != null) && (bm.isValid()));
  }

  /**
   * Get a list of URLs for the bundles under the parent URL
   * @param sourceDir The parent URL
   * @return the list of URLs for the bundles
   * @throws IOException
   */
  public static  List<URI> getBundlesRecursive(URI sourceDir) throws IOException {
    List<URI> filesFound = new ArrayList<URI>();
    if (sourceDir == null) {
      return filesFound;
    } if (sourceDir != null) {
      File sourceFile = new File(sourceDir);
      if (sourceFile.isFile()) {
        if (isBundle(sourceFile)) {
          filesFound.add(sourceDir);
        }
      } else if (sourceFile.isDirectory()) {
        File[] subFiles = sourceFile.listFiles();
        if ((subFiles !=null) && (subFiles.length >0)) {
          for (File file : subFiles) {
            filesFound.addAll(getBundlesRecursive(file.toURI()));
          }
        }
      }
    }
    return filesFound;
  }

}
