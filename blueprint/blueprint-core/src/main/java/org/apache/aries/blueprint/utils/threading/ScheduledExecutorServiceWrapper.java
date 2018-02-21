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
package org.apache.aries.blueprint.utils.threading;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.aries.blueprint.utils.threading.impl.Discardable;
import org.apache.aries.blueprint.utils.threading.impl.DiscardableCallable;
import org.apache.aries.blueprint.utils.threading.impl.DiscardableRunnable;
import org.apache.aries.blueprint.utils.threading.impl.WrappedFuture;
import org.apache.aries.blueprint.utils.threading.impl.WrappedScheduledFuture;
import org.apache.aries.util.tracker.SingleServiceTracker;
import org.apache.aries.util.tracker.SingleServiceTracker.SingleServiceListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

/**
 * This class looks like a ScheduledExecutorService to the outside world. Internally it uses either
 * a scheduled thread pool with a core size of 3, or it picks one up from the service registry. If
 * it picks one up from the service registry then it shuts the internal one down. This doesn't fully meet
 * the spec for a SchedueledExecutorService. It does not properly implement shutdownNow, but this isn't used
 * by blueprint so for now that should be fine.
 * <p>
 * <p>It also wraps the Runnables and Callables so when a task is canceled we quickly clean up memory rather
 * than waiting for the target to get to the task and purge it.
 * </p>
 */
public class ScheduledExecutorServiceWrapper implements ScheduledExecutorService, SingleServiceListener {

    public interface ScheduledExecutorServiceFactory {

        ScheduledExecutorService create(String name);

    }

    private final AtomicReference<ScheduledExecutorService> _current = new AtomicReference<ScheduledExecutorService>();
    private SingleServiceTracker<ScheduledExecutorService> _tracked;
    private final AtomicReference<ScheduledExecutorService> _default = new AtomicReference<ScheduledExecutorService>();
    private final AtomicBoolean _shutdown = new AtomicBoolean();
    private final Queue<Discardable<Runnable>> _unprocessedWork = new LinkedBlockingQueue<Discardable<Runnable>>();
    private final RWLock _lock = new RWLock();
    private final AtomicInteger _invokeEntryCount = new AtomicInteger();
    private final ScheduledExecutorServiceFactory _factory;
    private final String _name;

