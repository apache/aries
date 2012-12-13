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
package org.apache.aries.blueprint.container;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExecutorServiceWrapper extends AbstractExecutorService implements Runnable {

    private final ExecutorService delegate;
    private final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<Runnable>();
    private final AtomicBoolean triggered = new AtomicBoolean();
    private final AtomicBoolean shutdown = new AtomicBoolean();
    private Thread runningThread;

    public ExecutorServiceWrapper(ExecutorService delegate) {
        this.delegate = delegate;
    }

    public void shutdown() {
        shutdown.set(true);
    }

    public List<Runnable> shutdownNow() {
        List<Runnable> pending = new ArrayList<Runnable>();
        if (shutdown.compareAndSet(false, true)) {
            Runnable runnable;
            while ((runnable = queue.poll()) != null) {
                pending.add(runnable);
            }
        }
        return pending;
    }

    public boolean isShutdown() {
        return shutdown.get();
    }

    public boolean isTerminated() {
        return delegate.isTerminated() || isShutdown() && queue.isEmpty() && !triggered.get();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long millis = unit.toMillis(timeout);
        if (millis > 0) {
            long max = System.currentTimeMillis() + millis;
            synchronized (triggered) {
                while (System.currentTimeMillis() < max) {
                    if (isTerminated()) {
                        return true;
                    } else {
                        triggered.wait(millis);
                    }
                }
            }
        }
        return isTerminated();
    }

    public void execute(Runnable command) {
        if (isShutdown()) {
            throw new RejectedExecutionException("Executor has been shut down");
        }
        queue.add(command);
        triggerExecution();
    }

    protected void triggerExecution() {
        if( triggered.compareAndSet(false, true) ) {
            delegate.execute(this);
        }
    }

    public void run() {
        try {
            Runnable runnable;
            synchronized (triggered) {
                runningThread = Thread.currentThread();
            }
            while (true) {
                runnable = queue.poll();
                if (runnable == null) {
                    return;
                }
                try {
                    runnable.run();
                } catch (Throwable e) {
                    Thread thread = Thread.currentThread();
                    thread.getUncaughtExceptionHandler().uncaughtException(thread, e);
                }
            }
        } finally {
            synchronized (triggered) {
                runningThread = null;
                triggered.set(false);
                triggered.notifyAll();
            }
            if (!isShutdown() && !queue.isEmpty()) {
                triggerExecution();
            }
        }
    }
}
