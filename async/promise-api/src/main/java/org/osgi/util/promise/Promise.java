package org.osgi.util.promise;

import org.osgi.util.function.Function;
import org.osgi.util.function.Predicate;

import java.lang.reflect.InvocationTargetException;

/**
 * A Promise of a value.
 * <p/>
 * A Promise represents a future value. It handles the interactions to for asynchronous processing. A Deferred object
 * can be used to create a Promise and later resolve the Promise. A Promise is used by the caller of an asynchronous
 * function to get the result or handle the error. The caller can either get a callback when the Promise is resolved
 * with a value or an error, or the Promise can be used in chaining. In chaining, callbacks are provided that receive
 * the resolved Promise, and a new Promise is generated that resolves based upon the result of a callback.
 * <p/>
 * Both callbacks and chaining can be repeated any number of times, even after the Promise has been resolved.
 * <p/>
 * Example callback usage:
 * <pre>
 * final Promise<String> foo= foo();
 * foo.onResolve(new Runnable() {
 * public void run() {
 * System.out.println(foo.getValue());
 * }
 * });
 * </pre>
 * <p/>
 * Example chaining usage;
 * <pre>
 * Success<String,String> doubler = new Success<String,String>() {
 * public Promise<String> call(Promise<String> p) throws Exception {
 * return Promises.resolved(p.getValue()+p.getValue());
 * }
 * };
 * final Promise<String> foo = foo().then(doubler).then(doubler);
 * foo.onResolve(new Runnable() {
 * public void run() {
 * System.out.println(foo.getValue());
 * }
 * });
 * </pre>
 *
 * @param <T> The value type associated with this Promise.
 */
//@org.osgi.annotation.versioning.ProviderType
public interface Promise<T> {
    /**
     * Returns whether this Promise has been resolved.
     * <p/>
     * This Promise may be successfully resolved or resolved with a failure.
     *
     * @return true if this Promise was resolved either successfully or with a failure; false if this Promise is
     * unresolved.
     */
    boolean isDone();

    /**
     * Returns the value of this Promise.
     * <p/>
     * If this Promise is not resolved, this method must block and wait for this Promise to be resolved before
     * completing.
     * <p/>
     * If this Promise was successfully resolved, this method returns with the value of this Promise. If this Promise
     * was resolved with a failure, this method must throw an InvocationTargetException with the failure exception as
     * the cause.
     *
     * @return The value of this resolved Promise.
     * @throws InvocationTargetException If this Promise was resolved with a failure. The cause of the
     *                                   InvocationTargetException is the failure exception.
     * @throws InterruptedException      If the current thread was interrupted while waiting.
     */
    T getValue() throws InvocationTargetException, InterruptedException;

    /**
     * Returns the failure of this Promise.
     * <p/>
     * If this Promise is not resolved, this method must block and wait for this Promise to be resolved before
     * completing.
     * <p/>
     * If this Promise was resolved with a failure, this method returns with the failure of this Promise. If this
     * Promise was successfully resolved, this method must return null.
     *
     * @return The failure of this resolved Promise or null if this Promise was successfully resolved.
     * @throws InterruptedException If the current thread was interrupted while waiting.
     */
    Throwable getFailure() throws InterruptedException;

    /**
     * Register a callback to be called when this Promise is resolved.
     * <p/>
     * The specified callback is called when this Promise is resolved either successfully or with a failure.
     * <p/>
     * This method may be called at any time including before and after this Promise has been resolved.
     * <p/>
     * Resolving this Promise happens-before any registered callback is called. That is, in a registered callback,
     * isDone() must return true and getValue() and getFailure() must not block.
     * <p/>
     * A callback may be called on a different thread than the thread which registered the callback. So the callback
     * must be thread safe but can rely upon that the registration of the callback happens-before the registered
     * callback is called.
     *
     * @param callback A callback to be called when this Promise is resolved. Must not be null.
     * @return This Promise.
     */
    Promise<T> onResolve(Runnable callback);


