/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.util.filesystem;

import java.io.InputStream;

import org.apache.aries.util.filesystem.impl.UnpackingFileSystemImpl;

/**
 * An abstraction of a zip file system where the content is unpacked to a temporary location.
 */
public class UnpackingFileSystem {

	/**
	 * This method gets an ICloseableDirectory that represents the root of a virtual file
	 * system. The provided InputStream should represent a zip file.
	 *
	 * When this {@link ICloseableDirectory} is closed then backing resources will be
	 * cleaned up.
	 *
	 * @param is An input stream to a zip file.
	 * @return   the root of the virtual FS.
	 */
	public static ICloseableDirectory getFSRoot(InputStream is) {
		return UnpackingFileSystemImpl.getFSRoot(is);
	}
}
