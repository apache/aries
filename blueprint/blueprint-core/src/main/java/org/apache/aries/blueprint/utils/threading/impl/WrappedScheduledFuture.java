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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.utils.threading.impl;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WrappedScheduledFuture<T> implements ScheduledFuture<T> {
    private Discardable<?> _discardable;
    private ScheduledFuture<T> _future;

    public WrappedScheduledFuture(ScheduledFuture<T> f, Discardable<?> d) {
        _future = f;
        _discardable = d;
    }

    public long getDelay(TimeUnit timeunit) {
        return _future.getDelay(timeunit);
    }

    public int compareTo(Delayed other) {
        return _future.compareTo(other);
    }

    public boolean cancel(boolean arg0) {
        boolean result = _future.cancel(arg0);

        if (result) _discardable.discard();

        return result;
    }

    public T get() throws InterruptedException, ExecutionException {
        return _future.get();
    }

    public T get(long timeout, TimeUnit timeunit) throws InterruptedException, ExecutionException,
            TimeoutException {
        return _future.get(timeout, timeunit);
    }

    public boolean isCancelled() {
        return _future.isCancelled();
    }

    public boolean isDone() {
        return _future.isDone();
    }
}