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

package org.apache.aries.unittest.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.aries.application.utils.filesystem.IOUtils;

public class EbaUnitTestUtils {

private static final String TEMP_DIR = "unittest/tmpEbaContent";
  
  public static void createEba(String rootFolder, String outputFile) throws IOException
  {
    File tempDir = new File(TEMP_DIR);
    tempDir.mkdirs();
    
    createEbaRecursive(new File(rootFolder), tempDir, "");
    IOUtils.zipUp(tempDir, new File(outputFile));
    IOUtils.deleteRecursive(tempDir);
  }
  
  private static void createEbaRecursive(File folder, File tempDir, String prefix) throws IOException
  {
    for (File f : folder.listFiles())
    {
      if ((f.getName().endsWith(".jar") || f.getName().endsWith(".war")) && f.isDirectory())
      {
        File manifestFile = new File(f, "META-INF/MANIFEST.MF");
        Manifest m;
        
        if (manifestFile.isFile())
          m = new Manifest(new FileInputStream(manifestFile));
        else
        {
          m = new Manifest();
          m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        }
          
        File jarFile = new File(tempDir, prefix + f.getName());
        jarFile.getParentFile().mkdirs();
        
        IOUtils.jarUp(f, jarFile, m); 
      }
      else if (f.isFile())
      {
        IOUtils.writeOut(tempDir, prefix + f.getName(), new FileInputStream(f));
      }
      else if (f.isDirectory())
      {
        createEbaRecursive(f, tempDir, prefix + f.getName() + File.separator);
      }
    }
  }
  
  public static void cleanupEba(String outputFile)
  {
    new File(outputFile).delete();
  }
}
