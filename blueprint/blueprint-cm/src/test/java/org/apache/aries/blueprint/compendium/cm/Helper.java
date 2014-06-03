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
package org.apache.aries.blueprint.compendium.cm;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.jar.JarInputStream;

import de.kalpatec.pojosr.framework.PojoServiceRegistryFactoryImpl;
import de.kalpatec.pojosr.framework.launch.BundleDescriptor;
import de.kalpatec.pojosr.framework.launch.ClasspathScanner;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistry;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistryFactory;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundle;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class Helper {

    public static final long DEFAULT_TIMEOUT = 30000;
    public static final String BUNDLE_FILTER = "(Bundle-SymbolicName=*)";
    public static final String BUNDLE_VERSION = "1.0.0";
    private static final transient Logger LOG = LoggerFactory.getLogger(Helper.class);

    private Helper() {
    }

    public static BundleContext createBundleContext(String name, String descriptors, boolean includeTestBundle) throws Exception {
        return createBundleContext(name, descriptors, includeTestBundle, BUNDLE_FILTER, BUNDLE_VERSION);
    }

    public static BundleContext createBundleContext(String name, String descriptors, boolean includeTestBundle,
                                                    String bundleFilter, String testBundleVersion) throws Exception {
        TinyBundle bundle = null;

        if (includeTestBundle) {
            // add ourselves as a bundle
            bundle = createTestBundle(name, testBundleVersion, descriptors);
        }

        return createBundleContext(bundleFilter, new TinyBundle[] { bundle });
    }

    public static BundleContext createBundleContext(String bundleFilter, TinyBundle[] testBundles) throws Exception {
        deleteDirectory("target/bundles");
        createDirectory("target/bundles");

        // ensure pojosr stores bundles in an unique target directory
        System.setProperty("org.osgi.framework.storage", "target/bundles/" + System.currentTimeMillis());

        // get the bundles
        List<BundleDescriptor> bundles = getBundleDescriptors(bundleFilter);

        // Add the test bundles at the beginning of the list so that they get started first.
        // The reason is that the bundle tracker used by blueprint does not work well
        // with pojosr because it does not support bundle hooks, so events are lost.
        if (testBundles != null) {
            for (TinyBundle bundle : testBundles) {
                File tmp = File.createTempFile("test-", ".jar", new File("target/bundles/"));
                tmp.delete();
                bundles.add(0, getBundleDescriptor(tmp.getPath(), bundle));
            }
        }

        if (LOG.isDebugEnabled()) {
            for (int i = 0; i < bundles.size(); i++) {
                BundleDescriptor desc = bundles.get(i);
                LOG.debug("Bundle #{} -> {}", i, desc);
            }
        }

        // setup pojosr to use our bundles
        Map<String, List<BundleDescriptor>> config = new HashMap<String, List<BundleDescriptor>>();
        config.put(PojoServiceRegistryFactory.BUNDLE_DESCRIPTORS, bundles);

        // create pojorsr osgi service registry
        PojoServiceRegistry reg = new PojoServiceRegistryFactoryImpl().newPojoServiceRegistry(config);
        return reg.getBundleContext();
    }

    public static void disposeBundleContext(BundleContext bundleContext) throws BundleException {
        try {
            if (bundleContext != null) {
                bundleContext.getBundle().stop();
            }
        } finally {
            System.clearProperty("org.osgi.framework.storage");
        }
    }

    public static <T> T getOsgiService(BundleContext bundleContext, Class<T> type, long timeout) {
        return getOsgiService(bundleContext, type, null, timeout);
    }

    public static <T> T getOsgiService(BundleContext bundleContext, Class<T> type) {
        return getOsgiService(bundleContext, type, null, DEFAULT_TIMEOUT);
    }

    public static <T> T getOsgiService(BundleContext bundleContext, Class<T> type, String filter) {
        return getOsgiService(bundleContext, type, filter, DEFAULT_TIMEOUT);
    }

    public static <T> ServiceReference getOsgiServiceReference(BundleContext bundleContext, Class<T> type, String filter, long timeout) {
        ServiceTracker tracker = null;
        try {
            String flt;
            if (filter != null) {
                if (filter.startsWith("(")) {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")" + filter + ")";
                } else {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")(" + filter + "))";
                }
            } else {
                flt = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
            }
            Filter osgiFilter = FrameworkUtil.createFilter(flt);
            tracker = new ServiceTracker(bundleContext, osgiFilter, null);
            tracker.open(true);
            // Note that the tracker is not closed to keep the reference
            // This is buggy, as the service reference may change i think
            Object svc = tracker.waitForService(timeout);
            if (svc == null) {
                Dictionary<?, ?> dic = bundleContext.getBundle().getHeaders();
                System.err.println("Test bundle headers: " + explode(dic));

                for (ServiceReference ref : asCollection(bundleContext.getAllServiceReferences(null, null))) {
                    System.err.println("ServiceReference: " + ref + ", bundle: " + ref.getBundle() + ", symbolicName: " + ref.getBundle().getSymbolicName());
                }

                for (ServiceReference ref : asCollection(bundleContext.getAllServiceReferences(null, flt))) {
                    System.err.println("Filtered ServiceReference: " + ref + ", bundle: " + ref.getBundle() + ", symbolicName: " + ref.getBundle().getSymbolicName());
                }

                throw new RuntimeException("Gave up waiting for service " + flt);
            }
            return tracker.getServiceReference();
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T getOsgiService(BundleContext bundleContext, Class<T> type, String filter, long timeout) {
        ServiceTracker tracker = null;
        try {
            String flt;
            if (filter != null) {
                if (filter.startsWith("(")) {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")" + filter + ")";
                } else {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")(" + filter + "))";
                }
            } else {
                flt = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
            }
            Filter osgiFilter = FrameworkUtil.createFilter(flt);
            tracker = new ServiceTracker(bundleContext, osgiFilter, null);
            tracker.open(true);
            // Note that the tracker is not closed to keep the reference
            // This is buggy, as the service reference may change i think
            Object svc = tracker.waitForService(timeout);
            if (svc == null) {
                Dictionary<?, ?> dic = bundleContext.getBundle().getHeaders();
                System.err.println("Test bundle headers: " + explode(dic));

                for (ServiceReference ref : asCollection(bundleContext.getAllServiceReferences(null, null))) {
                    System.err.println("ServiceReference: " + ref + ", bundle: " + ref.getBundle() + ", symbolicName: " + ref.getBundle().getSymbolicName());
                }

                for (ServiceReference ref : asCollection(bundleContext.getAllServiceReferences(null, flt))) {
                    System.err.println("Filtered ServiceReference: " + ref + ", bundle: " + ref.getBundle() + ", symbolicName: " + ref.getBundle().getSymbolicName());
                }

                throw new RuntimeException("Gave up waiting for service " + flt);
            }
            return type.cast(svc);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected static TinyBundle createTestBundle(String name, String version, String descriptors) throws FileNotFoundException, MalformedURLException {
        TinyBundle bundle = TinyBundles.newBundle();
        for (URL url : getBlueprintDescriptors(descriptors)) {
            LOG.info("Using Blueprint XML file: " + url.getFile());
            bundle.add("OSGI-INF/blueprint/blueprint-" + url.getFile().replace("/", "-"), url);
        }
        bundle.set("Manifest-Version", "2")
                .set("Bundle-ManifestVersion", "2")
                .set("Bundle-SymbolicName", name)
                .set("Bundle-Version", version);
        return bundle;
    }

    /**
     * Explode the dictionary into a <code>,</code> delimited list of <code>key=value</code> pairs.
     */
    private static String explode(Dictionary<?, ?> dictionary) {
        Enumeration<?> keys = dictionary.keys();
        StringBuffer result = new StringBuffer();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            result.append(String.format("%s=%s", key, dictionary.get(key)));
            if (keys.hasMoreElements()) {
                result.append(", ");
            }
        }
        return result.toString();
    }

    /**
     * Provides an iterable collection of references, even if the original array is <code>null</code>.
     */
    private static Collection<ServiceReference> asCollection(ServiceReference[] references) {
        return references  == null ? new ArrayList<ServiceReference>(0) : Arrays.asList(references);
    }

    /**
     * Gets list of bundle descriptors.
     * @param bundleFilter Filter expression for OSGI bundles.
     *
     * @return List pointers to OSGi bundles.
     * @throws Exception If looking up the bundles fails.
     */
    private static List<BundleDescriptor> getBundleDescriptors(final String bundleFilter) throws Exception {
        return new ClasspathScanner().scanForBundles(bundleFilter);
    }

    /**
     * Gets the bundle descriptors as {@link URL} resources.
     *
     * @param descriptors the bundle descriptors, can be separated by comma
     * @return the bundle descriptors.
     * @throws FileNotFoundException is thrown if a bundle descriptor cannot be found
     */
    private static Collection<URL> getBlueprintDescriptors(String descriptors) throws FileNotFoundException, MalformedURLException {
        List<URL> answer = new ArrayList<URL>();
        String descriptor = descriptors;
        if (descriptor != null) {
            // there may be more resources separated by comma
            Iterator<Object> it = createIterator(descriptor);
            while (it.hasNext()) {
                String s = (String) it.next();
                LOG.trace("Resource descriptor: {}", s);

                // remove leading / to be able to load resource from the classpath
                s = stripLeadingSeparator(s);

                // if there is wildcards for *.xml then we need to find the urls from the package
                if (s.endsWith("*.xml")) {
                    String packageName = s.substring(0, s.length() - 5);
                    // remove trailing / to be able to load resource from the classpath
                    Enumeration<URL> urls = loadResourcesAsURL(packageName);
                    while (urls.hasMoreElements()) {
                        URL url = urls.nextElement();
                        File dir = new File(url.getFile());
                        if (dir.isDirectory()) {
                            File[] files = dir.listFiles();
                            if (files != null) {
                                for (File file : files) {
                                    if (file.isFile() && file.exists() && file.getName().endsWith(".xml")) {
                                        String name = packageName + file.getName();
                                        LOG.debug("Resolving resource: {}", name);
                                        URL xmlUrl = loadResourceAsURL(name);
                                        if (xmlUrl != null) {
                                            answer.add(xmlUrl);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LOG.debug("Resolving resource: {}", s);
                    URL url = resolveMandatoryResourceAsUrl(s);
                    if (url == null) {
                        throw new FileNotFoundException("Resource " + s + " not found");
                    }
                    answer.add(url);
                }
            }
        } else {
            throw new IllegalArgumentException("No bundle descriptor configured. Override getBlueprintDescriptor() or getBlueprintDescriptors() method");
        }

        if (answer.isEmpty()) {
            throw new IllegalArgumentException("Cannot find any resources in classpath from descriptor " + descriptors);
        }
        return answer;
    }

    private static BundleDescriptor getBundleDescriptor(String path, TinyBundle bundle) throws Exception {
        File file = new File(path);
        FileOutputStream fos = new FileOutputStream(file, true);
        try {
            copy(bundle.build(), fos);
        } finally {
            close(fos);
        }

        FileInputStream fis = null;
        JarInputStream jis = null;
        try {
            fis = new FileInputStream(file);
            jis = new JarInputStream(fis);
            Map<String, String> headers = new HashMap<String, String>();
            for (Map.Entry<Object, Object> entry : jis.getManifest().getMainAttributes().entrySet()) {
                headers.put(entry.getKey().toString(), entry.getValue().toString());
            }

            return new BundleDescriptor(
                    bundle.getClass().getClassLoader(),
                    new URL("jar:" + file.toURI().toString() + "!/"),
                    headers);
        } finally {
            close(fis, jis);
        }
    }

    /**
     * Closes the given resource if it is available, logging any closing exceptions to the given log.
     *
     * @param closeable the object to close
     * @param name the name of the resource
     * @param log the log to use when reporting closure warnings, will use this class's own {@link Logger} if <tt>log == null</tt>
     */
    public static void close(Closeable closeable, String name, Logger log) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                if (log == null) {
                    // then fallback to use the own Logger
                    log = LOG;
                }
                if (name != null) {
                    log.warn("Cannot close: " + name + ". Reason: " + e.getMessage(), e);
                } else {
                    log.warn("Cannot close. Reason: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Closes the given resource if it is available.
     *
     * @param closeable the object to close
     * @param name the name of the resource
     */
    public static void close(Closeable closeable, String name) {
        close(closeable, name, LOG);
    }

    /**
     * Closes the given resource if it is available.
     *
     * @param closeable the object to close
     */
    public static void close(Closeable closeable) {
        close(closeable, null, LOG);
    }

    /**
     * Closes the given resources if they are available.
     *
     * @param closeables the objects to close
     */
    public static void close(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            close(closeable);
        }
    }

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
    private static final String DEFAULT_DELIMITER = ",";

    public static int copy(InputStream input, OutputStream output) throws IOException {
        return copy(input, output, DEFAULT_BUFFER_SIZE);
    }

    public static int copy(final InputStream input, final OutputStream output, int bufferSize) throws IOException {
        int avail = input.available();
        if (avail > 262144) {
            avail = 262144;
        }
        if (avail > bufferSize) {
            bufferSize = avail;
        }

        final byte[] buffer = new byte[bufferSize];
        int n = input.read(buffer);
        int total = 0;
        while (-1 != n) {
            output.write(buffer, 0, n);
            total += n;
            n = input.read(buffer);
        }
        output.flush();
        return total;
    }

    /**
     * Creates an iterator over the value if the value is a collection, an
     * Object[], a String with values separated by comma,
     * or a primitive type array; otherwise to simplify the caller's code,
     * we just create a singleton collection iterator over a single value
     * <p/>
     * Will default use comma for String separating String values.
     * This method does <b>not</b> allow empty values
     *
     * @param value  the value
     * @return the iterator
     */
    public static Iterator<Object> createIterator(Object value) {
        return createIterator(value, DEFAULT_DELIMITER);
    }

    /**
     * Creates an iterator over the value if the value is a collection, an
     * Object[], a String with values separated by the given delimiter,
     * or a primitive type array; otherwise to simplify the caller's
     * code, we just create a singleton collection iterator over a single value
     * <p/>
     * This method does <b>not</b> allow empty values
     *
     * @param value      the value
     * @param delimiter  delimiter for separating String values
     * @return the iterator
     */
    public static Iterator<Object> createIterator(Object value, String delimiter) {
        return createIterator(value, delimiter, false);
    }

    /**
     * Creates an iterator over the value if the value is a collection, an
     * Object[], a String with values separated by the given delimiter,
     * or a primitive type array; otherwise to simplify the caller's
     * code, we just create a singleton collection iterator over a single value
     *
     * @param value             the value
     * @param delimiter         delimiter for separating String values
     * @param allowEmptyValues  whether to allow empty values
     * @return the iterator
     */
    @SuppressWarnings("unchecked")
    public static Iterator<Object> createIterator(Object value, String delimiter, final boolean allowEmptyValues) {
        if (value == null) {
            return Collections.emptyList().iterator();
        } else if (value instanceof Iterator) {
            return (Iterator<Object>)value;
        } else if (value instanceof Iterable) {
            return ((Iterable<Object>)value).iterator();
        } else if (value.getClass().isArray()) {
            // TODO we should handle primitive array types?
            List<Object> list = Arrays.asList((Object[])value);
            return list.iterator();
        } else if (value instanceof NodeList) {
            // lets iterate through DOM results after performing XPaths
            final NodeList nodeList = (NodeList) value;
            return cast(new Iterator<Node>() {
                int idx = -1;

                public boolean hasNext() {
                    return (idx + 1) < nodeList.getLength();
                }

                public Node next() {
                    idx++;
                    return nodeList.item(idx);
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            });
        } else if (value instanceof String) {
            final String s = (String) value;

            // this code is optimized to only use a Scanner if needed, eg there is a delimiter

            if (delimiter != null && s.contains(delimiter)) {
                // use a scanner if it contains the delimiter
                Scanner scanner = new Scanner((String)value);

                if (DEFAULT_DELIMITER.equals(delimiter)) {
                    // we use the default delimiter which is a comma, then cater for bean expressions with OGNL
                    // which may have balanced parentheses pairs as well.
                    // if the value contains parentheses we need to balance those, to avoid iterating
                    // in the middle of parentheses pair, so use this regular expression (a bit hard to read)
                    // the regexp will split by comma, but honor parentheses pair that may include commas
                    // as well, eg if value = "bean=foo?method=killer(a,b),bean=bar?method=great(a,b)"
                    // then the regexp will split that into two:
                    // -> bean=foo?method=killer(a,b)
                    // -> bean=bar?method=great(a,b)
                    // http://stackoverflow.com/questions/1516090/splitting-a-title-into-separate-parts
                    delimiter = ",(?!(?:[^\\(,]|[^\\)],[^\\)])+\\))";
                }

                scanner.useDelimiter(delimiter);
                return cast(scanner);
            } else {
                // use a plain iterator that returns the value as is as there are only a single value
                return cast(new Iterator<String>() {
                    int idx = -1;

                    public boolean hasNext() {
                        return idx + 1 == 0 && (allowEmptyValues || isNotEmpty(s));
                    }

                    public String next() {
                        idx++;
                        return s;
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                });
            }
        } else {
            return Collections.singletonList(value).iterator();
        }
    }

    /**
     * Tests whether the value is <b>not</b> <tt>null</tt> or an empty string.
     *
     * @param value  the value, if its a String it will be tested for text length as well
     * @return true if <b>not</b> empty
     */
    public static boolean isNotEmpty(Object value) {
        if (value == null) {
            return false;
        } else if (value instanceof String) {
            String text = (String) value;
            return text.trim().length() > 0;
        } else {
            return true;
        }
    }

    public static <T> Iterator<T> cast(Iterator<?> p) {
        return (Iterator<T>) p;
    }

    /**
     * Strip any leading separators
     */
    public static String stripLeadingSeparator(String name) {
        if (name == null) {
            return null;
        }
        while (name.startsWith("/") || name.startsWith(File.separator)) {
            name = name.substring(1);
        }
        return name;
    }

    /**
     * Attempts to load the given resources from the given package name using the thread context
     * class loader or the class loader used to load this class
     *
     * @param packageName the name of the package to load its resources
     * @return the URLs for the resources or null if it could not be loaded
     */
    public static Enumeration<URL> loadResourcesAsURL(String packageName) {
        Enumeration<URL> url = null;

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            try {
                url = contextClassLoader.getResources(packageName);
            } catch (IOException e) {
                // ignore
            }
        }
        if (url == null) {
            try {
                url = Helper.class.getClassLoader().getResources(packageName);
            } catch (IOException e) {
                // ignore
            }
        }

        return url;
    }

    /**
     * Attempts to load the given resource as a stream using the thread context
     * class loader or the class loader used to load this class
     *
     * @param name the name of the resource to load
     * @return the stream or null if it could not be loaded
     */
    public static URL loadResourceAsURL(String name) {
        URL url = null;

        String resolvedName = resolveUriPath(name);
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            url = contextClassLoader.getResource(resolvedName);
        }
        if (url == null) {
            url = Helper.class.getClassLoader().getResource(resolvedName);
        }

        return url;
    }

    /**
     * Helper operation used to remove relative path notation from
     * resources.  Most critical for resources on the Classpath
     * as resource loaders will not resolve the relative paths correctly.
     *
     * @param name the name of the resource to load
     * @return the modified or unmodified string if there were no changes
     */
    private static String resolveUriPath(String name) {
        String answer = name;
        if (answer.indexOf("//") > -1) {
            answer = answer.replaceAll("//", "/");
        }
        if (answer.indexOf("../") > -1) {
            answer = answer.replaceAll("[A-Za-z0-9]*/\\.\\./", "");
        }
        if (answer.indexOf("./") > -1) {
            answer = answer.replaceAll("\\./", "");
        }
        return answer;
    }

    /**
     * Resolves the mandatory resource.
     *
     * @param uri uri of the resource
     * @return the resource as an {@link InputStream}.  Remember to close this stream after usage.
     * @throws java.io.FileNotFoundException is thrown if the resource file could not be found
     * @throws java.net.MalformedURLException if the URI is malformed
     */
    public static URL resolveMandatoryResourceAsUrl(String uri) throws FileNotFoundException, MalformedURLException {
        if (uri.startsWith("file:")) {
            // check if file exists first
            String name = after(uri, "file:");
            File file = new File(name);
            if (!file.exists()) {
                throw new FileNotFoundException("File " + file + " not found");
            }
            return new URL(uri);
        } else if (uri.startsWith("http:")) {
            return new URL(uri);
        } else if (uri.startsWith("classpath:")) {
            uri = after(uri, "classpath:");
        }

        // load from classpath by default
        URL url = loadResourceAsURL(uri);
        if (url == null) {
            throw new FileNotFoundException("Cannot find resource in classpath for URI: " + uri);
        } else {
            return url;
        }
    }

    public static String after(String text, String after) {
        if (!text.contains(after)) {
            return null;
        }
        return text.substring(text.indexOf(after) + after.length());
    }

    /**
     * Recursively delete a directory, useful to zapping test data
     *
     * @param file the directory to be deleted
     * @return <tt>false</tt> if error deleting directory
     */
    public static boolean deleteDirectory(String file) {
        return deleteDirectory(new File(file));
    }

    /**
     * Recursively delete a directory, useful to zapping test data
     *
     * @param file the directory to be deleted
     * @return <tt>false</tt> if error deleting directory
     */
    public static boolean deleteDirectory(File file) {
        int tries = 0;
        int maxTries = 5;
        boolean exists = true;
        while (exists && (tries < maxTries)) {
            recursivelyDeleteDirectory(file);
            tries++;
            exists = file.exists();
            if (exists) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
        return !exists;
    }

    private static void recursivelyDeleteDirectory(File file) {
        if (!file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File child : files) {
                recursivelyDeleteDirectory(child);
            }
        }
        boolean success = file.delete();
        if (!success) {
            LOG.warn("Deletion of file: " + file.getAbsolutePath() + " failed");
        }
    }

    /**
     * create the directory
     *
     * @param file the directory to be created
     */
    public static void createDirectory(String file) {
        File dir = new File(file);
        dir.mkdirs();
    }


}
