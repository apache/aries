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

package org.apache.aries.application.utils.filesystem.impl;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.aries.application.filesystem.IDirectory;
import org.apache.aries.application.filesystem.IFile;

/**
 * A directory in the zip.
 */
public class ZipDirectory extends ZipFileImpl implements IDirectory
{
  /** The root of the zip FS. */
  private ZipDirectory root;
  
  /**
   * Constructs a directory in the zip.
   * 
   * @param zip1   the zip file.
   * @param entry1 the entry in the zip representing this dir.
   * @param parent the parent directory.
   */
  public ZipDirectory(File zip1, ZipEntry entry1, ZipDirectory parent)
  {
    super(zip1, entry1, parent);
  }

  /**
   * This constructor creates the root of the zip.
   * @param file
   * @param fs
   * @throws MalformedURLException 
   */
  public ZipDirectory(File file, File fs) throws MalformedURLException
  {
    super(file, fs);
    root = this;
  }

  public IFile getFile(String name)
  {
    IFile result = null;
    
    String entryName = isRoot() ? name : getName() + "/" + name;
    
    ZipEntry entryFile = getEntry(entryName);
    
    if (entryFile != null) {
      if (!!!entryFile.isDirectory()) {
        result = new ZipFileImpl(zip, entryFile, buildParent(entryFile));
      } else {
        result = new ZipDirectory(zip, entryFile, buildParent(entryFile));
      }
    }
    return result;
  }

  /**
   * This method builds the parent directory hierarchy for a file.
   * @param foundEntry
   * @return the parent of the entry.
   */
  private ZipDirectory buildParent(ZipEntry foundEntry)
  {
    ZipDirectory result = this;
    
    String name = foundEntry.getName();
    
    name = name.substring(getName().length());
    
    String[] paths = name.split("/");
    
    StringBuilder baseBuilderCrapThingToGetRoundFindBugs = new StringBuilder(getName());
    
    if (!!!isRoot()) baseBuilderCrapThingToGetRoundFindBugs.append('/');
    
    if (paths != null && paths.length > 1) {
      for (int i = 0; i < paths.length - 1; i++) {
        String path = paths[i];
        baseBuilderCrapThingToGetRoundFindBugs.append(path);
        ZipEntry dirEntry = getEntry(baseBuilderCrapThingToGetRoundFindBugs.toString());
        result = new ZipDirectory(zip, dirEntry, result);
        baseBuilderCrapThingToGetRoundFindBugs.append('/');
      }
    }
    return result;
  }

  public boolean isRoot()
  {
    boolean result = (root == this);
    return result;
  }

  public List<IFile> listFiles()
  {
    List<IFile> files = new ArrayList<IFile>();
    
    ZipFile z = openZipFile();
    Enumeration<? extends ZipEntry> entries = z.entries();
    
    while (entries.hasMoreElements()) {
      ZipEntry possibleEntry = entries.nextElement();
      
      if (isInDir(possibleEntry)) {
        if (possibleEntry.isDirectory()) {
          files.add(new ZipDirectory(zip, possibleEntry, this));
        } else {
          files.add(new ZipFileImpl(zip, possibleEntry, this));
        }
      }
    }
    closeZipFile(z);
    return files;
  }

  /**
   * This method works out if the provided entry is inside this directory. It
   * returns false if it is not, or if it is in a sub-directory.
   * 
   * @param possibleEntry
   * @return true if it is in this directory.
   */
  private boolean isInDir(ZipEntry possibleEntry)
  {
    boolean result;
    String name = possibleEntry.getName();
    String parentDir = getName();
    if (name.endsWith("/")) name = name.substring(0, name.length() - 1);
    result = (name.startsWith(parentDir) && !!!name.equals(parentDir) && name.substring(parentDir.length() + 1).indexOf('/') == -1);
    return result;
  }

  public Iterator<IFile> iterator()
  {
    Iterator<IFile> result = listFiles().iterator();
    return result;
  }

  @Override
  public IDirectory convert()
  {
    return this;
  }

  @Override
  public IDirectory getParent()
  {
    IDirectory result = isRoot() ? null : super.getParent();
    return result;
  }

  @Override
  public boolean isDirectory()
  {
    return true;
  }

  @Override
  public boolean isFile()
  {
    return false;
  }

  @Override
  public InputStream open() 
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public IDirectory getRoot()
  {
    return root;
  }
  
  // Although we only delegate to our super class if we removed this Findbugs
  // would correctly point out that we add fields in this class, but do not
  // take them into account for the equals method. In fact this is not a problem
  // we do not care about the root when doing an equality check, but by including
  // an equals or hashCode in here we can clearly document that we did this
  // on purpose. Hence this comment.
  @Override
  public boolean equals(Object other)
  {
    return super.equals(other);
  }
  
  @Override
  public int hashCode()
  {
    return super.hashCode();
  }
  
  private ZipEntry getEntry(String entryName){
    ZipFile z = openZipFile();
    ZipEntry entryFile = null;
    
    if (z != null) {
      entryFile = z.getEntry(entryName);
      closeZipFile(z);
    }
    return entryFile;
  }
}