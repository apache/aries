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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;

import org.apache.aries.proxy.InvocationListener;
import org.apache.aries.proxy.UnableToProxyException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.EmptyVisitor;
import org.osgi.framework.Bundle;

/**
 * This class is used to aggregate several interfaces into a real class which implements all of them
 */
public final class InterfaceProxyGenerator extends EmptyVisitor implements Opcodes {

  private static final Map<Bundle, WeakReference<ProxyClassLoader>> cache = 
            new WeakHashMap<Bundle, WeakReference<ProxyClassLoader>>(128);
  
  /**
   * Generate a new proxy instance implementing the supplied interfaces and using the supplied
   * dispatcher and listener
   * @param ifaces
   * @param dispatcher
   * @param listener
   * @return
   * @throws UnableToProxyException
   */
  public static final Object getProxyInstance(Bundle client, Collection<Class<?>> ifaces, 
      Callable<Object> dispatcher, InvocationListener listener) throws UnableToProxyException{
    
    ProxyClassLoader pcl = null;
    
    HashSet<Class<?>> classSet = createSet(ifaces);
    
    synchronized (cache) {
      WeakReference<ProxyClassLoader> ref = cache.get(client);
      
      if(ref != null)
        pcl = ref.get();
      
      if (pcl != null && pcl.isInvalid(classSet)) {
          pcl = null;
          cache.remove(client);
      }
      
      if(pcl == null) {
        pcl = new ProxyClassLoader(client);
        cache.put(client, new WeakReference<ProxyClassLoader>(pcl));
      }
    }
    
    Class<?> c = pcl.createProxyClass(classSet);
        
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
  private static HashSet<Class<?>> createSet(Collection<Class<?>> ifaces) {
    HashSet<Class<?>> classes = new HashSet<Class<?>>();
    for(Class<?> c : ifaces) {
      //If we already have a class contained then we have already covered its hierarchy
      if(classes.add(c))
        classes.addAll(createSet(Arrays.asList(c.getInterfaces())));
    }
    return classes;
  }
}