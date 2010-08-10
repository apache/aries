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
package org.apache.aries.application.utils.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ArrayServiceList<E> implements ServiceCollection<E>, List<E>
{
  private List<E> list = new ArrayList<E>();
  private List<ServiceReference> refList = new ArrayList<ServiceReference>();
  private BundleContext context;
  
  public ArrayServiceList(BundleContext ctx)
  {
    context = ctx;
  }

  public boolean add(E o)
  {
    throw new UnsupportedOperationException("The operation add is not supported. Use addService with a service reference.");
  }

  public void add(int index, E element)
  {
    throw new UnsupportedOperationException("The operation add is not supported. Use addService with a service reference.");
  }

  public boolean addAll(Collection<? extends E> c)
  {
    throw new UnsupportedOperationException("The operation addAll is not supported. Use addService with a service reference.");
  }

  public boolean addAll(int index, Collection<? extends E> c)
  {
    throw new UnsupportedOperationException("The operation addAll is not supported. Use addService with a service reference.");
  }

  public synchronized void clear()
  {
    list.clear();
    
    for (ServiceReference ref : refList) {
      context.ungetService(ref);
    }
    
    refList.clear();
  }

  public boolean contains(Object o)
  {
    return list.contains(o);
  }

  public boolean containsAll(Collection<?> c)
  {
    return list.containsAll(c);
  }

  public E get(int index)
  {    
    return list.get(index);
  }

  public int indexOf(Object o)
  {
    return list.indexOf(o);
  }

  public boolean isEmpty()
  {
    return list.isEmpty();
  }

  public Iterator<E> iterator()
  {
    return listIterator();
  }

  public int lastIndexOf(Object o)
  {
    return list.lastIndexOf(o);
  }

  public ListIterator<E> listIterator()
  {
    return listIterator(0);
  }

  public ListIterator<E> listIterator(int index)
  {
    final ListIterator<E> it = list.listIterator(index);
    final ListIterator<ServiceReference> refIt = refList.listIterator(index);
    
    ListIterator<E> result =  new ListIterator<E>() {
      private ServiceReference current;
      
      public void add(E o)
      {
        throw new UnsupportedOperationException();
      }

      public boolean hasNext()
      {
        return it.hasNext();
      }

      public boolean hasPrevious()
      {
        return it.hasPrevious();
      }

      public E next()
      {
        E result = it.next();
        current = refIt.next();
        return result;
      }

      public int nextIndex()
      {
        return it.nextIndex();
      }

      public E previous()
      {
        E result = it.previous();
        current = refIt.previous();
        return result;
      }

      public int previousIndex()
      {
        return it.previousIndex();
      }

      public void remove()
      {
        it.remove();
        refIt.remove();
        context.ungetService(current);
      }

      public void set(E o)
      {
        throw new UnsupportedOperationException();
      }
    };
    return result;
  }

  public synchronized boolean remove(Object o)
  {
    int index = list.indexOf(o);
    
    ServiceReference ref = refList.remove(index);
    
    context.ungetService(ref);
    return list.remove(o);
  }

  public synchronized E remove(int index)
  {
    ServiceReference ref = refList.remove(index);
    
    context.ungetService(ref);
    return list.remove(index);
  }

  public synchronized boolean removeAll(Collection<?> c)
  {boolean worked = false;
    
    for (Object obj : c) {
      worked |= remove(obj);
    }
    return worked;
  }

  public boolean retainAll(Collection<?> c)
  {
    throw new UnsupportedOperationException("The operation retainAll is not supported.");
  }

  public E set(int index, E element)
  {
    throw new UnsupportedOperationException("The operation set is not supported.");
  }

  public int size()
  {
    return list.size();
  }

  public List<E> subList(int fromIndex, int toIndex)
  {
    throw new UnsupportedOperationException("The operation subList is not supported.");
  }

  public Object[] toArray()
  {
    return list.toArray();
  }

  public Object[] toArray(Object[] a)
  {
    return list.toArray(a);
  }

  public synchronized void addService(ServiceReference ref)
  {
    @SuppressWarnings("unchecked")
    E service = (E)context.getService(ref);
    
    if (service != null) {
      list.add(service);
      refList.add(ref);
    }
  }
}