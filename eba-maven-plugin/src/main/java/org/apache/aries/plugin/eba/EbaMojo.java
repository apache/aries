package org.apache.aries.plugin.eba;

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

import org.apache.maven.archiver.PomPropertiesUtil;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * Builds Aries Enterprise Bundle Archive (eba) files.
 *
 * @version $Id: $
 * @goal eba
 * @phase package
 * @requiresDependencyResolution test
 */
public class EbaMojo
    extends AbstractMojo
{
    public static final String APPLICATION_MF_URI = "META-INF/application.mf";

    private static final String[] DEFAULT_INCLUDES = {"**/**"};

    /**
     * Single directory for extra files to include in the eba.
     *
     * @parameter expression="${basedir}/src/main/eba"
     * @required
     */
    private File ebaSourceDirectory;

    /**
     * The location of the application.mf file to be used within the eba file.
     *
     * @parameter expression="${basedir}/src/main/eba/META-INF/application.mf"
     */
    private File applicationManifestFile;

    /**
     * Specify if the generated jar file of this project should be
     * included in the eba file ; default is true.
     *
     * @parameter
     */
    private Boolean includeJar = Boolean.TRUE;

    /**
     * The location of the manifest file to be used within the eba file.
     *
     * @parameter expression="${basedir}/src/main/eba/META-INF/MANIFEST.MF"
     */
    private File manifestFile;

    /**
     * Directory that resources are copied to during the build.
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}"
     * @required
     */
    private String workDirectory;

    /**
     * Directory that remote-resources puts legal files.
     *
     * @parameter expression="${project.build.directory}/maven-shared-archive-resources"
     * @required
     */
    private String sharedResources;

    /**
     * The directory for the generated eba.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String outputDirectory;

    /**
     * The name of the eba file to generate.
     *
     * @parameter alias="ebaName" expression="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The Jar archiver.
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#zip}"
     * @required
     */
    private ZipArchiver zipArchiver;

    /**
     * Adding pom.xml and pom.properties to the archive.
     *
     * @parameter expression="${addMavenDescriptor}" default-value="true"
     */
    private boolean addMavenDescriptor;

    /**
     * Include or not empty directories
     *
     * @parameter expression="${includeEmptyDirs}" default-value="true"
     */
    private boolean includeEmptyDirs;

    /**
     * Whether creating the archive should be forced.
     *
     * @parameter expression="${forceCreation}" default-value="false"
     */
    private boolean forceCreation;

    /**
     * Whether to follow transitive dependencies or use explicit dependencies.
     *
     * @parameter expression="${useTransitiveDependencies}" default-value="false"
     */
    private boolean useTransitiveDependencies;


    private File buildDir;


    public void execute()
        throws MojoExecutionException
    {
        getLog().debug( " ======= EbaMojo settings =======" );
        getLog().debug( "ebaSourceDirectory[" + ebaSourceDirectory + "]" );
        getLog().debug( "manifestFile[" + manifestFile + "]" );
        getLog().debug( "applicationManifestFile[" + applicationManifestFile + "]" );
        getLog().debug( "workDirectory[" + workDirectory + "]" );
        getLog().debug( "outputDirectory[" + outputDirectory + "]" );
        getLog().debug( "finalName[" + finalName + "]" );

        zipArchiver.setIncludeEmptyDirs( includeEmptyDirs );
        zipArchiver.setCompress( true );
        zipArchiver.setForced( forceCreation );
        // Check if jar file is there and if requested, copy it
        try
        {
            if (includeJar.booleanValue()) {
                File generatedJarFile = new File( outputDirectory, finalName + ".jar" );
                if (generatedJarFile.exists()) {
                    getLog().info( "Including generated jar file["+generatedJarFile.getName()+"]");
                    zipArchiver.addFile(generatedJarFile, finalName + ".jar");
                }
            }
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error adding generated Jar file", e );

        }

        // Copy dependencies
        try
        {
            Set<Artifact> artifacts;
            if (useTransitiveDependencies) {
                artifacts = project.getArtifacts();
            } else {
                artifacts = project.getDependencyArtifacts();
            }
            for (Artifact artifact : artifacts) {

                ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);
                if (!artifact.isOptional() && filter.include(artifact)) {
                    getLog().info("Copying artifact[" + artifact.getGroupId() + ", " + artifact.getId() + ", " +
                            artifact.getScope() + "]");
                    zipArchiver.addFile(artifact.getFile(), artifact.getArtifactId() + "-" + artifact.getVersion() + "." + (artifact.getType() == null ? "jar" : artifact.getType()));
                }
            }
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error copying EBA dependencies", e );
        }

        // Copy source files
        try
        {
            File ebaSourceDir = ebaSourceDirectory;
            if ( ebaSourceDir.exists() )
            {
                getLog().info( "Copy eba resources to " + getBuildDir().getAbsolutePath() );

                DirectoryScanner scanner = new DirectoryScanner();
                scanner.setBasedir( ebaSourceDir.getAbsolutePath() );
                scanner.setIncludes( DEFAULT_INCLUDES );
                scanner.addDefaultExcludes();
                scanner.scan();

                String[] dirs = scanner.getIncludedDirectories();

                for ( int j = 0; j < dirs.length; j++ )
                {
                    new File( getBuildDir(), dirs[j] ).mkdirs();
                }

                String[] files = scanner.getIncludedFiles();

                for ( int j = 0; j < files.length; j++ )
                {
                    File targetFile = new File( getBuildDir(), files[j] );

                    targetFile.getParentFile().mkdirs();

                    File file = new File( ebaSourceDir, files[j] );
                    FileUtils.copyFileToDirectory( file, targetFile.getParentFile() );
                }
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error copying EBA resources", e );
        }

        // Include custom manifest if necessary
        try
        {
            includeCustomApplicationManifestFile();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying application.mf file", e );
        }

        // Check if connector deployment descriptor is there
        File ddFile = new File( getBuildDir(), APPLICATION_MF_URI);
        if ( !ddFile.exists() )
        {
            getLog().warn(
                "eba deployment descriptor: " + ddFile.getAbsolutePath() + " does not exist." );
        }

        try
        {
            if (addMavenDescriptor) {
                if (project.getArtifact().isSnapshot()) {
                    project.setVersion(project.getArtifact().getVersion());
                }

                String groupId = project.getGroupId();

                String artifactId = project.getArtifactId();

                zipArchiver.addFile(project.getFile(), "META-INF/maven/" + groupId + "/" + artifactId + "/pom.xml");
                PomPropertiesUtil pomPropertiesUtil = new PomPropertiesUtil();
                File dir = new File(project.getBuild().getDirectory(), "maven-zip-plugin");
                File pomPropertiesFile = new File(dir, "pom.properties");
                pomPropertiesUtil.createPomProperties(project, zipArchiver, pomPropertiesFile, forceCreation);
            }
            File ebaFile = new File( outputDirectory, finalName + ".eba" );
            zipArchiver.setDestFile(ebaFile);

            File buildDir = getBuildDir();
            if (buildDir.isDirectory()) {
                zipArchiver.addDirectory(buildDir);
            }
            //include legal files if any
            File sharedResourcesDir = new File(sharedResources);
            if (sharedResourcesDir.isDirectory()) {
                zipArchiver.addDirectory(sharedResourcesDir);
            }
            zipArchiver.createArchive();

            project.getArtifact().setFile( ebaFile );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error assembling eba", e );
        }
    }

    protected File getBuildDir()
    {
        if ( buildDir == null )
        {
            buildDir = new File( workDirectory );
        }
        return buildDir;
    }

    private void includeCustomApplicationManifestFile()
        throws IOException
    {
        if (applicationManifestFile == null) {
            throw new NullPointerException("application manifest file location not set");
        }
        File appMfFile = applicationManifestFile;
        if (appMfFile.exists()) {
            getLog().info( "Using application.mf "+ applicationManifestFile);
            File metaInfDir = new File(getBuildDir(), "META-INF");
            FileUtils.copyFileToDirectory( appMfFile, metaInfDir);
        }
    }
}
