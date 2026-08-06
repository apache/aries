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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;

import aQute.bnd.annotation.baseline.BaselineIgnore;

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

    public static <C,S> ServiceLoader<S> serviceLoaderLoad(Class<S> service, Class<C> caller) {
        if (BaseActivator.activator == null) {
            // The system is not yet initialized. We can't do anything.
            return null;
        }

        Bundle consumerBundle = getConsumerBundle(caller);
        if (consumerBundle == null) {
            return ServiceLoader.load(service);
        }
        final ClassLoader bundleClassloader = findContextClassloader(
            consumerBundle, ServiceLoader.class.getName(), "load", service, true);

        if (bundleClassloader == null
                && !BaseActivator.activator.isStandardConsumer(consumerBundle)) {
            return ServiceLoader.load(service);
        }

        return AccessController.doPrivileged(
            new PrivilegedAction<ServiceLoader<S>>() {
                @Override
                public ServiceLoader<S> run() {
                    ClassLoader contextClassLoader =
                            Thread.currentThread().getContextClassLoader();
                    return ServiceLoader.load(service, new ProviderViewClassLoader(
                            contextClassLoader, bundleClassloader, consumerBundle,
                            service.getName()));
                }
            }
        );

    }

    public static <C,S> ServiceLoader<S> serviceLoaderLoad(
        Class<S> service, ClassLoader specifiedClassLoader, Class<C> caller) {

        if (BaseActivator.activator == null) {
            // The system is not yet initialized. We can't do anything.
            return null;
        }

        Bundle consumerBundle = getConsumerBundle(caller);
        if (consumerBundle == null) {
            return ServiceLoader.load(service, specifiedClassLoader);
        }
        final ClassLoader bundleClassloader = findContextClassloader(
            consumerBundle, ServiceLoader.class.getName(), "load", service, true);

        if (bundleClassloader == null) {
            if (BaseActivator.activator.isStandardConsumer(consumerBundle)) {
                return ServiceLoader.load(service, new ProviderViewClassLoader(
                        specifiedClassLoader, null, consumerBundle, service.getName()));
            }
            return ServiceLoader.load(service, specifiedClassLoader);
        }

        if (BaseActivator.activator.isStandardConsumer(consumerBundle)) {
            return ServiceLoader.load(service, new ProviderViewClassLoader(
                    specifiedClassLoader, bundleClassloader, consumerBundle,
                    service.getName()));
        }
        return ServiceLoader.load(service, new WrapperCL(specifiedClassLoader, bundleClassloader));
    }

    @BaselineIgnore("1.4.0")
    public static <C,S> ServiceLoader<S> serviceLoaderLoadInstalled(
            Class<S> service, Class<C> caller) {
        if (BaseActivator.activator == null) {
            return null;
        }

        Bundle consumerBundle = getConsumerBundle(caller);
        if (consumerBundle == null) {
            return ServiceLoader.loadInstalled(service);
        }

        ClassLoader bundleClassloader = findContextClassloader(
                consumerBundle, ServiceLoader.class.getName(),
                "loadInstalled", service, true);
        if (bundleClassloader == null
                && !BaseActivator.activator.isStandardConsumer(consumerBundle)) {
            return ServiceLoader.loadInstalled(service);
        }

        ClassLoader installedClassLoader = getInstalledClassLoader();
        return ServiceLoader.load(service, new ProviderViewClassLoader(
                installedClassLoader, bundleClassloader, consumerBundle,
                service.getName()));
    }

    private static ClassLoader getInstalledClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                ClassLoader loader = ClassLoader.getSystemClassLoader();
                while (loader != null && loader.getParent() != null) {
                    loader = loader.getParent();
                }
                return loader;
            }
        });
    }

    private static Bundle getConsumerBundle(final Class<?> caller) {
        Bundle bundle = FrameworkUtil.getBundle(caller);
        if (bundle != null) {
            return bundle;
        }

        ClassLoader bundleLoader = AccessController.doPrivileged(
                new PrivilegedAction<ClassLoader>() {
                    @Override
                    public ClassLoader run() {
                        return caller.getClassLoader();
                    }
                });
        if (bundleLoader instanceof BundleReference) {
            return ((BundleReference) bundleLoader).getBundle();
        }

        BaseActivator.activator.log(Level.FINE,
                "Could not identify consuming bundle for class " + caller.getName());
        return null;
    }

    public static void fixContextClassloader(String cls, String method, Class<?> clsArg, ClassLoader bundleLoader) {
        BundleReference br = getBundleReference(bundleLoader);

        if (br == null) {
            return;
        }

        final ClassLoader cl = findContextClassloader(
                br.getBundle(), cls, method, clsArg, false);
        if (cl != null) {
            BaseActivator.activator.log(Level.FINE, "Temporarily setting Thread Context Classloader to: " + cl);
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    Thread.currentThread().setContextClassLoader(cl);
                    return null;
                }
            });
        } else {
            BaseActivator.activator.log(Level.FINE, "No classloader found for " + cls + ":" + method + "(" + clsArg + ")");
        }
    }

    private static ClassLoader findContextClassloader(Bundle consumerBundle, String className,
            String methodName, Class<?> clsArg, boolean permissionAware) {
        BaseActivator activator = BaseActivator.activator;

        String requestedClass;
        Map<Pair<Integer, String>, String> args;
        boolean serviceLoaderCall = ServiceLoader.class.getName().equals(className)
                && ("load".equals(methodName) || "loadInstalled".equals(methodName));
        if (serviceLoaderCall) {
            requestedClass = clsArg.getName();
            args = new HashMap<Pair<Integer,String>, String>();
            args.put(new Pair<Integer, String>(0, Class.class.getName()), requestedClass);

        } else {
            requestedClass = className;
            args = null; // only supported on ServiceLoader.load() at the moment
        }

        Collection<Bundle> bundles = new ArrayList<Bundle>(activator.findProviderBundles(requestedClass));
        activator.log(Level.FINE, "Found bundles providing " + requestedClass + ": " + bundles);

        Collection<Bundle> allowedBundles = activator.findConsumerRestrictions(consumerBundle, className, methodName, args);

        if (allowedBundles != null) {
            for (Iterator<Bundle> it = bundles.iterator(); it.hasNext(); ) {
                if (!allowedBundles.contains(it.next())) {
                    it.remove();
                }
            }
        }

        if (serviceLoaderCall) {
            bundles = activator.filterCompatibleProviderBundles(
                    consumerBundle, clsArg, bundles);
        }

        if (serviceLoaderCall && permissionAware
                && activator.isStandardConsumer(consumerBundle)
                && !bundles.isEmpty()) {
            return new ProviderAdvertisementClassLoader(
                    activator.findProviderAdvertisements(requestedClass, bundles),
                    requestedClass);
        }

        switch (bundles.size()) {
        case 0:
            return null;
        case 1:
            Bundle bundle = bundles.iterator().next();
            return serviceLoaderCall && permissionAware
                    ? getProviderClassLoader(bundle, requestedClass)
                    : getBundleClassLoader(bundle);
        default:
            List<ClassLoader> loaders = new ArrayList<ClassLoader>();
            for (Bundle b : bundles) {
                loaders.add(serviceLoaderCall && permissionAware
                        ? getProviderClassLoader(b, requestedClass)
                        : getBundleClassLoader(b));
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

    private static ClassLoader getProviderClassLoader(Bundle providerBundle,
            String serviceType) {
        return new ProviderBundleClassLoader(providerBundle,
                getBundleClassLoader(providerBundle), serviceType);
    }

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

    private static BundleReference getBundleReference(ClassLoader bundleLoader) {
        if (BaseActivator.activator == null) {
            // The system is not yet initialized. We can't do anything.
            return null;
        }

        if (!(bundleLoader instanceof BundleReference)) {
            BaseActivator.activator.log(Level.FINE, "Classloader of consuming bundle doesn't implement BundleReference: " + bundleLoader);
            return null;
        }

        return (BundleReference) bundleLoader;
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
            BaseActivator.activator.log(Level.FINE, "Problem loading class from embedded jar file: " + url +
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

    private static class WrapperCL extends ClassLoader {
        private final ClassLoader bundleClassloader;
        public WrapperCL(ClassLoader specifiedClassLoader, ClassLoader bundleClassloader) {
            super(specifiedClassLoader);
            this.bundleClassloader = bundleClassloader;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            return bundleClassloader.loadClass(name);
        }

        @Override
        protected URL findResource(String name) {
            return bundleClassloader.getResource(name);
        }

        @Override
        protected Enumeration<URL> findResources(String name) throws IOException {
            return bundleClassloader.getResources(name);
        }
    }

    private static class ProviderViewClassLoader extends ClassLoader {
        private final ClassLoader providerClassLoader;
        private final Bundle consumerBundle;
        private final String serviceType;
        private final String providerConfiguration;

        ProviderViewClassLoader(ClassLoader parent, ClassLoader providerClassLoader,
                Bundle consumerBundle, String serviceType) {
            super(parent);
            this.providerClassLoader = providerClassLoader;
            this.consumerBundle = consumerBundle;
            this.serviceType = serviceType;
            providerConfiguration = "META-INF/services/" + serviceType;
        }

        @Override
        public URL getResource(String name) {
            if (providerConfiguration.equals(name)) {
                return !hasGetPermission() || providerClassLoader == null
                        ? null : providerClassLoader.getResource(name);
            }
            return super.getResource(name);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            if (providerConfiguration.equals(name)) {
                return !hasGetPermission() || providerClassLoader == null
                        ? java.util.Collections.<URL>emptyEnumeration()
                        : providerClassLoader.getResources(name);
            }
            return super.getResources(name);
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            if (!hasGetPermission() || providerClassLoader == null) {
                throw new ClassNotFoundException(name);
            }
            Class<?> cls = providerClassLoader.loadClass(name);
            if (resolve) {
                resolveClass(cls);
            }
            return cls;
        }

        @Override
        protected URL findResource(String name) {
            return !hasGetPermission() || providerClassLoader == null
                    ? null : providerClassLoader.getResource(name);
        }

        @Override
        protected Enumeration<URL> findResources(String name) throws IOException {
            return !hasGetPermission() || providerClassLoader == null
                    ? java.util.Collections.<URL>emptyEnumeration()
                    : providerClassLoader.getResources(name);
        }

        private boolean hasGetPermission() {
            boolean permitted = consumerBundle.hasPermission(
                    new ServicePermission(serviceType, ServicePermission.GET));
            if (!permitted) {
                BaseActivator.activator.log(Level.FINE, "Bundle " + consumerBundle
                        + " does not have permission to obtain services of type: "
                        + serviceType);
            }
            return permitted;
        }
    }

    private static class ProviderAdvertisementClassLoader extends ClassLoader {
        private final String serviceType;
        private final String providerConfiguration;
        private final Map<String, List<BaseActivator.ProviderAdvertisement>>
                advertisersByImplementation =
                        new LinkedHashMap<String, List<BaseActivator.ProviderAdvertisement>>();

        ProviderAdvertisementClassLoader(
                List<BaseActivator.ProviderAdvertisement> advertisements,
                String serviceType) {
            super(null);
            this.serviceType = serviceType;
            providerConfiguration = "META-INF/services/" + serviceType;
            for (BaseActivator.ProviderAdvertisement advertisement : advertisements) {
                for (String implementation : advertisement.getImplementationNames()) {
                    advertisersByImplementation.computeIfAbsent(implementation,
                            key -> new ArrayList<BaseActivator.ProviderAdvertisement>())
                            .add(advertisement);
                }
            }
        }

        @Override
        public URL getResource(String name) {
            if (!providerConfiguration.equals(name)) {
                return null;
            }
            return createProviderConfiguration();
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            URL configuration = getResource(name);
            return configuration == null
                    ? java.util.Collections.<URL>emptyEnumeration()
                    : java.util.Collections.enumeration(
                            java.util.Collections.singleton(configuration));
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            List<BaseActivator.ProviderAdvertisement> advertisements =
                    advertisersByImplementation.get(name);
            if (advertisements == null) {
                throw new ClassNotFoundException(name);
            }

            ClassNotFoundException last = null;
            for (BaseActivator.ProviderAdvertisement advertisement : advertisements) {
                if (!isProviderAvailable(advertisement, serviceType)) {
                    continue;
                }
                try {
                    return advertisement.getBundle().loadClass(name);
                }
                catch (ClassNotFoundException e) {
                    last = e;
                }
            }
            throw last == null ? new ClassNotFoundException(name) : last;
        }

        private URL createProviderConfiguration() {
            Set<String> implementations = new LinkedHashSet<String>();
            for (Map.Entry<String, List<BaseActivator.ProviderAdvertisement>> entry
                    : advertisersByImplementation.entrySet()) {
                for (BaseActivator.ProviderAdvertisement advertisement : entry.getValue()) {
                    if (isProviderAvailable(advertisement, serviceType)) {
                        implementations.add(entry.getKey());
                        break;
                    }
                }
            }
            if (implementations.isEmpty()) {
                return null;
            }

            StringBuilder contents = new StringBuilder();
            for (String implementation : implementations) {
                contents.append(implementation).append('\n');
            }
            final byte[] bytes = contents.toString().getBytes(StandardCharsets.UTF_8);
            try {
                return new URL(null,
                        "spifly:" + serviceType + "/"
                                + Integer.toHexString(System.identityHashCode(this)),
                        new URLStreamHandler() {
                            @Override
                            protected URLConnection openConnection(URL url) {
                                return new URLConnection(url) {
                                    @Override
                                    public void connect() {
                                    }

                                    @Override
                                    public InputStream getInputStream() {
                                        return new ByteArrayInputStream(bytes);
                                    }
                                };
                            }
                        });
            }
            catch (java.net.MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static class ProviderBundleClassLoader extends ClassLoader {
        private final Bundle providerBundle;
        private final ClassLoader delegate;
        private final String serviceType;

        ProviderBundleClassLoader(Bundle providerBundle, ClassLoader delegate,
                String serviceType) {
            super(null);
            this.providerBundle = providerBundle;
            this.delegate = delegate;
            this.serviceType = serviceType;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (!hasRegisterPermission()) {
                throw new ClassNotFoundException(name);
            }
            return delegate.loadClass(name);
        }

        @Override
        public URL getResource(String name) {
            return hasRegisterPermission() ? delegate.getResource(name) : null;
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            return hasRegisterPermission()
                    ? delegate.getResources(name)
                    : java.util.Collections.<URL>emptyEnumeration();
        }

        private boolean hasRegisterPermission() {
            boolean permitted = providerBundle.hasPermission(
                    new ServicePermission(serviceType, ServicePermission.REGISTER));
            if (!permitted) {
                BaseActivator.activator.log(Level.FINE, "Bundle " + providerBundle
                        + " does not have permission to provide services of type: "
                        + serviceType);
            }
            return permitted;
        }
    }

    private static boolean isProviderAvailable(
            BaseActivator.ProviderAdvertisement advertisement, String serviceType) {
        return isProviderAvailable(advertisement.getBundle(),
                advertisement.getRevision(), serviceType);
    }

    private static boolean isProviderAvailable(Bundle providerBundle,
            BundleRevision providerRevision, String serviceType) {
        if (providerBundle.getState() != Bundle.ACTIVE) {
            BaseActivator.activator.log(Level.FINE, "Bundle " + providerBundle
                    + " is not active and cannot provide services of type: "
                    + serviceType);
            return false;
        }
        if (!providerBundle.hasPermission(
                new ServicePermission(serviceType, ServicePermission.REGISTER))) {
            BaseActivator.activator.log(Level.FINE, "Bundle " + providerBundle
                    + " does not have permission to provide services of type: "
                    + serviceType);
            return false;
        }
        if (providerRevision != null) {
            BundleWiring wiring = WiringUtils.getWiring(providerBundle);
            if (wiring == null || wiring.getRevision() != providerRevision) {
                BaseActivator.activator.log(Level.FINE, "Bundle " + providerBundle
                        + " no longer has the revision that advertised service type: "
                        + serviceType);
                return false;
            }
        }
        return true;
    }
}
