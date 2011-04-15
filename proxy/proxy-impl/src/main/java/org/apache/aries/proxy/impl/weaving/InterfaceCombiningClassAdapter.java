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
package org.apache.aries.proxy.impl.weaving;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.aries.proxy.InvocationListener;
import org.apache.aries.proxy.UnableToProxyException;
import org.apache.aries.proxy.weaving.WovenProxy;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.commons.Method;

/**
 * This class is used to aggregate several interfaces into a real class which implements all of them
 */
public final class InterfaceCombiningClassAdapter extends EmptyVisitor implements Opcodes {

  /** An implementation of ClassLoader that will be used to define our proxy class */
  private static final class ProxyClassLoader extends ClassLoader {
    /** A {@link Map} of classes we already know */
    private final Map<String, Class<?>> classes = new ConcurrentHashMap<String, Class<?>>();
    
    /** The bytes of the proxy class */
    private byte[] bytesToDefine;
    /** The name of the proxy class */
    private String classNameToDefine;
    
    public ProxyClassLoader(Collection<Class<?>> ifaces) {
      for(Class<?> c : ifaces) {
        classes.put(c.getName(), c);
      }
      classes.put(WovenProxy.class.getName(), WovenProxy.class);
    }

    @Override
    protected Class<?> findClass(String className) {
      Class<?> c = findLoadedClass(className);
      
      if(c == null) {
        if(className.equals(classNameToDefine))
          return defineClass(className, bytesToDefine, 0, bytesToDefine.length);
        
        c = classes.get(className);
        
        if(c == null) {
          for(Class<?> iface : classes.values()) {
            if(iface.getClassLoader() == null)
              continue;
            try {
              c = iface.getClassLoader().loadClass(className);
              break;
            } catch (ClassNotFoundException cnfe) {
              //No op
            }
          }
        }
      }
      return c;
    }
  }
  /** 
   * A cache of previously created proxy classes. Because we cache then update
   * the {@link InterfaceCombiningClassAdapter#accessList} we might temporarily 
   * end up with more in the cache than the maximum size of the access list. 
   * We therefore initialize this to be double the size of the access list so it
   * should never be necessary to resize
   */
  private static final ConcurrentMap<HashSet<Class<?>>, Class<?>> cache = 
    new ConcurrentHashMap<HashSet<Class<?>>, Class<?>>(256);
  /**
   * The access List that determines when we evict things from our cache, items
   * are removed and re-added when accessed so a {@link LinkedBlockingQueue}
   * is considerably faster 
   */
  private static final Queue<HashSet<Class<?>>> accessList = 
    new LinkedBlockingQueue<HashSet<Class<?>>>(128);
  
  /** The interfaces we need to implement */
  private final Collection<Class<?>> interfaces;
  /** The {@link ClassWriter} we use to write our class */
  private final ClassWriter writer;
  /** The adapter we use to weave in our method implementations */
  private final InterfaceUsingWovenProxyAdapter adapter;
  /** Whether we have already written the class bytes */
  private boolean done = false;
  
  /**
   * Construct an {@link InterfaceCombiningClassAdapter} to combine the supplied
   * interfaces into a class with the supplied name using the supplied classloader
   * @param className
   * @param loader
   * @param interfaces
   */
  private InterfaceCombiningClassAdapter(String className,
      ClassLoader loader, Collection<Class<?>> interfaces) {
    writer = new OSGiFriendlyClassWriter(ClassWriter.COMPUTE_FRAMES, loader);
    adapter = new InterfaceUsingWovenProxyAdapter(writer, className, loader);
    
    this.interfaces = interfaces;
    String[] interfaceNames = new String[interfaces.size()];
    
    int i = 0;
    for(Class<?> in : interfaces) {
      interfaceNames[i] = Type.getType(in).getInternalName();
      i++;
    }
    
    adapter.visit(V1_6, ACC_PUBLIC | ACC_SYNTHETIC, className, null,
        AbstractWovenProxyAdapter.OBJECT_TYPE.getInternalName(), interfaceNames);
  }


  @Override
  public final MethodVisitor visitMethod(int access, String name, String desc,
      String sig, String[] arg4) {
    //We're going to implement this method, so make it non abstract!
    access ^= ACC_ABSTRACT;
    //If we already implement this method (from another interface) then we don't
    //want a duplicate
    if(adapter.knownMethods.contains(new Method(name, desc)))
      return null;
    else 
      return adapter.visitMethod(access, name, desc, null, arg4);
  }
  
  /**
   * Generate the byte[] for our class
   * @return
   * @throws UnableToProxyException
   */
  private final byte[] generateBytes() throws UnableToProxyException {
    if(!!!done) {
      for(Class<?> c : interfaces) {
        adapter.setCurrentInterface(Type.getType(c));
        try {
          ClassReader reader = new ClassReader(c.getResourceAsStream(
              c.getSimpleName() + ".class"));
          reader.accept(this, 
              ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
          
        } catch (IOException e) {
          throw new UnableToProxyException(c, e);
        }
      }
      adapter.visitEnd();
      done  = true;
    }
    return writer.toByteArray();
  }

  /**
   * Generate a new proxy instance implementing the supplied interfaces and using the supplied
   * dispatcher and listener
   * @param ifaces
   * @param dispatcher
   * @param listener
   * @return
   * @throws UnableToProxyException
   */
  public static final Object getProxyInstance(Collection<Class<?>> ifaces, 
      Callable<Object> dispatcher, InvocationListener listener) throws UnableToProxyException{
    
    Class<?> c;
    
    HashSet<Class<?>> classes = new HashSet<Class<?>>(ifaces);
    
    c = cache.get(classes);
    
    if(c == null) {
      ProxyClassLoader cl = new ProxyClassLoader(classes);
      
      String className = "Proxy" + AbstractWovenProxyAdapter.getSanitizedUUIDString();
      
      InterfaceCombiningClassAdapter icca = new InterfaceCombiningClassAdapter(
          className, cl, classes);
      
      try {
        cl.bytesToDefine = icca.generateBytes();
        cl.classNameToDefine = className;
        c = cl.loadClass(className);
        Class<?> tmp = cache.putIfAbsent(classes, c);
        if(tmp != null) {
          //Someone beat us to it, just update the location in the access list
          c = tmp;
          
          if(accessList.remove(classes)) {
            //If we removed it we re-add it
            while(!!!accessList.offer(classes)) {
              HashSet<Class<?>> key = accessList.poll();
              cache.remove(key);
            }
          }
        } else {
          //We have added a new cache entry
          while(!!!accessList.offer(classes)) {
            HashSet<Class<?>> key = accessList.poll();
            cache.remove(key);
          }
        }
      } catch (Exception re) {
        throw new UnableToProxyException(classes.iterator().next(), re);
      }
    }
    
    try {
      Constructor<?> con = c.getDeclaredConstructor(Callable.class, InvocationListener.class);
      con.setAccessible(true);
      return con.newInstance(dispatcher, listener);
    } catch (Exception e) {
      throw new UnableToProxyException(classes.iterator().next(), e);
    }
  }
}