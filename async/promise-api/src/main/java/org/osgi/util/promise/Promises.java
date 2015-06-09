package org.osgi.util.promise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Static helper methods for Promises.
 */
public class Promises {
    /**
     * Create a new Promise that has been resolved with the specified value.
     *
     * @param value The value of the resolved Promise.
     * @param <T>   The value type associated with the returned Promise.
     * @return A new Promise that has been resolved with the specified value.
     */
    public static <T> Promise<T> resolved(T value) {
        Deferred<T> def = new Deferred<T>();
        def.resolve(value);
        return def.getPromise();
    }

    /**
     * Create a new Promise that has been resolved with the specified failure.
     *
     * @param failure The failure of the resolved Promise. Must not be null.
     * @param <T>     The value type associated with the returned Promise.
     * @return A new Promise that has been resolved with the specified failure.
     */
    public static <T> Promise<T> failed(Throwable failure) {
        if (failure == null)
            throw new NullPointerException();
        Deferred<T> def = new Deferred<T>();
        def.fail(failure);
        return def.getPromise();
    }

    /**
     * Create a new Promise that is a latch on the resolution of the specified Promises.
     * <p/>
     * The new Promise acts as a gate and must be resolved after all of the specified Promises are resolved.
     *
     * @param promises The Promises which must be resolved before the returned Promise must be resolved. Must not be
     *                 null.
     * @param <T>      The value type of the List value associated with the returned Promise.
     * @param <S>      A subtype of the value type of the List value associated with the returned Promise.
     * @return A Promise that is resolved only when all the specified Promises are resolved. The returned Promise will
     * be successfully resolved, with a List of the values in the order of the specified Promises, if all the specified
     * Promises are successfully resolved. The List in the returned Promise is the property of the caller and is
     * modifiable. The returned Promise will be resolved with a failure of FailedPromisesException if any of the
     * specified Promises are resolved with a failure. The failure FailedPromisesException must contain all of the
     * specified Promises which resolved with a failure.
     */
    public static <T, S> Promise<List<T>> all(final Collection<Promise<S>> promises) {
        if (promises == null)
            throw new NullPointerException();
        final Deferred<List<T>> result = new Deferred<List<T>>();
        final Collection<Promise<?>> failedPromises = new ArrayList<Promise<?>>();
        final List<T> resolvedValues = new ArrayList<T>();

        if (promises.size() == 0) {
            result.resolve(resolvedValues);
        }
        for (final Promise<S> promise : promises) {
            promise.then(new Success<S, T>() {
                @Override
                public Promise<T> call(Promise<S> resolved) throws Exception {
                    // "S is subtype of the value type of the List"
                    @SuppressWarnings("unchecked")
                    T value = (T) resolved.getValue();
                    resolvedValues.add(value);

                    if (resolvedValues.size() == promises.size()) {
                        result.resolve(resolvedValues);
                    } else if (failedPromises.size() + resolvedValues.size() == promises.size()) {
                        result.fail(new FailedPromisesException(failedPromises));
                    }
                    return null;
                }
            }, new Failure() {
                @Override
                public void fail(Promise<?> resolved) throws Exception {
                    failedPromises.add(resolved);
                    if (failedPromises.size() + resolvedValues.size() == promises.size()) {
                        result.fail(new FailedPromisesException(failedPromises));
                    }
                }
            });

        }

        return result.getPromise();
    }

    /**
     * Create a new Promise that is a latch on the resolution of the specified Promises.
     * <p/>
     * The new Promise acts as a gate and must be resolved after all of the specified Promises are resolved.
     *
     * @param promises The Promises which must be resolved before the returned Promise must be resolved. Must not be
     *                 null.
     * @param <T>      The value type associated with the specified Promises.
     * @return A Promise that is resolved only when all the specified Promises are resolved. The returned Promise will
     * be successfully resolved, with a List of the values in the order of the specified Promises, if all the specified
     * Promises are successfully resolved. The List in the returned Promise is the property of the caller and is
     * modifiable. The returned Promise will be resolved with a failure of FailedPromisesException if any of the
     * specified Promises are resolved with a failure. The failure FailedPromisesException must contain all of the
     * specified Promises which resolved with a failure.
     */
    public static <T> Promise<List<T>> all(final Promise<? extends T>... promises) {
        if (promises == null)
            throw new NullPointerException();
        List<Promise<T>> list = new ArrayList<Promise<T>>();
        for (Promise<? extends T> promise : promises) {
            @SuppressWarnings("unchecked")
            Promise<T> pt = (Promise<T>) promise;
            list.add(pt);
        }
        return all(list);
    }
}
