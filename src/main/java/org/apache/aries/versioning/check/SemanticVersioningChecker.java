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

package org.apache.aries.versioning.check;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.String;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;
import org.apache.aries.util.io.IOUtils;
import org.apache.aries.util.manifest.BundleManifest;
import org.apache.aries.versioning.utils.SemanticVersioningUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemanticVersioningChecker {

    private static final Logger _logger = LoggerFactory.getLogger(SemanticVersioningChecker.class);
    private URLClassLoader newJarsLoader;
    private URLClassLoader oldJarsLoader;
    private static final String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";

    /**
     * This method is to scan the current location against base and process each individual jars
     * in the current location against the jar with the same symbolic names in the base and produce a xml report specified by versioningReport.
     *
     * @param base             baseline jars
     * @param current          current version of jars
     * @param versioningReport the validation reports
     */
    public static void checkSemanticVersioning(URL base, URL current, File versioningReport) {
        //For each jar under the current location, open the jar and then use ASM to
        //work out whether there are binary incompatible changes against the base location.
        try {
            File baseDir = new File(base.toURI());
            File currentDir = new File(current.toExternalForm());
            if (baseDir.exists() && currentDir.exists()) {
                new SemanticVersioningChecker().performVersioningCheck(FileSystem.getFSRoot(baseDir), FileSystem.getFSRoot(currentDir), versioningReport);
            } else {
                _logger.debug("No bundles found to process.");
            }
        } catch (URISyntaxException use) {
            _logger.error(use.getMessage());
        }
    }

    // for each jar, open its manifest and found the packages
    private void performVersioningCheck(IDirectory baseDir, IDirectory currentDir, File versionStatusFile) {

        FileWriter versionStatusFileWriter = null;
        try {
            versionStatusFileWriter = new FileWriter(versionStatusFile, false);
            Map<String, BundleInfo> baseBundles;
            Map<String, BundleInfo> currentBundles;

            //scan each individual bundle and find the corresponding bundle in the baseline and verify the version changes
            currentBundles = getBundles(currentDir);
            baseBundles = getBundles(baseDir);
            URL[] newJarURLs = getListURLs(currentBundles.values()).toArray(new URL[0]);
            newJarsLoader = new URLClassLoader(newJarURLs);

            URL[] oldJarURLs = getListURLs(baseBundles.values()).toArray(new URL[0]);

            oldJarsLoader = new URLClassLoader(oldJarURLs);

            //Write the xml header
            writeRecordToWriter(versionStatusFileWriter, xmlHeader + "\r\n");

            // write the comparison base and current level into the file
            writeRecordToWriter(versionStatusFileWriter, "<semanticVersioning currentDir= \"" + currentDir + "\" baseDir = \"" + baseDir + "\">");
            for (Map.Entry<String, BundleInfo> entry : currentBundles.entrySet()) {
                String bundleSymbolicName = entry.getKey();

                String bundleElement = null;
                boolean bundleVersionCorrect = true;
                // find the same bundle in the base and check whether all the versions are correct
                BundleInfo currentBundle = entry.getValue();
                BundleInfo baseBundle = baseBundles.get(bundleSymbolicName);
                StringBuilder pkgElements = new StringBuilder();
                if (baseBundle == null) {
                    _logger.debug("The bundle " + bundleSymbolicName + " has no counterpart in the base. The semantic version validation does not apply to this bundle.");
                } else {
                    BundleCompatibility bundleCompatibility = new BundleCompatibility(bundleSymbolicName, currentBundle, baseBundle, oldJarsLoader, newJarsLoader).invoke();
                    bundleVersionCorrect = bundleCompatibility.isBundleVersionCorrect();
                    bundleElement = bundleCompatibility.getBundleElement();
                    pkgElements = bundleCompatibility.getPkgElements();
                }
                // Need to write bundle element and then package elements
                if ((!!!bundleVersionCorrect) || ((pkgElements.length() > 0))) {
                    writeRecordToWriter(versionStatusFileWriter, bundleElement);
                    writeRecordToWriter(versionStatusFileWriter, pkgElements.toString());
                    writeRecordToWriter(versionStatusFileWriter, "</bundle>");
                }
            }
            writeRecordToWriter(versionStatusFileWriter, "</semanticVersioning>");

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.close(versionStatusFileWriter);
        }

        return;
    }


    private void writeRecordToWriter(FileWriter fileWriter, String stringToWrite) throws IOException {
        if (fileWriter != null) {
            fileWriter.append(stringToWrite);
            fileWriter.append("\r\n");
        }
    }

    private Map<String, BundleInfo> getBundles(IDirectory ds) {
        Map<String, BundleInfo> bundles = new HashMap<String, BundleInfo>();
        List<IFile> includedFiles = ds.listAllFiles();

        for (IFile ifile : includedFiles) {


            if (ifile.getName().endsWith(SemanticVersioningUtils.jarExt)) {
                // scan its manifest
                try {
                    BundleManifest manifest = BundleManifest.fromBundle(ifile.open());
                    // find the bundle symbolic name, store them in a map with bundle symbolic name as key, bundleInfo as value
                    if (manifest.getSymbolicName() != null) {

                        bundles.put(manifest.getSymbolicName(), new BundleInfo(manifest, new File(ifile.toURL().getPath())));

                    }
                } catch (MalformedURLException mue) {
                    _logger.debug("Exception thrown when processing" + ifile.getName(), mue);
                } catch (IOException ioe) {
                    _logger.debug("Exception thrown when processing" + ifile.getName(), ioe);
                }
            }
        }
        return bundles;
    }

    private Collection<URL> getListURLs(Collection<BundleInfo> bundles) {
        Collection<URL> urls = new HashSet<URL>();
        try {
            for (BundleInfo bundle : bundles) {

                URL url = bundle.getBundle().toURI().toURL();
                urls.add(url);

            }

        } catch (MalformedURLException e) {
            _logger.debug(e.getMessage());
        }
        return urls;
    }

}
