package org.apache.aries.subsystem.core.internal.filesystem;

import java.io.InputStream;

import org.apache.aries.util.filesystem.ICloseableDirectory;

public class UnpackingFileSystem {

	public static ICloseableDirectory getFSRoot(InputStream is) {
		return UnpackingFileSystemImpl.getFSRoot(is);
	}
}
