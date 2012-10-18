/**
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
package org.apache.aries.spifly;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.ServicePermission;
import org.osgi.service.log.LogService;

/**
 * Methods used from ASM-generated code. They store, change and reset the thread context classloader.
 * The methods are static to make it easy to access them from generated code.
 */
public class Util {
    static ThreadLocal<ClassLoader> storedClassLoaders = new ThreadLocal<ClassLoader>();

    // Provided as static method to make it easier to call from ASM-modified code
    public static void storeContextClassloader() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                storedClassLoaders.set(Thread.currentThread().getContextClassLoader());
                return null;
            }
        });
    }

    // Provided as static method to make it easier to call from ASM-modified code
    public static void restoreContextClassloader() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                Thread.currentThread().setContextClassLoader(storedClassLoaders.get());
                storedClassLoaders.set(null);
                return null;
            }
        });
    }

    public static void fixContextClassloader(String cls, String method, Class<?> clsArg, ClassLoader bundleLoader) {
        if (!(bundleLoader instanceof BundleReference)) {
            BaseActivator.activator.log(LogService.LOG_WARNING, "Classloader of consuming bundle doesn't implement BundleReference: " + bundleLoader);
            return;
        }

        BundleReference br = ((BundleReference) bundleLoader);

        final ClassLoader cl = findContextClassloader(br.getBundle(), cls, method, clsArg);
        if (cl != null) {
            BaseActivator.activator.log(LogService.LOG_INFO, "Temporarily setting Thread Context Classloader to: " + cl);
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    Thread.currentThread().setContextClassLoader(cl);
                    return null;
                }
            });
        } else {
            BaseActivator.activator.log(LogService.LOG_WARNING, "No classloader found for " + cls + ":" + method + "(" + clsArg + ")");
        }
    }

    private static ClassLoader findContextClassloader(Bundle consumerBundle, String className, String methodName, Class<?> clsArg) {
        BaseActivator activator = BaseActivator.activator;

        String requestedClass;
        Map<Pair<Integer, String>, String> args;
        if (ServiceLoader.class.getName().equals(className) && "load".equals(methodName)) {
            requestedClass = clsArg.getName();
            args = new HashMap<Pair<Integer,String>, String>();
            args.put(new Pair<Integer, String>(0, Class.class.getName()), requestedClass);

            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                try {
                    sm.checkPermission(new ServicePermission(requestedClass, ServicePermission.GET));
                } catch (AccessControlException ace) {
                    // access denied
                    activator.log(LogService.LOG_INFO, "No permission to obtain service of type: " + requestedClass);
                    return null;
                }
            }
        } else {
            requestedClass = className;
            args = null; // only supported on ServiceLoader.load() at the moment
        }

        Collection<Bundle> bundles = new ArrayList<Bundle>(activator.findProviderBundles(requestedClass));
        activator.log(LogService.LOG_DEBUG, "Found bundles providing " + requestedClass + ": " + bundles);

        Collection<Bundle> allowedBundles = activator.findConsumerRestrictions(consumerBundle, className, methodName, args);

        if (allowedBundles != null) {
            for (Iterator<Bundle> it = bundles.iterator(); it.hasNext(); ) {
                if (!allowedBundles.contains(it.next())) {
                    it.remove();
                }
            }
        }

        switch (bundles.size()) {
        case 0:
            return null;
        case 1:
            Bundle bundle = bundles.iterator().next();
            return getBundleClassLoader(bundle);
        default:
            List<ClassLoader> loaders = new ArrayList<ClassLoader>();
            for (Bundle b : bundles) {
                loaders.add(getBundleClassLoader(b));
            }
            return new MultiDelegationClassloader(loaders.toArray(new ClassLoader[loaders.size()]));
        }
    }

    private static ClassLoader getBundleClassLoader(final Bundle b) {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return getBundleClassLoaderPrivileged(b);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static ClassLoader getBundleClassLoaderPrivileged(Bundle b) {
        // In 4.3 this can be done much easier by using the BundleWiring, but we want this code to
        // be 4.2 compliant.
        // Here we're just finding any class in the bundle, load that and then use its classloader.

        try {
            Method adaptMethod = Bundle.class.getMethod("adapt", Class.class);
            if (adaptMethod != null) {
                return getBundleClassLoaderViaAdapt(b, adaptMethod);
            }
        } catch (Exception e) {
            // No Bundle.adapt(), use the fallback approach to find the bundle classloader
        }

        List<String> rootPaths = new ArrayList<String>();
        rootPaths.add("/");

        while(rootPaths.size() > 0) {
            String rootPath = rootPaths.remove(0);

            Enumeration<String> paths = b.getEntryPaths(rootPath);
            while(paths != null && paths.hasMoreElements()) {
                String path = paths.nextElement();
                if (path.endsWith(".class")) {
                    ClassLoader cl = getClassLoaderFromClassResource(b, path);
                    if (cl != null)
                        return cl;
                } else if (path.endsWith("/")) {
                    rootPaths.add(path);
                }
            }
        }

        // if we can't find any classes in the bundle directly, try the Bundle-ClassPath
        Object bcp = b.getHeaders().get(Constants.BUNDLE_CLASSPATH);
        if (bcp instanceof String) {
            for (String entry : ((String) bcp).split(",")) {
                entry = entry.trim();
                if (entry.equals("."))
                    continue;

                URL url = b.getResource(entry);
                if (url != null) {
                    ClassLoader cl = getClassLoaderViaBundleClassPath(b, url);
                    if (cl != null)
                        return cl;
                }
            }
        }
        throw new RuntimeException("Could not obtain classloader for bundle " + b);
    }

    private static ClassLoader getBundleClassLoaderViaAdapt(Bundle b, Method adaptMethod) {
        // This method uses reflection to avoid a hard dependency on OSGi 4.3 APIs
        try {
            // Load the BundleRevision and BundleWiring classes from the System Bundle.
            Bundle systemBundle = b.getBundleContext().getBundle(0);

            Class<?> bundleRevisionClass = systemBundle.loadClass("org.osgi.framework.wiring.BundleRevision");
            Object bundleRevision = adaptMethod.invoke(b, bundleRevisionClass);

            Method getWiringMethod = bundleRevisionClass.getDeclaredMethod("getWiring");
            Object bundleWiring = getWiringMethod.invoke(bundleRevision);

            Class<?> bundleWiringClass = systemBundle.loadClass("org.osgi.framework.wiring.BundleWiring");
            Method getClassLoaderMethod = bundleWiringClass.getDeclaredMethod("getClassLoader");

            return (ClassLoader) getClassLoaderMethod.invoke(bundleWiring);
        } catch (Exception e) {
            throw new RuntimeException("Can't obtain Bundle Class Loader for bundle: " + b, e);
        }
    }

    private static ClassLoader getClassLoaderViaBundleClassPath(Bundle b, URL url) {
        try {
            JarInputStream jis = null;
            try {
                jis = new JarInputStream(url.openStream());

                JarEntry je = null;
                while ((je = jis.getNextJarEntry()) != null) {
                    String path = je.getName();
                    if (path.endsWith(".class")) {
                        ClassLoader cl = getClassLoaderFromClassResource(b, path);
                        if (cl != null)
                            return cl;
                    }
                }
            } finally {
                if (jis != null)
                    jis.close();
            }
        } catch (IOException e) {
            BaseActivator.activator.log(LogService.LOG_ERROR, "Problem loading class from embedded jar file: " + url +
                " in bundle " + b.getSymbolicName(), e);
        }
        return null;
    }

    private static ClassLoader getClassLoaderFromClassResource(Bundle b, String path) {
        String className = path.substring(0, path.length() - ".class".length());
        if (className.startsWith("/"))
            className = className.substring(1);

        className = className.replace('/', '.');
        try {
            Class<?> cls = b.loadClass(className);
            return cls.getClassLoader();
        } catch (ClassNotFoundException e) {
            // try the next class
        }
        return null;
    }
}
