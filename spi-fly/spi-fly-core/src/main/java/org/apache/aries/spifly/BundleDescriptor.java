/**
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
package org.apache.aries.spifly;

import org.osgi.framework.Version;

class BundleDescriptor {
    public static final int BUNDLE_ID_UNSPECIFIED = -1;

    final String symbolicName;
    final Version version;
    final long bundleID;

    BundleDescriptor(String symbolicName) {
        this(symbolicName, null);
    }

    BundleDescriptor(String symbolicName, Version version) {
        this.symbolicName = symbolicName;
        this.version = version;
        this.bundleID = BUNDLE_ID_UNSPECIFIED;
    }

    BundleDescriptor(long bundleID) {
        this.bundleID = bundleID;
        this.symbolicName = null;
        this.version = null;
    }

    public long getBundleID() {
        return bundleID;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public Version getVersion() {
        return version;
    }
}
