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
package org.apache.aries.blueprint.pojos;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class CachePojos {

    public interface BasicCache<K, V> {

    }

    public interface Cache<K, V> extends BasicCache<K, V> {

    }

    public interface CacheContainer extends BasicCacheContainer {
        <K, V> Cache<K, V> getCache();

        <K, V> Cache<K, V> getCache(String var1);
    }

    public interface BasicCacheContainer {

        <K, V> BasicCache<K, V> getCache();

        <K, V> BasicCache<K, V> getCache(String var1);
    }

    public static class SimpleCacheContainerFactory {
        public static CacheContainer create() {
            return (CacheContainer) Proxy.newProxyInstance(
                    SimpleCacheContainerFactory.class.getClassLoader(),
                    new Class<?>[] { CacheContainer.class },
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            return new Cache() {};
                        }
                    });
        }
    }

}
