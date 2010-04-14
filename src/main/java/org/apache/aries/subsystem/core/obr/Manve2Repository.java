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

package org.apache.aries.subsystem.core.obr;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class Manve2Repository {

    private File rootFile;
    public Manve2Repository(File rootFile) {
        this.rootFile = rootFile;
    }
    // list jar files of the repository
    public SortedSet<String> listFiles() {
        SortedSet<String> artifacts = new TreeSet<String>();
        File[] groupIds = rootFile.listFiles();
        for (int i = 0; i < groupIds.length; i++) {
            File groupId = groupIds[i];
            if (groupId.canRead() && groupId.isDirectory()) {
                File[] versionDirs = groupId.listFiles();
                for (int j = 0; j < versionDirs.length; j++) {
                    File versionDir = versionDirs[j];
                    if (versionDir.canRead() && versionDir.isDirectory()) {
                        artifacts.addAll(getArtifacts(null, versionDir, null, "jar", null));
                    }
                }
            }
        }
        
        return artifacts;
    }
    
    // reuse code from apache geronimo with slight modification
    private List<String> getArtifacts(String groupId, File versionDir, String artifactMatch, String typeMatch, String versionMatch) {
        // org/apache/xbean/xbean-classpath/2.2-SNAPSHOT/xbean-classpath-2.2-SNAPSHOT.jar
        List<String> artifacts = new ArrayList<String>();
        String artifactId = versionDir.getParentFile().getName();

        File[] files = versionDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.canRead()) {
                if (file.isDirectory()) {
                    File test = new File(file, "META-INF");
                    if(test.exists() && test.isDirectory() && test.canRead() && groupId != null) {
                        String version = versionDir.getName();
                        String fileHeader = artifactId + "-" + version + ".";

                        String fileName = file.getName();
                        if (fileName.startsWith(fileHeader)) {
                            // type is everything after the file header
                            String type = fileName.substring(fileHeader.length());

                            if (!type.endsWith(".sha1") && !type.endsWith(".md5")) {
                                if(artifactMatch != null && !artifactMatch.equals(artifactId)) {
                                    continue;
                                }
                                if(typeMatch != null && !typeMatch.equals(type)) {
                                    continue;
                                }
                                if(versionMatch != null && !versionMatch.equals(version)) {
                                    continue;
                                }
                                artifacts.add(file.getPath());
                            }
                        }
                    } else { // this is just part of the path to the artifact
                        String nextGroupId;
                        if (groupId == null) {
                            nextGroupId = artifactId;
                        } else {
                            nextGroupId = groupId + "." + artifactId;
                        }

                        artifacts.addAll(getArtifacts(nextGroupId, file, artifactMatch, typeMatch, versionMatch));
                    }
                } else if (groupId != null) {
                    String version = versionDir.getName();
                    String fileHeader = artifactId + "-" + version + ".";

                    String fileName = file.getName();
                    if (fileName.startsWith(fileHeader)) {
                        // type is everything after the file header
                        String type = fileName.substring(fileHeader.length());

                        if (!type.endsWith(".sha1") && !type.endsWith(".md5")) {
                            if(artifactMatch != null && !artifactMatch.equals(artifactId)) {
                                continue;
                            }
                            if(typeMatch != null && !typeMatch.equals(type)) {
                                continue;
                            }
                            if(versionMatch != null && !versionMatch.equals(version)) {
                                continue;
                            }
                            artifacts.add(file.getPath());
                        }
                    }
                }
            }
        }
        return artifacts;
    }
    
}
