package org.apache.aries.application.utils.filesystem.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.aries.application.filesystem.IDirectory;
import org.apache.aries.application.filesystem.IFile;

public class NestedZipFile implements IFile {
	private final String name;
	private final long size;
	private final long lastModified;
	private final IDirectory parent;
	protected final IFile archive;
	private final String nameInZip;
	
	/**
	 * Construct a nested zip file
	 * @param archive
	 * @param entry
	 * @param parent
	 */
	public NestedZipFile(IFile archive, ZipEntry entry, NestedZipDirectory parent) {
		this.archive = archive;
		this.parent = parent;
		this.nameInZip = entry.getName();

		name = archive.getName() + "/" + (nameInZip.endsWith("/") ? nameInZip.substring(0, nameInZip.length()-1) : nameInZip);
		size = entry.getSize();
		lastModified = entry.getTime();
	}
	
	public NestedZipFile(IFile archive, String pathInZip, NestedZipDirectory parent) {
		this.archive = archive;
		this.parent = parent;
		this.nameInZip = pathInZip;

		name = archive.getName() + "/" + (nameInZip.endsWith("/") ? nameInZip.substring(0, nameInZip.length()-1) : nameInZip);
		size = -1;
		lastModified = -1;
	}
	
	
	public NestedZipFile(IFile archive) {
		this.archive = archive;
		this.parent = archive.getParent();
		this.nameInZip = "";

		name = archive.getName();
		lastModified = archive.getLastModified();
		size = archive.getSize();
	}
	
	public String getNameInZip() {
		return nameInZip;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isDirectory() {
		return false;
	}

	@Override
	public boolean isFile() {
		return true;
	}

	@Override
	public long getLastModified() {
		return lastModified;
	}

	@Override
	public long getSize() {
		return size;
	}

	@Override
	public IDirectory convert() {
		return null;
	}

	@Override
	public IDirectory convertNested() {
		if (isDirectory()) return convert();
		else if (FileSystemImpl.isValidZip(this)) return new NestedZipDirectory(this);
		else return null;
	}

	@Override
	public IDirectory getParent() {
		return parent;
	}

	@Override
	public InputStream open() throws IOException, UnsupportedOperationException {
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

	@Override
	public IDirectory getRoot() {
		return archive.getRoot();
	}

	@Override
	public URL toURL() throws MalformedURLException
	{
		if (nameInZip.isEmpty()) return archive.toURL();
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
		if (nameInZip.isEmpty()) return archive.toString();
		return archive.toString() + "/" + nameInZip;
	}

}
