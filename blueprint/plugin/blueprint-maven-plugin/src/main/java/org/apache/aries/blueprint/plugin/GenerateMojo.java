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
import org.apache.aries.blueprint.plugin.model.ConflictDetected;
import org.apache.aries.blueprint.plugin.spi.Activation;
import org.apache.aries.blueprint.plugin.spi.Availability;
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
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates blueprint from CDI annotations
 */
@Mojo(name = "blueprint-generate", requiresDependencyResolution = ResolutionScope.COMPILE,
        defaultPhase = LifecyclePhase.PROCESS_CLASSES, inheritByDefault = false, threadSafe = true)
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
     * Base directory to write generated hierarchy.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/blueprint/")
    private String baseDir;

    /**
     * Base directory to write into
     * (relative to baseDir property).
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
     * Specifies the default availability setting that will be defined for components.
     * Default is null, which indicates mandatory (blueprint default).
     * If MANDATORY then default-activation will be set to mandatory.
     * If OPTIONAL then default-activation will be explicitly set to optional.
     */
    @Parameter
    protected Availability defaultAvailability;

    /**
     * Specifies the default timout setting that will be defined for components.
     * Default is null, which indicates 300000 seconds (blueprint default).
     */
    @Parameter
    protected Long defaultTimeout;

    /**
     * Specifies additional parameters which could be used in extensions
     */
    @Parameter
    protected Map<String, String> customParameters;

    /**
     * Which artifacts should be included in finding beans process
     */
    @Parameter
    private Set<String> includeArtifacts = new HashSet<>();

    /**
     * Which artifacts should be excluded from finding beans process
     */
    @Parameter
    private Set<String> excludeArtifacts = new HashSet<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> toScan = getPackagesToScan();

        if (!sourcesChanged()) {
            getLog().info("Skipping blueprint generation because source files were not changed");
            return;
        }

        try {
            BlueprintConfigurationImpl blueprintConfiguration = new BlueprintConfigurationImpl(namespaces, defaultActivation, customParameters, defaultAvailability, defaultTimeout);
            generateBlueprint(toScan, blueprintConfiguration);
        } catch (ConflictDetected e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (Exception e) {
            throw new MojoExecutionException("Error during blueprint generation", e);
        }
    }

    private void generateBlueprint(List<String> toScan, BlueprintConfigurationImpl blueprintConfiguration) throws Exception {
        long startTime = System.currentTimeMillis();
        ClassFinder classFinder = createProjectScopeFinder();
        getLog().debug("Creating package scope class finder: " + (System.currentTimeMillis() - startTime) + "ms");
        startTime = System.currentTimeMillis();
        Set<Class<?>> classes = FilteredClassFinder.findClasses(classFinder, toScan);
        getLog().debug("Finding bean classes: " + (System.currentTimeMillis() - startTime) + "ms");
        startTime = System.currentTimeMillis();
        Blueprint blueprint = new Blueprint(blueprintConfiguration, classes);
        getLog().debug("Creating blueprint model: " + (System.currentTimeMillis() - startTime) + "ms");
        startTime = System.currentTimeMillis();
        writeBlueprintIfNeeded(blueprint);
        getLog().debug("Writing blueprint: " + (System.currentTimeMillis() - startTime) + "ms");
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
        ResourceInitializer.prepareBaseDir(project, baseDir);

        File dir = new File(baseDir, generatedDir);
        File file = new File(dir, generatedFileName);
        file.getParentFile().mkdirs();
        getLog().info("Generating blueprint to " + file);

        OutputStream fos = buildContext.newFileOutputStream(file);
        new BlueprintFileWriter(fos).write(blueprint);
        fos.close();
    }

    private ClassFinder createProjectScopeFinder() throws Exception {
        List<URL> urls = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        ClassRealm classRealm = new ClassRealm(new ClassWorld(), "maven-blueprint-plugin-classloader", getClass().getClassLoader());
        classRealm.addURL(new File(project.getBuild().getOutputDirectory()).toURI().toURL());
        urls.add(new File(project.getBuild().getOutputDirectory()).toURI().toURL());

        ArtifactFilter artifactFilter = new ArtifactFilter(includeArtifacts, excludeArtifacts);

        for (Object artifactO : project.getArtifacts()) {
            Artifact artifact = (Artifact) artifactO;
            File file = artifact.getFile();
            if (file == null) {
                continue;
            }
            URL artifactUrl = file.toURI().toURL();
            classRealm.addURL(artifactUrl);
            if (artifactFilter.shouldExclude(artifact)) {
                getLog().debug("Excluded artifact: " + artifact);
                continue;
            }
            getLog().debug("Taken artifact: " + artifact);
            urls.add(artifactUrl);
        }
        getLog().debug(" Create class loader: " + (System.currentTimeMillis() - startTime) + "ms");
        startTime = System.currentTimeMillis();
        ClassFinder classFinder = new ClassFinder(classRealm, urls);
        getLog().debug(" Building class finder: " + (System.currentTimeMillis() - startTime) + "ms");
        return classFinder;
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
