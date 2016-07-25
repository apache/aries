package org.apache.aries.subsystem.core.internal.filesystem;

import java.io.File;

import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.impl.CloseableDirectory;

public class CleanableClosableDirectory extends CloseableDirectory {

	private final File tempDirectory;

	public CleanableClosableDirectory(IDirectory delegate, File tempDirectory) {
		super(delegate);
		this.tempDirectory = tempDirectory;
	}

	protected void cleanup() {
		deleteRecursively(this.tempDirectory);
	}

	void deleteRecursively(File file) {
		if (file.isDirectory()) {
			for (File content : file.listFiles()) {
				deleteRecursively(content);
			}
		}
		file.delete();
	}

}
