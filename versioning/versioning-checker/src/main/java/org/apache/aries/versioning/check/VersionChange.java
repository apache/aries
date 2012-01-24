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

import org.osgi.framework.Version;

/**
* @version $Rev:$ $Date:$
*/
class VersionChange {
    private BundleCompatibility.VERSION_CHANGE_TYPE changeType;
    private Version oldVersion;
    private Version newVersion;
    private Version recommendedNewVersion;
    private boolean correct;

    VersionChange(BundleCompatibility.VERSION_CHANGE_TYPE status, String oldVersionStr, String newVersionStr) {
        oldVersion = Version.parseVersion(oldVersionStr);
        newVersion = Version.parseVersion(newVersionStr);
        if (status == BundleCompatibility.VERSION_CHANGE_TYPE.MAJOR_CHANGE) {
            recommendedNewVersion = new Version(oldVersion.getMajor() + 1, 0, 0);
        } else if (status == BundleCompatibility.VERSION_CHANGE_TYPE.MINOR_CHANGE) {
            recommendedNewVersion = new Version(oldVersion.getMajor(), oldVersion.getMinor() + 1, 0);
        } else {
            recommendedNewVersion = oldVersion;
        }
        correct = BundleCompatibility.isVersionCorrect(status, oldVersionStr, newVersionStr);
    }

    VersionChange(BundleCompatibility.VERSION_CHANGE_TYPE changeType, Version newVersion, Version oldVersion, Version recommendedNewVersion, boolean correct) {
        this.changeType = changeType;
        this.newVersion = newVersion;
        this.oldVersion = oldVersion;
        this.recommendedNewVersion = recommendedNewVersion;
        this.correct = correct;
    }

    public BundleCompatibility.VERSION_CHANGE_TYPE getChangeType() {
        return changeType;
    }

    public Version getNewVersion() {
        return newVersion;
    }

    public Version getOldVersion() {
        return oldVersion;
    }

    public Version getRecommendedNewVersion() {
        return recommendedNewVersion;
    }

    public boolean isCorrect() {
        return correct;
    }
}
