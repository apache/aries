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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.util.filesystem.ICloseableDirectory;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;
import org.apache.aries.util.internal.MessageUtil;

public class CloseableDirectory implements ICloseableDirectory {
	protected IDirectory delegate;
	private final AtomicBoolean closed = new AtomicBoolean(false);

	public CloseableDirectory(IDirectory delegate) {
		this.delegate = delegate;
	}
	
	public String getName() {
		checkNotClosed();
		return delegate.getName();
	}

	public boolean isDirectory() {
		checkNotClosed();
		return delegate.isDirectory();
	}

	public boolean isFile() {
		checkNotClosed();
		return delegate.isFile();
	}

	public long getLastModified() {
		checkNotClosed();
		return delegate.getLastModified();
	}

	public IFile getFile(String name) {
		checkNotClosed();
		return delegate.getFile(name);
	}

	public long getSize() {
		checkNotClosed();
		return delegate.getSize();
	}

	public IDirectory convert() {
		checkNotClosed();
		return delegate.convert();
	}

	public IDirectory convertNested() {
		checkNotClosed();
		return delegate.convertNested();
	}

	public boolean isRoot() {
		checkNotClosed();
		return delegate.isRoot();
	}

	public IDirectory getParent() {
		checkNotClosed();
		return delegate.getParent();
	}

	public IDirectory getRoot() {
		checkNotClosed();
		return delegate.getRoot();
	}

	public Iterator<IFile> iterator() {
		checkNotClosed();
		return delegate.iterator();
	}

	public List<IFile> listFiles() {
		checkNotClosed();
		return delegate.listFiles();
	}

	public List<IFile> listAllFiles() {
		checkNotClosed();
		return delegate.listAllFiles();
	}

	public ICloseableDirectory toCloseable() {
		checkNotClosed();
		return delegate.toCloseable();
	}

	public InputStream open() throws IOException, UnsupportedOperationException {
		checkNotClosed();
		return delegate.open();
	}

	public URL toURL() throws MalformedURLException {
		checkNotClosed();
		return delegate.toURL();
	}

	public final void close() throws IOException {
		if (closed.compareAndSet(false, true)) {
			cleanup();
		}
	}
	
	protected void cleanup() {}
	
	protected void checkNotClosed() {
		if (isClosed()) throw new IllegalStateException(MessageUtil.getMessage("UTIL0018E"));
	}

	public boolean isClosed() {
		return closed.get();
	}
}
