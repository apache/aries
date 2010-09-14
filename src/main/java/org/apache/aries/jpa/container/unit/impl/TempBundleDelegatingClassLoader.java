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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;

import org.osgi.framework.Bundle;

/**
 * This is a simple temporary ClassLoader that delegates to the Bundle,
 * but does not call loadClass. It is used by the PersistenceUnitInfo
 */
public class TempBundleDelegatingClassLoader extends ClassLoader {

  private final Bundle bundle;
  
  public TempBundleDelegatingClassLoader(Bundle b, ClassLoader parent) {
    super(parent);
    bundle = b;
  }
  
  @Override
  protected Class<?> findClass(String className) throws ClassNotFoundException {
    String classResName = className.replace('.', '/').concat(".class");
    
    //Don't use loadClass, just load the bytes and call defineClass
    InputStream is = getResourceAsStream(classResName);
    
    if(is == null)
      throw new ClassNotFoundException(className);
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    byte[] buff = new byte[4096];
    try {
      int read = is.read(buff);
      while(read >0) {
        baos.write(buff, 0, read);
        read = is.read(buff);
      }
    } catch (IOException ioe) {
      throw new ClassNotFoundException(className, ioe);
    }
    
    buff = baos.toByteArray();
    
    return defineClass(className, buff, 0, buff.length);
  }

  @Override
  protected URL findResource(final String resName)
  {
    //Bundle.getResource requires privileges that the client may not have but we need
    //use a doPriv so that only this bundle needs the privileges
    return AccessController.doPrivileged(new PrivilegedAction<URL>() {

      public URL run()
      {
        return bundle.getResource(resName);
      }
    });
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Enumeration<URL> findResources(final String resName) throws IOException
  {
    Enumeration<URL> resources = null;
    try {
      //Bundle.getResources requires privileges that the client may not have but we need
      //use a doPriv so that only this bundle needs the privileges
      resources = AccessController.doPrivileged(new PrivilegedExceptionAction<Enumeration<URL>>() {

        public Enumeration<URL> run() throws IOException
        {
          return bundle.getResources(resName);
        }
      });
    } catch(PrivilegedActionException pae) {
      //thrownException can never be a RuntimeException, as that would escape
      //the doPriv normally
      Exception thrownException = pae.getException();
      if (thrownException instanceof IOException) {
        throw (IOException)thrownException;
      } else {
        // This code should never get called, but we don't
        // want to gobble the exception if we see it.
        throw new UndeclaredThrowableException(thrownException);
      }
    }
    return resources;
  }
}
