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

package org.apache.aries.plugin.esa;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.osgi.DefaultMaven2OsgiConverter;
import org.apache.maven.shared.osgi.Maven2OsgiConverter;

import aQute.lib.osgi.Analyzer;

public class ContentInfo {
   
    /**
     * Coverter for maven pom values to OSGi manifest values (pulled in from the maven-bundle-plugin)
     */
    private static Maven2OsgiConverter maven2OsgiConverter = new DefaultMaven2OsgiConverter();
    
    private String symbolicName;
    private String type;
    private String version;
    
    public String getSymbolicName() {
        return symbolicName;
    }
    
    public String getType() {
        return type;
    }
    
    public String getVersion() {
        return version;
    }
    
    public String getContentLine() {
        String line = symbolicName;
        if (type != null) {
            line += ";type=\"" + type + "\"";
        }
        if (version != null) {
            line += ";version=\"" + version + "\"";
        }
        return line;
    }
    
    public static ContentInfo create(Artifact artifact, Log log) {
        ZipFile zip = null;
        try {
            zip = new ZipFile(artifact.getFile());
            ZipEntry entry = zip.getEntry("META-INF/MANIFEST.MF");
            if (entry != null) {
                Manifest mf = getManifest(zip, entry);
                return handleManifest(artifact, mf);
            } else {
                // no manifest.mf
                entry = zip.getEntry("OSGI-INF/SUBSYSTEM.MF");
                if (entry != null) {
                    Manifest mf = getManifest(zip, entry);
                    return handleSubsystem(artifact, mf);
                } else {
                    // and no subsystem.mf
                    return handleUnknown(artifact);
                }
            }
        } catch (Exception e) {
            log.warn("Error creating content information", e);
            return null;
        } finally {
            if (zip != null) {
               try { zip.close(); } catch (IOException ignore) {}
            }
        }
    }

    private static ContentInfo handleUnknown(Artifact artifact) {
        ContentInfo info = new ContentInfo();
        info.symbolicName = maven2OsgiConverter.getBundleSymbolicName(artifact);
        info.version = Analyzer.cleanupVersion(artifact.getVersion());  
        return info;
    }

    private static ContentInfo handleSubsystem(Artifact artifact, Manifest mf) {
        ContentInfo info = new ContentInfo();
        
        Attributes mainAttributes = mf.getMainAttributes();
        
        String subsystemSymbolicName = mainAttributes.getValue(Constants.SUBSYSTEM_SYMBOLICNAME);
        if (subsystemSymbolicName != null) {
            Map<String, ?> header = Analyzer.parseHeader(subsystemSymbolicName, null);
            info.symbolicName = (String) header.keySet().iterator().next(); 
        }
        
        String subsystemVersion = mainAttributes.getValue(Constants.SUBSYSTEM_VERSION);
        if (subsystemVersion != null) {
            info.version = subsystemVersion;
        }
        
        String subsystemType = mainAttributes.getValue(Constants.SUBSYSTEM_TYPE);
        if (subsystemType == null) {
            info.type = Constants.APPLICATION_TYPE;
        } else {
            Map<String, ?> header = Analyzer.parseHeader(subsystemType, null);
            info.type = (String) header.keySet().iterator().next(); 
        }
        
        return info;
    }

    private static ContentInfo handleManifest(Artifact artifact, Manifest mf) {
        Attributes mainAttributes = mf.getMainAttributes();
        
        String bundleSymbolicName = mainAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
        if (bundleSymbolicName == null) {
            // not a bundle
            return handleUnknown(artifact);
        } else {
            ContentInfo info = new ContentInfo();
            
            Map<String, ?> header = Analyzer.parseHeader(bundleSymbolicName, null);
            info.symbolicName = (String) header.keySet().iterator().next();         
        
            String bundleVersion = mainAttributes.getValue(Constants.BUNDLE_VERSION);
            if (bundleVersion != null) {
                info.version = bundleVersion;
            }
        
            if (mainAttributes.getValue(Constants.FRAGMENT_HOST) != null) {
                info.type = Constants.FRAGMENT_TYPE;
            }
            
            return info;
        }
    }
   
    private static Manifest getManifest(ZipFile zip, ZipEntry entry) throws IOException {        
        InputStream in = null;
        try {
            in = zip.getInputStream(entry);
            Manifest mf = new Manifest(in);
            return mf;
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException ignore) {}
            }
        }
    }
}
