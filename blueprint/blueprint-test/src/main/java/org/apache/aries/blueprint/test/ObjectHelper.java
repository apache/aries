/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.blueprint.test;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

public class ObjectHelper {

    private static final String DEFAULT_DELIMITER = ",";
    public static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
    private static final Logger LOG = LoggerFactory.getLogger(ObjectHelper.class);

	private ObjectHelper() { }
	
	   /**
     * Creates an Iterable to walk the exception from the bottom up
     * (the last caused by going upwards to the root exception).
     *
     * @see java.lang.Iterable
     * @param exception  the exception
     * @return the Iterable
     */
    public static Iterable<Throwable> createExceptionIterable(Throwable exception) {
        List<Throwable> throwables = new ArrayList<Throwable>();

        Throwable current = exception;
        // spool to the bottom of the caused by tree
        while (current != null) {
            throwables.add(current);
            current = current.getCause();
        }
        Collections.reverse(throwables);

        return throwables;
    }

    /**
     * Creates an Iterator to walk the exception from the bottom up
     * (the last caused by going upwards to the root exception).
     *
     * @see Iterator
     * @param exception  the exception
     * @return the Iterator
     */
    public static Iterator<Throwable> createExceptionIterator(Throwable exception) {
        return createExceptionIterable(exception).iterator();
    }
	
    /**
     * Retrieves the given exception type from the exception.
     * <p/>
     * Is used to get the caused exception that typically have been wrapped in some sort
     * of wrapper exception
     * <p/>
     * The strategy is to look in the exception hierarchy to find the first given cause that matches the type.
     * Will start from the bottom (the real cause) and walk upwards.
     *
     * @param type the exception type wanted to retrieve
     * @param exception the caused exception
     * @return the exception found (or <tt>null</tt> if not found in the exception hierarchy)
     */
    public static <T> T getException(Class<T> type, Throwable exception) {
        if (exception == null) {
            return null;
        }
        
        //check the suppressed exception first
        for (Throwable throwable : exception.getSuppressed()) {
            if (type.isInstance(throwable)) {
                return type.cast(throwable);
            }
        }

        // walk the hierarchy and look for it
        for (final Throwable throwable : createExceptionIterable(exception)) {
            if (type.isInstance(throwable)) {
                return type.cast(throwable);
            }
        }

        // not found
        return null;
    }
    
    /**
     * Returns the string after the given token
     *
     * @param text  the text
     * @param after the token
     * @return the text after the token, or <tt>null</tt> if text does not contain the token
     */
    public static String after(String text, String after) {
        if (!text.contains(after)) {
            return null;
        }
        return text.substring(text.indexOf(after) + after.length());
    }

    /**
     * Returns the string before the given token
     *
     * @param text  the text
     * @param before the token
     * @return the text before the token, or <tt>null</tt> if text does not contain the token
     */
    public static String before(String text, String before) {
        if (!text.contains(before)) {
            return null;
        }
        return text.substring(0, text.indexOf(before));
    }
    
    /**
     * Attempts to load the given resource as a stream using the thread context
     * class loader or the class loader used to load this class
     *
     * @param name the name of the resource to load
     * @return the stream or null if it could not be loaded
     */
    public static URL loadResourceAsURL(String name) {
        return loadResourceAsURL(name, null);
    }

    /**
     * Attempts to load the given resource as a stream using the thread context
     * class loader or the class loader used to load this class
     *
     * @param name the name of the resource to load
     * @param loader optional classloader to attempt first
     * @return the stream or null if it could not be loaded
     */
    public static URL loadResourceAsURL(String name, ClassLoader loader) {
        URL url = null;

        String resolvedName = resolveUriPath(name);
        if (loader != null) {
            url = loader.getResource(resolvedName);
        }
        if (url == null) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (contextClassLoader != null) {
                url = contextClassLoader.getResource(resolvedName);
            }
        }
        if (url == null) {
            url = ObjectHelper.class.getClassLoader().getResource(resolvedName);
        }
        if (url == null) {
            url = ObjectHelper.class.getResource(resolvedName);
        }

