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

import org.apache.aries.util.nls.MessageUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jndi.JNDIConstants;

/**
 */
public final class Utils {

    public static final Comparator<ServiceReference> SERVICE_REFERENCE_COMPARATOR = 
        new ServiceReferenceComparator();
    public static final MessageUtil MESSAGES = MessageUtil.createMessageUtil(Utils.class, "org.apache.aries.jndi.nls.jndiMessages");

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
                                                 final Class<?> namingClass) {
        return AccessController.doPrivileged(new PrivilegedAction<BundleContext>() {
            public BundleContext run() {
                return doGetBundleContext(env, namingClass);
            }
        });
    }
    
    private static BundleContext doGetBundleContext(Map<?, ?> env, Class<?> namingClass) {
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

            // working from the root of the stack look for the first instance in the stack of this class
            int i = classStack.length - 1;
            for (; i >= 0; i--) {
              if (namingClass.isAssignableFrom(classStack[i])) {
                break;
              }
            }
            
            // then go to the parent of the namingClass down the stack until we find a BundleContext
            for (i++; i < classStack.length && result == null; i++) {
              result = getBundleContext(classStack[i].getClassLoader());
            }
        }

        return result;
    }

    private static BundleContext getBundleContext(ClassLoader cl2) {
        ClassLoader cl = cl2;
        BundleContext result = null;
        while (result == null && cl != null) {
            if (cl instanceof BundleReference) {
                Bundle b = ((BundleReference)cl).getBundle();
                result = b.getBundleContext();
                if (result == null) {
                  try {
                    b.start();
                    result = b.getBundleContext();
                  } catch (BundleException e) {
                  }
                  break;
                }
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
    
    public static Hashtable<?,?> toHashtable(Map<?,?> map) {
        Hashtable<?,?> env;
        if (map instanceof Hashtable<?,?>) {
            env = (Hashtable<?,?>) map;
        } else if (map == null) {
            env = new Hashtable<Object,Object>();
        } else {
            env = new Hashtable<Object,Object>(map);
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
    
    public static ServiceReference[] getReferencesPrivileged(final BundleContext ctx, final Class<?> clazz) {
    	return AccessController.doPrivileged(new PrivilegedAction<ServiceReference[]>() {
    		public ServiceReference[] run() {
    			try {
    				return ctx.getServiceReferences(clazz.getName(), null);
    			} catch (InvalidSyntaxException ise) {
    				// should not happen
    				throw new RuntimeException(MESSAGES.getMessage("null.is.invalid.filter"), ise);
    			}
    		}    		
		});
    }
    
    public static Object getServicePrivileged(final BundleContext ctx, final ServiceReference ref) {
    	return AccessController.doPrivileged(new PrivilegedAction<Object>() {
    		public Object run() {
    			return ctx.getService(ref);
    		}
		});
    }

}
