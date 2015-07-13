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

import org.junit.Test;
import org.osgi.util.promise.*;

import static org.junit.Assert.*;

public class ChainTest {

    @Test
    public void testThenSuccess() throws Exception {
        Deferred<String> def = new Deferred<String>();

        Promise<String> chain = def.getPromise().then(new Success<String, String>() {
            @Override
            public Promise<String> call(Promise<String> resolved) throws Exception {
                return Promises.resolved("success!");
            }
        });
        assertFalse("chain not resolved", chain.isDone());

        def.resolve("ok");
        assertTrue("chain resolved", chain.isDone());
        assertEquals("chain value matches", "success!", chain.getValue());
    }

    @Test
    public void testThenSuccessDeferred() throws Exception {
        Deferred<String> def = new Deferred<String>();
        final Deferred<String> def2 = new Deferred<String>();

        Promise<String> chain = def.getPromise().then(new Success<String, String>() {
            @Override
            public Promise<String> call(Promise<String> resolved) throws Exception {
                return def2.getPromise();
            }
        });
        assertFalse("chain not resolved", chain.isDone());

        def.resolve("ok");
        assertFalse("chain still not resolved", chain.isDone());

        def2.resolve("success!");
        assertTrue("chain resolved", chain.isDone());
        assertEquals("chain value matches", "success!", chain.getValue());
    }

    @Test
    public void testThenSuccessFailed() throws Exception {
        Deferred<String> def = new Deferred<String>();
        final Promise<String> promise = def.getPromise();
        final Throwable thenFail = new Throwable("failed!");

        Promise<String> chain = promise.then(new Success<String, String>() {
            @Override
            public Promise<String> call(Promise<String> resolved) throws Exception {
                return Promises.failed(thenFail);
            }
        });
        assertFalse("chain not resolved", chain.isDone());

        def.resolve("ok");
        assertTrue("chain resolved", chain.isDone());
        assertEquals("chain failure matches", thenFail, chain.getFailure());
    }

    @Test
    public void testThenSuccessException() throws Exception {
        Deferred<String> def = new Deferred<String>();
        final Promise<String> promise = def.getPromise();
        final Exception thenException = new Exception("then exception!");

        Promise<String> chain = promise.then(new Success<String, String>() {
            @Override
            public Promise<String> call(Promise<String> resolved) throws Exception {
                throw thenException;
            }
        });
        assertFalse("chain not resolved", chain.isDone());

        def.resolve("ok");
        assertTrue("chain resolved", chain.isDone());
        assertEquals("chain failure matches", thenException, chain.getFailure());
    }

    @Test
    public void testThenFail() throws Exception {
        Deferred<String> def = new Deferred<String>();
        final Promise<String> promise = def.getPromise();

        Promise<String> chain = promise.then(null, new Failure() {
            @Override
            public void fail(Promise<?> resolved) throws Exception {
            }
        });
        assertFalse("chain not resolved", chain.isDone());

        Throwable failure = new Throwable("fail!");
        def.fail(failure);
        assertTrue("chain resolved", chain.isDone());
        assertEquals("chain failure matches", failure, chain.getFailure());
    }

    @Test
    public void testThenFailNoCallback() throws Exception {
        Deferred<String> def = new Deferred<String>();
        final Promise<String> promise = def.getPromise();

        Promise<String> chain = promise.then(null);
        assertFalse("chain not resolved", chain.isDone());

        Throwable failure = new Throwable("fail!");
        def.fail(failure);
        assertTrue("chain resolved", chain.isDone());
        assertEquals("chain failure matches", failure, chain.getFailure());
    }

    @Test
    public void testThenFailException() throws Exception {
        Deferred<String> def = new Deferred<String>();
        final Promise<String> promise = def.getPromise();
        final Exception thenException = new Exception("eek!");

        Promise<String> chain = promise.then(null, new Failure() {
            @Override
            public void fail(Promise<?> resolved) throws Exception {
                throw thenException;
            }
        });
        assertFalse("chain not resolved", chain.isDone());

        def.fail(new Throwable("failed"));
        assertTrue("chain resolved", chain.isDone());
        assertEquals("chain failure matches", thenException, chain.getFailure());
    }

