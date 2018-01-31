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
package org.apache.aries.blueprint.container;

import org.apache.aries.blueprint.container.AggregateConverter.Convertible;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.osgi.service.blueprint.container.ReifiedType;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NullProxy implements Convertible {

    private static final Map<Class<?>, Object> returns;

    static {
		Map<Class<?>, Object> tmp = new HashMap<Class<?>, Object>();

		tmp.put(boolean.class, false);
		tmp.put(byte.class, Byte.valueOf("0"));
		tmp.put(short.class, Short.valueOf("0"));
		tmp.put(char.class, Character.valueOf((char)0));
		tmp.put(int.class, Integer.valueOf("0"));
		tmp.put(float.class, Float.valueOf("0"));
		tmp.put(long.class, Long.valueOf("0"));
		tmp.put(Double.class, Double.valueOf("0"));

		returns = Collections.unmodifiableMap(tmp);
	}

    private final ExtendedBlueprintContainer container;

    private final InvocationHandler nullProxyHandler = new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("toString".equals(method.getName()) && (args == null || args.length == 0)) {
                return NullProxy.this.toString();
            } else {
                return returns.get(method.getReturnType());
            }
        }
    };

    public NullProxy(ExtendedBlueprintContainer container) {
        this.container = container;
    }

    @Override
    public Object convert(ReifiedType type) {
        ClassLoader cl = container.getClassLoader();
        return Proxy.newProxyInstance(cl, new Class<?>[] { type.getRawClass() }, nullProxyHandler);
    }

    @Override
    public String toString() {
        return "Aries Blueprint Null Proxy";
    }
}
