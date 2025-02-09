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
package org.apache.aries.subsystem.scope.itests;

import java.io.File;
import java.io.FileOutputStream;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.osgi.framework.Constants;

/**
 * create multiple version of the same jars for testing purpose 
 * as OSGi require install bundles have unique jar by symbolicname and version.
 *
 */
public class JarCreator
{
  public static void main(String[] args) throws Exception{
    createJar("1.0.0");
    createJar("2.0.0");
  }

  private static void createJar(String version) throws Exception {
    createJarFromFile("../subsystem-example/subsystem-helloIsolation/target/org.apache.aries.subsystem.example.helloIsolation-0.4-SNAPSHOT.jar", version);
    createJarFromFile("../subsystem-example/subsystem-helloIsolationRef/target/org.apache.aries.subsystem.example.helloIsolationRef-0.4-SNAPSHOT.jar", version);
  }
  private static void createJarFromFile(String fileName, String version) throws Exception {
    JarOutputStream jos = null;
    FileOutputStream fos = null;
    ZipEntry entry = null;
    try {
      // let's get hold of jars on disk
      File jarFile = new File(fileName);
      JarInputStream jarInput = new JarInputStream(jarFile.toURL().openStream());
      Manifest manifest = jarInput.getManifest();
      
      //update manifest, Bundle-Version
      Attributes attr = manifest.getMainAttributes();
      attr.putValue(Constants.BUNDLE_VERSION, version);
      if (fileName.indexOf("helloIsolationRef") < 0) {
          attr.putValue(Constants.EXPORT_PACKAGE, "org.apache.aries.subsystem.example.helloIsolation;uses:=\"org.osgi.util.tracker,org.osgi.framework\";version=" + version);
      }
      
      int lastSlash = fileName.lastIndexOf("/");
      // trim the path
      fileName = fileName.substring(lastSlash + 1);
      int loc = fileName.indexOf("-");
      String jarFilePath = fileName.substring(0, loc + 1) + version + ".jar";
      
      if (fileName.indexOf("helloIsolationRef") < 0) {
          File directory = new File(System.getProperty("user.home") + "/.m2/repository/org/apache/aries/subsystem/example/org.apache.aries.subsystem.example.helloIsolation/" + version + "/");
          if (!directory.exists()) {
              directory.mkdir();
          }
          fos = new FileOutputStream(directory.getAbsolutePath() + "/" + jarFilePath);
      } else {
          File directory = new File(System.getProperty("user.home") + "/.m2/repository/org/apache/aries/subsystem/example/org.apache.aries.subsystem.example.helloIsolationRef/" + version + "/");
          if (!directory.exists()) {
              directory.mkdir();
          }
          fos = new FileOutputStream(directory.getAbsolutePath() + "/" + jarFilePath);  
      }
      jos = new JarOutputStream(fos, manifest);
      
      //Copy across all entries from the original jar
      int val;
      while ((entry = jarInput.getNextEntry()) != null) {
        jos.putNextEntry(entry);
        byte[] buffer = new byte[4096];
        while ((val = jarInput.read(buffer)) != -1)
          jos.write(buffer, 0, val);
      }
      
      jos.closeEntry();
      jos.finish();
      System.out.println("finishing creating jar file: " + jarFilePath + " in local m2 repo");
    } finally {
      if (jos != null) {
        jos.close();
      }
      if (fos != null) {
        fos.close();
      }
    }
  }
}
