/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.ejb.openejb.extender;

import java.util.Collection;
import java.util.Iterator;

public class  AllCollection<T> implements Collection<T> {

  public boolean add(T object) {
    throw new UnsupportedOperationException();
  }

  public boolean addAll(Collection<? extends T> collection) {
    throw new UnsupportedOperationException();
  }

  public void clear() {
    throw new UnsupportedOperationException();
  }

  public boolean contains(Object object) {
    return true;
  }

  public boolean containsAll(Collection<?> collection) {
    return true;
  }

  public boolean isEmpty() {
    return false;
  }

  public Iterator<T> iterator() {
    throw new UnsupportedOperationException();
  }

  public boolean remove(Object object) {
    throw new UnsupportedOperationException();
  }

  public boolean removeAll(Collection<?> collection) {
    throw new UnsupportedOperationException();
  }

  public boolean retainAll(Collection<?> collection) {
    throw new UnsupportedOperationException();
  }

  public int size() {
    throw new UnsupportedOperationException();
  }

  public Object[] toArray() {
    throw new UnsupportedOperationException();
  }

  public <T> T[] toArray(T[] array) {
    throw new UnsupportedOperationException();
  }

 
}
