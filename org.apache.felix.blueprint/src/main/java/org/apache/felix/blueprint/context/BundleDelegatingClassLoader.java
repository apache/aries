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
package org.apache.felix.blueprint.context;

import java.net.URL;
import java.util.Enumeration;
import java.io.IOException;

import org.osgi.framework.Bundle;

/**
 * A ClassLoader delegating to a given OSGi bundle.
 */
public class BundleDelegatingClassLoader extends ClassLoader {

    private final Bundle bundle;

    public BundleDelegatingClassLoader(Bundle bundle) {
        this.bundle = bundle;
    }

    protected Class findClass(String name) throws ClassNotFoundException {
        return bundle.loadClass(name);
    }

    protected URL findResource(String name) {
        return bundle.getResource(name);
    }

    protected Enumeration findResources(String name) throws IOException {
        return bundle.getResources(name);
    }

    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class clazz = findClass(name);
        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }

    public Bundle getBundle() {
        return bundle;
    }
}
