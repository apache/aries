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
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/** A classloader that delegates to a number of other classloaders.
 * This classloader can be used if a single classloader is needed that has
 * vibisility of a number of other classloaders. For example if a Thread Context
 * Classloader is needed that has visibility of a number of bundles so that 
 * ServiceLoader.load() can find all the services provided by these bundles.
 */
public class MultiDelegationClassloader extends ClassLoader {
    private final ClassLoader[] delegates;
    
    public MultiDelegationClassloader(ClassLoader ... classLoaders) {
        if (classLoaders == null) 
            throw new NullPointerException();
        
        delegates = classLoaders.clone();
    }
    
    @Override
    public URL getResource(String name) {
        for (ClassLoader cl : delegates) {
            URL res = cl.getResource(name);
            if (res != null)
                return res;
        }                
        return null;
    }
    
    @Override
    public InputStream getResourceAsStream(String name) {
        for (ClassLoader cl : delegates) {
            InputStream is = cl.getResourceAsStream(name);
            if (is != null)
                return is;
        }
        return null;
    }
    
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> urls = new ArrayList<URL>();
        
        for (ClassLoader cl : delegates) {
            urls.addAll(Collections.list(cl.getResources(name)));
        }
        return Collections.enumeration(urls);
    }
    
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        ClassNotFoundException lastEx = null;
        for (ClassLoader cl : delegates) {
            try {
                return cl.loadClass(name);
            } catch (ClassNotFoundException e) {
                lastEx = e;
            }
        }
        throw lastEx;
    }
}
