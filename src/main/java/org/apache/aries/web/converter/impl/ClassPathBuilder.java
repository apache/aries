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
package org.apache.aries.web.converter.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.Manifest;

public class ClassPathBuilder
{
  private Map<String, Manifest> manifests;
  
  /**
   * This class takes a map of <jarFileName, manifest> pairs which are contained in 
   * a particular jar file.
   * The updatePath method then uses this list to analyse the contents of the manifests
   * and looks for any dependencies in the other manifests in the jar.
   * @param manifests
   */
  public ClassPathBuilder(Map<String, Manifest> manifests)
  {
    this.manifests = manifests;
  }

  /**
   * We take a full qualified jar file name and search its manifest for any other classpath
   * dependencies within the other manifest in the parent jar file.
   * @param jarFile
   * @param classPath
   * @return
   * @throws IOException
   */
  public ArrayList<String> updatePath(String jarFile, ArrayList<String> classPath) throws IOException
  {
      // Get the classpath entries from this manifest and merge them into ours
      Manifest manifest = manifests.get(jarFile);
      
      if (manifest == null)
        return classPath;
      
      String dependencies = manifest.getMainAttributes().getValue("Class-Path");
      if (dependencies == null)
        dependencies = manifest.getMainAttributes().getValue("Class-path");
            
      if (dependencies != null)
      {
        // Search through the entries in the classpath
        StringTokenizer tok = new StringTokenizer(dependencies, ";");
        while (tok.hasMoreTokens()) {
          String path = jarFile.substring(0, jarFile.lastIndexOf('/'));;
          String entry = tok.nextToken();
          
          // Resolve the path to its canonical form
          path = new File("/"+path+"/"+entry).getCanonicalPath().replace('\\','/');
          path = path.substring(path.indexOf('/')+1);
     
          // If we havent already located this dependency before then we add this to our
          // list of dependencies
          if (entry.endsWith(".jar") && 
              manifests.keySet().contains(path) && 
              !classPath.contains(path) && 
              !path.startsWith("WEB-INF/lib/"))
          {
            classPath.add(path);
            
            // Recursively search the new classpath entry for more dependencies
            classPath = updatePath(path, classPath);
            
          }
        }
      }

    return classPath;
  }

}
