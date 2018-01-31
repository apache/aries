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
package org.apache.aries.blueprint.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ProxyUtils {
    public static final Callable<Object> passThrough(final Object target) {
        return new Callable<Object>() {
            public Object call() throws Exception {
                return target;
            }
        };
    }

    public static final List<Class<?>> asList(Class<?>... classesArray) {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        for (Class<?> clazz : classesArray) {
            classes.add(clazz);
        }
        return classes;
    }

    public static final List<Class<?>> asList(Class<?> clazz) {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(clazz);
        return classes;
    }
}
