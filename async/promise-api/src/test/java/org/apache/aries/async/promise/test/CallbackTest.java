/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
