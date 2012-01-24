/*
 * Copyright 20012 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.versioning.mojo;

import org.apache.aries.util.manifest.BundleManifest;
import org.apache.aries.versioning.check.BundleCompatibility;
import org.apache.aries.versioning.check.BundleInfo;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Goal which touches a timestamp file.
 *
 * @goal version-check
 * 
 * @phase install
 */
public class VersionCheckerMojo
    extends AbstractMojo
{
    /**
     * Location of the file.
     * @parameter expression="${project.build.directory}/classes"
     * @required
     */
    private File oldFile;
    /**
     * Location of the file.
     * @parameter expression="${project.build.directory}/classes"
     * @required
     */
    private File newFile;

    public void execute()
        throws MojoExecutionException
    {
        try {
            BundleInfo oldBundle = getBundleInfo(oldFile);
            BundleInfo newBundle = getBundleInfo(newFile);
            String bundleSymbolicName = newBundle.getBundleManifest().getSymbolicName();
            URLClassLoader oldClassLoader = new URLClassLoader(new URL[] {oldBundle.getBundle().toURI().toURL()});
            URLClassLoader newClassLoader = new URLClassLoader(new URL[] {newBundle.getBundle().toURI().toURL()});
            BundleCompatibility bundleCompatibility = new BundleCompatibility(bundleSymbolicName, newBundle, oldBundle, oldClassLoader, newClassLoader);
            bundleCompatibility.invoke();
            getLog().info(bundleCompatibility.getBundleElement());
            getLog().info(bundleCompatibility.getPkgElements());
        } catch (MalformedURLException e) {

        } catch (IOException e) {

        }


    }

    private BundleInfo getBundleInfo(File f) {
        BundleManifest bundleManifest = BundleManifest.fromBundle(f);
        return new BundleInfo(bundleManifest, f);
    }
}
