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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.aries.util.IORuntimeException;
import org.apache.aries.util.filesystem.ICloseableDirectory;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;
import org.apache.aries.util.io.IOUtils;

public class NestedZipDirectory extends NestedZipFile implements IDirectory {
	public NestedZipDirectory(IFile archive, ZipEntry entry, NestedZipDirectory parent, NestedCloseableDirectory cache) {
		super(archive, entry, parent, cache);
	}

	public NestedZipDirectory(IFile archive, String pathInZip, NestedZipDirectory parent, NestedCloseableDirectory cache) {
		super(archive, pathInZip, parent, cache);
	}

	public NestedZipDirectory(IFile archive) {
		super(archive);
	}

	public NestedZipDirectory(NestedZipDirectory other, NestedCloseableDirectory cache) {
		super(other, cache);
	}

	public IDirectory convert() {
		return this;
	}

	public Iterator<IFile> iterator() {
		return listFiles().iterator();
	}

	public List<IFile> listFiles() {
		return listFiles(false);
	}

	public List<IFile> listAllFiles() {
		return listFiles(true);
	}

	private List<IFile> listFiles(boolean includeFilesInNestedSubdirs) {
			Map<String, ZipEntry> entriesByName = new LinkedHashMap<String, ZipEntry>();
			for (ZipEntry entry : getAllEntries()) {
				if (ZipDirectory.isInDir(getNameInZip(), entry, includeFilesInNestedSubdirs)) {
					entriesByName.put(entry.getName(), entry);
				}
			}

			List<IFile> files = new ArrayList<IFile>();
			for (ZipEntry ze : entriesByName.values()) {
				NestedZipDirectory parent = includeFilesInNestedSubdirs ? buildParent(ze, entriesByName) : this;
				if (ze.isDirectory()) files.add(new NestedZipDirectory(archive, ze, parent, cache));
				else files.add(new NestedZipFile(archive, ze, parent, cache));
			}

			return files;
	}

	private List<? extends ZipEntry> getAllEntries() {
		if (cache != null && !!!cache.isClosed()) {
			return Collections.list(cache.getZipFile().entries());
		} else {
			ZipInputStream zis = null;
			try {
				zis = new ZipInputStream(archive.open());

				List<ZipEntry> result = new ArrayList<ZipEntry>();
				ZipEntry entry = zis.getNextEntry();
				while (entry != null) {
					result.add(entry);
					entry = zis.getNextEntry();
				}

				return result;
			} catch (IOException e) {
				throw new IORuntimeException("IOException reading nested ZipFile", e);
			} finally {
				IOUtils.close(zis);
			}
		}
	}

	private NestedZipDirectory buildParent(ZipEntry entry, Map<String,ZipEntry> entries) {
		NestedZipDirectory result = this;

		String path = entry.getName().substring(getNameInZip().length());
		String[] segments = path.split("/");

		if (segments != null && segments.length > 1) {
			StringBuilder entryPath = new StringBuilder(getNameInZip());
			for (int i=0; i<segments.length-1; i++) {
				String p = segments[i];
				entryPath.append(p).append("/");
				ZipEntry ze = entries.get(entryPath.toString());

				if (ze != null) {
					result = new NestedZipDirectory(archive, ze, result, cache);
				} else {
					result = new NestedZipDirectory(archive, entryPath.toString(), result, cache);
				}
			}
		}

		return result;
	}

	public IFile getFile(String name) {
		Map<String,ZipEntry> entries = new HashMap<String, ZipEntry>();
		ZipEntry ze;

		if (cache != null && !!!cache.isClosed()) {
			ZipFile zip = cache.getZipFile();

			String[] segments = name.split("/");
			StringBuilder path = new StringBuilder();
			for (String s : segments) {
				path.append(s).append('/');
				ZipEntry p = zip.getEntry(path.toString());
				if (p != null) entries.put(path.toString(), p);
			}

			ze = zip.getEntry(name);

		} else {
			ZipInputStream zis = null;

			try {
				zis = new ZipInputStream(archive.open());

				ze = zis.getNextEntry();

				while (ze != null && !!!ze.getName().equals(name)) {
					if (name.startsWith(ze.getName())) entries.put(ze.getName(), ze);

					ze = zis.getNextEntry();
				}
			} catch (IOException e) {
				throw new IORuntimeException("IOException reading nested ZipFile", e);
			} finally {
				IOUtils.close(zis);
			}
		}

		if (ze != null) {
			NestedZipDirectory parent = buildParent(ze, entries);
			if (ze.isDirectory()) return new NestedZipDirectory(archive, ze, parent, cache);
			else return new NestedZipFile(archive, ze, parent, cache);
		} else {
			return null;
		}
	}


	public boolean isDirectory() {
		return true;
	}

	public InputStream open() throws IOException, UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public boolean isFile() {
		return false;
	}

	public boolean isRoot() {
		return false;
	}

	public ICloseableDirectory toCloseable() {
		try {
			return new NestedCloseableDirectory(archive, this);
		} catch (IOException e) {
			throw new IORuntimeException("Exception while creating extracted version of nested zip file", e);
		}
	}
}
