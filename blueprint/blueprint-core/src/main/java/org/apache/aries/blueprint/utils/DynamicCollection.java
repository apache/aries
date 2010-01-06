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
package org.apache.aries.blueprint.utils;

import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Collection that allows iterators to see addition or removals of elements while iterating.
 * This collection and its iterators are thread safe but all operations happen under a
 * synchronization lock, so the performance in heavy concurrency load is far from optimal.
 * If such a use is needed, a CopyOnWriteArrayList may be more suited.
 *
 * @version $Rev$, $Date$
 */
public class DynamicCollection<E> extends AbstractCollection<E> {

    protected final Object lock = new Object();
    protected final List<E> storage;
    protected final List<WeakReference<DynamicIterator>> iterators;

    public DynamicCollection() {
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
            internalAdd(storage.size(), o);
            return true;
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

        protected void removedIndex(int index) {
            synchronized (lock) {
                if (index < this.index || (index == this.index && (hasNextCalled || hasPreviousCalled))) {
                    this.index--;
                }
            }
        }

        protected void addedIndex(int index) {
            synchronized (lock) {
                if (index < this.index || (index == this.index && (next != null || previous != null))) {
                    this.index++;
                }
            }
        }

        public boolean hasNext() {
            synchronized (lock) {
                hasPreviousCalled = false;
                hasNextCalled = true;
                next = index < storage.size() ? storage.get(index) : null;
                return next != null;
            }
        }

        public boolean hasPrevious() {
            synchronized (lock) {
                hasPreviousCalled = true;
                hasNextCalled = false;
                previous = index > 0 ? storage.get(index - 1) : null;
                return previous != null;
            }
        }

        public E next() {
            synchronized (lock) {
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
        }

        public E previous() {
            synchronized (lock) {
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
        }

        public int nextIndex() {
            synchronized (lock) {
                return index;
            }
        }

        public int previousIndex() {
            synchronized (lock) {
                return index - 1;
            }
        }

        public void set(E o) {
            throw new UnsupportedOperationException();
        }

        public void add(E o) {
            throw new UnsupportedOperationException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

}
