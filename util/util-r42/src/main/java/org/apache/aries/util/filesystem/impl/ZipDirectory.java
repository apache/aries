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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.aries.util.IORuntimeException;
import org.apache.aries.util.filesystem.ICloseableDirectory;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;

/**
 * A directory in the zip.
 */
public class ZipDirectory extends ZipFileImpl implements IDirectory
{
  /** The root of the zip FS. */
  private final IDirectory root;
  private final boolean zipRoot;

  /**
   * Constructs a directory in the zip.
   *
   * @param zip1   the zip file.
   * @param entry1 the entry in the zip representing this dir.
   * @param parent the parent directory.
   */
  public ZipDirectory(File zip1, ZipEntry entry1, ZipDirectory parent, ZipCloseableDirectory cache)
  {
    super(zip1, entry1, parent, cache);
    zipRoot = false;
    root = parent.getRoot();
  }

  /**
   * This constructor creates the root of the zip.
   * @param file
   * @param fs
   * @param parent
   * @throws MalformedURLException
   */
  public ZipDirectory(File fs, IDirectory parent) throws MalformedURLException
  {
    super(fs, parent);
    root = (parent == null) ? this : parent.getRoot();
    zipRoot = true;
  }

  public ZipDirectory(ZipDirectory other, ZipCloseableDirectory cache) {
	  super(other, cache);
	  root = other.root;
	  zipRoot = other.zipRoot;
  }

  public IFile getFile(String name)
  {
    IFile result = null;

    String entryName = isZipRoot() ? name : getNameInZip() + "/" + name;

    ZipEntry entryFile = getEntry(entryName);

    if (entryFile != null) {
      if (!!!entryFile.isDirectory()) {
        result = new ZipFileImpl(zip, entryFile, buildParent(entryFile), cache);
      } else {
        result = new ZipDirectory(zip, entryFile, buildParent(entryFile), cache);
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

    name = name.substring(getNameInZip().length());

    String[] paths = name.split("/");

    StringBuilder baseBuilderCrapThingToGetRoundFindBugs = new StringBuilder(getNameInZip());

    if (!!!isZipRoot()) baseBuilderCrapThingToGetRoundFindBugs.append('/');
    // Build 'result' as a chain of ZipDirectories. This will only work if java.util.ZipFile recognises every
    // directory in the chain as being a ZipEntry in its own right.
    outer: if (paths != null && paths.length > 1) {
      for (int i = 0; i < paths.length - 1; i++) {
        String path = paths[i];
        baseBuilderCrapThingToGetRoundFindBugs.append(path);
        ZipEntry dirEntry = getEntry(baseBuilderCrapThingToGetRoundFindBugs.toString());
        if (dirEntry == null) {
          result = this;
          break outer;
        }
        result = new ZipDirectory(zip, dirEntry, result, cache);
        baseBuilderCrapThingToGetRoundFindBugs.append('/');
      }
    }
    return result;
  }

  public boolean isRoot()
  {
	  return getParent() == null;
  }

  public List<IFile> listFiles()
  {
	  return listFiles(false);
  }

  public List<IFile> listAllFiles()
  {
	  return listFiles(true);
  }

  private List<IFile> listFiles(boolean includeFilesInNestedSubdirs)
  {
	  List<IFile> files = new ArrayList<IFile>();

	  ZipFile z = openZipFile();
	  List<? extends ZipEntry> entries = Collections.list(z.entries());

	  for (ZipEntry possibleEntry : entries) {
		  if (isInDir(getNameInZip(), possibleEntry, includeFilesInNestedSubdirs)) {
			  ZipDirectory parent = includeFilesInNestedSubdirs ? buildParent(possibleEntry) : this;
			  if (possibleEntry.isDirectory()) {
				  files.add(new ZipDirectory(zip, possibleEntry, parent, cache));
			  } else {
				  files.add(new ZipFileImpl(zip, possibleEntry, parent, cache));
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
   * @param whether files in subdirectories are to be included
   * @return true if it is in this directory.
   */
  protected static boolean isInDir(String parentDir, ZipEntry possibleEntry, boolean allowSubDirs)
  {
    boolean result;
    String name = possibleEntry.getName();
    if (name.endsWith("/")) name = name.substring(0, name.length() - 1);
    result = (name.startsWith(parentDir) && !!!name.equals(parentDir) && (allowSubDirs || name.substring(parentDir.length() + 1).indexOf('/') == -1));
    return result;
  }

  public Iterator<IFile> iterator()
  {
    return listFiles().iterator();
  }

  public IDirectory convert()
  {
    return this;
  }

  public boolean isDirectory()
  {
    return true;
  }

  public boolean isFile()
  {
    return false;
  }

  public InputStream open()
  {
    throw new UnsupportedOperationException();
  }

  public IDirectory getRoot()
  {
    return root;
  }

  public boolean isZipRoot() {
	  return zipRoot;
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

  private ZipEntry getEntry(String entryName) {
    ZipFile z = openZipFile();
    ZipEntry entryFile = null;

    if (z != null) {
      entryFile = z.getEntry(entryName);
      closeZipFile(z);
    }
    return entryFile;
  }

  public ICloseableDirectory toCloseable() {
	  try {
		  return new ZipCloseableDirectory(zip, this);
	  } catch (IOException e) {
		  throw new IORuntimeException("IOException opening zip file: " + this, e);
	  }
  }
}


