/*
 * Copyright (c) OSGi Alliance 2015. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
