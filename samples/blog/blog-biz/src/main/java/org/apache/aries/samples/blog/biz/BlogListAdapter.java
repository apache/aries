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
package org.apache.aries.samples.blog.biz;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class BlogListAdapter<F, B> implements List<F> {

	private List<? extends B> backendList;
	private Class<? extends F> frontendClazz;
	private Class<B> backendClazz;

	public BlogListAdapter(List<? extends B> backendList,
			Class<? extends F> frontendClazz, Class<B> backendClazz) {
		this.backendList = backendList;
		this.frontendClazz = frontendClazz;
		this.backendClazz = backendClazz;
	}

	public void add() {
		throw new UnsupportedOperationException(
				"Modifications to the list are not supported");
	}

	public boolean add(F e) {
		throw new UnsupportedOperationException(
				"Modifications to the list are not supported");
	}

	public void add(int index, F element) {
		throw new UnsupportedOperationException(
				"Modifications to the list are not supported");
	}

	public boolean addAll(Collection<? extends F> c) {
		throw new UnsupportedOperationException(
				"Modifications to the list are not supported");
	}

	public boolean addAll(int index, Collection<? extends F> c) {
		throw new UnsupportedOperationException(
				"Modifications to the list are not supported");
	}

	public void clear() {
		throw new UnsupportedOperationException(
				"Modifications to the list are not supported");

	}

	public boolean contains(Object o) {
		throw new UnsupportedOperationException("Contains() is not supported");

	}

	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException(
				"ContainsAll() is not supported");
	}

	public F get(int index) {
		Constructor<F> c;
		try {
			c = (Constructor<F>) frontendClazz.getConstructor(backendClazz);
			return c.newInstance(backendList.get(index));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return null;
	}

	public int indexOf(Object o) {
		throw new UnsupportedOperationException("IndexOf() is not supported");
	}

	public boolean isEmpty() {
		return backendList.isEmpty();
	}

	public Iterator iterator() {
		return new BlogListIterator(backendList.listIterator(), frontendClazz,
				backendClazz);
	}

	public int lastIndexOf(Object o) {
		throw new UnsupportedOperationException(
				"lastIndexOf() is not supported");
	}

	public ListIterator listIterator() {
		return new BlogListIterator(backendList.listIterator(), frontendClazz,
				backendClazz);
	}

	public ListIterator listIterator(int index) {
		return new BlogListIterator(backendList.listIterator(index),
				frontendClazz, backendClazz);
	}

	public boolean remove(Object o) {
		throw new UnsupportedOperationException(
				"Modifications to the list are not supported");
	}

	public F remove(int index) {
		throw new UnsupportedOperationException(
				"Modifications to the list are not supported");
	}

	public boolean removeAll(Collection c) {
		throw new UnsupportedOperationException(
				"Modifications to the list are not supported");
	}

	public boolean retainAll(Collection c) {
		throw new UnsupportedOperationException(
				"Modifications to the list are not supported");
	}

	public Object set(int index, Object element) {
		throw new UnsupportedOperationException(
				"Modifications to the list are not supported");
	}

	public int size() {
		return backendList.size();
	}

	public List subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException("subList() is not supported");
	}

	public Object[] toArray() {
		throw new UnsupportedOperationException("toArray() is not supported");
	}

	public Object[] toArray(Object[] a) {
		throw new UnsupportedOperationException(
				"toArray(Object[] a) is not supported");
	}

}

