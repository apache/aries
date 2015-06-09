package org.osgi.util.promise;

/**
 * Failure callback for a Promise.
 * <p>
 * A Failure callback is registered with a Promise using the Promise.then(Success, Failure) method and is called if the Promise is resolved with a failure.
 * <p>
 * This is a functional interface and can be used as the assignment target for a lambda expression or method reference.
 */
//@org.osgi.annotation.versioning.ConsumerType
public interface Failure {
    /**
     * Failure callback for a Promise.
     * <p>
     * This method is called if the Promise with which it is registered resolves with a failure.
     * <p>
     * In the remainder of this description we will refer to the Promise returned by Promise.then(Success, Failure) when this Failure callback was registered as the chained Promise.
     * <p>
     * If this method completes normally, the chained Promise will be failed with the same exception which failed the resolved Promise. If this method throws an exception, the chained Promise will be failed with the thrown exception.
     *
     * @param resolved The failed resolved Promise.
     * @throws Exception The chained Promise will be failed with the thrown exception.
     */
    void fail(Promise<?> resolved) throws Exception;
}
