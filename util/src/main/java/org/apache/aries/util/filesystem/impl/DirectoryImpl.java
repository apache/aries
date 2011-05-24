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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.aries.util.filesystem.ICloseableDirectory;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;

/**
 * An IDirectory representing a java.io.File whose isDirectory method returns true.
 */
public class DirectoryImpl extends FileImpl implements IDirectory
{
  /**
   * @param dir      the file to represent.
   * @param rootFile the file that represents the FS root.
   */
  public DirectoryImpl(File dir, File rootFile)
  {
    super(dir, rootFile);
  }

  @Override
  public IFile getFile(String name)
  {
    File desiredFile = new File(file, name);
    IFile result = null;
    
    if (desiredFile.exists()) 
    {
        if(!desiredFile.isDirectory())
          result = new FileImpl(desiredFile, rootDirFile);
        else
          result = new DirectoryImpl(desiredFile, rootDirFile);
    }
    
    return result;
  }

  @Override
  public boolean isRoot()
  {
    boolean result = (rootDirFile == file);
    return result;
  }

  @Override
  public List<IFile> listFiles()
  {
    List<IFile> files = new ArrayList<IFile>();
    File[] filesInDir = file.listFiles();
    if (filesInDir != null) {
      for (File f : filesInDir) {
        if (f.isFile()) {
          files.add(new FileImpl(f, rootDirFile));
        } else if (f.isDirectory()) {
          files.add(new DirectoryImpl(f, rootDirFile));
        }
      }
    }
    return files;
  }
  
  @Override
  public List<IFile> listAllFiles()
  {
    List<IFile> files = new ArrayList<IFile>();
    File[] filesInDir = file.listFiles();
    if (filesInDir != null) {
      for (File f : filesInDir) {
        if (f.isFile()) {
          files.add(new FileImpl(f, rootDirFile));
        } else if (f.isDirectory()) {
          IDirectory subdir = new DirectoryImpl(f, rootDirFile);
          files.add(subdir);
          files.addAll(subdir.listAllFiles());
        }
      }
    }
    return files;
  }
  
  @Override
  public Iterator<IFile> iterator()
  {
	return listFiles().iterator();
  }

  @Override
  public IDirectory getParent()
  {
    return isRoot() ? null : super.getParent();
  }

  @Override
  public IDirectory convert()
  {
    return this;
  }

  @Override
  public InputStream open() throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getLastModified()
  {
    long result = super.getLastModified();
    for (IFile aFile : this) {
      long tmpLastModified = aFile.getLastModified();
      
      if (tmpLastModified > result) result = tmpLastModified;
    }
    return result;
  }

  @Override
  public ICloseableDirectory toCloseable() {
	return new CloseableDirectory(this);
  }
}
