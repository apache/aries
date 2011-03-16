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
package org.apache.aries.spifly.statictool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DirTree {
    List<File> fileList = new ArrayList<File>();

    public DirTree(File f) {
        String[] names = f.list();

        if (names == null) {
            fileList.add(f);
            return;
        }

        for (String name : names) {
            File curFile = new File(f, name);

            if (curFile.isDirectory()) {
                fileList.addAll(new DirTree(curFile).getFiles());
            } else {
                fileList.add(curFile);
            }
        }
        fileList.add(f);
    }

    public List<File> getFiles() {
        return fileList;
    }
}
