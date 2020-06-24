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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.FailedPromisesException;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

public class PromisesTest {

    @Test
    public void testResolved() throws Exception {
//IC see: https://issues.apache.org/jira/browse/ARIES-1317
        final Promise<String> promise = Promises.resolved("Resolved");
        assertTrue("Promise resolved", promise.isDone());
        assertEquals("Value matches", "Resolved", promise.getValue());
    }

    @Test
    public void testFailed() throws Exception {
        Exception failed = new Exception("Failed");
        final Promise<String> promise = Promises.failed(failed);
        assertTrue("Promise resolved", promise.isDone());
        assertEquals("Value matches", failed, promise.getFailure());
    }

    @Test
    public void testLatch() throws Exception {
        testLatch(false, "hello", "world");
        testLatch(true, "hello", "world");
        testLatch(false, "goodbye", "!cruel", "world");
        testLatch(true, "goodbye", "!cruel", "world");
        testLatch(false, "goodbye", "!cruel", "!world");
        testLatch(false);
        testLatch(1, 2);
        testLatch(1, -2, 3);
        testLatch(1, -2, 3, -4);
        testLatch(new Integer[0]);
    }

    // T = String
    private void testLatch(boolean preResolve, String... rv) throws Exception {
        @SuppressWarnings("unchecked")
        Deferred<String>[] dv = new Deferred[rv.length];
        @SuppressWarnings("unchecked")
        Promise<String>[] pv = new Promise[rv.length];

        for (int i = 0; i < rv.length; i++) {
            dv[i] = new Deferred<String>();
            pv[i] = dv[i].getPromise();
        }

        Promise<List<String>> latch = null;
        if (!preResolve) {
            Promise<List<String>> latch2 = Promises.all(pv);
            latch = latch2;

            if (rv.length == 0) {
                assertTrue("latch resolved", latch.isDone());
                return;
            }
            assertFalse("latch not resolved", latch.isDone());
        }

        int nFail = 0;
        for (int i = 0; i < rv.length; i++) {
            String res = rv[i];
            if (res.startsWith("!")) {
                dv[i].fail(new Exception(res));
                nFail++;
            } else {
                dv[i].resolve(res);
            }
        }

        if (preResolve) {
            Promise<List<String>> latch2 = Promises.all(pv);
            latch = latch2;
        }

        assertTrue("latch resolved", latch.isDone());

        if (nFail > 0) {
            @SuppressWarnings({"not thrown", "all"})
            Throwable failure = latch.getFailure();
            assertTrue("failure instanceof FailedPromisesException", failure instanceof FailedPromisesException);
            Collection<Promise<?>> failedPromises = ((FailedPromisesException) failure).getFailedPromises();
            assertEquals("failedPromises size matches", nFail, failedPromises.size());

            for (int i = 0; i < rv.length; i++) {
                Promise<String> promise = pv[i];
                if (rv[i].startsWith("!")) {
                    assertTrue("failedPromises contains", failedPromises.contains(promise));
                } else {
                    assertFalse("failedPromises doesn't contain", failedPromises.contains(promise));
                }
            }
        } else {
            List<String> list = latch.getValue();
            assertEquals("list size matches", rv.length, list.size());
            for (int i = 0; i < rv.length; i++) {
                assertEquals("list[i] matches", rv[i], list.get(i));
            }

            // check list is modifiable
            list.add(0, "new item");
            assertEquals("list modifiable", "new item", list.get(0));
        }
    }

    // T = Number
    // S = Integer
    private void testLatch(Integer... rv) throws Exception {
        @SuppressWarnings("unchecked")
        Deferred<Integer>[] dv = new Deferred[rv.length];

        List<Promise<Integer>> promises = new ArrayList<Promise<Integer>>();

        for (int i = 0; i < rv.length; i++) {
            dv[i] = new Deferred<Integer>();
            promises.add(dv[i].getPromise());
        }

        Promise<List<Number>> latch = Promises.all(promises);
        if (rv.length == 0) {
            assertTrue("latch resolved", latch.isDone());
            return;
        }
        assertFalse("latch not resolved", latch.isDone());

        int nFail = 0;
        for (int i = 0; i < rv.length; i++) {
            Integer res = rv[i];
            if (res < 0) {
                dv[i].fail(new Exception("fail" + res));
                nFail++;
            } else {
                dv[i].resolve(res);
            }
        }
        assertTrue("latch resolved", latch.isDone());

        if (nFail > 0) {
            @SuppressWarnings({"not thrown", "all"})
            Throwable failure = latch.getFailure();
            assertTrue("failure instanceof FailedPromisesException", failure instanceof FailedPromisesException);
            Collection<Promise<?>> failedPromises = ((FailedPromisesException) failure).getFailedPromises();
            assertEquals("failedPromises size matches", nFail, failedPromises.size());

            for (int i = 0; i < rv.length; i++) {
                Promise<Integer> promise = promises.get(i);
                if (rv[i] < 0) {
                    assertTrue("failedPromises contains", failedPromises.contains(promise));
                } else {
                    assertFalse("failedPromises doesn't contain", failedPromises.contains(promise));
                }
            }
        } else {
            List<Number> list = latch.getValue();
            assertEquals("list size matches", rv.length, list.size());
            for (int i = 0; i < rv.length; i++) {
                assertEquals("list[i] matches", rv[i], list.get(i));
            }

            // check list is modifiable
            list.add(0, 3.14);
            assertEquals("list modifiable", 3.14, list.get(0));
        }
    }


}
