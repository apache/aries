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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.geronimo.blueprint.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

/**
 * Same as DynamicCollection but implementing the List interface.
 * All list method implemetations are actually provided by the DynamicCollection class
 * itself as it already relies on a List storage.
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 766653 $, $Date: 2009-04-20 13:19:48 +0200 (Mon, 20 Apr 2009) $
 */
public class DynamicList<E> extends DynamicCollection<E> implements List<E>, RandomAccess {

    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    public ListIterator<E> listIterator(int index) {
        return (ListIterator<E>) iterator(index);
    }

    @Override
    protected DynamicIterator createIterator(int index) {
        return new DynamicListIterator(index);
    }

    public E get(int index) {
        synchronized (lock) {
            return storage.get(index);
        }
    }

    public E set(int index, E o) {
        if (o == null) {
            throw new NullPointerException();
        }
        synchronized (lock) {
            return storage.set(index, o);
        }
    }

    public int indexOf(Object o) {
        if (o == null) {
            throw new NullPointerException();
        }
        synchronized (lock) {
            return storage.indexOf(o);
        }
    }

    public int lastIndexOf(Object o) {
        if (o == null) {
            throw new NullPointerException();
        }
        synchronized (lock) {
            return storage.lastIndexOf(o);
        }
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        synchronized (lock) {
            boolean modified = false;
            Iterator<? extends E> e = c.iterator();
            while (e.hasNext()) {
                add(index++, e.next());
                modified = true;
            }
            return modified;
        }
    }

    public List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("Not implemented");
    }

    protected class DynamicListIterator extends DynamicIterator implements ListIterator<E> {

        public DynamicListIterator(int index) {
            super(index);
        }

        public synchronized void set(E o) {
            DynamicList.this.set(index, o);
        }

        public synchronized void add(E o) {
            DynamicList.this.add(index++, o);
        }

    }
}
