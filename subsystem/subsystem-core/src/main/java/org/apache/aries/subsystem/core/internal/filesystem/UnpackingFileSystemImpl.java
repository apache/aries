package org.apache.aries.subsystem.core.internal.filesystem;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.aries.util.IORuntimeException;
import org.apache.aries.util.filesystem.ICloseableDirectory;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.impl.DirectoryImpl;
import org.apache.aries.util.filesystem.impl.ZipDirectory;

import com.google.common.io.Files;

public class UnpackingFileSystemImpl {

	private static final int BUFFER_SIZE = 4096;

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

	private static boolean isValidZip(File zip) {
		try {
			ZipFile zf = new ZipFile(zip);
			zf.close();
			return true;
		} catch (IOException e) {
			throw new IORuntimeException("Not a valid zip: "+zip, e);
		}
	}

	public static ICloseableDirectory getFSRoot(InputStream is) {
		File tempFile = null;
		try {
			tempFile = Files.createTempDir();
			unzip(is, tempFile.getAbsolutePath());
		} catch (IOException e1) {
			throw new IORuntimeException("IOException in IDirectory.getFSRoot", e1);
		}
		IDirectory dir = getFSRoot(tempFile, null);

		if(dir == null)
			return null;
		else
			return new CleanableClosableDirectory(dir, tempFile);

	}

	private static void unzip(InputStream stream, String directoryLocation) throws IOException {
		File directory = new File(directoryLocation);
		if (!directory.exists()) {
			directory.mkdir();
		}
		ZipInputStream zipInputStream = new ZipInputStream(stream);
		ZipEntry entry = zipInputStream.getNextEntry();
		while (entry != null) {
			String filePath = directoryLocation + File.separator + entry.getName();
			if (!entry.isDirectory()) {
				extractFile(zipInputStream, filePath);
			} else {
				File dir = new File(filePath);
				dir.mkdir();
			}
			zipInputStream.closeEntry();
			entry = zipInputStream.getNextEntry();
		}
		zipInputStream.close();
	}

	private static void extractFile(ZipInputStream zipInputStream, String path) throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));
		byte[] bytesIn = new byte[BUFFER_SIZE];
		int readByte = zipInputStream.read(bytesIn);
		while (readByte != -1) {
			bos.write(bytesIn, 0, readByte);
			readByte = zipInputStream.read(bytesIn);
		}
		bos.close();
	}
}