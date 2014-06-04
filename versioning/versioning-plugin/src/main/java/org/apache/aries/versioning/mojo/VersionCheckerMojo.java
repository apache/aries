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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import org.apache.aries.util.manifest.BundleManifest;
import org.apache.aries.versioning.check.BundleCompatibility;
import org.apache.aries.versioning.check.BundleInfo;
import org.apache.aries.versioning.check.VersionChange;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

/**
 * Check semantic version changes between an explicitly named old artifact and
 * the project output artifact. Optionally write packageinfo files for wrong
 * package versions.
 */
@Mojo(name = "version-check", defaultPhase = LifecyclePhase.VERIFY)
public class VersionCheckerMojo extends AbstractMojo {

    /**
     * name of old artifact in
     * groupId:artifactId[:extension[:classifier]]:version notation
     */
    @Parameter(property="aries.oldArtifact")
    private String oldArtifact;

    /**
     * Location of the file.
     */
    @Parameter(required = true, defaultValue = "${project.build.directory}/${project.build.finalName}.jar")
    private File newFile;

    /**
     * whether to write packageinfo files into source tree
     */
    @Parameter(property="writePackageInfos", defaultValue="false")
    private boolean writePackageinfos = false;

    /**
     * source tree location
     */
    @Parameter(defaultValue="${project.basedir}/src/main/java")
    private File source;

    @Component
    private RepositorySystem repository;
    
    @Component
    protected MavenProject project;
    
    @Component
    private MavenSession session;
    
    public void execute() throws MojoExecutionException {
        if (oldArtifact != null) {
            try {
                BundleInfo oldBundle = getBundleInfo(resolve(oldArtifact));
                BundleInfo newBundle = getBundleInfo(newFile);
                String bundleSymbolicName = newBundle.getBundleManifest().getSymbolicName();
                URLClassLoader oldClassLoader = new URLClassLoader(new URL[] {oldBundle.getBundle().toURI()
                    .toURL()});
                URLClassLoader newClassLoader = new URLClassLoader(new URL[] {newBundle.getBundle().toURI()
                    .toURL()});
                BundleCompatibility bundleCompatibility = new BundleCompatibility(bundleSymbolicName,
                                                                                  newBundle, oldBundle,
                                                                                  oldClassLoader,
                                                                                  newClassLoader);
                bundleCompatibility.invoke();
                String bundleElement = bundleCompatibility.getBundleElement();
                String pkgElement = bundleCompatibility.getPkgElements().toString();
                
                boolean failed = false;
                if ((bundleElement != null) && (bundleElement.trim().length() > 0)) {
                    getLog().error(bundleElement + "\r\n");
                    failed = true;
                }
                if ((pkgElement != null) && (pkgElement.trim().length() > 0)) {
                    getLog().error(pkgElement);
                    failed = true;
                }

                if (writePackageinfos) {
                    writePackageInfos(bundleCompatibility);
                }
                if (failed) {
                    throw new RuntimeException("Semantic Versioning incorrect");
                } else {
                    getLog().info("All package or bundle versions are semanticly versioned correctly.");
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
        for (Map.Entry<String, VersionChange> packageChange : packages.entrySet()) {
            VersionChange versionChange = packageChange.getValue();
            if (!versionChange.isCorrect()) {
                String packageName = packageChange.getKey();
                String[] bits = packageName.split("\\.");
                File packageInfo = source;
                for (String bit : bits) {
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

    private File resolve(String artifactDescriptor) {
        String[] s = artifactDescriptor.split(":");

        String type = (s.length >= 4 ? s[3] : "jar");
        Artifact artifact = repository.createArtifact(s[0], s[1], s[2], type);

        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        
        request.setResolveRoot(true).setResolveTransitively(false);
        request.setServers( session.getRequest().getServers() );
        request.setMirrors( session.getRequest().getMirrors() );
        request.setProxies( session.getRequest().getProxies() );
        request.setLocalRepository(session.getLocalRepository());
        request.setRemoteRepositories(session.getRequest().getRemoteRepositories());
        repository.resolve(request);
        return artifact.getFile();
    }

    private BundleInfo getBundleInfo(File f) {
        BundleManifest bundleManifest = BundleManifest.fromBundle(f);
        return new BundleInfo(bundleManifest, f);
    }
}
