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

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.*;

public class DeferredTest {

    @Test
    public void testResolve() throws Exception {
        Deferred<String> def = new Deferred<String>();
        Promise<String> promise = def.getPromise();
        assertFalse("Initial Promise not resolved", promise.isDone());

        def.resolve("Hello");
        assertTrue("Promise resolved", promise.isDone());
        assertEquals("Value matches", "Hello", promise.getValue());
        assertNull("Failure is null", promise.getFailure());

        try {
            def.resolve("Again");
            fail("Already resolved didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
            // suppress empty catch block warning
        }
    }

    @Test
    public void testResolveWithSuccess() throws Exception {
        Deferred<String> def = new Deferred<String>();
        Promise<String> promise = def.getPromise();

        Deferred<String> with = new Deferred<String>();
        Promise<Void> resolvedWith = def.resolveWith(with.getPromise());

        // If the specified Promise is successfully resolved,
        // the associated Promise is resolved with the value of the specified Promise.
        with.resolve("resolveWith");
        assertTrue("Promise resolved", promise.isDone());
        assertEquals("Value matches", "resolveWith", promise.getValue());

        // The returned Promise will be successfully resolved, with the value null,
        // if the associated Promise was resolved by the specified Promise.
        assertNull("resolveWith null", resolvedWith.getValue());
    }

    @Test
    public void testResolveWithAlreadyResolved() throws Exception {
        Deferred<String> def = new Deferred<String>();
        Deferred<String> with = new Deferred<String>();
        Promise<Void> resolvedWith = def.resolveWith(with.getPromise());

        // The returned Promise will be resolved with a failure of IllegalStateException
        // if the associated Promise was already resolved when the specified Promise was resolved.
        def.resolve("Already resolved");
        with.resolve("resolveWith");
        @SuppressWarnings({"not thrown", "all"})
        Throwable failure = resolvedWith.getFailure();
        assertTrue("resolveWith IllegalStateException", failure instanceof IllegalStateException);
    }

    @Test
    public void testResolveWithAlreadyFailed() throws Exception {
        Deferred<String> def = new Deferred<String>();
        Deferred<String> with = new Deferred<String>();
        Promise<Void> resolvedWith = def.resolveWith(with.getPromise());

        // The returned Promise will be resolved with a failure of IllegalStateException
        // if the associated Promise was already resolved when the specified Promise was resolved.
        def.resolve("Already resolved");
        with.fail(new Throwable("failed"));
        @SuppressWarnings({"not thrown", "all"})
        Throwable failure = resolvedWith.getFailure();
        assertTrue("resolveWith IllegalStateException", failure instanceof IllegalStateException);
    }

    @Test
    public void testResolveWithFailure() throws Exception {
        Deferred<String> def = new Deferred<String>();
        Promise<String> promise = def.getPromise();

        Deferred<String> def2 = new Deferred<String>();
        Promise<String> promise2 = def2.getPromise();
        Promise<Void> with = def.resolveWith(promise2);

        // If the specified Promise is resolved with a failure,
        // the associated Promise is resolved with the failure of the specified Promise.
        Exception failure = new Exception("resolveWithFailure");
        def2.fail(failure);
        assertTrue("Promise resolved", promise.isDone());
        assertEquals("Failure matches", failure, promise.getFailure());

        // The returned Promise will be successfully resolved, with the value null,
        // if the associated Promise was resolved by the specified Promise.
        assertNull("resolveWith null", with.getValue());
    }

    @Test
    public void testFail() throws Exception {
        Deferred<String> def = new Deferred<String>();
        Promise<String> promise = def.getPromise();
        Exception failure = new Exception("Oops");

        def.fail(failure);
        assertTrue("Promise resolved", promise.isDone());
        assertEquals("Failure matches", failure, promise.getFailure());

        try {
            promise.getValue();
            fail("getValue didn't throw InvocationTargetException");
        } catch (InvocationTargetException e) {
            assertEquals("Failure matches", failure, e.getCause());
        }

        try {
            def.fail(failure);
            fail("Already failed didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
    }


}
