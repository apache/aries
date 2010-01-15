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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.application.runtime.defaults;

import java.util.Set;

import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationResolver;
import org.apache.aries.application.management.BundleInfo;

/** AriesApplicationManager requires that there be at least one 
 * AriesApplicationResolver service present. This class provides a null 
 * implementation: it simply returns the bundles that it was provided with - 
 * enough to permit the testing of Aries applications that have no external 
 * dependencies.   
 */
public class NoOpResolver implements AriesApplicationResolver {

  public Set<BundleInfo> resolve(AriesApplication app) {
    return app.getBundleInfo();
  }

}
