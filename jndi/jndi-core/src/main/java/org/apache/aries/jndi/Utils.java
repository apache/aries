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
package org.apache.aries.jndi;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.NamingException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jndi.JNDIConstants;

/**
 */
public final class Utils {

    public static final Comparator<ServiceReference> SERVICE_REFERENCE_COMPARATOR = 
        new ServiceReferenceComparator();

    /** Ensure no one constructs us */
    private Utils() {
        throw new RuntimeException();
    }
     
    private static class StackFinder extends SecurityManager {
        public Class<?>[] getClassContext() {
            return super.getClassContext();
        }
    }
    
    /**
     * @param env
     * @return the bundle context for the caller.
     * @throws NamingException
     */
    public static BundleContext getBundleContext(final Map<?, ?> env, 
                                                 final String namingClass) {
        return AccessController.doPrivileged(new PrivilegedAction<BundleContext>() {
            public BundleContext run() {
                return doGetBundleContext(env, namingClass);
            }
        });
    }
    
    private static BundleContext doGetBundleContext(Map<?, ?> env, String namingClass) {
        BundleContext result = null;

        Object bc = (env == null) ? null : env.get(JNDIConstants.BUNDLE_CONTEXT);

        if (bc != null && bc instanceof BundleContext) {
            result = (BundleContext) bc;
        } else {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            result = getBundleContext(cl);
        }

        if (result == null) {
            StackFinder finder = new StackFinder();
            Class<?>[] classStack = finder.getClassContext();

            // find constructor of given naming class
            int indexOfConstructor = -1;
            for (int i = 0 ; i < classStack.length; i++) {
                if (classStack[i].getName().equals(namingClass)) {
                    indexOfConstructor = i;
                }
            }
            
            // get the caller of the constructor
            if (indexOfConstructor >= 0 && (indexOfConstructor + 1) < classStack.length) {
                Class callerClass = classStack[indexOfConstructor + 1];
                result = getBundleContext(callerClass.getClassLoader());
            }
        }

        return result;
    }

    private static BundleContext getBundleContext(ClassLoader cl2) {
        ClassLoader cl = cl2;
        BundleContext result = null;
        while (result == null && cl != null) {
            if (cl instanceof BundleReference) {
                result = ((BundleReference) cl).getBundle().getBundleContext();
            } else if (cl != null) {
                cl = cl.getParent();
            }
        }

        return result;
    }
    
    private static class ServiceReferenceComparator implements Comparator<ServiceReference> {        
        public int compare(ServiceReference o1, ServiceReference o2) {        
          return o2.compareTo(o1);
        }
    }
    
    public static String getSystemProperty(final String key, final String defaultValue) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty(key, defaultValue);
            }            
        });
    }
    
    public static Hashtable toHashtable(Map map) {
        Hashtable env;
        if (map instanceof Hashtable) {
            env = (Hashtable) map;
        } else {
            env = new Hashtable();
            if (map != null) {
                env.putAll(map);
            }
        }
        return env;
    }
    
    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action) throws Exception {
        try {
            return AccessController.doPrivileged(action);
        } catch (PrivilegedActionException e) {
            Exception cause = e.getException();
            throw cause;
        }
    }
    
    public static <T> T doPrivilegedNaming(PrivilegedExceptionAction<T> action) throws NamingException {
        try {
            return AccessController.doPrivileged(action);
        } catch (PrivilegedActionException e) {
            Exception cause = e.getException();
            if (cause instanceof NamingException) {
                throw (NamingException) cause;
            } else {
                NamingException ex = new NamingException(cause.getMessage());
                ex.initCause(cause);
                throw ex;
            }
        }
    }
}
