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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Iterator;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.archiver.zip.ZipEntry;
import org.codehaus.plexus.archiver.zip.ZipFile;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 */
public class EbaMojoTest
    extends AbstractMojoTestCase
{
    public void testEbaTestEnvironment()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-eba-test/plugin-config.xml" );

        EbaMojo mojo = ( EbaMojo ) lookupMojo( "eba", testPom );

        assertNotNull( mojo );
    }

    public void testBasicEba()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-eba-test/plugin-config.xml" );

        EbaMojo mojo = ( EbaMojo ) lookupMojo( "eba", testPom );

        assertNotNull( mojo );

        String finalName = ( String ) getVariableValueFromObject( mojo, "finalName" );

        String workDir = ( String ) getVariableValueFromObject( mojo, "workDirectory" );

        String outputDir = ( String ) getVariableValueFromObject( mojo, "outputDirectory" );

        Boolean includeJar = ( Boolean ) getVariableValueFromObject(  mojo, "includeJar" );

        assertTrue( includeJar.booleanValue() );

        //include the project jar to the eba
        File projectJar = new File( getBasedir(), "src/test/resources/unit/basic-eba-test/target/test-eba.jar" );

        FileUtils.copyFileToDirectory( projectJar, new File( outputDir ) );

        mojo.execute();

        //check the generated eba file
        File ebaFile = new File( outputDir, finalName + ".eba" );

        assertTrue( ebaFile.exists() );

        //expected files/directories inside the eba file
        List expectedFiles = new ArrayList();

        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/pom.properties" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/pom.xml" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/" );
        expectedFiles.add( "META-INF/maven/" );
//        expectedFiles.add( "META-INF/MANIFEST.MF" );
        expectedFiles.add( "META-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "test-eba.jar" );

        ZipFile eba = new ZipFile( ebaFile );

        Enumeration entries = eba.getEntries();

        assertTrue( entries.hasMoreElements() );

        assertTrue( entries.hasMoreElements() );

        int missing = getSizeOfExpectedFiles(entries, expectedFiles);
        assertEquals("Missing files: " + expectedFiles,  0, missing);
    }

    public void testBasicEbaWithDescriptor()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-eba-with-descriptor/plugin-config.xml" );

        EbaMojo mojo = ( EbaMojo ) lookupMojo( "eba", testPom );

        assertNotNull( mojo );

        String finalName = ( String ) getVariableValueFromObject( mojo, "finalName" );

        String workDir = ( String ) getVariableValueFromObject( mojo, "workDirectory" );

        String outputDir = ( String ) getVariableValueFromObject( mojo, "outputDirectory" );

        mojo.execute();

        //check the generated eba file
        File ebaFile = new File( outputDir, finalName + ".eba" );

        assertTrue( ebaFile.exists() );

        //expected files/directories inside the eba file
        List expectedFiles = new ArrayList();

        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/pom.properties" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/pom.xml" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/" );
        expectedFiles.add( "META-INF/maven/" );