    /**
     * Chain a new Promise to this Promise with Success and Failure callbacks.
     * <p/>
     * The specified Success callback is called when this Promise is successfully resolved and the specified Failure
     * callback is called when this Promise is resolved with a failure.
     * <p/>
     * This method returns a new Promise which is chained to this Promise. The returned Promise must be resolved when
     * this Promise is resolved after the specified Success or Failure callback is executed. The result of the executed
     * callback must be used to resolve the returned Promise. Multiple calls to this method can be used to create a
     * chain of promises which are resolved in sequence.
     * <p/>
     * If this Promise is successfully resolved, the Success callback is executed and the result Promise, if any, or
     * thrown exception is used to resolve the returned Promise from this method. If this Promise is resolved with a
     * failure, the Failure callback is executed and the returned Promise from this method is failed.
     * <p/>
     * This method may be called at any time including before and after this Promise has been resolved.
     * <p/>
     * Resolving this Promise happens-before any registered callback is called. That is, in a registered callback,
     * isDone() must return true and getValue() and getFailure() must not block.
     * <p/>
     * A callback may be called on a different thread than the thread which registered the callback. So the callback
     * must be thread safe but can rely upon that the registration of the callback happens-before the registered
     * callback is called.
     *
     * @param <R>     The value type associated with the returned Promise.
     * @param success A Success callback to be called when this Promise is successfully resolved. May be null if no
     *                Success callback is required. In thi
     * @param failure A Failure callback to be called when this Promise is resolved with a failure. May be null if no
     *                Failure callback is required.
     * @return A new Promise which is chained to this Promise. The returned Promise must be resolved when this Promise
     * is resolved after the specified Success or Failure callback, if any, is executed
     */
    <R> Promise<R> then(Success<? super T, ? extends R> success, Failure failure);


    /**
     * Chain a new Promise to this Promise with a Success callback.
     * <p/>
     * This method performs the same function as calling then(Success, Failure) with the specified Success callback and
     * null for the Failure callback
     *
     * @param success A Success callback to be called when this Promise is successfully resolved. May be null if no
     *                Success callback is required. In this case, the returned Promise must be resolved with the value
     *                null when this Promise is successfully resolved.
     * @param <R>     The value type associated with the returned Promise.
     * @return A new Promise which is chained to this Promise. The returned Promise must be resolved when this Promise
     * is resolved after the specified Success, if any, is executed.
     * @see #then(Success, Failure)
     */
    <R> Promise<R> then(Success<? super T, ? extends R> success);


    /**
     * Filter the value of this Promise.
     * <p/>
     * If this Promise is successfully resolved, the returned Promise will either be resolved with the value of this
     * Promise if the specified Predicate accepts that value or failed with a NoSuchElementException if the specified
     * Predicate does not accept that value. If the specified Predicate throws an exception, the returned Promise will
     * be failed with the exception.
     * <p/>
     * If this Promise is resolved with a failure, the returned Promise will be failed with that failure.
     * <p/>
     * This method may be called at any time including before and after this Promise has been resolved.
     *
     * @param predicate The Predicate to evaluate the value of this Promise. Must not be null.
     * @return A Promise that filters the value of this Promise.
     */
    Promise<T> filter(Predicate<? super T> predicate);

    /**
     * Map the value of this Promise.
     * <p/>
     * If this Promise is successfully resolved, the returned Promise will be resolved with the value of specified
     * Function as applied to the value of this Promise. If the specified Function throws an exception, the returned
     * Promise will be failed with the exception.
     * <p/>
     * If this Promise is resolved with a failure, the returned Promise will be failed with that failure.
     * <p/>
     * This method may be called at any time including before and after this Promise has been resolved.
     *
     * @param mapper The Function that will map the value of this Promise to the value that will be used to resolve the
     *               returned Promise. Must not be null.
     * @param <R>    The value type associated with the returned Promise.
     * @return A Promise that returns the value of this Promise as mapped by the specified Function.
     */
    <R> Promise<R> map(Function<? super T, ? extends R> mapper);

