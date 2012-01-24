/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.aries.versioning.check;

import java.io.File;

import org.apache.aries.util.manifest.BundleManifest;

/**
* @version $Rev:$ $Date:$
*/
public class BundleInfo {
    private final BundleManifest bundleManifest;
    private final File bundle;

    BundleInfo(BundleManifest bm, File bundle) {
        this.bundleManifest = bm;
        this.bundle = bundle;
    }

    public BundleManifest getBundleManifest() {
        return bundleManifest;
    }

    public File getBundle() {
        return bundle;
    }
}
