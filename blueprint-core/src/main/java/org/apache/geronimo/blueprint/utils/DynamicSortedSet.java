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

import java.util.Comparator;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.NoSuchElementException;

/**
 *
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 766653 $, $Date: 2009-04-20 13:19:48 +0200 (Mon, 20 Apr 2009) $
 */
public class DynamicSortedSet<E> extends DynamicSet<E> implements SortedSet<E> {

    protected Comparator<? super E> comparator;

    public DynamicSortedSet() {
    }

    public DynamicSortedSet(Comparator<? super E> comparator) {
        this.comparator = comparator;
    }

    @Override
    public boolean add(E o) {
        if (o == null) {
            throw new NullPointerException();
        }
        if (comparator == null && !(o instanceof Comparable)) {
            throw new IllegalArgumentException("Elements in this sorted list must implement the " + Comparable.class.getName() + " interface");
        }
        synchronized (lock) {
            int index = Collections.binarySearch(storage, o, comparator);
            if (index < 0) {
                super.add(-index - 1, o);
                return true;
            } else {
                return false;
            }
        }
    }

    public Comparator<? super E> comparator() {
        return comparator;
    }

    public SortedSet<E> subSet(E fromElement, E toElement) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public SortedSet<E> headSet(E toElement) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public SortedSet<E> tailSet(E fromElement) {
        throw new UnsupportedOperationException("Not implemented");
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

    @Override
    public void add(int index, E o) {
        throw new UnsupportedOperationException("Insertion at a given position is not allowed on a sorted list");
    }
}