    /**
     * FlatMap the value of this Promise.
     * <p/>
     * If this Promise is successfully resolved, the returned Promise will be resolved with the Promise from the
     * specified Function as applied to the value of this Promise. If the specified Function throws an exception, the
     * returned Promise will be failed with the exception.
     * <p/>
     * If this Promise is resolved with a failure, the returned Promise will be failed with that failure.
     * <p/>
     * This method may be called at any time including before and after this Promise has been resolved.
     *
     * @param mapper The Function that will flatMap the value of this Promise to a Promise that will be used to resolve
     *               the returned Promise. Must not be null.
     * @param <R>    The value type associated with the returned Promise.
     * @return A Promise that returns the value of this Promise as mapped by the specified Function.
     */
    <R> Promise<R> flatMap(Function<? super T, Promise<? extends R>> mapper);

    /**
     * Recover from a failure of this Promise with a recovery value.
     * <p/>
     * If this Promise is successfully resolved, the returned Promise will be resolved with the value of this Promise.
     * <p/>
     * If this Promise is resolved with a failure, the specified Function is applied to this Promise to produce a
     * recovery value.
     * <p/>
     * If the recovery value is not null, the returned Promise will be resolved with the recovery value.
     * <p/>
     * If the recovery value is null, the returned Promise will be failed with the failure of this Promise.
     * <p/>
     * If the specified Function throws an exception, the returned Promise will be failed with that exception.
     * <p/>
     * To recover from a failure of this Promise with a recovery value of null, the recoverWith(Function) method must be
     * used. The specified Function for recoverWith(Function) can return Promises.resolved(null) to supply the desired
     * null value.
     * <p/>
     * This method may be called at any time including before and after this Promise has been resolved.
     *
     * @param recovery If this Promise resolves with a failure, the specified Function is called to produce a recovery
     *                 value to be used to resolve the returned Promise. Must not be null.
     * @return A Promise that resolves with the value of this Promise or recovers from the failure of this Promise.
     */
    Promise<T> recover(Function<Promise<?>, ? extends T> recovery);

    /**
     * Recover from a failure of this Promise with a recovery Promise.
     * <p/>
     * If this Promise is successfully resolved, the returned Promise will be resolved with the value of this Promise.
     * <p/>
     * If this Promise is resolved with a failure, the specified Function is applied to this Promise to produce a
     * recovery Promise.
     * <p/>
     * If the recovery Promise is not null, the returned Promise will be resolved with the recovery Promise.
     * <p/>
     * If the recovery Promise is null, the returned Promise will be failed with the failure of this Promise.
     * <p/>
     * If the specified Function throws an exception, the returned Promise will be failed with that exception.
     * <p/>
     * This method may be called at any time including before and after this Promise has been resolved.
     *
     * @param recovery If this Promise resolves with a failure, the specified Function is called to produce a recovery
     *                 Promise to be used to resolve the returned Promise. Must not be null.
     * @return A Promise that resolves with the value of this Promise or recovers from the failure of this Promise.
     */
    Promise<T> recoverWith(Function<Promise<?>, Promise<? extends T>> recovery);

    /**
     * Fall back to the value of the specified Promise if this Promise fails.
     * <p/>
     * If this Promise is successfully resolved, the returned Promise will be resolved with the value of this Promise.
     * <p/>
     * If this Promise is resolved with a failure, the successful result of the specified Promise is used to resolve the
     * returned Promise. If the specified Promise is resolved with a failure, the returned Promise will be failed with
     * the failure of this Promise rather than the failure of the specified Promise.
     * <p/>
     * This method may be called at any time including before and after this Promise has been resolved.
     *
     * @param fallback The Promise whose value will be used to resolve the returned Promise if this Promise resolves
     *                 with a failure. Must not be null.
     * @return A Promise that returns the value of this Promise or falls back to the value of the specified Promise.
     */
    Promise<T> fallbackTo(Promise<? extends T> fallback);

}
