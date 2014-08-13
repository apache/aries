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
package org.apache.aries.jpa.container.weaving.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.spi.ClassTransformer;

import org.apache.aries.jpa.container.impl.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;

/**
 * This weaving hook delegates to any registered {@link ClassTransformer} instances
 * for a given bundle
 */
public class JPAWeavingHook implements WeavingHook, TransformerRegistry {

  /**
   * This constructor should not be called directly, the {@link JPAWeavingHookFactory} 
   * should be used to ensure that Weaving support is available.
   */
  JPAWeavingHook() { }
  
  /** 
   * With luck we will only have one persistence unit per bundle, but
   * if we don't we'll need to call them until one of them does a transform
   * or we run out.
   */
  private final ConcurrentMap<Bundle, LinkedHashSet<WrappingTransformer>> registeredTransformers
      = new ConcurrentHashMap<Bundle, LinkedHashSet<WrappingTransformer>>();
  
  public void weave(WovenClass wovenClass) {
    
    BundleWiring wiring = wovenClass.getBundleWiring();
    
    Collection<WrappingTransformer> transformers = registeredTransformers.get(
        wiring.getBundle());
    
    if(transformers != null) {
      Collection<WrappingTransformer> transformersToTry;
      synchronized (transformers) {
        transformersToTry = new ArrayList<WrappingTransformer>(transformers);
      }
      for(WrappingTransformer transformer : transformersToTry) {
        try {
          byte[] result = transformer.transform(wiring.getClassLoader(), 
              wovenClass.getClassName(), wovenClass.getDefinedClass(), 
              wovenClass.getProtectionDomain(), wovenClass.getBytes());
          if(result != null) {
            wovenClass.setBytes(result);
            wovenClass.getDynamicImports().addAll(transformer.getPackagesToAdd());
            break;
          }
        } catch (Throwable t) {
          if(t instanceof ThreadDeath)
            throw (ThreadDeath)t;
          else if (t instanceof OutOfMemoryError)
            throw (OutOfMemoryError) t;
          else {
            Bundle b = wovenClass.getBundleWiring().getBundle();
            throw new WeavingException(NLS.MESSAGES.getMessage("jpa.weaving.failure", wovenClass.getClassName(), b.getSymbolicName(), b.getVersion(), transformer), t);
          }
        }
      }
    }
  }
  
  public void addTransformer(Bundle pBundle, ClassTransformer transformer, ServiceReference<?> provider) {
    
    //Optimised for single adds
    
    LinkedHashSet<WrappingTransformer> set = new LinkedHashSet<WrappingTransformer>();
    WrappingTransformer wt = new WrappingTransformer(transformer, provider);
    set.add(wt);
    
    LinkedHashSet<WrappingTransformer> existingSet = registeredTransformers.putIfAbsent(pBundle, set);
    
    if(existingSet != null) {
      synchronized (existingSet) {
        existingSet.add(wt);
      }
    }
  }
   
  public void removeTransformer(Bundle pBundle, ClassTransformer transformer) {
    LinkedHashSet<WrappingTransformer> set = registeredTransformers.get(pBundle);
    
    if(set == null || !!!safeRemove(set, transformer))
      throw new IllegalStateException(NLS.MESSAGES.getMessage("jpa.weaving.transformer.not.registered", transformer));
    
    if(set.isEmpty())
      registeredTransformers.remove(pBundle);
  }

  /**
   * Perform a remove on the collection while synchronized on it
   * @param set
   * @param t
   * @return
   */
  private boolean safeRemove(Collection<WrappingTransformer> set, ClassTransformer t) {
    synchronized(set) {
        return set.remove(new WrappingTransformer(t));
      }
  }
}
