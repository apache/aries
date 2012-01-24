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
//    /**
//     * Location of the file.
//     * @parameter expression="${project.build.directory}/classes"
//     * @required
//     */
//    private File oldFile;

    /**
     * name of old artifact in <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version> notation
     * @parameter
     * @required
     */
    private String oldArtifact;

    /**
     * Location of the file.
     * @parameter expression="${project.artifact.file}"
     * @required
     */
    private File newFile;

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
        } catch (MalformedURLException e) {

        } catch (IOException e) {

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
