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
package org.apache.aries.jpa.container.unit.impl;

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
/**
 * This is a simple ClassLoader that delegates to the Bundle
 * and is used by the PersistenceUnitInfo
 */
public class BundleDelegatingClassLoader extends ClassLoader {

  private final Bundle bundle;
  
  public BundleDelegatingClassLoader(Bundle b) {
    bundle = b;
  }
  
  @Override
  protected Class<?> findClass(final String name) throws ClassNotFoundException {
    try {
      return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
        public Class<?> run() throws ClassNotFoundException 
        {
          return bundle.loadClass(name);
        }
      });
    } catch (PrivilegedActionException e) {
      Exception cause = e.getException();
        
      if (cause instanceof ClassNotFoundException) throw (ClassNotFoundException)cause;
      else throw (RuntimeException)cause;
    }
  }

  @Override
  protected URL findResource(final String name) {
    return AccessController.doPrivileged(new PrivilegedAction<URL>() {
      public URL run()
      {
        return bundle.getResource(name);
      }
    });
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Enumeration<URL> findResources(final String name) throws IOException {
    Enumeration<URL> urls;
    try {
      urls =  AccessController.doPrivileged(new PrivilegedExceptionAction<Enumeration<URL>>() {
        @SuppressWarnings("unchecked")
        public Enumeration<URL> run() throws IOException
        {
          return (Enumeration<URL>)bundle.getResources(name);
        }
        
      });
    } catch (PrivilegedActionException e) {
      Exception cause = e.getException();
      
      if (cause instanceof IOException) throw (IOException)cause;
      else throw (RuntimeException)cause;
    }
    
    if (urls == null) {
      urls = Collections.enumeration(new ArrayList<URL>());
    }
    
    return urls;
  }

}
