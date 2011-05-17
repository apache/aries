package org.apache.aries.application.utils.filesystem.impl;

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
import java.util.zip.ZipInputStream;

import org.apache.aries.application.filesystem.IDirectory;
import org.apache.aries.application.filesystem.IFile;
import org.apache.aries.application.utils.filesystem.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NestedZipDirectory extends NestedZipFile implements IDirectory {
	
	private static final Logger logger = LoggerFactory.getLogger("org.apache.aries.application.utils");
	
	public NestedZipDirectory(IFile archive, ZipEntry entry, NestedZipDirectory parent) {
		super(archive, entry, parent);
	}
	
	public NestedZipDirectory(IFile archive, String pathInZip, NestedZipDirectory parent) {
		super(archive, pathInZip, parent);
	}
	
	public NestedZipDirectory(IFile archive) {
		super(archive);
	}
	
	@Override
	public IDirectory convert() {
		return this;
	}

	@Override
	public Iterator<IFile> iterator() {
		return listFiles().iterator();
	}

	@Override
	public List<IFile> listFiles() {
		return listFiles(false);
	}

	@Override
	public List<IFile> listAllFiles() {
		return listFiles(true);
	}
	
	private List<IFile> listFiles(boolean includeFilesInNestedSubdirs) {
		ZipInputStream zis = null;
		
		try {
			zis = new ZipInputStream(archive.open());
			
			ZipEntry entry = zis.getNextEntry();
			
			Map<String, ZipEntry> entriesByName = new LinkedHashMap<String, ZipEntry>();
			while (entry != null) {
				if (ZipDirectory.isInDir(getNameInZip(), entry, includeFilesInNestedSubdirs)) {
					entriesByName.put(entry.getName(), entry);
				}
				entry = zis.getNextEntry();
			}

			List<IFile> files = new ArrayList<IFile>();
			for (ZipEntry ze : entriesByName.values()) {
				NestedZipDirectory parent = includeFilesInNestedSubdirs ? buildParent(ze, entriesByName) : this;
				if (ze.isDirectory()) files.add(new NestedZipDirectory(archive, ze, parent));
				else files.add(new NestedZipFile(archive, ze, parent));
			}
			
			return files;
			
		} catch (IOException e) {
			logger.error("IOException reading nested ZipFile", e);
			return Collections.emptyList();
		} finally {
			IOUtils.close(zis);
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
					result = new NestedZipDirectory(archive, ze, result);
				} else {
					result = new NestedZipDirectory(archive, entryPath.toString(), result);
				}
			}
		}
		
		return result;
	}

	@Override
	public IFile getFile(String name) {
		ZipInputStream zis = null;
		
		try {
			zis = new ZipInputStream(archive.open());
			
			Map<String,ZipEntry> entries = new HashMap<String, ZipEntry>();
			
			ZipEntry ze = zis.getNextEntry();
			
			while (ze != null && !!!ze.getName().equals(name)) {
				if (name.startsWith(ze.getName())) entries.put(ze.getName(), ze);
				
				ze = zis.getNextEntry();
			}
			
			if (ze != null) {
				NestedZipDirectory parent = buildParent(ze, entries);
				if (ze.isDirectory()) return new NestedZipDirectory(archive, ze, parent);
				else return new NestedZipFile(archive, ze, parent);
			} else {
				return null;
			}
			
		} catch (IOException e) {
			logger.error("IOException reading nested ZipFile", e);
			return null;
		} finally {
			IOUtils.close(zis);
		}
	}

	
	@Override
	public boolean isDirectory() {
		return true;
	}

	@Override
	public InputStream open() throws IOException, UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isFile() {
		return false;
	}

	@Override
	public boolean isRoot() {
		return false;
	}

}
