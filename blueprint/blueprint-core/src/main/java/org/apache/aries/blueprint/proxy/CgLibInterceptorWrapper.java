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
package org.apache.aries.blueprint.proxy;

import java.lang.reflect.Method;
import java.util.List;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.aries.blueprint.Interceptor;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple class that coordinates creation of object wrappers that invoke
 * interceptors.
 */
public class CgLibInterceptorWrapper {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(CgLibInterceptorWrapper.class);

    /**
     * Classloader for CGLib usage, that spans the delegate classloader (for the
     * bean being intercepted), the blueprint classloader (for the wrapped bean
     * interface), and the cglib classloader (for cglib packages).
     */
    private static class BridgingClassLoader extends ClassLoader {
        private ClassLoader cgLibClassLoader;
        private ClassLoader blueprintClassLoader;
        private ClassLoader delegateClassLoader;

        public BridgingClassLoader(ClassLoader delegate) {
            this.delegateClassLoader = delegate;
            this.cgLibClassLoader = Enhancer.class.getClassLoader();
            this.blueprintClassLoader = this.getClass().getClassLoader();
        }

        public Class<?> loadClass(String className)
                throws ClassNotFoundException {
            if (className.equals("org.apache.aries.blueprint.proxy.WrapperedObject")) {
                //CgLib will need to use 'WrapperedObject' which is from us.
                return blueprintClassLoader.loadClass(className);
            } else if (className.startsWith("net.sf.cglib")) {
                //CgLib will need to load classes from within its bundle, that we 
                //cannot load with the blueprintClassLoader.
                return cgLibClassLoader.loadClass(className);
            } else {
                //CgLib will need to load classes from the application bundle.
                return delegateClassLoader.loadClass(className);
            }
        }
    }

    /**
     * Create a proxy object, given a delegate instance, associated metadata,
     * and a list of interceptors.
     * <p>
     * 
     * @param cl
     *            Classloader to use to create proxy instance
     * @param cm
     *            ComponentMetadata for delegate instance
     * @param interceptors
     *            List of Interceptor for invocation pre/post.
     * @param delegate
     *            Instance to delegate method calls to.
     * @param classesToProxy
     *            List of interfaces/classes this proxy should present itself
     *            as.
     * @return Interceptor wrappered proxy. Can be used functionally the same as
     *         'delegate' but will invoke interceptors. Will implement WrapperedObject
     *         which can be used to unwrap the original object.
     * @throws ComponentDefinitionException
     *             if the delegate cannot be proxied.
     */
    public static Object createProxyObject(ClassLoader cl,
            ComponentMetadata cm, List<Interceptor> interceptors,
            Object delegate, Class<?>... classesToProxy) {

        LOGGER.debug("createProxyObject " + cm.getId() + " " + delegate);
        final Collaborator c = new Collaborator(cm, interceptors, delegate);
        Enhancer e = new Enhancer();

        e.setClassLoader(new BridgingClassLoader(cl));
        e.setInterceptDuringConstruction(false);

        // add the WrapperedObject interface to the list of interfaces for the
        // subclass.
        int origIfLen = 0;
        if (classesToProxy != null)
            origIfLen = classesToProxy.length;
        Class<?> ifs[] = new Class<?>[origIfLen + 1];
        ifs[0] = WrapperedObject.class;
        if (classesToProxy != null && classesToProxy.length > 0) {
            for (int i = 1; i < (classesToProxy.length + 1); i++) {
                ifs[i] = classesToProxy[i - 1];
            }
        }
        e.setInterfaces(ifs);
        
        e.setSuperclass(delegate.getClass());
        e.setCallback(new MethodInterceptor() {
            public Object intercept(Object arg0, Method arg1, Object[] arg2,
                    MethodProxy arg3) throws Throwable {
                return c.invoke(arg0, arg1, arg2);
            }
        });

        Object wrappered = null;
        try {
            wrappered = e.create();
        } catch (IllegalArgumentException iae) {
            // thrown if the bean has no zero-arg constructor,
            // or is final, or otherwise unable to be proxied.
            throw new ComponentDefinitionException(
                    "Unable to proxy bean for interceptors: " + iae);
        }
        return wrappered;
    }
}
