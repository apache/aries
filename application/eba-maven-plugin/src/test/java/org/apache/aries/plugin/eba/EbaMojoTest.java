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

import java.io.File;
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
        expectedFiles.add( "META-INF/MANIFEST.MF" );
        expectedFiles.add( "META-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "test-eba.jar" );

        ZipFile eba = new ZipFile( ebaFile );

        Enumeration entries = eba.getEntries();

        assertTrue( entries.hasMoreElements() );

        assertEquals( 0, getSizeOfExpectedFiles( entries, expectedFiles ) );
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
        expectedFiles.add( "META-INF/MANIFEST.MF" );
        expectedFiles.add( "META-INF/application.mf" );
        expectedFiles.add( "META-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );

        ZipFile eba = new ZipFile( ebaFile );

        Enumeration entries = eba.getEntries();

        assertTrue( entries.hasMoreElements() );

        assertEquals( 0, getSizeOfExpectedFiles( entries, expectedFiles ) );
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
        expectedFiles.add( "META-INF/application.mf" );
        expectedFiles.add( "META-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );

        ZipFile eba = new ZipFile( ebaFile );

        Enumeration entries = eba.getEntries();

        assertTrue( entries.hasMoreElements() );

        assertEquals( 0, getSizeOfExpectedFiles( entries, expectedFiles ) );
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

    private int getSizeOfExpectedFiles( List fileList, List expectedFiles )
    {
        for( Iterator iter=fileList.iterator(); iter.hasNext(); )
        {
            String fileName = ( String ) iter.next();

            if( expectedFiles.contains(  fileName ) )
            {
                expectedFiles.remove( fileName );
                assertFalse( expectedFiles.contains( fileName ) );
            }
            else
            {
                fail( fileName + " is not included in the expected files" );
            }
        }
        return expectedFiles.size();
    }

    private void addFileToList( File file, List fileList )
    {
        if( !file.isDirectory() )
        {
            fileList.add( file.getName() );
        }
        else
        {
            fileList.add( file.getName() );

            File[] files = file.listFiles();

            for( int i=0; i<files.length; i++ )
            {
                addFileToList( files[i], fileList );
            }
        }
    }
}
