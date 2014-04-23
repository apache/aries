package org.apache.aries.plugin.esa;

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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Builds OSGi Enterprise Subsystem Archive (esa) files.
 *
 * @version $Id: $
 * @goal esa
 * @phase package
 * @requiresDependencyResolution test
 */
public class EsaMojo
    extends AbstractMojo
{

    public enum EsaContent {none, all, content};
    
    public static final String SUBSYSTEM_MF_URI = "OSGI-INF/SUBSYSTEM.MF";

    private static final String[] DEFAULT_INCLUDES = {"**/**"};

    /*
     * Subsystem manifest headers
     */
    private static final String SUBSYSTEM_MANIFESTVERSION = "Subsystem-ManifestVersion";
    private static final String SUBSYSTEM_SYMBOLICNAME = "Subsystem-SymbolicName";
    private static final String SUBSYSTEM_VERSION = "Subsystem-Version";
    private static final String SUBSYSTEM_NAME = "Subsystem-Name";
    private static final String SUBSYSTEM_DESCRIPTION = "Subsystem-Description";
    private static final String SUBSYSTEM_CONTENT = "Subsystem-Content";
    private static final String SUBSYSTEM_USEBUNDLE = "Use-Bundle";
    private static final String SUBSYSTEM_TYPE = "Subsystem-Type";

    private static final Set<String> SKIP_INSTRUCTIONS = new HashSet<String>();

    static {
        SKIP_INSTRUCTIONS.add(SUBSYSTEM_MANIFESTVERSION);
        SKIP_INSTRUCTIONS.add(SUBSYSTEM_SYMBOLICNAME);
        SKIP_INSTRUCTIONS.add(SUBSYSTEM_VERSION);
        SKIP_INSTRUCTIONS.add(SUBSYSTEM_NAME);
        SKIP_INSTRUCTIONS.add(SUBSYSTEM_DESCRIPTION);
        SKIP_INSTRUCTIONS.add(SUBSYSTEM_CONTENT);
    }

    /**
     * Coverter for maven pom values to OSGi manifest values (pulled in from the maven-bundle-plugin)
     */
    private Maven2OsgiConverter maven2OsgiConverter = new DefaultMaven2OsgiConverter();
    
    /**
     * Single directory for extra files to include in the esa.
     *
     * @parameter expression="${basedir}/src/main/esa"
     * @required
     */
    private File esaSourceDirectory;

    /**
     * The location of the SUBSYSTEM.MF file to be used within the esa file.
     *
     * @parameter expression="${basedir}/src/main/esa/OSGI-INF/SUBSYSTEM.MF"
     */
    private File subsystemManifestFile;


    /**
     * The location of the manifest file to be used within the esa file.
     *
     * @parameter expression="${basedir}/src/main/esa/META-INF/MANIFEST.MF"
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
     * The directory for the generated esa.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String outputDirectory;

    /**
     * The name of the esa file to generate.
     *
     * @parameter alias="esaName" expression="${project.build.finalName}"
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
    private Map instructions = new LinkedHashMap();
    
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
     * Define which bundles to include in the archive.
     *   none - no bundles are included 
     *   subsystemContent - direct dependencies go into the content
     *   all - direct and transitive dependencies go into the content 
     *
     * @parameter expression="${archiveContent}" default-value="content"
     */
    private String archiveContent;

    /**
     * Define the start order for content bundles.
     *   none - no start orders are added
     *   dependencies - start order based on pom dependency order
     *
     * @parameter expression="${startOrder}" default-value="none"
     */
    private String startOrder;

    private File buildDir;

    /**
     * add the dependencies to the archive depending on the configuration of <archiveContent />
     */
    private void addDependenciesToArchive() throws MojoExecutionException {
        try
        {
            Set<Artifact> artifacts = null;
            switch (EsaContent.valueOf(archiveContent)) {
            case none:
                getLog().info("archiveContent=none: subsystem archive will not contain any bundles.");                  
                break;
            case content:
                // only include the direct dependencies in the archive
                artifacts = project.getDependencyArtifacts();                   
                break;
            case all:
                // include direct and transitive dependencies in the archive
                artifacts = project.getArtifacts();                 
                break;
            default:
                throw new MojoExecutionException("Invalid configuration for <archiveContent/>.  Valid values are none, content and all." );                    
            }
              
            if (artifacts != null) {
                // Explicitly add self to bundle set (used when pom packaging
                // type != "esa" AND a file is present (no point to add to
                // zip archive without file)
                final Artifact selfArtifact = project.getArtifact();
                if (!"esa".equals(selfArtifact.getType()) && selfArtifact.getFile() != null) {
                    getLog().info("Explicitly adding artifact[" + selfArtifact.getGroupId() + ", " + selfArtifact.getId() + ", " + selfArtifact.getScope() + "]");
                    artifacts.add(project.getArtifact());
                }
                
                artifacts = selectArtifacts(artifacts);
                int cnt = 0;
                for (Artifact artifact : artifacts) {

                    if (!artifact.isOptional() /*&& filter.include(artifact)*/) {
                        getLog().info("Copying artifact[" + artifact.getGroupId() + ", " + artifact.getId() + ", " +
                                artifact.getScope() + "]");
                        zipArchiver.addFile(artifact.getFile(), artifact.getArtifactId() + "-" + artifact.getVersion() + "." + (artifact.getType() == null ? "jar" : artifact.getType()));
                        cnt++;
                    }
                }               
                getLog().info(String.format("Added %s artifacts to subsystem subsystem archive.", cnt));
            }
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error copying esa dependencies", e );
        }

    }
    
    /**
     * 
     * Copies source files to the esa
     * 
     * @throws MojoExecutionException
     */
    private void copyEsaSourceFiles() throws MojoExecutionException {
        try
        {
            File esaSourceDir = esaSourceDirectory;
            if ( esaSourceDir.exists() )
            {
                getLog().info( "Copy esa resources to " + getBuildDir().getAbsolutePath() );

                DirectoryScanner scanner = new DirectoryScanner();
                scanner.setBasedir( esaSourceDir.getAbsolutePath() );
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

                    File file = new File( esaSourceDir, files[j] );
                    FileUtils.copyFileToDirectory( file, targetFile.getParentFile() );
                }
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error copying esa resources", e );
        }
    }
    
    private void includeCustomManifest() throws MojoExecutionException {
        try
        {
            if (!generateManifest) {
                includeCustomSubsystemManifestFile();
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying SUBSYSTEM.MF file", e );
        }
    }
    
    private void generateSubsystemManifest() throws MojoExecutionException {
        if (generateManifest) {
            String fileName = new String(getBuildDir() + "/"
                    + SUBSYSTEM_MF_URI);
            File appMfFile = new File(fileName);

            try {
                // Delete any old manifest
                if (appMfFile.exists()) {
                    FileUtils.fileDelete(fileName);
                }

                appMfFile.getParentFile().mkdirs();
                if (appMfFile.createNewFile()) {
                    writeSubsystemManifest(fileName);
                }
            } catch (java.io.IOException e) {
                throw new MojoExecutionException(
                        "Error generating SUBSYSTEM.MF file: " + fileName, e);
            }
        }
        
        // Check the manifest exists
        File ddFile = new File( getBuildDir(), SUBSYSTEM_MF_URI);
        if ( !ddFile.exists() )
        {
            getLog().warn(
                "Subsystem manifest: " + ddFile.getAbsolutePath() + " does not exist." );
        }

    }
    
    private void addMavenDescriptor() throws MojoExecutionException {
        try {

            if (addMavenDescriptor) {
                if (project.getArtifact().isSnapshot()) {
                    project.setVersion(project.getArtifact().getVersion());
                }

                String groupId = project.getGroupId();

                String artifactId = project.getArtifactId();

                zipArchiver.addFile(project.getFile(), "META-INF/maven/"
                        + groupId + "/" + artifactId + "/pom.xml");
                PomPropertiesUtil pomPropertiesUtil = new PomPropertiesUtil();
                File dir = new File(project.getBuild().getDirectory(),
                        "maven-zip-plugin");
                File pomPropertiesFile = new File(dir, "pom.properties");
                pomPropertiesUtil.createPomProperties(project, zipArchiver,
                        pomPropertiesFile, forceCreation);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error assembling esa", e);
        }
    }
    
    
    private void init() {
        getLog().debug( " ======= esaMojo settings =======" );
        getLog().debug( "esaSourceDirectory[" + esaSourceDirectory + "]" );
        getLog().debug( "manifestFile[" + manifestFile + "]" );
        getLog().debug( "subsystemManifestFile[" + subsystemManifestFile + "]" );
        getLog().debug( "workDirectory[" + workDirectory + "]" );
        getLog().debug( "outputDirectory[" + outputDirectory + "]" );
        getLog().debug( "finalName[" + finalName + "]" );
        getLog().debug( "generateManifest[" + generateManifest + "]" );

        if (archiveContent == null) {
            archiveContent = new String("content");
        }
        
        getLog().debug( "archiveContent[" + archiveContent + "]" );        
        getLog().info( "archiveContent[" + archiveContent + "]" );        
        
        zipArchiver.setIncludeEmptyDirs( includeEmptyDirs );
        zipArchiver.setCompress( true );
        zipArchiver.setForced( forceCreation );        
    }
    

    private void writeSubsystemManifest(String fileName)
            throws MojoExecutionException {
        try {
            // TODO: add support for dependency version ranges. Need to pick
            // them up from the pom and convert them to OSGi version ranges.
            FileUtils.fileAppend(fileName, SUBSYSTEM_MANIFESTVERSION + ": " + "1" + "\n");
            FileUtils.fileAppend(fileName, SUBSYSTEM_SYMBOLICNAME + ": "
                    + getSubsystemSymbolicName(project.getArtifact()) + "\n");
            FileUtils.fileAppend(fileName, SUBSYSTEM_VERSION + ": "
                    + getSubsystemVersion() + "\n");
            FileUtils.fileAppend(fileName, SUBSYSTEM_NAME + ": " + project.getName() + "\n");
            FileUtils.fileAppend(fileName, SUBSYSTEM_DESCRIPTION + ": "
                    + project.getDescription() + "\n");

            // Write the SUBSYSTEM-CONTENT
            // TODO: check that the dependencies are bundles (currently, the converter
            // will throw an exception)
            Set<Artifact> artifacts = null;
            // only include the direct dependencies in the content
            artifacts = project.getDependencyArtifacts();                   
            
            artifacts = selectArtifacts(artifacts);
            Iterator<Artifact> iter = artifacts.iterator();

            FileUtils.fileAppend(fileName, SUBSYSTEM_CONTENT + ": ");
            int order = 1;
            if (iter.hasNext()) {
                Artifact artifact = iter.next(); 
                String entry = new String(
                        maven2OsgiConverter.getBundleSymbolicName(artifact)
                        + ";version=\""
                        + Analyzer.cleanupVersion(artifact.getVersion())
                        + "\"");
                if ("dependencies".equals(startOrder)) {
                    entry += ";start-order=\"" + order + "\"";                  
                }
                FileUtils.fileAppend(fileName, entry);
            }
            while (iter.hasNext()) {
                Artifact artifact = iter.next();
                order++;
                String entry = new String(",\n "
                        + maven2OsgiConverter.getBundleSymbolicName(artifact)
                        + ";version=\""
                        + Analyzer.cleanupVersion(artifact.getVersion())
                        + "\"");
                if ("dependencies".equals(startOrder)) {
                    entry += ";start-order=\"" + order + "\"";                  
                }
                FileUtils.fileAppend(fileName, entry);
            }

            FileUtils.fileAppend(fileName, "\n");

            Iterator<Map.Entry<?, ?>> instructionIter = instructions.entrySet().iterator();
            while(instructionIter.hasNext()) {
                Map.Entry<?, ?> entry = instructionIter.next();
                String header = entry.getKey().toString();
                if (SKIP_INSTRUCTIONS.contains(header)) {
                    continue;
                }
                getLog().debug("Adding header: " + header);
                FileUtils.fileAppend(fileName, header + ": " + entry.getValue() + "\n");
            }

        } catch (Exception e) {
            throw new MojoExecutionException(
                    "Error writing dependencies into SUBSYSTEM.MF", e);
        }

    }

    // The maven2OsgiConverter assumes the artifact is a jar so we need our own
    // This uses the same fallback scheme as the converter
    private String getSubsystemSymbolicName(Artifact artifact) {
        if (instructions.containsKey(SUBSYSTEM_SYMBOLICNAME)) {
            return instructions.get(SUBSYSTEM_SYMBOLICNAME).toString();
        }
        return artifact.getGroupId() + "." + artifact.getArtifactId();
    }
    
    private String getSubsystemVersion() {
        if (instructions.containsKey(SUBSYSTEM_VERSION)) {
            return instructions.get(SUBSYSTEM_VERSION).toString();
        }
        return aQute.lib.osgi.Analyzer.cleanupVersion(project.getVersion());
    }
    
    private File getBuildDir() {
        if (buildDir == null) {
            buildDir = new File(workDirectory);
        }
        return buildDir;
    }
    
    private void addBuildDir() throws MojoExecutionException {
        try {
            if (buildDir.isDirectory()) {
                zipArchiver.addDirectory(buildDir);
            }

        } catch (Exception e) {
            throw new MojoExecutionException(
                    "Error writing dependencies into SUBSYSTEM.MF", e);
        }
    }

    private void includeCustomSubsystemManifestFile()
        throws IOException
    {
        if (subsystemManifestFile == null) {
            throw new NullPointerException("Subsystem manifest file location not set.  Use <generateManifest>true</generateManifest> if you want it to be generated.");
        }
        File appMfFile = subsystemManifestFile;
        if (appMfFile.exists()) {
            getLog().info( "Using SUBSYSTEM.MF "+ subsystemManifestFile);
            File osgiInfDir = new File(getBuildDir(), "OSGI-INF");
            FileUtils.copyFileToDirectory( appMfFile, osgiInfDir);
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

    private void includeSharedResources() throws MojoExecutionException {
        try
        {
            //include legal files if any
            File sharedResourcesDir = new File(sharedResources);
            if (sharedResourcesDir.isDirectory()) {
                zipArchiver.addDirectory(sharedResourcesDir);
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error assembling esa", e );
        }
    }

    /**
     * Creates the final archive.
     * 
     * @throws MojoExecutionException
     */
    private void createEsaFile() throws MojoExecutionException {
        try
        {
            File esaFile = new File( outputDirectory, finalName + ".esa" );
            zipArchiver.setDestFile(esaFile);


            zipArchiver.createArchive();

            project.getArtifact().setFile( esaFile );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error assembling esa", e );
        }

    }
    
    public void execute()
        throws MojoExecutionException
    {
        init();

        addDependenciesToArchive();
        
        copyEsaSourceFiles();

        includeCustomManifest();
        
        generateSubsystemManifest();

        addMavenDescriptor();
        
        addBuildDir();
        
        includeSharedResources();
        
        createEsaFile();
    }
}
