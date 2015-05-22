/**
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

package org.apache.aries.ant.taskdefs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.junit.Test;

/**
 * @version $Id: $
 */
public class EsaTaskTest {

	@Test
	public void generateArchiveNoManifest() {

		File srcDir = new File("../src/test/resources");

		File destfile = new File("target/esa-test1.esa");
		if (destfile.exists()) {
			destfile.delete();
		}

		assertFalse(destfile.exists());
		EsaTask esaTask = new EsaTask();
		Project testProject = new Project();
		esaTask.setProject(testProject);
		FileSet fileSet = new FileSet();
		fileSet.setDir(srcDir);
		fileSet.setIncludes("*.jar");
		esaTask.addFileset(fileSet);
		esaTask.setDestFile(destfile);
		esaTask.setSymbolicName("esatask-test");
		esaTask.setVersion("1.0.0");
		esaTask.execute();
		assertTrue(destfile.exists());

		try {
			ZipFile esaArchive = new ZipFile(destfile);
			assertNotNull(esaArchive);
			ZipEntry subsystemManifest =
				esaArchive.getEntry("OSGI-INF/SUBSYSTEM.MF");
			assertNull(subsystemManifest);
		}
		catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void generateArchiveWithFileManifest() {

		File srcDir = new File("../src/test/resources");

		File destfile = new File("target/esa-test2.esa");
		if (destfile.exists()) {
			destfile.delete();
		}

		assertFalse(destfile.exists());
		EsaTask esaTask = new EsaTask();
		Project testProject = new Project();
		esaTask.setProject(testProject);
		FileSet fileSet = new FileSet();
		fileSet.setDir(srcDir);
		fileSet.setIncludes("*.jar");
		esaTask.addFileset(fileSet);
		esaTask.setDestFile(destfile);
		esaTask.setSymbolicName("esatask-test");
		esaTask.setVersion("1.0.0");
		esaTask.setManifest(new File(srcDir, "SUBSYSTEM.MF"));
		esaTask.execute();
		assertTrue(destfile.exists());

		try {
			ZipFile esaArchive = new ZipFile(destfile);
			assertNotNull(esaArchive);
			ZipEntry subsystemManifest =
				esaArchive.getEntry("OSGI-INF/SUBSYSTEM.MF");
			assertNotNull(subsystemManifest);
		}
		catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void generateArchiveWithNewManifest() {

		File srcDir = new File("../src/test/resources");
		assertTrue(srcDir.exists());

		File destfile = new File("target/esa-test.esa");

		if (destfile.exists()) {
			destfile.delete();
		}

		assertFalse(destfile.exists());

		EsaTask esaTask = new EsaTask();

		Project testProject = new Project();

		esaTask.setProject(testProject);

		FileSet fileSet = new FileSet();
		fileSet.setDir(srcDir);
		fileSet.setIncludes("*.jar");

		esaTask.addFileset(fileSet);
		esaTask.setDestFile(destfile);
		esaTask.setSymbolicName("esatask-test");
		esaTask.setName("ESA Test Task");
		esaTask.setVersion("1.0.0");
		esaTask.setGenerateManifest(true);
		esaTask.execute();
		assertTrue(destfile.exists());

		try {
			ZipFile esaArchive = new ZipFile(destfile);
			assertNotNull(esaArchive);
			ZipEntry subsystemManifest =
				esaArchive.getEntry("OSGI-INF/SUBSYSTEM.MF");
			assertNotNull(subsystemManifest);
		}
		catch (IOException e) {
			fail(e.getMessage());
		}
	}
}
