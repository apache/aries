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
package org.apache.aries.subsystem.itests.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.aries.application.Content;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.util.io.IOUtils;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class BundleInfoImpl implements BundleInfo {

    private Map<String, String> attributeMap = new HashMap<String, String>();
    private String path;
    private Attributes attributes;

    public BundleInfoImpl(String pathToJar) {
        Manifest manifest = null;
        try {
        	File jarFile = new File(pathToJar);
            this.path = jarFile.toURI().toURL().toString();
            JarFile f = new JarFile(new File(pathToJar));
            try {
            	manifest = f.getManifest();
            }
            finally {
            	IOUtils.close(f);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        process(manifest);
    }

    private void process(Manifest manifest) {
        if (manifest != null) {
            this.attributes = manifest.getMainAttributes();
            Set<Object> set = this.attributes.keySet();
            for (Object entry : set) {
                String key = entry.toString();
                attributeMap.put(key, this.attributes.getValue(key));
            }
        }
    }

    public Map<String, String> getBundleAttributes() {
        return attributeMap;
    }

    public Map<String, String> getBundleDirectives() {
        // TODO Auto-generated method stub
        return new HashMap<String, String>();
    }

    public Set<Content> getExportPackage() {
        String exportPkgs = attributeMap.get(Constants.EXPORT_PACKAGE);
        List<String> list = ManifestHeaderProcessor.split(exportPkgs, ",");
        Set<Content> contents = new HashSet<Content>();
        for (String content : list) {
            contents.add(new ContentImpl(content));
        }

        return contents;
    }

    public Set<Content> getExportService() {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<String, String> getHeaders() {
        return attributeMap;
    }

    public Set<Content> getImportPackage() {
        String importPkgs = attributeMap.get(Constants.IMPORT_PACKAGE);
        List<String> list = ManifestHeaderProcessor.split(importPkgs, ",");
        Set<Content> contents = new HashSet<Content>();
        for (String content : list) {
            contents.add(new ContentImpl(content));
        }

        return contents;
    }

    public Set<Content> getImportService() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getLocation() {
        return path;
    }

    public Set<Content> getRequireBundle() {
        String requireBundle = attributeMap.get(Constants.REQUIRE_BUNDLE);
        List<String> list = ManifestHeaderProcessor.split(requireBundle, ",");
        Set<Content> contents = new HashSet<Content>();
        for (String content : list) {
            contents.add(new ContentImpl(content));
        }

        return contents;
    }

    public String getSymbolicName() {
        return attributeMap.get(Constants.BUNDLE_SYMBOLICNAME);
    }

    public Version getVersion() {
        return Version.parseVersion(attributeMap.get(Constants.BUNDLE_VERSION));
    }

	public Attributes getRawAttributes() {
        return this.attributes;
	}

}
