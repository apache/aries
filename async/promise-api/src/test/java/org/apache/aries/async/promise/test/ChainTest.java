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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import org.osgi.util.function.Callback;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Failure;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import org.osgi.util.promise.Success;
import org.osgi.util.promise.TimeoutException;

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

        Promise<String> chain = promise.then((Success)null);
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
        Promise<String> chain = promise.then((Success)null);
        assertFalse("chain not resolved", chain.isDone());

        def.resolve("ok");
        assertTrue("chain resolved", chain.isDone());
        assertNull("chain value null", chain.getValue());
    }

    @Test
    public void testThenNullResolved() throws Exception {
        Deferred<String> def = new Deferred<String>();
        def.resolve("ok");
        Promise<String> chain = def.getPromise().then((Success)null);

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
    
    @Test
    public void testThenCallbackSuccess() throws Exception {
    	Deferred<String> def = new Deferred<String>();

    	final AtomicBoolean run = new AtomicBoolean(false);
    	
        Promise<String> chain = def.getPromise().then(new Callback() {
            @Override
            public void run() throws Exception {
                run.set(true);
            }
        });
        assertFalse("chain should not be resolved", chain.isDone());
        assertFalse("callback should not have been run", run.get());

        def.resolve("ok");
        assertTrue("chain resolved", chain.isDone());
        assertEquals("chain value matches", "ok", chain.getValue());
        assertTrue("callback should have been run", run.get());
    	
    }

    @Test
    public void testThenCallbackFail() throws Exception {
    	Deferred<String> def = new Deferred<String>();
    	
    	final AtomicBoolean run = new AtomicBoolean(false);
    	
    	Promise<String> chain = def.getPromise().then(new Callback() {
    		@Override
    		public void run() throws Exception {
    			run.set(true);
    		}
    	});
    	
    	Exception failure = new Exception("bang!");
    	
    	assertFalse("chain should not be resolved", chain.isDone());
    	assertFalse("callback should not have been run", run.get());
    	
    	def.fail(failure);
    	assertTrue("chain resolved", chain.isDone());
    	assertSame("chain value matches", failure, chain.getFailure());
    	assertTrue("callback should have been run", run.get());
    	
    }

    @Test
    public void testThenCallbackThrowsExceptionSuccess() throws Exception {
    	Deferred<String> def = new Deferred<String>();
    	
    	final Exception failure = new Exception("bang!");
    	
    	Promise<String> chain = def.getPromise().then(new Callback() {
    		@Override
    		public void run() throws Exception {
    			throw failure;
    		}
    	});
    	
    	assertFalse("chain should not be resolved", chain.isDone());
    	
    	def.resolve("ok");
    	assertTrue("chain resolved", chain.isDone());
    	assertSame("chain value matches", failure, chain.getFailure());
    	
    }

    @Test
    public void testThenCallbackThrowsExceptionFail() throws Exception {
    	Deferred<String> def = new Deferred<String>();
    	
    	final Exception failure = new Exception("bang!");
    	
    	Promise<String> chain = def.getPromise().then(new Callback() {
    		@Override
    		public void run() throws Exception {
    			throw failure;
    		}
    	});
    	
    	assertFalse("chain should not be resolved", chain.isDone());
    	
    	def.fail(new IllegalStateException());
    	assertTrue("chain resolved", chain.isDone());
    	assertSame("chain value matches", failure, chain.getFailure());
    	
    }
    
    @Test
    public void testTimeout() throws Exception {
    	Deferred<String> def = new Deferred<String>();

        Promise<String> promise = def.getPromise();
        
        long start = System.nanoTime();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicLong finish = new AtomicLong();
        
		Promise<String> chain = promise.timeout(500)
				.onResolve(new Runnable() {
					@Override
					public void run() {
						finish.set(System.nanoTime());
						latch.countDown();
					}
				});
		
		assertFalse("promise should not be resolved", promise.isDone());
		assertFalse("chain should not be resolved", chain.isDone());
		
		assertTrue("Did not time out!", latch.await(1, SECONDS));
		assertTrue("Finished too fast", NANOSECONDS.toMillis(finish.get() - start) > 450);

		assertFalse("promise should not be resolved", promise.isDone());
		assertTrue("chain should now be resolved", chain.isDone());
		
        assertTrue("Should fail with a timeout exception", chain.getFailure() instanceof TimeoutException);
    }

    @Test
    public void testTimeoutSuccess() throws Exception {
    	Deferred<String> def = new Deferred<String>();
    	
    	Promise<String> promise = def.getPromise();
    	
    	final CountDownLatch latch = new CountDownLatch(1);
    	
    	Promise<String> chain = promise.timeout(500)
    			.onResolve(new Runnable() {
    				@Override
    				public void run() {
    					latch.countDown();
    				}
    			});
    	
    	assertFalse("promise should not be resolved", promise.isDone());
    	assertFalse("chain should not be resolved", chain.isDone());
    	
    	def.resolve("ok");
    	
    	assertTrue("Did not eagerly complete!", latch.await(100, MILLISECONDS));
    	
    	assertTrue("promise should not be resolved", promise.isDone());
    	assertTrue("chain should now be resolved", chain.isDone());
    	
    	assertEquals(promise.getValue(), chain.getValue());
    }
    
    @Test
    public void testTimeoutFailure() throws Exception{
    	Deferred<String> def = new Deferred<String>();
    	
    	Promise<String> promise = def.getPromise();
    	
    	final CountDownLatch latch = new CountDownLatch(1);
    	
    	Promise<String> chain = promise.timeout(500)
    			.onResolve(new Runnable() {
    				@Override
    				public void run() {
    					latch.countDown();
    				}
    			});
    	
    	assertFalse("promise should not be resolved", promise.isDone());
    	assertFalse("chain should not be resolved", chain.isDone());
    	
    	Exception failure = new Exception("bang!");
    	
    	def.fail(failure);
    	
    	assertTrue("Did not eagerly complete!", latch.await(100, MILLISECONDS));
    	
    	assertTrue("promise should not be resolved", promise.isDone());
    	assertTrue("chain should now be resolved", chain.isDone());
    	
    	assertSame(promise.getFailure(), chain.getFailure());
    }
}
