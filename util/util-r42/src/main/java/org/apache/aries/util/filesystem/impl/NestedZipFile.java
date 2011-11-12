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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;

public class NestedZipFile implements IFile {
	private final String name;
	private final long size;
	private final long lastModified;
	private final IDirectory parent;
	protected final IFile archive;
	private final String nameInZip;
	protected final NestedCloseableDirectory cache;
	
	/**
	 * Construct a nested zip file
	 * @param archive
	 * @param entry
	 * @param parent
	 */
	public NestedZipFile(IFile archive, ZipEntry entry, NestedZipDirectory parent, NestedCloseableDirectory cache) {
		this.archive = archive;
		this.parent = parent;
		this.nameInZip = entry.getName();

		name = archive.getName() + "/" + (nameInZip.endsWith("/") ? nameInZip.substring(0, nameInZip.length()-1) : nameInZip);
		size = entry.getSize();
		lastModified = entry.getTime();
		this.cache = cache;
	}
	
	public NestedZipFile(IFile archive, String pathInZip, NestedZipDirectory parent, NestedCloseableDirectory cache) {
		this.archive = archive;
		this.parent = parent;
		this.nameInZip = pathInZip;

		name = archive.getName() + "/" + (nameInZip.endsWith("/") ? nameInZip.substring(0, nameInZip.length()-1) : nameInZip);
		size = -1;
		lastModified = -1;
		this.cache = cache;
	}
	
	
	public NestedZipFile(IFile archive) {
		this.archive = archive;
		this.parent = archive.getParent();
		this.nameInZip = "";

		name = archive.getName();
		lastModified = archive.getLastModified();
		size = archive.getSize();
		cache = null;
	}
	
	public NestedZipFile(NestedZipFile other, NestedCloseableDirectory cache) {
		name = other.name;
		size = other.size;
		lastModified = other.lastModified;
		parent = other.parent;
		archive = other.archive;
		nameInZip = other.nameInZip;
		
		this.cache = cache;
	}
	
	public String getNameInZip() {
		return nameInZip;
	}
	
	public String getName() {
		return name;
	}

	public boolean isDirectory() {
		return false;
	}

	public boolean isFile() {
		return true;
	}

	public long getLastModified() {
		return lastModified;
	}

	public long getSize() {
		return size;
	}

	public IDirectory convert() {
		return null;
	}

	public IDirectory convertNested() {
		if (isDirectory()) return convert();
		else if (FileSystemImpl.isValidZip(this)) return new NestedZipDirectory(this);
		else return null;
	}

	public IDirectory getParent() {
		return parent;
	}

	public InputStream open() throws IOException, UnsupportedOperationException {
		if (cache != null && !!!cache.isClosed()) {
			ZipFile zip = cache.getZipFile();
			ZipEntry ze = zip.getEntry(nameInZip);
			
			if (ze != null) return zip.getInputStream(ze);
			else return null;
		} else {
			final ZipInputStream zis = new ZipInputStream(archive.open());
			
			ZipEntry entry = zis.getNextEntry();
			while (entry != null && !!!entry.getName().equals(nameInZip)) {
				entry = zis.getNextEntry();
			}
			
			if (entry != null) {
				return zis;
			} else {
				zis.close();
				return null;
			}
		}
	}

	public IDirectory getRoot() {
		return archive.getRoot();
	}

	public URL toURL() throws MalformedURLException
	{
		if (nameInZip.length() == 0) return archive.toURL();
		else {
			String entryURL = "jar:" + archive.toURL() + "!/" + nameInZip;
			return new URL(entryURL);
		}
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null) return false;
		if (obj == this) return true;

		if (obj.getClass() == getClass()) {
			return toString().equals(obj.toString());
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	@Override
	public String toString()
	{
		if (nameInZip.length() == 0) return archive.toString();
		return archive.toString() + "/" + nameInZip;
	}

}
