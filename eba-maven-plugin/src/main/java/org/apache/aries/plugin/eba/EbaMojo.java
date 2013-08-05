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
import org.apache.maven.shared.osgi.DefaultMaven2OsgiConverter;
import org.apache.maven.shared.osgi.Maven2OsgiConverter;
import aQute.lib.osgi.Analyzer;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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

    
	public static final String APPLICATION_MF_URI = "META-INF/APPLICATION.MF";

    private static final String[] DEFAULT_INCLUDES = {"**/**"};

    /**
     * Application manifest headers
     */
    private static final String MANIFEST_VERSION = "Manifest-Version";
    private static final String APPLICATION_MANIFESTVERSION = "Application-ManifestVersion";
    private static final String APPLICATION_SYMBOLICNAME = "Application-SymbolicName";
    private static final String APPLICATION_VERSION = "Application-Version";
    private static final String APPLICATION_NAME = "Application-Name";
    private static final String APPLICATION_DESCRIPTION = "Application-Description";
    private static final String APPLICATION_CONTENT = "Application-Content";
    private static final String APPLICATION_EXPORTSERVICE = "Application-ExportService";
    private static final String APPLICATION_IMPORTSERVICE = "Application-ImportService";
    private static final String APPLICATION_USEBUNDLE = "Use-Bundle";
    
    /**
     * Coverter for maven pom values to OSGi manifest values (pulled in from the maven-bundle-plugin)
     */
    private Maven2OsgiConverter maven2OsgiConverter = new DefaultMaven2OsgiConverter();
    
    /**
     * Single directory for extra files to include in the eba.
     *
     * @parameter expression="${basedir}/src/main/eba"
     * @required
     */
    private File ebaSourceDirectory;

    /**
     * The location of the APPLICATION.MF file to be used within the eba file.
     *
     * @parameter expression="${basedir}/src/main/eba/META-INF/APPLICATION.MF"
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
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="zip"
     * @required
     */
    private ZipArchiver zipArchiver;

    /**
     * Whether to generate a manifest based on maven configuration.
     *
     * @parameter expression="${generateManifest}" default-value="false"
     */
    private boolean generateManifest;

    /**
     * Configuration for the plugin.
     *
     * @parameter
     */
    private Map instructions = new LinkedHashMap();;    
    
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

    /**
     * Define which bundles to include in the archive.
     *   none - no bundles are included 
     *   applicationContent - direct dependencies go into the content
     *   all - direct and transitive dependencies go into the content 
     *
     * @parameter expression="${archiveContent}" default-value="applicationContent"
     */
    private String archiveContent;


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
        getLog().debug( "generateManifest[" + generateManifest + "]" );

        if (archiveContent == null) {
        	archiveContent = new String("applicationContent");
        }
        
        getLog().debug( "archiveContent[" + archiveContent + "]" );        
        getLog().info( "archiveContent[" + archiveContent + "]" );        
        
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
            Set<Artifact> artifacts = null;
            if (useTransitiveDependencies || "all".equals(archiveContent)) {
                // if use transitive is set (i.e. true) then we need to make sure archiveContent does not contradict (i.e. is set
                // to the same compatible value or is the default).
            	if ("none".equals(archiveContent)) {
                    throw new MojoExecutionException("<useTransitiveDependencies/> and <archiveContent/> incompatibly configured.  <useTransitiveDependencies/> is deprecated in favor of <archiveContent/>." );            		
            	}
            	else {
                    artifacts = project.getArtifacts();            		
            	}
            } else {
            	// check that archiveContent is compatible
            	if ("applicationContent".equals(archiveContent)) {
                    artifacts = project.getDependencyArtifacts();            		
            	}
            	else {
                	// the only remaining options should be applicationContent="none"
                    getLog().info("archiveContent=none: application arvhive will not contain any bundles.");            		
            	}
            }
            if (artifacts != null) {
                for (Artifact artifact : artifacts) {

                    ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);
                    if (!artifact.isOptional() && filter.include(artifact)) {
                        getLog().info("Copying artifact[" + artifact.getGroupId() + ", " + artifact.getId() + ", " +
                                artifact.getScope() + "]");
                        zipArchiver.addFile(artifact.getFile(), artifact.getArtifactId() + "-" + artifact.getVersion() + "." + (artifact.getType() == null ? "jar" : artifact.getType()));
                    }
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
            if (!generateManifest) {
            	includeCustomApplicationManifestFile();
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying APPLICATION.MF file", e );
        }

		// Generate application manifest if requested
		if (generateManifest) {
			String fileName = new String(getBuildDir() + "/"
					+ APPLICATION_MF_URI);
			File appMfFile = new File(fileName);

			try {
				// Delete any old manifest
				if (appMfFile.exists()) {
					FileUtils.fileDelete(fileName);
				}

				appMfFile.getParentFile().mkdirs();
				if (appMfFile.createNewFile()) {
					writeApplicationManifest(fileName);
				}
			} catch (java.io.IOException e) {
				throw new MojoExecutionException(
						"Error generating APPLICATION.MF file: " + fileName, e);
			}
		}
        
        // Check if connector deployment descriptor is there
        File ddFile = new File( getBuildDir(), APPLICATION_MF_URI);
        if ( !ddFile.exists() )
        {
            getLog().warn(
                "Application manifest: " + ddFile.getAbsolutePath() + " does not exist." );
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

	private void writeApplicationManifest(String fileName)
			throws MojoExecutionException {
		try {
			// TODO: add support for dependency version ranges. Need to pick
			// them up from the pom and convert them to OSGi version ranges.
			FileUtils.fileAppend(fileName, MANIFEST_VERSION + ": " + "1" + "\n");
			FileUtils.fileAppend(fileName, APPLICATION_MANIFESTVERSION + ": " + "1" + "\n");
			FileUtils.fileAppend(fileName, APPLICATION_SYMBOLICNAME + ": "
					+ getApplicationSymbolicName(project.getArtifact()) + "\n");
			FileUtils.fileAppend(fileName, APPLICATION_VERSION + ": "
					+ getApplicationVersion() + "\n");
			FileUtils.fileAppend(fileName, APPLICATION_NAME + ": " + project.getName() + "\n");
			FileUtils.fileAppend(fileName, APPLICATION_DESCRIPTION + ": "
					+ project.getDescription() + "\n");

			// Write the APPLICATION-CONTENT
			// TODO: check that the dependencies are bundles (currently, the converter
			// will throw an exception)
			Set<Artifact> artifacts;
			if (useTransitiveDependencies) {
				artifacts = project.getArtifacts();
			} else {
				artifacts = project.getDependencyArtifacts();
			}
			artifacts = selectArtifacts(artifacts);
			Iterator<Artifact> iter = artifacts.iterator();

			FileUtils.fileAppend(fileName, APPLICATION_CONTENT + ": ");
			if (iter.hasNext()) {
				Artifact artifact = iter.next();
				FileUtils.fileAppend(fileName, maven2OsgiConverter
						.getBundleSymbolicName(artifact)
						+ ";version=\""
						+ Analyzer.cleanupVersion(artifact.getVersion())
//						+ maven2OsgiConverter.getVersion(artifact.getVersion())
						+ "\"");
			}
			while (iter.hasNext()) {
				Artifact artifact = iter.next();
				FileUtils.fileAppend(fileName, ",\n "
						+ maven2OsgiConverter.getBundleSymbolicName(artifact)
						+ ";version=\""
						+ Analyzer.cleanupVersion(artifact.getVersion())
//						+ maven2OsgiConverter.getVersion(artifact.getVersion())
						+ "\"");
			}

			FileUtils.fileAppend(fileName, "\n");

			// Add any service imports or exports
			if (instructions.containsKey(APPLICATION_EXPORTSERVICE)) {
				FileUtils.fileAppend(fileName, APPLICATION_EXPORTSERVICE + ": "
						+ instructions.get(APPLICATION_EXPORTSERVICE) + "\n");
			}
			if (instructions.containsKey(APPLICATION_IMPORTSERVICE)) {
				FileUtils.fileAppend(fileName, APPLICATION_IMPORTSERVICE + ": "
						+ instructions.get(APPLICATION_IMPORTSERVICE) + "\n");
			}
			if (instructions.containsKey(APPLICATION_USEBUNDLE)) {
				FileUtils.fileAppend(fileName, APPLICATION_USEBUNDLE + ": "
						+ instructions.get(APPLICATION_USEBUNDLE) + "\n");
			}
                        // Add any use bundle entry

		} catch (Exception e) {
			throw new MojoExecutionException(
					"Error writing dependencies into APPLICATION.MF", e);
		}

	}
    
    // The maven2OsgiConverter assumes the artifact is a jar so we need our own
	// This uses the same fallback scheme as the converter
    private String getApplicationSymbolicName(Artifact artifact) {
		if (instructions.containsKey(APPLICATION_SYMBOLICNAME)) {
			return instructions.get(APPLICATION_SYMBOLICNAME).toString();
		}
    	return artifact.getGroupId() + "." + artifact.getArtifactId();
    }
    
    private String getApplicationVersion() {
        if (instructions.containsKey(APPLICATION_VERSION)) {
            return instructions.get(APPLICATION_VERSION).toString();
        }
        return aQute.lib.osgi.Analyzer.cleanupVersion(project.getVersion());
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
            throw new NullPointerException("Application manifest file location not set.  Use <generateManifest>true</generateManifest> if you want it to be generated.");
        }
        File appMfFile = applicationManifestFile;
        if (appMfFile.exists()) {
            getLog().info( "Using APPLICATION.MF "+ applicationManifestFile);
            File metaInfDir = new File(getBuildDir(), "META-INF");
            FileUtils.copyFileToDirectory( appMfFile, metaInfDir);
        }
    }
    
    /**
     * Return artifacts in 'compile' or 'runtime' scope only.   
     */
    private Set<Artifact> selectArtifacts(Set<Artifact> artifacts) 
    {
        Set<Artifact> selected = new LinkedHashSet<Artifact>();
        for (Artifact artifact : artifacts) {
            String scope = artifact.getScope();
            if (scope == null 
                || Artifact.SCOPE_COMPILE.equals(scope)
                || Artifact.SCOPE_RUNTIME.equals(scope)) {
                selected.add(artifact);
            }
        }
        return selected;
    }
}
