package org.apache.aries.blueprint.plugin;

import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

class ResourceInitializer {
    static String generateResourceEntry(MavenProject project){
        String buildDir = project.getBuild().getDirectory();
        String generatedBaseDir = buildDir + "/generated-sources/blueprint";
        Resource resource = new Resource();
        resource.setDirectory(generatedBaseDir);
        project.addResource(resource);
        return generatedBaseDir;
    }
}
