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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.zip.ZipOutputStream;

/**
 * The Ant task that will build the Enterprise Subsystem Archive, this task is
 * modeled based on the Ant {@link Jar} task
 * 
 * @version $Id: $
 */
public class EsaTask extends Zip {

	/* name of the subsystem */
	private String symbolicName;

	/* name of the subsystem */
	private String name;

	/* subsystem description */
	private String description;

	/* version of the subsystem */
	private String version;

	/* subsystem type */
	private String type = Constants.FEATURE_TYPE;

	/* the file holder of the esa manifest */
	private File manifestFile;

	/* Flag to indicate whether to generate manifest */
	protected boolean generateManifest;

	/* Used for dry runs */
	protected boolean skipWriting = false;

	/* Used build the subsystem content header */
	private StringBuilder subsystemContent = new StringBuilder(
		Constants.SUBSYSTEM_CONTENT + ":");

	public EsaTask() {

		super();
		archiveType = "esa";
		setEncoding("UTF8");
	}

	/**
	 * @param symbolicName
	 *            the symbolicName to set
	 */
	public void setSymbolicName(String symbolicName) {

		this.symbolicName = symbolicName;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {

		this.name = name;
	}

	/**
	 * @param version
	 *            the version to set
	 */
	public void setVersion(String subsystemVersion) {

		this.version = subsystemVersion;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(String subsystemType) {

		this.type = subsystemType;
	}

	/**
	 * @param manifest
	 *            file to use the manifest to set
	 */
	public void setManifest(File manifestFile) {

		if (!manifestFile.exists()) {
			throw new BuildException("Manifest:" + manifestFile +
				" does not exist", getLocation());
		}

		this.manifestFile = manifestFile;
	}

	/**
	 * @param generateManifest
	 *            the generateManifest to set
	 */
	public void setGenerateManifest(boolean generateManifest) {

		this.generateManifest = generateManifest;
	}

	/**
	 * @param skipWriting
	 *            the skipWriting to set
	 */
	public void setSkipWriting(boolean skipWriting) {

		this.skipWriting = skipWriting;
	}

	@Override
	protected void zipFile(
		File file, ZipOutputStream zOut, String vPath, int mode)
		throws IOException {

		super.zipFile(file, zOut, vPath, mode);

		/*
		 * this handling is only for OSGi bundles and we need to exclude other
		 * entries
		 */
		if (file.isFile() &&
			!Constants.SUBSYSTEM_MANIFEST_NAME.equalsIgnoreCase(vPath)) {

			JarFile bundleFile = new JarFile(file);

			Manifest jarManifest = bundleFile.getManifest();

			if (jarManifest != null) {

				Attributes mainAttributes = jarManifest.getMainAttributes();

				String bundleSymbolicName =
					mainAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME);

				String bundleVersion =
					mainAttributes.getValue(Constants.BUNDLE_VERSION);

				bundleFile.close();

				String strSubsystemContentEntry =
					bundleSymbolicName + ";version=" + bundleVersion;

				subsystemContent.append(strSubsystemContentEntry);
				subsystemContent.append(",");
			}
		}
	}

	@Override
	protected void initZipOutputStream(ZipOutputStream zOut)
		throws IOException, BuildException {

		if (manifestFile != null && !generateManifest) {

			zipDir(
				(Resource) null, zOut, Constants.OSGI_INF_PATH,
				ZipFileSet.DEFAULT_DIR_MODE, null);

			zipFile(
				manifestFile, zOut, Constants.SUBSYSTEM_MANIFEST_NAME,
				ZipFileSet.DEFAULT_FILE_MODE);
		}
	}

	@Override
	protected void finalizeZipOutputStream(ZipOutputStream zOut)
		throws IOException, BuildException {

		if (!skipWriting) {
			if (generateManifest) {
				addNewManifest(zOut);
			}
		}
	}

	/**
	 * This method will add the SUBSYSTEM.MF to the esa archieve
	 * 
	 * @param zOut
	 *            -the zip output stream
	 */
	private void addNewManifest(ZipOutputStream zOut) {

		try {

			log("Generating SUBSYSTEM.MF", Project.MSG_VERBOSE);

			zipDir(
				(Resource) null, zOut, Constants.OSGI_INF_PATH,
				ZipFileSet.DEFAULT_DIR_MODE, null);

			ByteArrayOutputStream bout = new ByteArrayOutputStream();

			OutputStreamWriter osWriter = new OutputStreamWriter(bout, "UTF-8");

			PrintWriter printWriter = new PrintWriter(osWriter);

			// Start writing manifest content
			printWriter.write(Constants.SUBSYSTEM_MANIFESTVERSION + ": " +
				Constants.SUBSYSTEM_MANIFEST_VERSION_VALUE + "\n");

			printWriter.write(Constants.SUBSYSTEM_SYMBOLICNAME + ": " +
				symbolicName + "\n");

			if (version == null) {
				version = "1.0.0";
			}

			printWriter.write(Constants.SUBSYSTEM_VERSION + ": " + version +
				"\n");

			if (name == null) {
				name = symbolicName;
			}

			printWriter.write(Constants.SUBSYSTEM_NAME + ": " + name + "\n");

			printWriter.write(Constants.SUBSYSTEM_TYPE + ": " + type + "\n");

			if (description != null) {
				printWriter.write(Constants.SUBSYSTEM_DESCRIPTION + ": " +
					description + "\n");
			}

			// Subsystem-content header

			String subsystemContentHeader = subsystemContent.toString();
			// strip the last ,
			subsystemContentHeader =
				subsystemContentHeader.substring(
					0, (subsystemContentHeader.length() - 1)) +
					"\n";

			printWriter.write(subsystemContentHeader);

			printWriter.close();

			ByteArrayInputStream bais =
				new ByteArrayInputStream(bout.toByteArray());
			try {
				super.zipFile(
					bais, zOut, Constants.SUBSYSTEM_MANIFEST_NAME,
					System.currentTimeMillis(), null,
					ZipFileSet.DEFAULT_FILE_MODE);
			}
			finally {
				// not really required
				FileUtils.close(bais);
			}
		}
		catch (IOException e) {
			log("Error generating manifest", Project.MSG_ERR);
		}
	}
}
