/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.util.filesystem.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.aries.util.IORuntimeException;
import org.apache.aries.util.filesystem.ICloseableDirectory;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;
import org.apache.aries.util.io.IOUtils;

public class FileSystemImpl {
	/**
	 * This method gets the IDirectory that represents the root of a virtual file
	 * system. The provided file can either identify a directory, or a zip file.
	 *
	 * @param fs the zip file.
	 * @return   the root of the virtual FS.
	 */
	public static IDirectory getFSRoot(File fs, IDirectory parent)
	{
		IDirectory dir = null;

		if (fs.exists()) {
			if (fs.isDirectory()) {
				dir = new DirectoryImpl(fs, fs);
			} else if (fs.isFile() && isValidZip(fs)) {
				try {
					dir = new ZipDirectory(fs, parent);
				} catch (IOException e) {
					throw new IORuntimeException("IOException in IDirectory.getFSRoot", e);
				}
			}
		}
		else {
			throw new IORuntimeException("File not found in IDirectory.getFSRoot", new FileNotFoundException(fs.getPath()));
		}
		return dir;
	}

	/**
	 * Check whether a file is actually a valid zip
	 * @param zip
	 * @return
	 */
	public static boolean isValidZip(File zip) {
		try {
			ZipFile zf = new ZipFile(zip);
			zf.close();
			return true;
		} catch (IOException e) {
			throw new IORuntimeException("Not a valid zip: "+zip, e);
		}
	}

	/**
	 * Check whether a file is actually a valid zip
	 * @param zip
	 * @return
	 */
	public static boolean isValidZip(IFile zip) {
		ZipInputStream zis = null;
		try {
			// just opening the stream ain't enough, we have to check the first entry
			zis = new ZipInputStream(zip.open());
			return zis.getNextEntry() != null;
		} catch (IOException e) {
			throw new IORuntimeException("Not a valid zip: "+zip, e);
		} finally {
			IOUtils.close(zis);
		}
	}

  public static ICloseableDirectory getFSRoot(InputStream is) {
    File tempFile = null;
    try {
      tempFile = File.createTempFile("inputStreamExtract", ".zip");
    } catch (IOException e1) {
      throw new IORuntimeException("IOException in IDirectory.getFSRoot", e1);
    }
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(tempFile);
      IOUtils.copy(is, fos);
    } catch (IOException e) {
      return null;
    } finally {
      IOUtils.close(fos);
    }

    IDirectory dir = getFSRoot(tempFile, null);

    if(dir == null)
      return null;
    else
      return new InputStreamClosableDirectory(dir, tempFile);

  }
}
