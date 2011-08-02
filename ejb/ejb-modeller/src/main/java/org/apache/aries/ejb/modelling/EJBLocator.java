/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.ejb.modelling;

import org.apache.aries.application.modelling.ModellerException;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.manifest.BundleManifest;

/**
 * A plug point for locating session EJBs in a bundle.
 */
public interface EJBLocator {

  /**
   * Find any session beans defined in the IDirectory bundle and register them
   * with the supplied {@link EJBRegistry}.
   * 
   * @param manifest The manifest for the bundle
   * @param bundle The bundle binary
   * @param registry The registry of located Session EJBs
   * @throws ModellerException
   */
  public void findEJBs(BundleManifest manifest, IDirectory bundle, EJBRegistry registry)
    throws ModellerException;
  
}
