package org.osgi.service.async;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.ServiceReference;
import org.osgi.util.promise.Promise;

/**
 * The Async Service, as defined in OSGi RFC 204
 * https://github.com/osgi/design/tree/master/rfcs/rfc0206
 */
@ProviderType
public interface Async {
    
    /**
     * Create a mediated object for asynchronous calls
     *
     * @param target The object to mediate
     * @param iface The type that the mediated object should implement or extend
     * @return A mediated object
     * @throws IllegalArgumentException if mediation fails
     */
    <T> T mediate(T target, Class<T> iface);
    
    /**
     * Create a mediated object for asynchronous calls
     *
     * @param target The service reference to mediate
     * @param iface The type that the mediated object should implement or extend
     * @return A mediated service
     * @throws IllegalArgumentException if mediation fails
     */
    <T> T mediate(ServiceReference<? extends T> target, Class<T> iface);
    
    /**
     * Asynchronously run the last method call registered by a mediated object,
     * returning the result as a Promise.
     *
     * @param r the return value of the mediated call
     * @return a Promise
     */
    <R> Promise<R> call(R r);
    
    /**
     * Asynchronously run the last method call registered by a mediated object,
     * returning the result as a Promise.
     *
     * @return a Promise
     */
    Promise<?> call();
    
    /**
     * Asynchronously run the last method call registered by a mediated object,
     * ignoring the return value.
     *
     * @return a Promise indicating whether the task started successfully
     */
    Promise<Void> execute();
    
}
