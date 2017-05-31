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

import java.net.URI;

/**
 * Provides a means by which a virtual directory can be cached and returned on 
 * demand.
 * <p>
 * 
 * A typical scenario for this interface is to implement it as a service which 
 * is used to hold virtual directories containing application installation 
 * artifacts, with this directory being retrieved when required during
 * application installation (a URI identifying the virtual directory having been
 * passed to the installation code).
 * <p>
 * 
 * Implementing classes should use URIs of the form
 * <code>idirfinder://?finderID=xxx&amp;directoryID=yyy</code> where the finder ID
 * within the query part of the URI may be used to assist in determining the 
 * directory finder instance that can retrieve the virtual directory identified 
 * by the directory ID part (or alternatively, the URI as a whole). When 
 * implemented as a service, a directory finder should configure a corresponding
 * service property of "finderID=xxx".
 * <p>
 */
public interface IDirectoryFinder
{
  /**
   * The scheme for directory finder URI ids. Using this scheme enables code 
   * receiving such a URI to infer that it is intended for use with a 
   * IDirectoryFinder instance.
   * <p>
   */
  final static String IDIR_SCHEME = "idirfinder";
  
  /**
   * The key used in the query part of the URI whose corresponding value 
   * assists in identifying the directory finder to be used.
   * <p>
   */
  final static String IDIR_FINDERID_KEY = "finderID";
  
  /**
   * The key used in the query part of the URI whose corresponding value 
   * identifies the directory to be returned.
   * <p>
   */
  final static String IDIR_DIRECTORYID_KEY = "directoryID";
  
  /**
   * Get the directory that corresponds to the given identifier, and remove it 
   * from the cache, or return null if no corresponding directory is found.
   * <p>
   *  
   * As the found directory is removed, it is not subsequently retrievable by 
   * re-issuing the request.
   * <p>
   * 
   * @param id a URI that identifies the desired directory.
   * @return IDirectory instance that corresponds to the given id URI, or null 
   *         if unknown.
   */
  IDirectory retrieveIDirectory(URI id);
}
