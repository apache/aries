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
package org.apache.aries.ejb.modelling.impl;

import org.apache.aries.application.modelling.ModellerException;
import org.apache.aries.ejb.modelling.EJBLocator;
import org.apache.aries.ejb.modelling.EJBRegistry;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.manifest.BundleManifest;

/**
 * An EJB Locator implementation for when EJB location is unavailable.
 * It will cause any modelling that might involve EJBs to fail.
 */
public class EJBLocationUnavailable implements EJBLocator {

  public void findEJBs(BundleManifest manifest, IDirectory bundle,
      EJBRegistry registry) throws ModellerException {
    throw new ModellerException("No OpenEJB runtime present");
  }

}
