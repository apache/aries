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

package org.apache.aries.util.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A virtual file on the virtual file system. This may represent a file or a
 * directory.
 */
public interface IFile
{
  /**
   * @return the name of the file relative to the root of the virtual FS. This will return a '/' separated path
   * indepedent of underlying filesystem
   */
  public String getName();
  /**
   * @return true iff this IFile is also an IDirectory
   */
  public boolean isDirectory();
  /**
   * @return true iff this IFile is not an IDirectory
   */
  public boolean isFile();
  /**
   * @return the last modified date of the file.
   */
  public long getLastModified();
  /**
   * @return the size of the file.
   */
  public long getSize();
  
  /**
   * @return if this is a directory return this as an IDirectory, otherwise return null.
   */
  public IDirectory convert();
  
  /**
   * @return if this is a directory or an archive, returns the opened IDirectory
   */
  public IDirectory convertNested();
  
  /**
   * @return returns the parent directory of this IFile, or null if this is the root.
   */
  public IDirectory getParent();
  
  /**
   * The input stream returned by this method should always be closed after use.
   * 
   * @return An InputStream to read the file from.
   * 
   * @throws IOException
   * @throws UnsupportedOperationException If the IFile is also an IDirectory.
   */
  public InputStream open() throws IOException, UnsupportedOperationException;
  
  /**
   * @return the root of this file system.
   */
  public IDirectory getRoot();
  /**
   * @return a URL that can be used to get at this file at a later date.
   * @throws MalformedURLException 
   */
  public URL toURL() throws MalformedURLException ;
}
