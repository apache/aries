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
import java.util.Deque;
import java.util.Enumeration;
import java.util.LinkedList;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

/**
 * This is a simple temporary ClassLoader that delegates to the Bundle,
 * but does not call loadClass. It is used by the PersistenceUnitInfo
 */
public class TempBundleDelegatingClassLoader extends ClassLoader {

  private static final boolean CONTEXT_TRACKING_ENABLED; 
  
  static {
	boolean enabled = true;
    try {
    	Class.forName("org.osgi.framework.wiring.BundleWiring");
    } catch (ClassNotFoundException cnfe) {
    	enabled = false;
    }
    CONTEXT_TRACKING_ENABLED = enabled;
  }
	
  private final Bundle bundle;

  private final ThreadLocal<Deque<Bundle>> currentLoadingBundle = new ThreadLocal<Deque<Bundle>>(){
	@Override
	protected Deque<Bundle> initialValue() {
		return new LinkedList<Bundle>();
	}
  };
  
  public TempBundleDelegatingClassLoader(Bundle b, ClassLoader parent) {
    super(parent);
    bundle = b;
  }
  
  @Override
  protected Class<?> findClass(String className) throws ClassNotFoundException {
    String classResName = className.replace('.', '/').concat(".class");
    
    //Don't use loadClass, just load the bytes and call defineClass
    Bundle currentContext = currentLoadingBundle.get().peek();
    InputStream is;
    if(currentContext == null) {
      is = getResourceAsStream(classResName);
    } else {
      is = getResourceInBundleAsStream(classResName, currentContext);
    }
    
    if(is == null)
      throw new ClassNotFoundException(className);
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    byte[] buff = new byte[4096];
    try {
      try {
        int read = is.read(buff);
        while(read >0) {
          baos.write(buff, 0, read);
          read = is.read(buff);
        }
      }finally {
        is.close();
      }
    } catch (IOException ioe) {
      throw new ClassNotFoundException(className, ioe);
    } 
    
    buff = baos.toByteArray();

    if(CONTEXT_TRACKING_ENABLED) {
    	updateContext(currentContext, className);
    }
    try {
    	return defineClass(className, buff, 0, buff.length);
    } finally {
    	if(CONTEXT_TRACKING_ENABLED) {
        	currentLoadingBundle.get().pop();
        }
    }
  }

  private void updateContext(Bundle currentContext, String className) {
	if(currentContext == null) {
		currentContext = bundle;
	}
	
	int idx = className.lastIndexOf('.');
	String packageName = (idx == -1) ? "" : className.substring(0, idx);
	
	Bundle contextToSet = currentContext;
	
	BundleWiring wiring = currentContext.adapt(BundleWiring.class);
	for(BundleWire wire : wiring.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE)) {
	  if(wire.getCapability().getAttributes().get(BundleRevision.PACKAGE_NAMESPACE).equals(packageName)) {
	    contextToSet = wire.getProviderWiring().getBundle();
	    break;
	  }
	}
	currentLoadingBundle.get().push(contextToSet);
  }

@Override
  protected URL findResource(final String resName)
  {
    return findResourceInBundle(resName, bundle);
  }
  
  protected URL findResourceInBundle(final String resName, final Bundle inBundle)
  {
    //Bundle.getResource requires privileges that the client may not have but we need
    //use a doPriv so that only this bundle needs the privileges
    return AccessController.doPrivileged(new PrivilegedAction<URL>() {

      public URL run()
      {
        return inBundle.getResource(resName);
      }
    });
  }

  private InputStream getResourceInBundleAsStream(final String resName, final Bundle inBundle) {
	  URL url = findResourceInBundle(resName, inBundle);
	  try {
		return (url == null) ? null : url.openStream();
	} catch (IOException e) {
		return null;
	}
  }
  
  @Override
  protected Enumeration<URL> findResources(final String resName) throws IOException
  {
    return findResourcesInBundle(resName, bundle);
  }
  
  protected Enumeration<URL> findResourcesInBundle(final String resName, final Bundle inBundle) throws IOException
  {
    Enumeration<URL> resources = null;
    try {
      //Bundle.getResources requires privileges that the client may not have but we need
      //use a doPriv so that only this bundle needs the privileges
      resources = AccessController.doPrivileged(new PrivilegedExceptionAction<Enumeration<URL>>() {

        public Enumeration<URL> run() throws IOException
        {
          return inBundle.getResources(resName);
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