        return url;
    }

    /**
     * Attempts to load the given resources from the given package name using the thread context
     * class loader or the class loader used to load this class
     *
     * @param packageName the name of the package to load its resources
     * @return the URLs for the resources or null if it could not be loaded
     */
    public static Enumeration<URL> loadResourcesAsURL(String packageName) {
        return loadResourcesAsURL(packageName, null);
    }

    /**
     * Attempts to load the given resources from the given package name using the thread context
     * class loader or the class loader used to load this class
     *
     * @param packageName the name of the package to load its resources
     * @param loader optional classloader to attempt first
     * @return the URLs for the resources or null if it could not be loaded
     */
    public static Enumeration<URL> loadResourcesAsURL(String packageName, ClassLoader loader) {
        Enumeration<URL> url = null;

        if (loader != null) {
            try {
                url = loader.getResources(packageName);
            } catch (IOException e) {
                // ignore
            }
        }

        if (url == null) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (contextClassLoader != null) {
                try {
                    url = contextClassLoader.getResources(packageName);
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        if (url == null) {
            try {
                url = ObjectHelper.class.getClassLoader().getResources(packageName);
            } catch (IOException e) {
                // ignore
            }
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
        // compact the path and use / as separator as that's used for loading resources on the classpath
        return compactPath(name, '/');
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
     * </p> In case of primitive type arrays the returned {@code Iterator} iterates
     * over the corresponding Java primitive wrapper objects of the given elements
     * inside the {@code value} array. That's we get an autoboxing of the primitive
     * types here for free as it's also the case in Java language itself.
     *
     * @param value             the value
     * @param delimiter         delimiter for separating String values
     * @param allowEmptyValues  whether to allow empty values
     * @return the iterator
     */
    public static Iterator<Object> createIterator(Object value, String delimiter, boolean allowEmptyValues) {
        return createIterable(value, delimiter, allowEmptyValues, false).iterator();
    }

    /**
     * Creates an iterator over the value if the value is a collection, an
     * Object[], a String with values separated by the given delimiter,
     * or a primitive type array; otherwise to simplify the caller's
     * code, we just create a singleton collection iterator over a single value
     *
     * </p> In case of primitive type arrays the returned {@code Iterator} iterates
     * over the corresponding Java primitive wrapper objects of the given elements
     * inside the {@code value} array. That's we get an autoboxing of the primitive
     * types here for free as it's also the case in Java language itself.
     *
     * @param value             the value
     * @param delimiter         delimiter for separating String values
     * @param allowEmptyValues  whether to allow empty values
     * @param pattern           whether the delimiter is a pattern
     * @return the iterator
     */
    public static Iterator<Object> createIterator(Object value, String delimiter,
                                                  boolean allowEmptyValues, boolean pattern) {
        return createIterable(value, delimiter, allowEmptyValues, pattern).iterator();
    }
    
    /**
     * Creates an iterable over the value if the value is a collection, an
     * Object[], a String with values separated by the given delimiter,
     * or a primitive type array; otherwise to simplify the caller's
     * code, we just create a singleton collection iterator over a single value
     * 
     * </p> In case of primitive type arrays the returned {@code Iterable} iterates
     * over the corresponding Java primitive wrapper objects of the given elements
     * inside the {@code value} array. That's we get an autoboxing of the primitive
     * types here for free as it's also the case in Java language itself.
     * 
     * @param value             the value
     * @param delimiter         delimiter for separating String values
     * @param allowEmptyValues  whether to allow empty values
     * @return the iterable
     * @see java.lang.Iterable
     */
    public static Iterable<Object> createIterable(Object value, String delimiter,
                                                  final boolean allowEmptyValues) {
        return createIterable(value, delimiter, allowEmptyValues, false);
    }

    /**
     * Creates an iterable over the value if the value is a collection, an
     * Object[], a String with values separated by the given delimiter,
     * or a primitive type array; otherwise to simplify the caller's
     * code, we just create a singleton collection iterator over a single value
     *
     * </p> In case of primitive type arrays the returned {@code Iterable} iterates
     * over the corresponding Java primitive wrapper objects of the given elements
     * inside the {@code value} array. That's we get an autoboxing of the primitive
     * types here for free as it's also the case in Java language itself.
     *
     * @param value             the value
     * @param delimiter         delimiter for separating String values
     * @param allowEmptyValues  whether to allow empty values
     * @param pattern           whether the delimiter is a pattern
     * @return the iterable
     * @see java.lang.Iterable
     */
    @SuppressWarnings("unchecked")
    public static Iterable<Object> createIterable(Object value, String delimiter,
                                                  final boolean allowEmptyValues, final boolean pattern) {

        if (value == null) {
            return Collections.emptyList();
        } else if (value instanceof Iterator) {
            final Iterator<Object> iterator = (Iterator<Object>)value;
            return new Iterable<Object>() {
                @Override
                public Iterator<Object> iterator() {
                    return iterator;
                }
            };
        } else if (value instanceof Iterable) {
            return (Iterable<Object>)value;
        } else if (value.getClass().isArray()) {
            if (isPrimitiveArrayType(value.getClass())) {
                final Object array = value;
                return new Iterable<Object>() {
                    @Override
                    public Iterator<Object> iterator() {
                        return new Iterator<Object>() {
                            private int idx;

                            public boolean hasNext() {
                                return idx < Array.getLength(array);
                            }

                            public Object next() {
                                if (!hasNext()) {
                                    throw new NoSuchElementException("no more element available for '" + array + "' at the index " + idx);
                                }

                                return Array.get(array, idx++);
                            }

                            public void remove() {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                };
            } else {
                return Arrays.asList((Object[]) value);
            }
        } else if (value instanceof NodeList) {
            // lets iterate through DOM results after performing XPaths
            final NodeList nodeList = (NodeList) value;
            return new Iterable<Object>() {
                @Override
                public Iterator<Object> iterator() {
                    return new Iterator<Object>() {
                        private int idx;

                        public boolean hasNext() {
                            return idx < nodeList.getLength();
                        }

                        public Object next() {
                            if (!hasNext()) {
                                throw new NoSuchElementException("no more element available for '" + nodeList + "' at the index " + idx);
                            }

                            return nodeList.item(idx++);
                        }

                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
            };
        } else if (value instanceof String) {
            final String s = (String) value;

            // this code is optimized to only use a Scanner if needed, eg there is a delimiter

            if (delimiter != null && (pattern || s.contains(delimiter))) {
                // use a scanner if it contains the delimiter or is a pattern
                final Scanner scanner = new Scanner((String)value);

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

                return new Iterable<Object>() {
                    @Override
                    public Iterator<Object> iterator() {
                        return cast(scanner);
                    }
                };
            } else {
                return new Iterable<Object>() {
                    @Override
                    public Iterator<Object> iterator() {
                        // use a plain iterator that returns the value as is as there are only a single value
                        return new Iterator<Object>() {
                            private int idx;

                            public boolean hasNext() {
                                return idx == 0 && (allowEmptyValues || ObjectHelper.isNotEmpty(s));
                            }

                            public Object next() {
                                if (!hasNext()) {
                                    throw new NoSuchElementException("no more element available for '" + s + "' at the index " + idx);
                                }

                                idx++;
                                return s;
                            }

                            public void remove() {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                };
            }
        } else {
            return Collections.singletonList(value);
        }
    }
    
    /**
     * Returns if the given {@code clazz} type is a Java primitive array type.
     * 
     * @param clazz the Java type to be checked
     * @return {@code true} if the given type is a Java primitive array type
     */
    public static boolean isPrimitiveArrayType(Class<?> clazz) {
        if (clazz != null && clazz.isArray()) {
            return clazz.getComponentType().isPrimitive();
        }
        return false;
    }
    
    public static <T> Iterator<T> cast(Iterator<?> p) {
        return (Iterator<T>) p;
    }
    
    /**
     * Tests whether the value is <tt>null</tt> or an empty string.
     *
     * @param value  the value, if its a String it will be tested for text length as well
     * @return true if empty
     */
    public static boolean isEmpty(Object value) {
        return !isNotEmpty(value);
    }

    /**
     * Asserts whether the string is <b>not</b> empty.
     *
     * @param value  the string to test
     * @param name   the key that resolved the value
     * @return the passed {@code value} as is
     * @throws IllegalArgumentException is thrown if assertion fails
     */
    public static String notEmpty(String value, String name) {
        if (isEmpty(value)) {
            throw new IllegalArgumentException(name + " must be specified and not empty");
        }

        return value;
    }

    /**
     * Asserts whether the string is <b>not</b> empty.
     *
     * @param value  the string to test
     * @param on     additional description to indicate where this problem occurred (appended as toString())
     * @param name   the key that resolved the value
     * @return the passed {@code value} as is
     * @throws IllegalArgumentException is thrown if assertion fails
     */
    public static String notEmpty(String value, String name, Object on) {
        if (on == null) {
            notNull(value, name);
        } else if (isEmpty(value)) {
            throw new IllegalArgumentException(name + " must be specified and not empty on: " + on);
        }

        return value;
    }
    
    /**
     * Asserts whether the value is <b>not</b> <tt>null</tt>
     *
     * @param value  the value to test
     * @param name   the key that resolved the value
     * @return the passed {@code value} as is
     * @throws IllegalArgumentException is thrown if assertion fails
     */
    public static <T> T notNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must be specified");
        }

        return value;
    }

    /**
     * Asserts whether the value is <b>not</b> <tt>null</tt>
     *
     * @param value  the value to test
     * @param on     additional description to indicate where this problem occurred (appended as toString())
     * @param name   the key that resolved the value
     * @return the passed {@code value} as is
     * @throws IllegalArgumentException is thrown if assertion fails
     */
    public static <T> T notNull(T value, String name, Object on) {
        if (on == null) {
            notNull(value, name);
        } else if (value == null) {
            throw new IllegalArgumentException(name + " must be specified on: " + on);
        }

        return value;
    }

    /**
     * Attempts to load the given resource as a stream using the thread context
     * class loader or the class loader used to load this class
     *
     * @param name the name of the resource to load
     * @param loader optional classloader to attempt first
     * @return the stream or null if it could not be loaded
     */
    public static InputStream loadResourceAsStream(String name, ClassLoader loader) {
        InputStream in = null;

        String resolvedName = resolveUriPath(name);
        if (loader != null) {
            in = loader.getResourceAsStream(resolvedName);
        }
        if (in == null) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (contextClassLoader != null) {
                in = contextClassLoader.getResourceAsStream(resolvedName);
            }
        }
        if (in == null) {
            in = ObjectHelper.class.getClassLoader().getResourceAsStream(resolvedName);
        }
        if (in == null) {
            in = ObjectHelper.class.getResourceAsStream(resolvedName);
        }

        return in;
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
    
    /// FROM o.a.camel.FileUtil
    
    /**
     * Compacts a path by stacking it and reducing <tt>..</tt>,
     * and uses the given separator.
     */
    public static String compactPath(String path, char separator) {
        if (path == null) {
            return null;
        }
        
        // only normalize if contains a path separator
        if (path.indexOf('/') == -1 && path.indexOf('\\') == -1)  {
            return path;
        }

        // need to normalize path before compacting
        path = normalizePath(path);

        // preserve ending slash if given in input path
        boolean endsWithSlash = path.endsWith("/") || path.endsWith("\\");

        // preserve starting slash if given in input path
        boolean startsWithSlash = path.startsWith("/") || path.startsWith("\\");
        
        Stack<String> stack = new Stack<String>();

        // separator can either be windows or unix style
        String separatorRegex = "\\\\|/";
        String[] parts = path.split(separatorRegex);
        for (String part : parts) {
            if (part.equals("..") && !stack.isEmpty() && !"..".equals(stack.peek())) {
                // only pop if there is a previous path, which is not a ".." path either
                stack.pop();
            } else if (part.equals(".") || part.isEmpty()) {
                // do nothing because we don't want a path like foo/./bar or foo//bar
            } else {
                stack.push(part);
            }
        }

        // build path based on stack
        StringBuilder sb = new StringBuilder();
        
        if (startsWithSlash) {
            sb.append(separator);
        }
        
        for (Iterator<String> it = stack.iterator(); it.hasNext();) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(separator);
            }
        }

        if (endsWithSlash && stack.size() > 0) {
            sb.append(separator);
        }

        return sb.toString();
    }
    
    /**
     * Normalizes the path to cater for Windows and other platforms
     */
    public static String normalizePath(String path) {
        if (path == null) {
            return null;
        }

        if (isWindows()) {
            // special handling for Windows where we need to convert / to \\
            return path.replace('/', '\\');
        } else {
            // for other systems make sure we use / as separators
            return path.replace('\\', '/');
        }
    }
    
    private static boolean isWindows() {
        // initialize once as System.getProperty is not fast
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        return osName.contains("windows");
    }
    
    // IOHelper
    
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
    
    public static void copyAndCloseInput(InputStream input, OutputStream output) throws IOException {
        copyAndCloseInput(input, output, DEFAULT_BUFFER_SIZE);
    }
    
    public static void copyAndCloseInput(InputStream input, OutputStream output, int bufferSize) throws IOException {
        copy(input, output, bufferSize);
        close(input, null, LOG);
    }

    public static int copy(InputStream input, OutputStream output) throws IOException {
        return copy(input, output, DEFAULT_BUFFER_SIZE);
    }

    public static int copy(final InputStream input, final OutputStream output, int bufferSize) throws IOException {
        return copy(input, output, bufferSize, false);
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

    public static int copy(final InputStream input, final OutputStream output, int bufferSize, boolean flushOnEachWrite) throws IOException {
        if (input instanceof ByteArrayInputStream) {
            // optimized for byte array as we only need the max size it can be
            input.mark(0);
            input.reset();
            bufferSize = input.available();
        } else {
            int avail = input.available();
            if (avail > bufferSize) {
                bufferSize = avail;
            }
        }

        if (bufferSize > 262144) {
            // upper cap to avoid buffers too big
            bufferSize = 262144;
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("Copying InputStream: {} -> OutputStream: {} with buffer: {} and flush on each write {}",
                    new Object[]{input, output, bufferSize, flushOnEachWrite});
        }

        final byte[] buffer = new byte[bufferSize];
        int n = input.read(buffer);
        int total = 0;
        while (-1 != n) {
            output.write(buffer, 0, n);
            if (flushOnEachWrite) {
                output.flush();
            }
            total += n;
            n = input.read(buffer);
        }
        if (!flushOnEachWrite) {
            // flush at end, if we didn't do it during the writing
            output.flush();
        }
        return total;
    }

}
