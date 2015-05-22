package org.apache.aries.async.promise;

import org.osgi.util.function.Function;
import org.osgi.util.function.Predicate;
import org.osgi.util.promise.Failure;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PromiseImpl<T> implements Promise<T> {

    private final ExecutorService exec;
    private final List<Runnable> tasks = new ArrayList<Runnable>();
    private final CountDownLatch resolved = new CountDownLatch(1);

    private List<PromiseImpl> chain;
    private Success onSuccess;
    private Failure onFailure;
    private Throwable failure;
    private T value;

    public PromiseImpl() {
        // Executor for onResolve() callbacks
        // We could use an Executor that runs tasks in current thread
        exec = Executors.newSingleThreadExecutor();
    }

    public void fail(Throwable failure) {
        if (failure == null)
            throw new NullPointerException();
        complete(null, failure);
    }

    public void resolve(T value) {
        complete(value, null);
    }

    public Promise<Void> resolveWith(final Promise<? extends T> with) {
        if (with == null)
            throw new NullPointerException();
        final PromiseImpl<Void> result = new PromiseImpl<Void>();

        with.then(new Success<T, T>() {
            @Override
            public Promise<T> call(Promise<T> resolved) throws Exception {
                if (isDone()) {
                    result.fail(new IllegalStateException("associated Promise already resolved"));
                }
                PromiseImpl.this.resolve(resolved.getValue());
                result.resolve(null);
                return null;
            }
        }, new Failure() {
            @Override
            public void fail(Promise<?> resolved) throws Exception {
                if (isDone()) {
                    result.fail(new IllegalStateException("associated Promise already resolved"));
                }
                PromiseImpl.this.fail(resolved.getFailure());
                result.resolve(null);
            }
        });

        return result;
    }

    private synchronized void complete(T value, Throwable failure) {
        if (isDone()) {
            throw new IllegalStateException("Promise is already resolved");
        }

        // mark this Promise as complete before invoking callbacks
        if (failure != null) {
            this.failure = failure;
        } else {
            this.value = value;
        }
        resolved.countDown();

        if (chain != null) {
            runChain();
        }

        // run onResolve() callbacks
        for (Runnable task : tasks) {
            exec.submit(task);
        }
    }

    // run chained success/failure callbacks
    @SuppressWarnings("unchecked")
    private void runChain() {
        while (!chain.isEmpty()) {
            PromiseImpl next = chain.remove(0);
            if (failure != null) {
                try {
                    if (next.onFailure != null) {
                        // "This method is called if the Promise with which it is registered resolves with a failure."
                        next.onFailure.fail(this);
                    }
                    // "If this method completes normally, the chained Promise will be failed
                    // with the same exception which failed the resolved Promise."
                    next.fail(failure);
                } catch (Exception e) {
                    // "If this method throws an exception, the chained Promise will be failed with the thrown exception."
                    next.fail(e);
                }
            } else {
                try {
                    // "This method is called if the Promise with which it is registered resolves successfully."
                    Promise<T> p = null;
                    if (next.onSuccess != null) {
                        p = next.onSuccess.call(this);
                    }
                    if (p == null) {
                        // "If the returned Promise is null then the chained Promise will resolve immediately with a successful value of null."
                        next.resolve(null);
                    } else {
                        // "If the returned Promise is not null then the chained Promise will be resolved when the returned Promise is resolved"
                        next.resolveWith(p);
                    }
                } catch (InvocationTargetException e) {
                    next.fail(e.getCause());
                } catch (Exception e) {
                    next.fail(e);
                }
            }
        }
    }

    // Promise API methods

    @Override
    public boolean isDone() {
        return resolved.getCount() == 0;
    }

    @Override
    public T getValue() throws InvocationTargetException, InterruptedException {
        resolved.await();
        if (failure != null) {
            throw new InvocationTargetException(failure);
        }
        return value;
    }

    @Override
    public Throwable getFailure() throws InterruptedException {
        resolved.await();
        return failure;
    }

    @Override
    public synchronized Promise<T> onResolve(Runnable callback) {
        if (callback == null)
            throw new NullPointerException();

        if (isDone()) {
            exec.submit(callback);
        } else {
            tasks.add(callback);
        }
        return this;
    }

    @Override
    public <R> Promise<R> then(Success<? super T, ? extends R> success, Failure failure) {
        PromiseImpl<R> result = new PromiseImpl<R>();
        result.onSuccess = success;
        result.onFailure = failure;
        synchronized (this) {
            if (chain == null) {
                chain = new ArrayList<PromiseImpl>();
            }
            chain.add(result);
            if (isDone()) {
                runChain();
            }
        }
        return result;
    }

    @Override
    public <R> Promise<R> then(Success<? super T, ? extends R> success) {
        return then(success, null);
    }

    @Override
    public Promise<T> filter(final Predicate<? super T> predicate) {
        if (predicate == null)
            throw new NullPointerException();
        final PromiseImpl<T> result = new PromiseImpl<T>();

        then(new Success<T, T>() {
            @Override
            public Promise<T> call(Promise<T> resolved) throws Exception {
                try {
                    if (predicate.test(resolved.getValue())) {
                        result.resolve(resolved.getValue());
                    } else {
                        result.fail(new NoSuchElementException("predicate does not accept value"));
                    }
                } catch (Throwable t) {
                    result.fail(t);
                }
                return null;
            }
        }, new Failure() {
            @Override
            public void fail(Promise<?> resolved) throws Exception {
                result.fail(resolved.getFailure());
            }
        });

        return result;
    }

    @Override
    public <R> Promise<R> map(final Function<? super T, ? extends R> mapper) {
        if (mapper == null)
            throw new NullPointerException();
        final PromiseImpl<R> result = new PromiseImpl<R>();

        then(new Success<T, T>() {
            @Override
            public Promise<T> call(Promise<T> resolved) throws Exception {
                try {
                    R val = mapper.apply(resolved.getValue());
                    result.resolve(val);
                } catch (Throwable t) {
                    result.fail(t);
                }
                return null;
            }
        }, new Failure() {
            @Override
            public void fail(Promise<?> resolved) throws Exception {
                result.fail(resolved.getFailure());
            }
        });

        return result;
    }

    @Override
    public <R> Promise<R> flatMap(final Function<? super T, Promise<? extends R>> mapper) {
        if (mapper == null)
            throw new NullPointerException();
        final PromiseImpl<R> result = new PromiseImpl<R>();

        then(new Success<T, T>() {
            @Override
            public Promise<T> call(Promise<T> resolved) throws Exception {
                try {
                    Promise<? extends R> p = mapper.apply(resolved.getValue());
                    result.resolveWith(p);
                } catch (Throwable t) {
                    result.fail(t);
                }
                return null;
            }
        }, new Failure() {
            @Override
            public void fail(Promise<?> resolved) throws Exception {
                result.fail(resolved.getFailure());
            }
        });

        return result;
    }

    @Override
    public Promise<T> recover(final Function<Promise<?>, ? extends T> recovery) {
        if (recovery == null)
            throw new NullPointerException();

        final PromiseImpl<T> result = new PromiseImpl<T>();

        then(new Success<T, T>() {
            @Override
            public Promise<T> call(Promise<T> resolved) throws Exception {
                result.resolve(resolved.getValue());
                return null;
            }
        }, new Failure() {
            @Override
            public void fail(Promise<?> resolved) throws Exception {
                try {
                    T recover = recovery.apply(resolved);
                    if (recover != null) {
                        result.resolve(recover);
                    } else {
                        result.fail(resolved.getFailure());
                    }
                } catch (Throwable t) {
                    result.fail(t);
                }
            }
        });

        return result;
    }

    @Override
    public Promise<T> recoverWith(final Function<Promise<?>, Promise<? extends T>> recovery) {
        if (recovery == null)
            throw new NullPointerException();

        final PromiseImpl<T> result = new PromiseImpl<T>();

        then(new Success<T, T>() {
            @Override
            public Promise<T> call(Promise<T> resolved) throws Exception {
                result.resolve(resolved.getValue());
                return null;
            }
        }, new Failure() {
            @Override
            public void fail(Promise<?> resolved) throws Exception {
                try {
                    Promise<? extends T> recover = recovery.apply(resolved);
                    if (recover != null) {
                        result.resolveWith(recover);
                    } else {
                        result.fail(resolved.getFailure());
                    }
                } catch (Throwable t) {
                    result.fail(t);
                }
            }
        });

        return result;
    }

    @Override
    public Promise<T> fallbackTo(final Promise<? extends T> fallback) {
        if (fallback == null)
            throw new NullPointerException();

        final PromiseImpl<T> result = new PromiseImpl<T>();

        then(new Success<T, T>() {
            @Override
            public Promise<T> call(Promise<T> resolved) throws Exception {
                result.resolve(resolved.getValue());
                return null;
            }
        }, new Failure() {
            @Override
            public void fail(Promise<?> resolved) throws Exception {
                @SuppressWarnings({"not thrown", "all"})
                Throwable fail = fallback.getFailure();
                if (fail != null) {
                    result.fail(resolved.getFailure());
                } else {
                    result.resolve(fallback.getValue());
                }
            }
        });

        return result;
    }
}
