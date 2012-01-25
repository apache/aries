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
import org.apache.aries.versioning.check.VersionChange;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;

/**
 * Check semantic version changes between an explicitly named old artifact and the project output artifact.
 * Optionally write packageinfo files for wrong package versions.
 *
 * @goal version-check
 * 
 * @phase verify
 */
public class VersionCheckerMojo
    extends AbstractMojo
{

    /**
     * name of old artifact in <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version> notation
     * @parameter expression="${oldArtifact}"
     */
    private String oldArtifact;

//    * @parameter expression="${project.artifact.file}"
    /**
     * Location of the file.
     * @parameter expression="${project.build.directory}/${project.build.finalName}.jar"
     * @required
     */
    private File newFile;

    /**
     * whether to write packageinfo files into source tree
     * @parameter expression="${writePackageInfos}" default-value="true"
     */
    private boolean writePackageinfos = true;

    /**
     * source tree location
     * @parameter expression="${project.basedir}/src/main/java"
     */
    private File source;

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     * @required
     * @readonly
     */
    private RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @required
     * @readonly
     */
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of project dependencies.
     *
     * @parameter default-value="${project.remoteProjectRepositories}"
     * @readonly
     */
    private List<RemoteRepository> projectRepos;

    /**
     * The project's remote repositories to use for the resolution of plugins and their dependencies.
     *
     * @parameter default-value="${project.remotePluginRepositories}"
     * @required
     * @readonly
     */
    private List<RemoteRepository> pluginRepos;



    public void execute()
        throws MojoExecutionException
    {
        if (oldArtifact != null) {
            try {
                BundleInfo oldBundle = getBundleInfo(resolve(oldArtifact));
                BundleInfo newBundle = getBundleInfo(newFile);
                String bundleSymbolicName = newBundle.getBundleManifest().getSymbolicName();
                URLClassLoader oldClassLoader = new URLClassLoader(new URL[] {oldBundle.getBundle().toURI().toURL()});
                URLClassLoader newClassLoader = new URLClassLoader(new URL[] {newBundle.getBundle().toURI().toURL()});
                BundleCompatibility bundleCompatibility = new BundleCompatibility(bundleSymbolicName, newBundle, oldBundle, oldClassLoader, newClassLoader);
                bundleCompatibility.invoke();
                getLog().info(bundleCompatibility.getBundleElement());
                getLog().info(bundleCompatibility.getPkgElements());
                if (writePackageinfos) {
                    writePackageInfos(bundleCompatibility);
                }
            } catch (MalformedURLException e) {
                throw new MojoExecutionException("Problem analyzing sources");
            } catch (IOException e) {
                throw new MojoExecutionException("Problem analyzing sources");
            }
        }
    }

    private void writePackageInfos(BundleCompatibility bundleCompatibility) {
        Map<String, VersionChange> packages = bundleCompatibility.getPackageChanges();
        for (Map.Entry<String, VersionChange> packageChange: packages.entrySet()) {
            VersionChange versionChange = packageChange.getValue();
            if (!versionChange.isCorrect()) {
                String packageName = packageChange.getKey();
                String[] bits = packageName.split("\\.");
                File packageInfo = source;
                for (String bit: bits) {
                    packageInfo = new File(packageInfo, bit);
                }
                packageInfo.mkdirs();
                packageInfo = new File(packageInfo, "packageinfo");
                try {
                    FileWriter w = new FileWriter(packageInfo);
                    try {
                        w.append("# generated by Apache Aries semantic versioning tool\n");
                        w.append("version " + versionChange.getRecommendedNewVersion().toString() + "\n");
                        w.flush();
                    } finally {
                        w.close();
                    }
                } catch (IOException e) {
                    getLog().error("Could not write packageinfo for package " + packageName, e);
                }
            }
        }
    }

    private File resolve(String oldArtifact) {
        Artifact artifact = new DefaultArtifact(oldArtifact);
        return resolve(artifact);
    }

    private File resolve(Artifact artifact) {
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(artifact);
        request.setRepositories(projectRepos);

        getLog().debug("Resolving artifact " + artifact +
                " from " + projectRepos);

        ArtifactResult result;
        try {
            result = repoSystem.resolveArtifact(repoSession, request);
        } catch (org.sonatype.aether.resolution.ArtifactResolutionException e) {
            getLog().warn("could not resolve " + artifact, e);
            return null;
        }

        getLog().debug("Resolved artifact " + artifact + " to " +
                result.getArtifact().getFile() + " from "
                + result.getRepository());
        return result.getArtifact().getFile();
    }

    private BundleInfo getBundleInfo(File f) {
        BundleManifest bundleManifest = BundleManifest.fromBundle(f);
        return new BundleInfo(bundleManifest, f);
    }
}
