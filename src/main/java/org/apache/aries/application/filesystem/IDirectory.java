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

package org.apache.aries.application.filesystem;

import java.util.List;

/**
 * A virtual directory in a file system. Widely used to present a common view of regular 
 * file sytems, jar and zip files. 
 */
public interface IDirectory extends Iterable<IFile>, IFile
{
  /**
   * @return the list of files in this directory. Files must be in this directory
   *         and not in sub-directories.
   */
  public List<IFile> listFiles();
  /**
   * Gets the requested file under this directory. The file may be located any
   * number of levels within this directory. The name is relative to this
   * directory. If the file cannot be found it will return null.
   * 
   * @param name the name of the file.
   * @return     the IFile, or null if no such file exists.
   */
  public IFile getFile(String name);
  /**
   * @return true if this IDirectory is the root of the virtual file system.
   */
  public boolean isRoot();
}