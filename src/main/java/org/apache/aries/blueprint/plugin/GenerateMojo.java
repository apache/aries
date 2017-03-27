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

import org.apache.aries.blueprint.plugin.model.Blueprint;
import org.apache.aries.blueprint.plugin.spi.Activation;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.xbean.finder.ClassFinder;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates blueprint from CDI annotations
 */
@Mojo(name = "blueprint-generate", requiresDependencyResolution = ResolutionScope.COMPILE,
        defaultPhase = LifecyclePhase.PROCESS_CLASSES, inheritByDefault = false)
public class GenerateMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true)
    protected MavenProject project;

    @Parameter
    protected List<String> scanPaths;

    /**
     * Which extension namespaces should the plugin support
     */
    @Parameter
    protected Set<String> namespaces;

    @Component
    private BuildContext buildContext;

    /**
     * Name of file to write
     */
    @Parameter(defaultValue = "autowire.xml")
    protected String generatedFileName;

    /**
     * Base directory to write into
     * (relative to ${project.build.directory}/generated-sources/blueprint).
     */
    @Parameter(defaultValue = "OSGI-INF/blueprint/")
    private String generatedDir;

    /**
     * Specifies the default activation setting that will be defined for components.
     * Default is null, which indicates eager (blueprint default).
     * If LAZY then default-activation will be set to lazy.
     * If EAGER then default-activation will be explicitly set to eager.
     */
    @Parameter
    protected Activation defaultActivation;

    /**
     * Specifies additional parameters which could be used in extensions
     */
    @Parameter
    protected Map<String, String> customParameters;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> toScan = getPackagesToScan();

        if (!sourcesChanged()) {
            getLog().info("Skipping blueprint generation because source files were not changed");
            return;
        }

        BlueprintConfigurationImpl blueprintConfiguration = new BlueprintConfigurationImpl(namespaces, defaultActivation, customParameters);

        try {
            ClassFinder classFinder = createProjectScopeFinder();
            Set<Class<?>> classes = FilteredClassFinder.findClasses(classFinder, toScan);
            Blueprint blueprint = new Blueprint(blueprintConfiguration, classes);
            writeBlueprintIfNeeded(blueprint);
        } catch (Exception e) {
            throw new MojoExecutionException("Error building commands help", e);
        }
    }

    private void writeBlueprintIfNeeded(Blueprint blueprint) throws Exception {
        if (blueprint.shouldBeGenerated()) {
            writeBlueprint(blueprint);
        } else {
            getLog().warn("Skipping blueprint generation because no beans were found");
        }
    }

    private boolean sourcesChanged() {
        return buildContext.hasDelta(new File(project.getCompileSourceRoots().iterator().next()));
    }

    private void writeBlueprint(Blueprint blueprint) throws Exception {
        String generatedBaseDir = ResourceInitializer.generateResourceEntry(project);

        File dir = new File(generatedBaseDir, generatedDir);
        File file = new File(dir, generatedFileName);
        file.getParentFile().mkdirs();
        getLog().info("Generating blueprint to " + file);

        OutputStream fos = buildContext.newFileOutputStream(file);
        new BlueprintFileWriter(fos).write(blueprint);
        fos.close();
    }

    private ClassFinder createProjectScopeFinder() throws MalformedURLException {
        List<URL> urls = new ArrayList<>();

        urls.add(new File(project.getBuild().getOutputDirectory()).toURI().toURL());
        for (Object artifactO : project.getArtifacts()) {
            Artifact artifact = (Artifact) artifactO;
            File file = artifact.getFile();
            if (file != null) {
                urls.add(file.toURI().toURL());
            }
        }
        ClassLoader loader = new URLClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
        return new ClassFinder(loader, urls);
    }

    private List<String> getPackagesToScan() throws MojoExecutionException {
        List<String> toScan = scanPaths;
        if (scanPaths == null || scanPaths.size() == 0 || scanPaths.iterator().next() == null) {
            getLog().info("Scan paths not specified - searching for packages");
            Set<String> packages = PackageFinder.findPackagesInSources(project.getCompileSourceRoots());
            if (packages.contains(null)) {
                throw new MojoExecutionException("Found file without package");
            }
            toScan = new ArrayList<>(packages);
            Collections.sort(toScan);
        }

        for (String aPackage : toScan) {
            getLog().info("Package " + aPackage + " will be scanned");
        }
        return toScan;
    }
}