    @Test
    public void testThenNull() throws Exception {
        Deferred<String> def = new Deferred<String>();
        final Promise<String> promise = def.getPromise();
        Promise<String> chain = promise.then(null);
        assertFalse("chain not resolved", chain.isDone());

        def.resolve("ok");
        assertTrue("chain resolved", chain.isDone());
        assertNull("chain value null", chain.getValue());
    }

    @Test
    public void testThenNullResolved() throws Exception {
        Deferred<String> def = new Deferred<String>();
        def.resolve("ok");
        Promise<String> chain = def.getPromise().then(null);

        assertTrue("chain resolved", chain.isDone());
        assertNull("chain value null", chain.getValue());
    }

    @Test
    public void testThenAlreadyResolved() throws Exception {
        Deferred<String> def = new Deferred<String>();
        final Promise<String> promise = def.getPromise();
        def.resolve("ok");

        Promise<String> chain = promise.then(new Success<String, String>() {
            @Override
            public Promise<String> call(Promise<String> resolved) throws Exception {
                return Promises.resolved("success!");
            }
        });
        assertTrue("chain resolved", chain.isDone());
        assertEquals("chain value matches", "success!", chain.getValue());
    }

    @Test
    public void testExampleChain() throws Exception {
        Success<String, String> doubler = new Success<String, String>() {
            public Promise<String> call(Promise<String> p) throws Exception {
                return Promises.resolved(p.getValue() + p.getValue());
            }
        };

        Deferred<String> def = new Deferred<String>();
        final Promise<String> foo = def.getPromise().then(doubler).then(doubler);

        def.resolve("hello!");
        assertEquals("doubler matches", "hello!hello!hello!hello!", foo.getValue());
    }

    @Test
    public void testThen2() throws Exception {
        Deferred<String> def = new Deferred<String>();

        Promise<String> chain1 = def.getPromise().then(new Success<String, String>() {
            @Override
            public Promise<String> call(Promise<String> resolved) throws Exception {
                return Promises.resolved("success1");
            }
        });
        assertFalse("chain not resolved", chain1.isDone());

        Promise<String> chain2 = def.getPromise().then(new Success<String, String>() {
            @Override
            public Promise<String> call(Promise<String> resolved) throws Exception {
                return Promises.resolved("success2");
            }
        });
        assertFalse("chain not resolved", chain2.isDone());

        def.resolve("ok");
        assertTrue("chain1 resolved", chain1.isDone());
        assertEquals("chain1 value matches", "success1", chain1.getValue());
        assertTrue("chain2 resolved", chain2.isDone());
        assertEquals("chain2 value matches", "success2", chain2.getValue());
    }

    @Test
    public void testThenResolved() throws Exception {
        Deferred<String> def = new Deferred<String>();
        def.resolve("already resolved");

        Promise<String> chain = def.getPromise().then(new Success<String, String>() {
            @Override
            public Promise<String> call(Promise<String> resolved) throws Exception {
                return Promises.resolved("success!");
            }
        });
        assertTrue("chain resolved", chain.isDone());

        assertEquals("chain value matches", "success!", chain.getValue());
    }

    @Test
    public void testThenResolved2() throws Exception {
        Deferred<String> def = new Deferred<String>();
        def.resolve("already resolved");

        Promise<String> chain = def.getPromise().then(new Success<String, String>() {
            @Override
            public Promise<String> call(Promise<String> resolved) throws Exception {
                return Promises.resolved("success1");
            }
        }).then(new Success<String, String>() {
            @Override
            public Promise<String> call(Promise<String> resolved) throws Exception {
                return Promises.resolved("success2");
            }
        });

        assertTrue("chain resolved", chain.isDone());

        assertEquals("chain value matches", "success2", chain.getValue());
    }
}
