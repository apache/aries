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
package org.apache.aries.jndi.services;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.aries.util.BundleToClassLoaderAdapter;
import org.osgi.framework.Bundle;

public class JdkProxyFactory implements ProxyFactory {

    public Object createProxy(final Bundle bundle,
                              final List<Class<?>> classes,
                              final Callable<Object> dispatcher) {
      
        List<Class<?>> classesToUse = new ArrayList<Class<?>>();
        
        for (Class<?> clazz : classes) {
          if (clazz.isInterface()) classesToUse.add(clazz);
        }
        
        if (classesToUse.isEmpty()) throw new IllegalArgumentException("Trying to proxy a service with the classes: " + classes + " but none of them are interfaces.");
        
        return Proxy.newProxyInstance(new BundleToClassLoaderAdapter(bundle), classesToUse.toArray(new Class[classesToUse.size()]),
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        try {
                            return method.invoke(dispatcher.call(), args);
                        } catch (InvocationTargetException ite) {
                            throw ite.getTargetException();
                        }
                    }
                });
    }

}