    public ScheduledExecutorServiceWrapper(BundleContext context, String name, ScheduledExecutorServiceFactory sesf) {
        _name = name;
        _factory = sesf;
        try {
            _tracked = new SingleServiceTracker<ScheduledExecutorService>(context, ScheduledExecutorService.class, "(aries.blueprint.poolName=" + _name + ")", this);
            _tracked.open();
        } catch (InvalidSyntaxException e) {
            // Just ignore and stick with the default one.
        }

        if (_current.get() == null) {
            _default.set(_factory.create(name));
            if (!!!_current.compareAndSet(null, _default.get())) {
                _default.getAndSet(null).shutdown();
            }
        }
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long timeLeftToWait = unit.toMillis(timeout);
        long pausePeriod = timeLeftToWait;
        if (pausePeriod > 1000) pausePeriod = 1000;
        while (!!!_unprocessedWork.isEmpty() && _invokeEntryCount.get() > 0 && timeLeftToWait > 0) {
            Thread.sleep(pausePeriod);
            timeLeftToWait -= pausePeriod;
            if (timeLeftToWait < pausePeriod) pausePeriod = timeLeftToWait;
        }
        return _unprocessedWork.isEmpty() && _invokeEntryCount.get() > 0;
    }

    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        try {
            return runUnlessShutdown(new Callable<List<Future<T>>>() {

                public List<Future<T>> call() throws Exception {
                    _invokeEntryCount.incrementAndGet();
                    try {
                        return _current.get().invokeAll(tasks);
                    } finally {
                        _invokeEntryCount.decrementAndGet();
                    }
                }

            });
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new RejectedExecutionException();
        }
    }

    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks,
                                         final long timeout,
                                         final TimeUnit unit) throws InterruptedException {
        try {
            return runUnlessShutdown(new Callable<List<Future<T>>>() {

                public List<Future<T>> call() throws Exception {
                    _invokeEntryCount.incrementAndGet();
                    try {
                        return _current.get().invokeAll(tasks, timeout, unit);
                    } finally {
                        _invokeEntryCount.decrementAndGet();
                    }
                }

            });
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new RejectedExecutionException();
        }
    }

    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException,
            ExecutionException {
        try {
            return runUnlessShutdown(new Callable<T>() {

                public T call() throws Exception {
                    _invokeEntryCount.incrementAndGet();
                    try {
                        return _current.get().invokeAny(tasks);
                    } finally {
                        _invokeEntryCount.decrementAndGet();
                    }
                }

            });
        } catch (InterruptedException e) {
            throw e;
        } catch (ExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new RejectedExecutionException();
        }
    }

    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return runUnlessShutdown(new Callable<T>() {

                public T call() throws Exception {
                    _invokeEntryCount.incrementAndGet();
                    try {
                        return _current.get().invokeAny(tasks, timeout, unit);
                    } finally {
                        _invokeEntryCount.decrementAndGet();
                    }
                }

            });
        } catch (InterruptedException e) {
            throw e;
        } catch (ExecutionException e) {
            throw e;
        } catch (TimeoutException e) {
            throw e;
        } catch (Exception e) {
            throw new RejectedExecutionException();
        }
    }

    public boolean isShutdown() {
        return _shutdown.get();
    }

    public boolean isTerminated() {
        if (isShutdown()) return _unprocessedWork.isEmpty();
        else return false;
    }

    public void shutdown() {
        _lock.runWriteOperation(new Runnable() {

            public void run() {
                _shutdown.set(true);
                ScheduledExecutorService s = _default.get();

                if (s != null) s.shutdown();
            }
        });
    }

    public List<Runnable> shutdownNow() {
        try {
            return _lock.runWriteOperation(new Callable<List<Runnable>>() {

                public List<Runnable> call() {
                    _shutdown.set(true);

                    ScheduledExecutorService s = _default.get();

                    if (s != null) s.shutdownNow();

                    List<Runnable> runnables = new ArrayList<Runnable>();

                    for (Discardable<Runnable> r : _unprocessedWork) {
                        Runnable newRunnable = r.discard();
                        if (newRunnable != null) {
                            runnables.add(newRunnable);
                        }
                    }

                    return runnables;
                }

            });
        } catch (Exception e) {
            // This wont happen since our callable doesn't throw any exceptions, so we just return an empty list
            return Collections.emptyList();
        }
    }

    public <T> Future<T> submit(final Callable<T> task) {
        try {
            return runUnlessShutdown(new Callable<Future<T>>() {

                public Future<T> call() throws Exception {
                    DiscardableCallable<T> t = new DiscardableCallable<T>(task, _unprocessedWork);
                    try {
                        return new WrappedFuture<T>(_current.get().submit((Callable<T>) t), t);
                    } catch (RuntimeException e) {
                        t.discard();
                        throw e;
                    }
                }

            });
        } catch (Exception e) {
            throw new RejectedExecutionException();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Future<?> submit(final Runnable task) {
        try {
            return runUnlessShutdown(new Callable<Future<?>>() {

                public Future<?> call() {
                    DiscardableRunnable t = new DiscardableRunnable(task, _unprocessedWork);
                    try {
                        return new WrappedFuture(_current.get().submit(t), t);
                    } catch (RuntimeException e) {
                        t.discard();
                        throw e;
                    }
                }
            });
        } catch (Exception e) {
            throw new RejectedExecutionException();
        }
    }

    public <T> Future<T> submit(final Runnable task, final T result) {
        try {
            return runUnlessShutdown(new Callable<Future<T>>() {

                public Future<T> call() {
                    DiscardableRunnable t = new DiscardableRunnable(task, _unprocessedWork);
                    try {
                        return new WrappedFuture<T>(_current.get().submit(t, result), t);
                    } catch (RuntimeException e) {
                        t.discard();
                        throw e;
                    }
                }
            });
        } catch (Exception e) {
            throw new RejectedExecutionException();
        }
    }

    public void execute(final Runnable command) {
        try {
            runUnlessShutdown(new Callable<Object>() {

                public Object call() {
                    DiscardableRunnable t = new DiscardableRunnable(command, _unprocessedWork);
                    try {
                        _current.get().execute(t);
                    } catch (RuntimeException e) {
                        t.discard();
                        throw e;
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            throw new RejectedExecutionException();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
        try {
            return runUnlessShutdown(new Callable<ScheduledFuture<?>>() {

                public ScheduledFuture<?> call() {
                    DiscardableRunnable t = new DiscardableRunnable(command, _unprocessedWork);
                    try {
                        return new WrappedScheduledFuture(_current.get().schedule(t, delay, unit), t);
                    } catch (RuntimeException e) {
                        t.discard();
                        throw e;
                    }
                }
            });
        } catch (Exception e) {
            throw new RejectedExecutionException();
        }
    }

    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
        try {
            return runUnlessShutdown(new Callable<ScheduledFuture<V>>() {

                public ScheduledFuture<V> call() {
                    DiscardableCallable<V> c = new DiscardableCallable<V>(callable, _unprocessedWork);
                    try {
                        return new WrappedScheduledFuture<V>(_current.get().schedule((Callable<V>) c, delay, unit), c);
                    } catch (RuntimeException e) {
                        c.discard();
                        throw e;
                    }
                }
            });
        } catch (Exception e) {
            throw new RejectedExecutionException();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period,
                                                  final TimeUnit unit) {
        try {
            return runUnlessShutdown(new Callable<ScheduledFuture<?>>() {

                public ScheduledFuture<?> call() {
                    DiscardableRunnable t = new DiscardableRunnable(command, _unprocessedWork);
                    try {
                        return new WrappedScheduledFuture(_current.get().scheduleAtFixedRate(t, initialDelay, period, unit), t);
                    } catch (RuntimeException e) {
                        t.discard();
                        throw e;
                    }
                }
            });
        } catch (Exception e) {
            throw new RejectedExecutionException();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay,
                                                     final TimeUnit unit) {
        try {
            return runUnlessShutdown(new Callable<ScheduledFuture<?>>() {

                public ScheduledFuture<?> call() {
                    DiscardableRunnable t = new DiscardableRunnable(command, _unprocessedWork);
                    try {
                        return new WrappedScheduledFuture(_current.get().scheduleWithFixedDelay(t, initialDelay, delay, unit), t);
                    } catch (RuntimeException e) {
                        t.discard();
                        throw e;
                    }
                }
            });
        } catch (Exception e) {
            throw new RejectedExecutionException();
        }
    }

    public void serviceFound() {
        ScheduledExecutorService s = _default.get();
        if (_current.compareAndSet(s, _tracked.getService())) {
            if (s != null) {
                if (_default.compareAndSet(s, null)) {
                    s.shutdown();
                }
            }
        }
    }

    // TODO when lost or replaced we need to move work to the "new" _current. This is a huge change because the futures are not currently stored.
    public void serviceLost() {
        ScheduledExecutorService s = _default.get();

        if (s == null) {
            s = _factory.create(_name);
            if (_default.compareAndSet(null, s)) {
                _current.set(s);
            }
        }
    }

    public void serviceReplaced() {
        _current.set(_tracked.getService());
    }

    private <T> T runUnlessShutdown(final Callable<T> call) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return _lock.runReadOperation(new Callable<T>() {
                public T call() throws Exception {
                    if (isShutdown()) throw new RejectedExecutionException();
                    return call.call();
                }
            });
        } catch (InterruptedException e) {
            throw e;
        } catch (ExecutionException e) {
            throw e;
        } catch (TimeoutException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RejectedExecutionException();
        }
    }
}