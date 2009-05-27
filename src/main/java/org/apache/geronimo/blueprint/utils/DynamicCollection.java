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

import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Comparator;
import java.util.Collections;
import java.util.ListIterator;

/**
 * Collection that allows iterators to see addition or removals of elements while iterating.
 * This collection and its iterators are thread safe but all operations happen under a
 * synchronization lock, so the performance in heavy concurrency load is far from optimal.
 * If such a use is needed, a CopyOnWriteArrayList may be more suited.
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 766653 $, $Date: 2009-04-20 13:19:48 +0200 (Mon, 20 Apr 2009) $
 */
public class DynamicCollection<E> extends AbstractCollection<E> {

    protected final boolean allowDuplicates;
    protected final Comparator comparator;
    protected final Object lock = new Object();
    protected final List<E> storage;
    protected final List<WeakReference<DynamicIterator>> iterators;

    public DynamicCollection(boolean allowDuplicates, Comparator comparator) {
        this.allowDuplicates = allowDuplicates;
        this.comparator = comparator;
        this.storage = new ArrayList<E>();
        this.iterators = new ArrayList<WeakReference<DynamicIterator>>();
    }

    public DynamicIterator iterator() {
        return iterator(0);
    }

    public DynamicIterator iterator(int index) {
        DynamicIterator iterator = createIterator(index);
        synchronized (lock) {
            for (Iterator<WeakReference<DynamicIterator>> it = iterators.iterator(); it.hasNext();) {
                if (it.next().get() == null) {
                    it.remove();
                }
            }
            iterators.add(new WeakReference<DynamicIterator>(iterator));
        }
        return iterator;
    }

    protected DynamicIterator createIterator(int index) {
        return new DynamicIterator(index);
    }

    public int size() {
        synchronized (lock) {
            return storage.size();
        }
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean contains(Object o) {
        if (o == null) {
            throw new NullPointerException();
        }
        synchronized (lock) {
            return storage.contains(o);
        }
    }

    public Object[] toArray() {
        synchronized (lock) {
            return storage.toArray();
        }
    }

    public <T> T[] toArray(T[] a) {
        synchronized (lock) {
            return storage.toArray(a);
        }
    }

    public boolean containsAll(Collection<?> c) {
        synchronized (lock) {
            return storage.containsAll(c);
        }
    }

    public boolean add(E o) {
        if (o == null) {
            throw new NullPointerException();
        }
        synchronized (lock) {
            if (comparator != null) {
                if (allowDuplicates) {
                    int index = Collections.binarySearch(storage, o, comparator);
                    if (index < 0) {
                        index = -index - 1;
                    } else {
                        index = index + 1;
                    }
                    internalAdd(index, o);
                    return true;
                } else {
                    int index = Collections.binarySearch(storage, o, comparator);
                    if (index < 0) {
                        internalAdd(-index - 1, o);
                        return true;
                    } else {
                        return false;
                    }
                }
            } else {
                if (allowDuplicates) {
                    internalAdd(storage.size(), o);
                    return true;
                } else {
                    if (!storage.contains(o)) {
                        internalAdd(storage.size(), o);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
    }

    public boolean remove(Object o) {
        if (o == null) {
            throw new NullPointerException();
        }
        synchronized (lock) {
            int index = storage.indexOf(o);
            return remove(index) != null;
        }
    }

    public E get(int index) {
        synchronized (lock) {
            return storage.get(index);
        }
    }

    private void internalAdd(int index, E o) {
        if (o == null) {
            throw new NullPointerException();
        }
        synchronized (lock) {
            storage.add(index, o);
            for (Iterator<WeakReference<DynamicIterator>> it = iterators.iterator(); it.hasNext();) {
                DynamicIterator i = it.next().get();
                if (i == null) {
                    it.remove();
                } else {
                    i.addedIndex(index);
                }
            }
        }
    }

    @Override
    public void clear() {
        synchronized (lock) {
            storage.clear();
        }
    }

    public E remove(int index) {
        synchronized (lock) {
            E o = storage.remove(index);
            for (Iterator<WeakReference<DynamicIterator>> it = iterators.iterator(); it.hasNext();) {
                WeakReference<DynamicIterator> r = it.next();
                DynamicIterator i = r.get();
                if (i == null) {
                    it.remove();
                } else {
                    i.removedIndex(index);
                }
            }
            return o;
        }
    }

    public E first() {
        synchronized (lock) {
            if (storage.isEmpty()) {
                throw new NoSuchElementException();
            } else {
                return storage.get(0);
            }
        }
    }

    public E last() {
        synchronized (lock) {
            if (storage.isEmpty()) {
                throw new NoSuchElementException();
            } else {
                return storage.get(storage.size() - 1);
            }
        }
    }

    public class DynamicIterator implements ListIterator<E> {

        protected int index;
        protected boolean hasNextCalled;
        protected E next;
        protected boolean hasPreviousCalled;
        protected E previous;
        protected E last;

        public DynamicIterator() {
            this(0);
        }

        public DynamicIterator(int index) {
            this.index = index;
        }

        protected synchronized void removedIndex(int index) {
            if (index < this.index || (index == this.index && (hasNextCalled || hasPreviousCalled))) {
                this.index--;
            }
        }

        protected synchronized void addedIndex(int index) {
            if (index < this.index || (index == this.index && (next != null || previous != null))) {
                this.index++;
            }
        }

        public synchronized boolean hasNext() {
            synchronized (lock) {
                hasPreviousCalled = false;
                hasNextCalled = true;
                next = index < storage.size() ? storage.get(index) : null;
                return next != null;
            }
        }

        public synchronized boolean hasPrevious() {
            synchronized (lock) {
                hasPreviousCalled = true;
                hasNextCalled = false;
                previous = index > 0 ? storage.get(index - 1) : null;
                return previous != null;
            }
        }

        public synchronized E next() {
            try {
                if (!hasNextCalled) {
                    hasNext();
                }
                last = next;
                if (next != null) {
                    ++index;
                    return next;
                } else {
                    throw new NoSuchElementException();
                }
            } finally {
                hasPreviousCalled = false;
                hasNextCalled = false;
                next = null;
                previous = null;
            }
        }

        public synchronized E previous() {
            try {
                if (!hasPreviousCalled) {
                    hasPrevious();
                }
                last = previous;
                if (previous != null) {
                    --index;
                    return previous;
                } else {
                    throw new NoSuchElementException();
                }
            } finally {
                hasPreviousCalled = false;
                hasNextCalled = false;
                next = null;
                previous = null;
            }
        }

        public synchronized int nextIndex() {
            return index;
        }

        public synchronized int previousIndex() {
            return index - 1;
        }

        public void set(E o) {
            throw new UnsupportedOperationException();
        }

        public void add(E o) {
            throw new UnsupportedOperationException();
        }

        public synchronized void remove() {
            throw new UnsupportedOperationException();
        }

    }

}
