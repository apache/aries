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
package org.apache.aries.blueprint.container;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.DomainCombiner;
import java.security.ProtectionDomain;

import org.osgi.framework.BundleContext;

public class BlueprintDomainCombiner implements DomainCombiner {
    private final BundleContext bundleContext;

    public static AccessControlContext createAccessControlContext(BundleContext bundleContext) {
        return new AccessControlContext(AccessController.getContext(), new BlueprintDomainCombiner(bundleContext));
    }

    BlueprintDomainCombiner(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public ProtectionDomain[] combine(ProtectionDomain[] arg0,
                                      ProtectionDomain[] arg1) {
        return new ProtectionDomain[] { new BlueprintProtectionDomain(bundleContext) };
    }

}
