/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Implementation for this class was based on org.apache.commons.io.FileUtils
 * from the Apache commons-io library which is licensed under Apache License, version 2
 * https://github.com/apache/commons-io
 *
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * The only modifications to the original code are accessibility changes of methods and
 * the removal of unused methods.
 *
 */
package org.apache.aries.util.filesystem.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;

class FileUtils {

	/**
	 * Deletes a directory recursively.
	 *
	 * @param directory directory to delete
	 * @throws IOException              in case deletion is unsuccessful
	 * @throws IllegalArgumentException if {@code directory} does not exist or is not a directory
	 */
	static void deleteDirectory(final File directory) throws IOException {
		if (!directory.exists()) {
			return;
		}

		if (!isSymlink(directory)) {
			cleanDirectory(directory);
		}

		if (!directory.delete()) {
			final String message =
					"Unable to delete directory " + directory + ".";
			throw new IOException(message);
		}
	}

	/**
	 * Cleans a directory without deleting it.
	 *
	 * @param directory directory to clean
	 * @throws IOException              in case cleaning is unsuccessful
	 * @throws IllegalArgumentException if {@code directory} does not exist or is not a directory
	 */
	private static void cleanDirectory(final File directory) throws IOException {
		final File[] files = verifiedListFiles(directory);

		IOException exception = null;
		for (final File file : files) {
			try {
				forceDelete(file);
			} catch (final IOException ioe) {
				exception = ioe;
			}
		}

		if (null != exception) {
			throw exception;
		}
	}

	/**
	 * Lists files in a directory, asserting that the supplied directory satisfies exists and is a directory
	 *
	 * @param directory The directory to list
	 * @return The files in the directory, never null.
	 * @throws IOException if an I/O error occurs
	 */
	private static File[] verifiedListFiles(File directory) throws IOException {
		if (!directory.exists()) {
			final String message = directory + " does not exist";
			throw new IllegalArgumentException(message);
		}

		if (!directory.isDirectory()) {
			final String message = directory + " is not a directory";
			throw new IllegalArgumentException(message);
		}

		final File[] files = directory.listFiles();
		if (files == null) {  // null if security restricted
			throw new IOException("Failed to list contents of " + directory);
		}
		return files;
	}

	//-----------------------------------------------------------------------

	/**
	 * Deletes a file. If file is a directory, delete it and all sub-directories.
	 * <p/>
	 * The difference between File.delete() and this method are:
	 * <ul>
	 * <li>A directory to be deleted does not have to be empty.</li>
	 * <li>You get exceptions when a file or directory cannot be deleted.
	 * (java.io.File methods returns a boolean)</li>
	 * </ul>
	 *
	 * @param file file or directory to delete, must not be {@code null}
	 * @throws NullPointerException  if the directory is {@code null}
	 * @throws FileNotFoundException if the file was not found
	 * @throws IOException           in case deletion is unsuccessful
	 */
	private static void forceDelete(final File file) throws IOException {
		if (file.isDirectory()) {
			deleteDirectory(file);
		} else {
			final boolean filePresent = file.exists();
			if (!file.delete()) {
				if (!filePresent) {
					throw new FileNotFoundException("File does not exist: " + file);
				}
				final String message =
						"Unable to delete file: " + file;
				throw new IOException(message);
			}
		}
	}

	/**
	 * Determines whether the specified file is a Symbolic Link rather than an actual file.
	 * <p/>
	 * Will not return true if there is a Symbolic Link anywhere in the path,
	 * only if the specific file is.
	 * <p/>
	 * When using jdk1.7, this method delegates to {@code boolean java.nio.file.Files.isSymbolicLink(Path path)}
	 * <p/>
	 * <b>Note:</b> the current implementation always returns {@code false} if running on
	 * jkd1.6 and the system is detected as Windows using {@link FilenameUtils#isSystemWindows()}
	 * <p/>
	 * For code that runs on Java 1.7 or later, use the following method instead:
	 * <br>
	 * {@code boolean java.nio.file.Files.isSymbolicLink(Path path)}
	 *
	 * @param file the file to check
	 * @return true if the file is a Symbolic Link
	 * @throws IOException if an IO error occurs while checking the file
	 * @since 2.0
	 */
	private static boolean isSymlink(final File file) throws IOException {
		if (Java7Support.isAtLeastJava7()) {
			return Java7Support.isSymLink(file);
		}

		if (file == null) {
			throw new NullPointerException("File must not be null");
		}
		if (FilenameUtils.isSystemWindows()) {
			return false;
		}
		File fileInCanonicalDir = null;
		if (file.getParent() == null) {
			fileInCanonicalDir = file;
		} else {
			final File canonicalDir = file.getParentFile().getCanonicalFile();
			fileInCanonicalDir = new File(canonicalDir, file.getName());
		}

		if (fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile())) {
			return isBrokenSymlink(file);
		} else {
			return true;
		}
	}

	/**
	 * Determines if the specified file is possibly a broken symbolic link.
	 *
	 * @param file the file to check
	 * @return true if the file is a Symbolic Link
	 * @throws IOException if an IO error occurs while checking the file
	 */
	private static boolean isBrokenSymlink(final File file) throws IOException {
		// if file exists then if it is a symlink it's not broken
		if (file.exists()) {
			return false;
		}
		// a broken symlink will show up in the list of files of its parent directory
		final File canon = file.getCanonicalFile();
		File parentDir = canon.getParentFile();
		if (parentDir == null || !parentDir.exists()) {
			return false;
		}

		// is it worthwhile to create a FileFilterUtil method for this?
		// is it worthwhile to create an "identity"  IOFileFilter for this?
		File[] fileInDir = parentDir.listFiles(
				new FileFilter() {
					public boolean accept(File aFile) {
						return aFile.equals(canon);
					}
				}
		);
		return fileInDir != null && fileInDir.length > 0;
	}
}