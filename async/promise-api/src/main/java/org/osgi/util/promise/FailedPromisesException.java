package org.osgi.util.promise;

import java.util.Collection;
import java.util.Collections;

/**
 * Promise failure exception for a collection of failed Promises.
 */
public class FailedPromisesException extends RuntimeException {

    private final Collection<Promise<?>> failed;

    /**
     * Create a new FailedPromisesException with the specified Promises.
     *
     * @param failed A collection of Promises that have been resolved with a failure. Must not be null.
     */
    public FailedPromisesException(Collection<Promise<?>> failed) {
        this(failed, null);
    }

    /**
     * Create a new FailedPromisesException with the specified Promises.
     *
     * @param failed A collection of Promises that have been resolved with a failure. Must not be null.
     * @param cause  the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A <tt>null</tt>
     *               value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public FailedPromisesException(Collection<Promise<?>> failed, Throwable cause) {
        super(cause);
        assert failed != null;
        this.failed = failed;
    }

    /**
     * Returns the collection of Promises that have been resolved with a failure.
     *
     * @return The collection of Promises that have been resolved with a failure. The returned collection is
     * unmodifiable.
     */
    public Collection<Promise<?>> getFailedPromises() {
        return Collections.unmodifiableCollection(failed);
    }
}
