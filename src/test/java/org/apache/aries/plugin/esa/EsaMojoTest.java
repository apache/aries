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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import aQute.lib.osgi.Analyzer;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.archiver.zip.ZipEntry;
import org.codehaus.plexus.archiver.zip.ZipFile;

/**
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 */
public class EsaMojoTest
    extends AbstractMojoTestCase
{
    public void testEsaTestEnvironment()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-esa-test/plugin-config.xml" );

        EsaMojo mojo = ( EsaMojo ) lookupMojo( "esa", testPom );

        assertNotNull( mojo );
    }

    public void testBasicEsa()
        throws Exception
    {
        testBasicEsa( "target/test-classes/unit/basic-esa-test/plugin-config.xml", null );
    }

    public void testBasicEsaPgkType()
        throws Exception
    {
        testBasicEsa( "target/test-classes/unit/basic-esa-test-with-pgk-type/plugin-config.xml", "maven-esa-test-1.0-SNAPSHOT.jar" );
    }

    private void testBasicEsa(String path, String extraExpectedFiles)
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 path );

        EsaMojo mojo = ( EsaMojo ) lookupMojo( "esa", testPom );

        assertNotNull( mojo );

        String finalName = ( String ) getVariableValueFromObject( mojo, "finalName" );

        String workDir = ( String ) getVariableValueFromObject( mojo, "workDirectory" );

        String outputDir = ( String ) getVariableValueFromObject( mojo, "outputDirectory" );

        mojo.execute();

        //check the generated esa file
        File esaFile = new File( outputDir, finalName + ".esa" );

        assertTrue( esaFile.exists() );

        //expected files/directories inside the esa file
        List expectedFiles = new ArrayList();

        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-esa-test/pom.properties" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-esa-test/pom.xml" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-esa-test/" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/" );
        expectedFiles.add( "META-INF/maven/" );
        expectedFiles.add( "META-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );
        if (extraExpectedFiles != null)
        {
            expectedFiles.add( extraExpectedFiles );
        }

        ZipFile esa = new ZipFile( esaFile );

        Enumeration entries = esa.getEntries();

        assertTrue( entries.hasMoreElements() );

        assertTrue( entries.hasMoreElements() );

        int missing = getSizeOfExpectedFiles(entries, expectedFiles);
        assertEquals("Missing files: " + expectedFiles,  0, missing);
    }

    public void testBasicEsaWithDescriptor()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-esa-with-descriptor/plugin-config.xml" );

        EsaMojo mojo = ( EsaMojo ) lookupMojo( "esa", testPom );

        assertNotNull( mojo );

        String finalName = ( String ) getVariableValueFromObject( mojo, "finalName" );

        String workDir = ( String ) getVariableValueFromObject( mojo, "workDirectory" );

        String outputDir = ( String ) getVariableValueFromObject( mojo, "outputDirectory" );

        mojo.execute();

        //check the generated esa file
        File esaFile = new File( outputDir, finalName + ".esa" );

        assertTrue( esaFile.exists() );

        //expected files/directories inside the esa file
        List expectedFiles = new ArrayList();

        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-esa-test/pom.properties" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-esa-test/pom.xml" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-esa-test/" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/" );
        expectedFiles.add( "META-INF/maven/" );
        expectedFiles.add( "META-INF/" );
        expectedFiles.add( "OSGI-INF/SUBSYSTEM.MF" );
        expectedFiles.add( "OSGI-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );

        ZipFile esa = new ZipFile( esaFile );

        Enumeration entries = esa.getEntries();

        assertTrue( entries.hasMoreElements() );

        assertTrue( entries.hasMoreElements() );

        int missing = getSizeOfExpectedFiles(entries, expectedFiles);
        assertEquals("Missing files: " + expectedFiles,  0, missing);
    }

    public void testBasicEsaWithManifest()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-esa-with-manifest/plugin-config.xml" );

        EsaMojo mojo = ( EsaMojo ) lookupMojo( "esa", testPom );

        assertNotNull( mojo );

        String finalName = ( String ) getVariableValueFromObject( mojo, "finalName" );

        String workDir = ( String ) getVariableValueFromObject( mojo, "workDirectory" );

        String outputDir = ( String ) getVariableValueFromObject( mojo, "outputDirectory" );

        mojo.execute();


        //check the generated esa file
        File esaFile = new File( outputDir, finalName + ".esa" );

        assertTrue( esaFile.exists() );

        //expected files/directories inside the esa file
        List expectedFiles = new ArrayList();

        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-esa-test/pom.properties" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-esa-test/pom.xml" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-esa-test/" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/" );
        expectedFiles.add( "META-INF/maven/" );
        expectedFiles.add( "META-INF/" );
        expectedFiles.add( "META-INF/MANIFEST.MF" );
        expectedFiles.add( "OSGI-INF/SUBSYSTEM.MF" );
        expectedFiles.add( "OSGI-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );

        ZipFile esa = new ZipFile( esaFile );

        Enumeration entries = esa.getEntries();

        assertTrue( entries.hasMoreElements() );

        int missing = getSizeOfExpectedFiles(entries, expectedFiles);
        assertEquals("Missing files: " + expectedFiles,  0, missing);
    }

    private Manifest getSubsystemManifest(ZipFile esa) throws Exception {
        ZipEntry entry = esa.getEntry("OSGI-INF/SUBSYSTEM.MF");

        InputStream in = esa.getInputStream(entry);
        Manifest mf = new Manifest(in);

        return mf;
    }

    private Map<String, Map<String, String>> getHeader(Manifest mf, String header) {
        Attributes attributes = mf.getMainAttributes();
        String value = attributes.getValue(header);
        assertNotNull("Header " + header + " not found", value);
        return Analyzer.parseHeader(value, null);
    }

    private void testForHeader(ZipFile esa, String header, String exactEntry) throws Exception {

        Enumeration entries = esa.getEntries();


        // Test Use-Bundle & Subsytem-Type inclusion
        ZipEntry entry = esa.getEntry("OSGI-INF/SUBSYSTEM.MF");
        BufferedReader br = new BufferedReader(new InputStreamReader(esa.getInputStream(entry)));

        Boolean foundHeader=false;

        String line;
        while ((line = br.readLine()) != null) {
            if (line.contains(header)) {
                assertEquals(exactEntry, line);
                foundHeader = true;
            }
        }
        assertTrue("Found " + header + ":", foundHeader);

    }

    public void testSubsystemManifestGeneration()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-esa-without-manifest/plugin-config.xml" );

        EsaMojo mojo = ( EsaMojo ) lookupMojo( "esa", testPom );

        assertNotNull( mojo );

        String finalName = ( String ) getVariableValueFromObject( mojo, "finalName" );

        String workDir = ( String ) getVariableValueFromObject( mojo, "workDirectory" );

        String outputDir = ( String ) getVariableValueFromObject( mojo, "outputDirectory" );

        mojo.execute();


        //check the generated esa file
        File esaFile = new File( outputDir, finalName + ".esa" );

        assertTrue( esaFile.exists() );

        //expected files/directories inside the esa file
        List expectedFiles = new ArrayList();

        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-esa-test/pom.properties" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-esa-test/pom.xml" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-esa-test/" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/" );
        expectedFiles.add( "META-INF/maven/" );
        expectedFiles.add( "META-INF/" );
        expectedFiles.add( "OSGI-INF/SUBSYSTEM.MF" );
        expectedFiles.add( "OSGI-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );

        ZipFile esa = new ZipFile( esaFile );

        Enumeration entries = esa.getEntries();

        assertTrue( entries.hasMoreElements() );

        int missing = getSizeOfExpectedFiles(entries, expectedFiles);
        assertEquals("Missing files: " + expectedFiles,  0, missing);

        // Test for the Use-Bundle header
        testForHeader(esa, "Use-Bundle", "Use-Bundle: org.apache.aries.test.Bundle;version=1.0.0-SNAPSHOT");

        // Test for the Subsystem-Type header
        testForHeader(esa, "Subsystem-Type", "Subsystem-Type: feature");
    }

    public void testSubsystemStartOrder()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-esa-start-order/plugin-config.xml" );

        EsaMojo mojo = ( EsaMojo ) lookupMojo( "esa", testPom );

        assertNotNull( mojo );

        String finalName = ( String ) getVariableValueFromObject( mojo, "finalName" );

        String workDir = ( String ) getVariableValueFromObject( mojo, "workDirectory" );

        String outputDir = ( String ) getVariableValueFromObject( mojo, "outputDirectory" );

        mojo.execute();


        //check the generated esa file
        File esaFile = new File( outputDir, finalName + ".esa" );

        assertTrue( esaFile.exists() );

        //expected files/directories inside the esa file
        List expectedFiles = new ArrayList();

        expectedFiles.add( "OSGI-INF/SUBSYSTEM.MF" );
        expectedFiles.add( "OSGI-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );

        ZipFile esa = new ZipFile( esaFile );

        Enumeration entries = esa.getEntries();

        assertTrue( entries.hasMoreElements() );

        int missing = getSizeOfExpectedFiles(entries, expectedFiles);
        assertEquals("Missing files: " + expectedFiles,  0, missing);

        Manifest mf = getSubsystemManifest(esa);
        Map<String, Map<String, String>> header = getHeader(mf, "Subsystem-Content");

        Map<String, String> attributes = null;

        attributes = header.get("maven-artifact01-1.0-SNAPSHOT");
        assertNotNull(attributes);
        assertEquals("[1.0.0.SNAPSHOT,1.0.0.SNAPSHOT]", attributes.get("version"));
        // start-order is actually a directive, shows up here as the name+":"
        assertEquals("1", attributes.get("start-order:"));
        assertNull(attributes.get("type"));

        attributes = header.get("maven-artifact02-1.0-SNAPSHOT");
        assertNotNull(attributes);
        assertEquals("[1.0.0.SNAPSHOT,1.0.0.SNAPSHOT]", attributes.get("version"));
        assertEquals("2", attributes.get("start-order:"));
        assertNull(attributes.get("type"));
    }


    public void testArchiveContentConfigurationNoBundles()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-esa-no-bundles/plugin-config.xml" );

        EsaMojo mojo = ( EsaMojo ) lookupMojo( "esa", testPom );

        assertNotNull( mojo );

        String finalName = ( String ) getVariableValueFromObject( mojo, "finalName" );

        String workDir = ( String ) getVariableValueFromObject( mojo, "workDirectory" );

        String outputDir = ( String ) getVariableValueFromObject( mojo, "outputDirectory" );

        mojo.execute();


        //check the generated esa file
        File esaFile = new File( outputDir, finalName + ".esa" );

        assertTrue( esaFile.exists() );

        //expected files/directories inside the esa file
        List expectedFiles = new ArrayList();

        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-esa-test/pom.properties" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-esa-test/pom.xml" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-esa-test/" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/" );
        expectedFiles.add( "META-INF/maven/" );
        expectedFiles.add( "META-INF/" );
        expectedFiles.add( "OSGI-INF/SUBSYSTEM.MF" );
        expectedFiles.add( "OSGI-INF/" );

        ZipFile esa = new ZipFile( esaFile );

        Enumeration entries = esa.getEntries();

        assertTrue( entries.hasMoreElements() );

        int missing = getSizeOfExpectedFiles(entries, expectedFiles);
        assertEquals("Missing files: " + expectedFiles,  0, missing);

    }

    public void testArchiveContentConfigurationSubsystemContentBundles()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-esa-content-bundles-only/plugin-config.xml" );

        EsaMojo mojo = ( EsaMojo ) lookupMojo( "esa", testPom );

        assertNotNull( mojo );

        String finalName = ( String ) getVariableValueFromObject( mojo, "finalName" );

        String workDir = ( String ) getVariableValueFromObject( mojo, "workDirectory" );

        String outputDir = ( String ) getVariableValueFromObject( mojo, "outputDirectory" );

        mojo.execute();


        //check the generated esa file
        File esaFile = new File( outputDir, finalName + ".esa" );

        assertTrue( esaFile.exists() );

        //expected files/directories inside the esa file
        List expectedFiles = new ArrayList();

        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-esa-test/pom.properties" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-esa-test/pom.xml" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-esa-test/" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/" );
        expectedFiles.add( "META-INF/maven/" );
        expectedFiles.add( "META-INF/" );
        expectedFiles.add( "OSGI-INF/SUBSYSTEM.MF" );
        expectedFiles.add( "OSGI-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );

        ZipFile esa = new ZipFile( esaFile );

        Enumeration entries = esa.getEntries();

        assertTrue( entries.hasMoreElements() );

        int missing = getSizeOfExpectedFiles(entries, expectedFiles);
        assertEquals("Missing files: " + expectedFiles,  0, missing);

    }

    public void testCustomInstructions()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-esa-custom-instructions/plugin-config.xml" );

        EsaMojo mojo = ( EsaMojo ) lookupMojo( "esa", testPom );

        assertNotNull( mojo );

        String finalName = ( String ) getVariableValueFromObject( mojo, "finalName" );

        String workDir = ( String ) getVariableValueFromObject( mojo, "workDirectory" );

        String outputDir = ( String ) getVariableValueFromObject( mojo, "outputDirectory" );

        mojo.execute();


        //check the generated esa file
        File esaFile = new File( outputDir, finalName + ".esa" );

        assertTrue( esaFile.exists() );

        //expected files/directories inside the esa file
        List expectedFiles = new ArrayList();

        expectedFiles.add( "OSGI-INF/SUBSYSTEM.MF" );
        expectedFiles.add( "OSGI-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );

        ZipFile esa = new ZipFile( esaFile );

        Enumeration entries = esa.getEntries();

        assertTrue( entries.hasMoreElements() );

        int missing = getSizeOfExpectedFiles(entries, expectedFiles);
        assertEquals("Missing files: " + expectedFiles,  0, missing);

        // Test for the Foo header
        testForHeader(esa, "Foo", "Foo: bar");

        // Test for the MyHeader header
        testForHeader(esa, "MyHeader", "MyHeader: myValue");

        // Test for the Subsystem-Name header
        testForHeader(esa, "Subsystem-Name", "Subsystem-Name: myName");
    }

    public void testSubsystemContentType()
        throws Exception
    {
        File testPom = new File(getBasedir(),
                "target/test-classes/unit/basic-esa-content-type/plugin-config.xml");

        EsaMojo mojo = (EsaMojo) lookupMojo("esa", testPom);

        assertNotNull(mojo);

        String finalName = (String) getVariableValueFromObject(mojo, "finalName");

        String workDir = (String) getVariableValueFromObject(mojo, "workDirectory");

        String outputDir = (String) getVariableValueFromObject(mojo, "outputDirectory");

        mojo.execute();

        // check the generated esa file
        File esaFile = new File(outputDir, finalName + ".esa");

        assertTrue(esaFile.exists());

        // expected files/directories inside the esa file
        List expectedFiles = new ArrayList();

        expectedFiles.add("OSGI-INF/SUBSYSTEM.MF");
        expectedFiles.add("OSGI-INF/");
        expectedFiles.add("maven-artifact01-1.0-SNAPSHOT.jar");
        expectedFiles.add("maven-artifact02-1.0-SNAPSHOT.jar");
        expectedFiles.add("maven-artifact03-1.1-SNAPSHOT.jar");

        ZipFile esa = new ZipFile(esaFile);

        Enumeration entries = esa.getEntries();

        assertTrue(entries.hasMoreElements());

        int missing = getSizeOfExpectedFiles(entries, expectedFiles);
        assertEquals("Missing files: " + expectedFiles, 0, missing);

        Manifest mf = getSubsystemManifest(esa);
        Map<String, Map<String, String>> header = getHeader(mf, "Subsystem-Content");

        Map<String, String> attributes = null;

        attributes = header.get("maven-artifact01-1.0-SNAPSHOT");
        assertNotNull(attributes);
        assertEquals("[1.0.0.SNAPSHOT,1.0.0.SNAPSHOT]", attributes.get("version"));
        assertNull(attributes.get("type"));

        attributes = header.get("maven-artifact02-1.0-SNAPSHOT");
        assertNotNull(attributes);
        assertEquals("[1.0.0.SNAPSHOT,1.0.0.SNAPSHOT]", attributes.get("version"));
        assertNull(attributes.get("type"));

        attributes = header.get("maven-artifact03");
        assertNotNull(attributes);
        assertEquals("[1.1.0.SNAPSHOT.NNN,1.1.0.SNAPSHOT.NNN]", attributes.get("version"));
        assertEquals("osgi.fragment", attributes.get("type"));

        attributes = header.get("maven-artifact04");
        assertNotNull(attributes);
        assertEquals("[1.2.0.SNAPSHOT,1.2.0.SNAPSHOT]", attributes.get("version"));
        assertEquals("feature", attributes.get("type"));
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
