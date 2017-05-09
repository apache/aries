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
package org.apache.aries.proxy.impl.interfaces;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;

import org.apache.aries.proxy.FinalModifierException;
import org.apache.aries.proxy.InvocationListener;
import org.apache.aries.proxy.UnableToProxyException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

/**
 * This class is used to aggregate several interfaces into a real class which implements all of them
 * It also allows you specify a superclass that the class should implement - this will add delegating
 * method overrides for any abstract methods in the hierarchy, but not override any non-abstract methods.
 * To be safely used as a supertype the superclass should be a WovenProxy.
 */
public final class InterfaceProxyGenerator extends ClassVisitor implements Opcodes {

  public InterfaceProxyGenerator()
  {
    super(Opcodes.ASM5);
    
  }

  private static final Map<BundleWiring, WeakReference<ProxyClassLoader>> cache =
            new WeakHashMap<BundleWiring, WeakReference<ProxyClassLoader>>(128);
  
  /**
   * Generate a new proxy instance implementing the supplied interfaces and using the supplied
   * dispatcher and listener
   * @param client the bundle that is trying to generate this proxy (can be null)
   * @param superclass The superclass to use (or null for Object)
   * @param ifaces The set of interfaces to implement (may be empty if superclass is non null)
   * @param dispatcher
   * @param listener
   * @return
   * @throws UnableToProxyException
   */
  public static Object getProxyInstance(Bundle client, Class<?> superclass,
      Collection<Class<?>> ifaces, Callable<Object> dispatcher, InvocationListener listener) throws UnableToProxyException{
    
    if(superclass != null && (superclass.getModifiers() & Modifier.FINAL) != 0)
      throw new FinalModifierException(superclass);
    
    ProxyClassLoader pcl = null;
    
    SortedSet<Class<?>> interfaces = createSet(ifaces);
    
    synchronized (cache) {
      BundleWiring wiring = client == null ? null : (BundleWiring)client.adapt(BundleWiring.class);
      WeakReference<ProxyClassLoader> ref = cache.get(wiring);
      
      if(ref != null)
        pcl = ref.get();
      
      if (pcl != null && pcl.isInvalid(interfaces)) {
          pcl = null;
          cache.remove(wiring);
      }
      
      if(pcl == null) {
        pcl = new ProxyClassLoader(client);
        cache.put(wiring, new WeakReference<ProxyClassLoader>(pcl));
      }
    }

    Class<?> c = pcl.createProxyClass(superclass, interfaces);

    try {
      Constructor<?> con = c.getDeclaredConstructor(Callable.class, InvocationListener.class);
      con.setAccessible(true);
      return con.newInstance(dispatcher, listener);
    } catch (Exception e) {
      throw new UnableToProxyException(ifaces.iterator().next(), e);
    }
  }

  /**
   * Get the set of interfaces we need to process. This will return a HashSet 
   * that includes includes the supplied collection and any super-interfaces of 
   * those classes 
   * @param ifaces
   * @return
   */
  private static SortedSet<Class<?>> createSet(Collection<Class<?>> ifaces) {
    SortedSet<Class<?>> classes = new TreeSet<Class<?>>(new Comparator<Class<?>>() {
      public int compare(Class<?> object1, Class<?> object2) {
        if (object1.getName().equals(object2.getName())) {
          return 0;
        } else if (object1.isAssignableFrom(object2)) {
          // first class is parent of second, it occurs earlier in type hierarchy
          return -1;
        } else if (object2.isAssignableFrom(object1)) {
          // second class is subclass of first one, it occurs later in hierarchy
          return 1;
        }
        // types have separate inheritance trees, but it does matter which one is first or second, so we
        // won't end up with duplicates
        // however we can't mark them as equal cause one of them will be removed
        return object1.getName().compareTo(object2.getName());
      }
    });
    for(Class<?> c : ifaces) {
      //If we already have a class contained then we have already covered its hierarchy
      if(classes.add(c))
        classes.addAll(createSet(Arrays.asList(c.getInterfaces())));
    }
    return classes;
  }
}