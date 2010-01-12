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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.aries.application.utils.internal.MessageUtil;

public class IOUtils
{
  /**
   * Copy an InputStream to an OutputStream and close the InputStream afterwards.
   */
  public static void copy(InputStream in, OutputStream out) throws IOException
  {
    try {
      int len;
      byte[] b = new byte[1024];
      while ((len = in.read(b)) != -1)
        out.write(b,0,len);
    }
    finally {
      close(in);
    }
  }
  
  /**
   * Close some xStream for good :)
   */
  public static void close(Closeable c)
  {
    try {
      if (c != null)
        c.close();
    }
    catch (IOException e) { c=null; } //in other words do nothing in a language findbugs can understand 
  }
  
  public static OutputStream getOutputStream(File outputDir, String relativePath) throws IOException
  {
    int lastSeparatorIndex = relativePath.replace(File.separatorChar,'/').lastIndexOf("/");
    String dirName = null;
    String fileName = null;
    
    File outputDirectory;
    if (lastSeparatorIndex != -1)
    {
      dirName = relativePath.substring(0, lastSeparatorIndex);
      fileName = relativePath.substring(lastSeparatorIndex + 1);

      outputDirectory = new File(outputDir, dirName);
      
      if (!!!outputDirectory.exists() && !!!outputDirectory.mkdirs())
        throw new IOException(MessageUtil.getMessage("APPUTILS0012E", relativePath));
    }
    else
    {
      outputDirectory = outputDir;
      fileName = relativePath;
    }
    
    File outputFile = new File(outputDirectory, fileName);
    return new FileOutputStream(outputFile);
  }
  
  /**
   * Write the given InputStream to a file given by a root directory (outputDir) and a relative directory.
   * Necessary subdirectories will be created. This method will close the supplied InputStream.
   */
  public static void writeOut(File outputDir, String relativePath, InputStream content) throws IOException
  {
    OutputStream out = null;
    try {
      out = getOutputStream(outputDir, relativePath);
      IOUtils.copy(content, out);
    }
    finally {
      close(out);
    }
  }
  
  /** 
   * Zip up all contents of rootDir (recursively) into targetStream
   */
  @SuppressWarnings("unchecked")
  public static void zipUp (File rootDir, OutputStream targetStream) throws IOException
  {
    ZipOutputStream out = null;
    try { 
      out = new ZipOutputStream (targetStream);
      zipUpRecursive(out, "", rootDir, (Set<String>) Collections.EMPTY_SET);
    } finally { 
      close(out);
    }
  }
  
  /**
   * Zip up all contents of rootDir (recursively) into targetFile
   */
  public static void zipUp(File rootDir, File targetFile) throws IOException
  {
    zipUp (rootDir, new FileOutputStream (targetFile));
  }
  
  /**
   * Jar up all the contents of rootDir (recursively) into targetFile and add the manifest
   */
  public static void jarUp(File rootDir, File targetFile, Manifest manifest) throws IOException
  {
    JarOutputStream out = null;
    try {
      out = new JarOutputStream(new FileOutputStream(targetFile), manifest);
      zipUpRecursive(out, "", rootDir, new HashSet<String>(Arrays.asList("META-INF/MANIFEST.MF")));
    }
    finally {
      close(out);
    }
  }
  
  /**
   * Helper method used by zipUp
   */
  private static void zipUpRecursive(ZipOutputStream out, String prefix, 
      File directory, Set<String> filesToExclude) throws IOException
  {
    File[] files = directory.listFiles();
    if (files != null) 
    {
      for (File f : files)
      {        
        String fileName; 
        if (f.isDirectory())
          fileName = prefix + f.getName() + "/";
        else
          fileName = prefix + f.getName();
        
        if (filesToExclude.contains(fileName))
          continue;
        
        ZipEntry ze = new ZipEntry(fileName);
        ze.setSize(f.length());
        ze.setTime(f.lastModified());
        out.putNextEntry(ze);

        if (f.isDirectory()) 
          zipUpRecursive(out, fileName, f, filesToExclude);
        else 
        {
          IOUtils.copy(new FileInputStream(f), out);
        }
      }
    }
  }
  
  /**
   * Do rm -rf
   */
  public static boolean deleteRecursive(File root)
  {
    if (!!!root.exists())
      return false;
    else if (root.isFile())
      return root.delete();
    else {
      boolean result = true;
      for (File f : root.listFiles())
      {
        result = deleteRecursive(f) && result;
      }
      return root.delete() && result;
    }
  }
}

