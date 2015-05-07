package org.osgi.util.promise;

import org.apache.aries.async.promise.PromiseImpl;

/**
 * A Deferred Promise resolution.
 * <p/>
 * Instances of this class can be used to create a Promise that can be resolved in the future. The associated Promise
 * can be successfully resolved with resolve(Object) or resolved with a failure with fail(Throwable).
 * <p/>
 * It can also be resolved with the resolution of another promise using resolveWith(Promise).
 * <p/>
 * The associated Promise can be provided to anyone, but the Deferred object should be made available only to the party
 * that will responsible for resolving the Promise.
 *
 * @param <T> The value type associated with the created Promise.
 */
public class Deferred<T> {
    private final PromiseImpl<T> promise;

    /**
     * Create a new Deferred with an associated Promise.
     */
    public Deferred() {
        promise = new PromiseImpl<T>();
    }

    /**
     * Returns the Promise associated with this Deferred.
     *
     * @return The Promise associated with this Deferred.
     */
    public Promise<T> getPromise() {
        return promise;
    }

    /**
     * Successfully resolve the Promise associated with this Deferred.
     * <p/>
     * After the associated Promise is resolved with the specified value, all registered callbacks are called and any
     * chained Promises are resolved.
     * <p/>
     * Resolving the associated Promise happens-before any registered callback is called. That is, in a registered
     * callback, Promise.isDone() must return true and Promise.getValue() and Promise.getFailure() must not block.
     *
     * @param value The value of the resolved Promise.
     * @throws IllegalStateException If the associated Promise was already resolved.
     */
    public void resolve(T value) {
        promise.resolve(value);
    }

    /**
     * Fail the Promise associated with this Deferred.
     * <p/>
     * After the associated Promise is resolved with the specified failure, all registered callbacks are called and any
     * chained Promises are resolved.
     * <p/>
     * Resolving the associated Promise happens-before any registered callback is called. That is, in a registered
     * callback, Promise.isDone() must return true and Promise.getValue() and Promise.getFailure() must not block.
     *
     * @param failure The failure of the resolved Promise. Must not be null.
     * @throws IllegalStateException If the associated Promise was already resolved.
     */
    public void fail(Throwable failure) {
        promise.fail(failure);
    }

    /**
     * Resolve the Promise associated with this Deferred with the specified Promise.
     * <p/>
     * If the specified Promise is successfully resolved, the associated Promise is resolved with the value of the
     * specified Promise. If the specified Promise is resolved with a failure, the associated Promise is resolved with
     * the failure of the specified Promise.
     * <p/>
     * After the associated Promise is resolved with the specified Promise, all registered callbacks are called and any
     * chained Promises are resolved.
     * <p/>
     * Resolving the associated Promise happens-before any registered callback is called. That is, in a registered
     * callback, Promise.isDone() must return true and Promise.getValue() and Promise.getFailure() must not block
     *
     * @param with A Promise whose value or failure will be used to resolve the associated Promise. Must not be null.
     * @return A Promise that is resolved only when the associated Promise is resolved by the specified Promise. The
     * returned Promise will be successfully resolved, with the value null, if the associated Promise was resolved by
     * the specified Promise. The returned Promise will be resolved with a failure of IllegalStateException if the
     * associated Promise was already resolved when the specified Promise was resolved.
     */
    public Promise<Void> resolveWith(Promise<? extends T> with) {
        return promise.resolveWith(with);
    }

}
