/**
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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.samples.blog;

import java.lang.reflect.Constructor;
import java.util.ListIterator;

public class BlogListIterator<B, F> implements ListIterator<F> {

	private ListIterator<? extends B> internalListIterator;

	private Class<? extends F> frontendClazz;
	private Class<B> backendClazz;

	public BlogListIterator(ListIterator<? extends B> listIterator,
			Class<? extends F> frontendClazz, Class<B> backendClazz) {
		this.internalListIterator = listIterator;
		this.frontendClazz = frontendClazz;
		this.backendClazz = backendClazz;
	}

	public void add(Object e) {
		throw new UnsupportedOperationException("");
	}

	public boolean hasNext() {
		return internalListIterator.hasNext();
	}

	public boolean hasPrevious() {
		return internalListIterator.hasPrevious();
	}

	public F next() {
		try {
			return getConstructor().newInstance(internalListIterator.next());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public int nextIndex() {
		return internalListIterator.nextIndex();
	}

	public F previous() {	
		try {
			return getConstructor().newInstance(internalListIterator.previous());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public int previousIndex() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void remove() {
		throw new UnsupportedOperationException(
				"Modifications to the list are not supported");
	}

	public void set(Object e) {
		throw new UnsupportedOperationException(
				"Modifications to the list are not supported");
	}

	private Constructor<F> getConstructor() {
		Constructor<F> c;
		try {
			c = (Constructor<F>) frontendClazz.getConstructor(backendClazz);
			return c;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