//        expectedFiles.add( "META-INF/MANIFEST.MF" );
        expectedFiles.add( "META-INF/APPLICATION.MF" );
        expectedFiles.add( "META-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );

        ZipFile eba = new ZipFile( ebaFile );

        Enumeration entries = eba.getEntries();

        assertTrue( entries.hasMoreElements() );

        assertTrue( entries.hasMoreElements() );

        int missing = getSizeOfExpectedFiles(entries, expectedFiles);
        assertEquals("Missing files: " + expectedFiles,  0, missing);
    }

    public void testBasicEbaWithManifest()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-eba-with-manifest/plugin-config.xml" );

        EbaMojo mojo = ( EbaMojo ) lookupMojo( "eba", testPom );

        assertNotNull( mojo );

        String finalName = ( String ) getVariableValueFromObject( mojo, "finalName" );

        String workDir = ( String ) getVariableValueFromObject( mojo, "workDirectory" );

        String outputDir = ( String ) getVariableValueFromObject( mojo, "outputDirectory" );

        mojo.execute();


        //check the generated eba file
        File ebaFile = new File( outputDir, finalName + ".eba" );

        assertTrue( ebaFile.exists() );

        //expected files/directories inside the eba file
        List expectedFiles = new ArrayList();

        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/pom.properties" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/pom.xml" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/" );
        expectedFiles.add( "META-INF/maven/" );
        expectedFiles.add( "META-INF/MANIFEST.MF" );
        expectedFiles.add( "META-INF/APPLICATION.MF" );
        expectedFiles.add( "META-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );

        ZipFile eba = new ZipFile( ebaFile );

        Enumeration entries = eba.getEntries();

        assertTrue( entries.hasMoreElements() );

        int missing = getSizeOfExpectedFiles(entries, expectedFiles);
        assertEquals("Missing files: " + expectedFiles,  0, missing);
    }

    public void testApplicationManifestGeneration()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-eba-without-manifest/plugin-config.xml" );

        EbaMojo mojo = ( EbaMojo ) lookupMojo( "eba", testPom );

        assertNotNull( mojo );

        String finalName = ( String ) getVariableValueFromObject( mojo, "finalName" );

        String workDir = ( String ) getVariableValueFromObject( mojo, "workDirectory" );

        String outputDir = ( String ) getVariableValueFromObject( mojo, "outputDirectory" );

        mojo.execute();


        //check the generated eba file
        File ebaFile = new File( outputDir, finalName + ".eba" );

        assertTrue( ebaFile.exists() );

        //expected files/directories inside the eba file
        List expectedFiles = new ArrayList();

        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/pom.properties" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/pom.xml" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/" );
        expectedFiles.add( "META-INF/maven/" );
        expectedFiles.add( "META-INF/APPLICATION.MF" );
        expectedFiles.add( "META-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );

        ZipFile eba = new ZipFile( ebaFile );

        Enumeration entries = eba.getEntries();

        assertTrue( entries.hasMoreElements() );

        int missing = getSizeOfExpectedFiles(entries, expectedFiles);
        assertEquals("Missing files: " + expectedFiles,  0, missing);

	//Test Application-ImportService Application-ExportService and Use-Bundle inclusion
        ZipEntry entry = eba.getEntry("META-INF/APPLICATION.MF");
        BufferedReader br = new BufferedReader(new InputStreamReader(eba.getInputStream(entry)));

        String appServiceExport = new String("Application-ExportService: test.ExportService");
        String appServiceImport = new String("Application-ImportService: test.ImportService");
        String useBundle = new String("Use-Bundle: org.apache.aries.test.Bundle;version=1.0.0-SNAPSHOT");
        Boolean foundAppExport=false;
        Boolean foundAppImport=false;
        Boolean foundUseBundle=false;
        
        String line;
        while ((line = br.readLine()) != null) {
        	if (line.contains(new String("Application-ExportService"))) {
        		assertEquals(appServiceExport, line);
        		foundAppExport = true;
        	}
        	if (line.contains(new String("Application-ImportService"))) {
        		assertEquals(appServiceImport, line);
        		foundAppImport = true;
        	}
        	if (line.contains(new String("Use-Bundle"))) {
        		assertEquals(useBundle, line);
        		foundUseBundle = true;
        	}
		}
        assertTrue("Found Application-ExportService:", foundAppExport);
        assertTrue("Found Application-ImportService:", foundAppImport);
        assertTrue("Found Use-Bundle:", foundUseBundle);
    }


    public void testArchiveContentConfigurationNoBundles()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-eba-no-bundles/plugin-config.xml" );

        EbaMojo mojo = ( EbaMojo ) lookupMojo( "eba", testPom );

        assertNotNull( mojo );

        String finalName = ( String ) getVariableValueFromObject( mojo, "finalName" );

        String workDir = ( String ) getVariableValueFromObject( mojo, "workDirectory" );

        String outputDir = ( String ) getVariableValueFromObject( mojo, "outputDirectory" );

        mojo.execute();


        //check the generated eba file
        File ebaFile = new File( outputDir, finalName + ".eba" );

        assertTrue( ebaFile.exists() );

        //expected files/directories inside the eba file
        List expectedFiles = new ArrayList();

        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/pom.properties" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/pom.xml" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/" );
        expectedFiles.add( "META-INF/maven/" );
        expectedFiles.add( "META-INF/APPLICATION.MF" );
        expectedFiles.add( "META-INF/" );

        ZipFile eba = new ZipFile( ebaFile );

        Enumeration entries = eba.getEntries();

        assertTrue( entries.hasMoreElements() );

        int missing = getSizeOfExpectedFiles(entries, expectedFiles);
        assertEquals("Missing files: " + expectedFiles,  0, missing);

    }

    public void testArchiveContentConfigurationApplicationContentBundles()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-eba-content-bundles-only/plugin-config.xml" );

        EbaMojo mojo = ( EbaMojo ) lookupMojo( "eba", testPom );

        assertNotNull( mojo );

        String finalName = ( String ) getVariableValueFromObject( mojo, "finalName" );

        String workDir = ( String ) getVariableValueFromObject( mojo, "workDirectory" );

        String outputDir = ( String ) getVariableValueFromObject( mojo, "outputDirectory" );

        mojo.execute();


        //check the generated eba file
        File ebaFile = new File( outputDir, finalName + ".eba" );

        assertTrue( ebaFile.exists() );

        //expected files/directories inside the eba file
        List expectedFiles = new ArrayList();

        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/pom.properties" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/pom.xml" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/" );
        expectedFiles.add( "META-INF/maven/" );
        expectedFiles.add( "META-INF/APPLICATION.MF" );
        expectedFiles.add( "META-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );

        ZipFile eba = new ZipFile( ebaFile );

        Enumeration entries = eba.getEntries();

        assertTrue( entries.hasMoreElements() );

        int missing = getSizeOfExpectedFiles(entries, expectedFiles);
        assertEquals("Missing files: " + expectedFiles,  0, missing);

    }

    public void testArchiveContentConfigurationAllBundles()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-eba-all-bundles/plugin-config.xml" );

        EbaMojo mojo = ( EbaMojo ) lookupMojo( "eba", testPom );

        assertNotNull( mojo );

        String finalName = ( String ) getVariableValueFromObject( mojo, "finalName" );

        String workDir = ( String ) getVariableValueFromObject( mojo, "workDirectory" );

        String outputDir = ( String ) getVariableValueFromObject( mojo, "outputDirectory" );

        mojo.execute();


        //check the generated eba file
        File ebaFile = new File( outputDir, finalName + ".eba" );

        assertTrue( ebaFile.exists() );

        //expected files/directories inside the eba file
        List expectedFiles = new ArrayList();

        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/pom.properties" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/pom.xml" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-eba-test/" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/" );
        expectedFiles.add( "META-INF/maven/" );
        expectedFiles.add( "META-INF/APPLICATION.MF" );
        expectedFiles.add( "META-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact03-1.0-SNAPSHOT.jar" );

        ZipFile eba = new ZipFile( ebaFile );

        Enumeration entries = eba.getEntries();

        assertTrue( entries.hasMoreElements() );

        int missing = getSizeOfExpectedFiles(entries, expectedFiles);
        assertEquals("Missing files: " + expectedFiles,  0, missing);

    }

    private int getSizeOfExpectedFiles( Enumeration entries, List expectedFiles )
    {
        while( entries.hasMoreElements() )
        {
            ZipEntry entry = ( ZipEntry ) entries.nextElement();

            if( expectedFiles.contains( entry.getName() ) )
            {
                expectedFiles.remove( entry.getName() );
                assertFalse( expectedFiles.contains( entry.getName() ) );
            }
            else
            {
                fail( entry.getName() + " is not included in the expected files" );
            }
        }
        return expectedFiles.size();
    }

}
