package org.apache.aries.util.filesystem.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;
import org.apache.aries.util.filesystem.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemImpl {

	private static final Logger _logger = LoggerFactory.getLogger(FileSystemImpl.class.getName());

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
					_logger.error ("IOException in IDirectory.getFSRoot", e);
				}
			}
		}
		else {
			// since this method does not throw an exception but just returns null, make sure we do not lose the error
			_logger.error("File not found in IDirectory.getFSRoot", new FileNotFoundException(fs.getPath()));
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
			_logger.debug("Not a valid zip: "+zip);
			return false;
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
			_logger.debug("Not a valid zip: "+zip);
			return false;
		} finally {
			IOUtils.close(zis);
		}
	}
}
