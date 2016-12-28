/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

class PackageFinder {
    static Set<String> findPackagesInSources(List<String> compileSourceRoots) {
        Set<String> packages = new HashSet<>();
        for (String src : compileSourceRoots) {
            File root = new File(src);
            if (root.exists()) {
                packages.addAll(findPackageRoots(root));
            }
        }
        return packages;
    }

    private static Set<String> findPackageRoots(File file) {
        Set<String> packages = new HashSet<>();
        Stack<SearchFile> stack = new Stack<>();
        stack.add(new SearchFile(null, file));
        while (!stack.isEmpty()) {
            SearchFile cur = stack.pop();
            File[] files = cur.f.listFiles();
            boolean foundFile = false;
            for (File child : files) {
                if (child.isFile()) {
                    packages.add(cur.prefix);
                    foundFile = true;
                }
            }
            if (foundFile) {
                continue;
            }
            for (File child : files) {
                if (child.isDirectory()) {
                    stack.add(new SearchFile(cur.prefix != null ? cur.prefix + "." + child.getName() : child.getName(), child));
                }
            }
        }
        return packages;
    }

    static class SearchFile {
        String prefix;
        File f;

        public SearchFile(String prefix, File f) {
            this.prefix = prefix;
            this.f = f;
        }
    }
}
