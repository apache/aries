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
package org.apache.aries.application.modelling;

import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.manifest.BundleManifest;

public interface ServiceModeller {

  /**
   * Determine whether any additional services will be imported or exported 
   * by this bundle. For example as EJBs, Declarative Services etc
   *
   * @param bundle
   * @return
   */
  public ParsedServiceElements modelServices(BundleManifest manifest, IDirectory bundle)
    throws ModellerException;
  
}
