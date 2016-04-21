/*
// * Licensed to the Apache Software Foundation (ASF) under one
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
package org.apache.aries.jpa.eclipselink.adapter;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UnionClassLoader extends ClassLoader implements BundleReference {
    private static final String ORG_OSGI_FRAMEWORK = "org.osgi.framework.";
    private static final int DOT_INDEX = ORG_OSGI_FRAMEWORK.lastIndexOf('.');
    private static final Logger LOG = LoggerFactory.getLogger(UnionClassLoader.class);
    private final Bundle eclipseLinkBundle;
    private final Bundle adaptorBundle;

    public UnionClassLoader(ClassLoader parentLoader, Bundle b, Bundle adaptor) {
        super(parentLoader);
        this.eclipseLinkBundle = b;
        this.adaptorBundle = adaptor;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if ("org.apache.aries.jpa.eclipselink.adapter.platform.OSGiTSServer".equals(name) 
            || "org.apache.aries.jpa.eclipselink.adapter.platform.OSGiTSWrapper".equals(name)) {
            return loadTempClass(name);
        } else if (name.startsWith(ORG_OSGI_FRAMEWORK) && name.lastIndexOf('.') == DOT_INDEX) {
            return adaptorBundle.loadClass(name);
        }
        return eclipseLinkBundle.loadClass(name);
    }

    private Class<?> loadTempClass(String name) throws ClassNotFoundException, ClassFormatError {
        InputStream is = getClass().getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class");
        if (is == null) {
            throw new ClassNotFoundException(name);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            copy(is, baos);
        } catch (IOException ioe) {
            throw new ClassNotFoundException(name, ioe);
        }
        return defineClass(name, baos.toByteArray(), 0, baos.size());
    }

    @Override
    public Bundle getBundle() {
        return adaptorBundle;
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        try {
            int len;
            byte[] b = new byte[1024];
            while ((len = in.read(b)) != -1) {
                out.write(b, 0, len);
            }
        } finally {
            close(in);
        }
    }

    private static void close(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (IOException e) {
            LOG.debug("Exception closing", e);
        }
    }
}
