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
package org.apache.aries.subsystem.core.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.ICloseableDirectory;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;
import org.osgi.framework.Bundle;
import org.osgi.service.subsystem.SubsystemException;

public class BundleDirectory implements IDirectory {
	private static class BundleFile implements IFile {
		private final BundleDirectory directory;
		private final String name;
		private final URL url;
		
		public BundleFile(String name, URL url, BundleDirectory directory) {
			this.name = name;
			this.url = url;
			this.directory = directory;
		}

		@Override
		public IDirectory convert() {
			return null;
		}

		@Override
		public IDirectory convertNested() {
			try {
				return FileSystem.getFSRoot(url.openStream());
			}
			catch (IOException e) {
				throw new SubsystemException(e);
			}
		}

		@Override
		public long getLastModified() {
			return 0;
		}

		@Override
		public String getName() {
			if (name.startsWith("/"))
				return name.substring(1);
			return name;
		}

		@Override
		public IDirectory getParent() {
			return directory;
		}

		@Override
		public IDirectory getRoot() {
			return directory;
		}

		@Override
		public long getSize() {
			return 0;
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
		public InputStream open() throws IOException,
		UnsupportedOperationException {
			return url.openStream();
		}

		@Override
		public URL toURL() throws MalformedURLException {
			return url;
		}
	}
	
	private final Bundle bundle;
	
	public BundleDirectory(Bundle bundle) {
		if (bundle == null)
			throw new NullPointerException();
		this.bundle = bundle;
	}
	
	@Override
	public Iterator<IFile> iterator() {
		return listAllFiles().iterator();
	}

	@Override
	public IDirectory convert() {
		return this;
	}

	@Override
	public IDirectory convertNested() {
		return this;
	}

	@Override
	public long getLastModified() {
		return 0;
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public IDirectory getParent() {
		return null;
	}

	@Override
	public IDirectory getRoot() {
		return this;
	}

	@Override
	public long getSize() {
		return 0;
	}

	@Override
	public boolean isDirectory() {
		return true;
	}

	@Override
	public boolean isFile() {
		return false;
	}

	@Override
	public InputStream open() throws IOException, UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public URL toURL() throws MalformedURLException {
		return bundle.getEntry("/");
	}

	@Override
	public IFile getFile(final String name) {
		if (name == null || name.length() == 0)
			return null;
		if ("/".equals(name))
			return this;
		URL entry = bundle.getEntry(name);
		if (entry == null)
			return null;
		return new BundleFile(name, entry, this);
	}

	@Override
	public boolean isRoot() {
		return true;
	}

	@Override
	public List<IFile> listAllFiles() {
		return listFiles(true);
	}

	@Override
	public List<IFile> listFiles() {
		return listFiles(false);
	}

	@Override
	public ICloseableDirectory toCloseable() {
		return null;
	}
	
	private List<IFile> listFiles(boolean recurse) {
		Enumeration<URL> entries = bundle.findEntries("/", null, recurse);
		if (entries == null)
			return Collections.emptyList();
		ArrayList<IFile> files = new ArrayList<IFile>();
		while (entries.hasMoreElements()) {
			URL entry = entries.nextElement();
			if (entry.getPath().endsWith("/"))
				continue;
			files.add(new BundleFile(entry.getPath(), entry, this));
		}
		files.trimToSize();
		return files;
	}
}
