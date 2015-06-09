package org.apache.aries.async.promise.test;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CallbackTest {

    @Test
    public void testCallbacks() throws Exception {
        Deferred<String> def = new Deferred<String>();
        final Promise<String> promise = def.getPromise();

        Callback cb1 = new Callback(promise, "Hello");
        assertEquals("onResolve returns promise", promise, promise.onResolve(cb1));

        Callback cb2 = new Callback(promise, "Hello");
        promise.onResolve(cb2);

        def.resolve("Hello");

        assertTrue("callback1 executed", cb1.latch.await(1, TimeUnit.SECONDS));
        assertEquals("callback1 succeeded", null, cb1.error);

        assertTrue("callback2 executed", cb2.latch.await(1, TimeUnit.SECONDS));
        assertEquals("callback2 succeeded", null, cb2.error);

        // register callback after promise is resolved
        Callback cb3 = new Callback(promise, "Hello");
        promise.onResolve(cb3);
        assertTrue("callback3 executed", cb3.latch.await(1, TimeUnit.SECONDS));
        assertEquals("callback3 succeeded", null, cb3.error);
    }


    class Callback implements Runnable {
        final CountDownLatch latch = new CountDownLatch(1);
        Throwable error = null;

        private final Promise promise;
        private final Object value;

        Callback(Promise promise, Object value) {
            this.promise = promise;
            this.value = value;
        }

        @Override
        public void run() {
            try {
                assertTrue("Promise resolved", promise.isDone());
                assertEquals("Value matches", value, promise.getValue());
            }
            catch (Throwable t) {
                error = t;
            }
            finally {
                latch.countDown();
            }
        }
    }


}
