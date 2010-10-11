package org.apache.aries.subsystem.core.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 
 * @author charters
 *
 * @param <V>
 */
public class ImmediateFuture<V> implements Future<V> {

    V result;
    
    public ImmediateFuture(V result) {
        this.result = result;
    }
    
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    public boolean isCancelled() {
        return false;
    }

    public boolean isDone() {
        return true;
    }

    public V get() throws InterruptedException, ExecutionException {
        return result;
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        return result;
    }

}
