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
package org.apache.aries.proxy.impl.weaving;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProxyWeavingHookTest {

    @Test
    public void tesDefault() {
        BundleContext ctx = (BundleContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { BundleContext.class },
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return null;
                    }
                });
        ProxyWeavingHook hook = new ProxyWeavingHook(ctx);
        assertTrue(hook.isEnabled("org.apache.foo.Bar"));
        assertTrue(hook.isDisabled("javax.foo.Bar"));
    }

    @Test
    public void testFilters() {
        BundleContext ctx = (BundleContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { BundleContext.class },
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("getProperty")) {
                            if (ProxyWeavingHook.WEAVING_ENABLED_CLASSES.equals(args[0])) {
                                return "";
                            }
                            if (ProxyWeavingHook.WEAVING_DISABLED_CLASSES.equals(args[0])) {
                                return "org.apache.foo.*";
                            }
                        }
                        return null;
                    }
                });
        ProxyWeavingHook hook = new ProxyWeavingHook(ctx);
        assertFalse(hook.isEnabled("org.apache.foo.Bar"));
        assertTrue(hook.isDisabled("org.apache.foo.Bar"));
        assertTrue(hook.isDisabled("org.apache.foo.bar.Bar"));
        assertFalse(hook.isDisabled("org.apache.fooBar"));
        assertFalse(hook.isDisabled("orgXapache.foo.Bar"));
    }
}
