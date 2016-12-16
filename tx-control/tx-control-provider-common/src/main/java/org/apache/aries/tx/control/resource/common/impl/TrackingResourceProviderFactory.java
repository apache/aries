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
package org.apache.aries.tx.control.resource.common.impl;

import static java.util.Collections.newSetFromMap;

import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.Callable;

import org.osgi.framework.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TrackingResourceProviderFactory<T extends AutoCloseable> {

	private static final Logger LOG = LoggerFactory.getLogger(TrackingResourceProviderFactory.class);
	
	private final Set<T> toClose = newSetFromMap(new IdentityHashMap<>());
	
	private boolean closed;
	
	protected T doGetResult(Callable<T> getter) {
		synchronized (getter) {
			if (closed) {
				throw new IllegalStateException("This ResourceProvider has been reclaimed because the factory service that provided it was released");
			}
		}
		T t;
		try {
			t = getter.call();
		} catch (Exception e) {
			LOG.warn("A failure occurred obtaining the resource provider", e);
			throw new ServiceException("A failure occurred obtaining the resource provider", e);
		}
		boolean destroy = false;
		synchronized (toClose) {
			if (closed) {
				destroy = true;
			} else {
			    toClose.add(t);
			}
		}
		if(destroy) {
			try {
				t.close();
			} catch (Exception e) {
				LOG.warn("A failure occurred closing the resource provider", e);
			}
			throw new IllegalStateException("This ResourceProvider has been reclaimed because the factory service that provided it was released");
		}
		return t;
	}

	public void closeAll() {
		synchronized (toClose) {
			closed = true;
		}
		// toClose is now up to date and no other thread will write it
		toClose.stream().forEach(ajcp -> {
			try {
				ajcp.close();
			} catch (Exception e) {}
		});
		
		toClose.clear();
	}
	
	protected void release(T t) {
		synchronized (toClose) {
			if(closed) {
				throw new IllegalStateException("This resource factory is closed");
			}
			
			if (!toClose.remove(t)) {
				throw new IllegalArgumentException("The resource " + t + " is not managed by this factory");
			}
		}
		try {
			t.close();
		} catch (Exception e) {}
	}
}