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
package org.apache.aries.blueprint.intercept;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.aries.blueprint.Interceptor;
import org.osgi.service.blueprint.reflect.ComponentMetadata;

public class TheInterceptor implements Interceptor {

    public static final AtomicInteger calls = new AtomicInteger();

    @Override
    public Object preCall(ComponentMetadata componentMetadata, Method method, Object... objects) throws Throwable {
        calls.incrementAndGet();
        return null;
    }

    @Override
    public void postCallWithReturn(ComponentMetadata componentMetadata, Method method, Object o, Object o1) throws Throwable {

    }

    @Override
    public void postCallWithException(ComponentMetadata componentMetadata, Method method, Throwable throwable, Object o) throws Throwable {

    }

    @Override
    public int getRank() {
        return 0;
    }

}
