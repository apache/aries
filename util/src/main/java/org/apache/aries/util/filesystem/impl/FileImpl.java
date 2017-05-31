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

package org.apache.aries.util.filesystem.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.aries.util.IORuntimeException;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;

/**
 * An implementation of IFile that represents a java.io.File.
 */
public class FileImpl implements IFile
{
  /** The name of the root directory of the file system */
  protected String rootDir;
  /** This file in the file system */
  protected File file;
  /** The root File in the file system */
  protected File rootDirFile;
  /** The name of this file in the vFS */
  private String name;

  /**
   * @param f        this file.
   * @param rootFile the root of the vFS.
   */
  public FileImpl(File f, File rootFile)
  {
    file = f;
    this.rootDirFile = rootFile;
    rootDir = rootFile.getAbsolutePath();

    if (f.equals(rootFile)) name = "";
    else name = file.getAbsolutePath().substring(rootDir.length() + 1).replace('\\', '/');
  }

  public IDirectory convert()
  {
    return null;
  }

  public long getLastModified()
  {
    long result = file.lastModified();
    return result;
  }

  public String getName()
  {
    return name;
  }

  public IDirectory getParent()
  {
    IDirectory parent = new DirectoryImpl(file.getParentFile(), rootDirFile);
    return parent;
  }

  public long getSize()
  {
    long size = file.length();
    return size;
  }

  public boolean isDirectory()
  {
    boolean result = file.isDirectory();
    return result;
  }

  public boolean isFile()
  {
    boolean result = file.isFile();
    return result;
  }

  public InputStream open() throws IOException
  {
    InputStream is = new FileInputStream(file);
    return is;
  }

  public IDirectory getRoot()
  {
    IDirectory root = new DirectoryImpl(rootDirFile, rootDirFile);
    return root;
  }

  public URL toURL() throws MalformedURLException
  {
    URL result = file.toURI().toURL();
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == null) return false;
    if (obj == this) return true;

    if (obj.getClass() == getClass()) {
      return file.equals(((FileImpl)obj).file);
    }

    return false;
  }

  @Override
  public int hashCode()
  {
    return file.hashCode();
  }

  @Override
  public String toString()
  {
    return file.getAbsolutePath();
  }

  public IDirectory convertNested() {
	if (isDirectory()) {
	  return convert();
	} else {
	  try {
        return FileSystemImpl.getFSRoot(file, getParent());
      } catch (IORuntimeException e) {
        return null;
      }
	}
  }
}