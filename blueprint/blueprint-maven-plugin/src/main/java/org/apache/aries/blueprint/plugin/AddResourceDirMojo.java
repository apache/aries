package org.apache.aries.blueprint.plugin;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Creates resource base dir where blueprint file will be generated for IDE support
 */
@Mojo(name="add-resource-dir",
    requiresDependencyResolution= ResolutionScope.COMPILE,
    defaultPhase= LifecyclePhase.GENERATE_RESOURCES,
    inheritByDefault=false, threadSafe = true)
public class AddResourceDirMojo extends AbstractMojo {

    @Parameter(defaultValue="${project}", required=true)
    protected MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String buildDir = project.getBuild().getDirectory();
        String generatedBaseDir = buildDir + "/generated-sources/blueprint";
        Resource resource = new Resource();
        resource.setDirectory(generatedBaseDir);
        project.addResource(resource);
    }
}
