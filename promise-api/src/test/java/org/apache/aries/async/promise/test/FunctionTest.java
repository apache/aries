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
import static org.junit.Assert.assertTrue;

import java.util.NoSuchElementException;

import org.junit.Test;
import org.osgi.util.function.Function;
import org.osgi.util.function.Predicate;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

public class FunctionTest {

    @Test
    public void testFilter() throws Exception {
        testFilter("hello");
        testFilter("!reject");
        testFilter("fail");
        testFilter(null);
        testFilter("already");
        testFilter("already!");
    }

    @Test
    public void testMap() throws Exception {
        testMap("hello");
        testMap("hello!");
        testMap("fail");
        testMap("fail!");
    }

    @Test
    public void testFlatMap() throws Exception {
        testFlatMap("hello");
        testFlatMap("hello!");
        testFlatMap("fail");
    }

    @Test
    public void testRecover() throws Exception {
        testRecover("hello");
        testRecover("fail");
        testRecover("null");
        testRecover(null);
    }

    @Test
    public void testRecoverWith() throws Exception {
        testRecoverWith("hello");
        testRecoverWith("fail");
        testRecoverWith("null");
        testRecoverWith(null);
    }

    @Test
    public void testFallback() throws Exception {
        testFallback("hello", "world");
        testFallback("fail", "world");
        testFallback("hello", "fail");
        testFallback("fail", "fail");
    }

    @Test
    public void testFilter2() throws Exception {
        String bigValue = new String("value");
        Promise<String> p1 = Promises.resolved(bigValue);

        Promise<String> p2 = p1.filter(new Predicate<String>() {
            public boolean test(String t) {
                return t.length() > 0;
            }
        });
        assertTrue("Filter2 resolved", p2.isDone());
        assertEquals("Value2 matches", bigValue, p2.getValue());

        Promise<String> p3 = p1.filter(new Predicate<String>() {
            public boolean test(String t) {
                return t.length() == 0;
            }
        });
        assertTrue("Filter3 resolved", p3.isDone());
        assertTrue("Value3 fail matches", p3.getFailure() instanceof NoSuchElementException);
    }

    public static class Nasty extends RuntimeException {
        public Nasty(String msg) {
            super(msg);
        }
    }

    private void testFilter(String r) throws Exception {
        Deferred<String> def = new Deferred<String>();

        Throwable fail = new Throwable("fail");
        boolean already = (r != null && r.startsWith("already"));

        if (already) {
            if (r.contains("!")) {
                def.fail(fail);
            } else {
                def.resolve(r);
            }
        }

        Promise<String> filter = def.getPromise().filter(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                if (s == null) {
                    throw new Nasty(null);
                }
                return !s.startsWith("!");
            }
        });

        if (!already) {
            if ("fail".equals(r)) {
                def.fail(fail);
            } else {
                def.resolve(r);
            }
        }
        assertTrue("Filter resolved", filter.isDone());

        @SuppressWarnings({"not thrown", "all"})
        Throwable failure = filter.getFailure();

        if ("fail".equals(r)) {
            assertEquals("Failure matches", fail, filter.getFailure());
        } else if (already && r.contains("!")) {
            assertEquals("Failure matches", fail, filter.getFailure());
        } else if (r == null) {
            assertTrue("Failure instance Nasty", failure instanceof Nasty);
        } else if (r.startsWith("!")) {
            assertTrue("Failure instanceof NoSuchElementException", failure instanceof NoSuchElementException);
        } else {
            assertEquals("Value matches", r, filter.getValue());
        }
    }

    private void testMap(String r) throws Exception {
        Deferred<String> def = new Deferred<String>();

        Promise<String> result = def.getPromise().map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                if (s.contains("!"))
                    throw new Nasty(s);
                return s + s.length();
            }
        });

        Throwable fail = new Throwable("fail");

        if (r.startsWith("fail")) {
            def.fail(fail);
        } else {
            def.resolve(r);
        }
        assertTrue("Map resolved", result.isDone());

        @SuppressWarnings({"not thrown", "all"})
        Throwable failure = result.getFailure();
        if (r.startsWith("fail")) {
            assertEquals("Failure matches", fail, failure);
        } else if (r.contains("!")) {
            assertTrue("Failure instance Nasty", failure instanceof Nasty);
        } else {
            assertEquals("Value matches", r + r.length(), result.getValue());
        }
    }

    private void testFlatMap(String r) throws Exception {
        Deferred<String> def = new Deferred<String>();

        Promise<String> flatMap = def.getPromise().flatMap(new Function<String, Promise<? extends String>>() {
            @Override
            public Promise<String> apply(String s) {
                if (s.contains("!"))
                    throw new Nasty(s);
                return Promises.resolved(s + s.length());
            }
        });

        Throwable fail = new Throwable("fail");

        if ("fail".equals(r)) {
            def.fail(fail);
        } else {
            def.resolve(r);
        }
        assertTrue("FlatMap resolved", flatMap.isDone());

        @SuppressWarnings({"not thrown", "all"})
        Throwable failure = flatMap.getFailure();

        if ("fail".equals(r)) {
            assertEquals("Failure matches", fail, failure);
        } else if (r.contains("!")) {
            assertTrue("Failure instance Nasty", failure instanceof Nasty);
        } else {
            // "the returned Promise will be resolved with the Promise from the specified Function,
            // as applied to the value of this Promise"
            assertEquals("Value matches", r + r.length(), flatMap.getValue());
        }
    }

    private void testRecover(String r) throws Exception {
        Deferred<String> def = new Deferred<String>();

        Promise<String> recover = def.getPromise().recover(new Function<Promise<?>, String>() {
            @Override
            public String apply(Promise<?> promise) {
                try {
                    @SuppressWarnings({"not thrown", "all"})
                    String msg = promise.getFailure().getMessage();
                    if (msg == null) {
                        throw new Nasty(null);
                    }
                    if (msg.equals("null")) {
                        return null;
                    }
                    return "recover:" + msg;
                } catch (InterruptedException e) {
                    return null;
                }
            }
        });

        Throwable fail = new Throwable(r);

        if (null == r || "fail".equals(r) || "null".equals(r)) {
            def.fail(fail);
        } else {
            def.resolve(r);
        }
        assertTrue("Recover resolved", recover.isDone());

        if ("fail".equals(r)) {
            // "recover Promise will be resolved with the recovery value"
            assertEquals("Recovery value matches", "recover:" + r, recover.getValue());
        } else if ("null".equals(r)) {
            // "recover Promise will be failed with the failure of this Promise"
            assertEquals("Recovery failed matches", fail, recover.getFailure());
        } else if (r == null) {
            @SuppressWarnings({"not thrown", "all"})
            Throwable failure = recover.getFailure();
            assertTrue("Failure instance Nasty", failure instanceof Nasty);
        } else {
            // "the returned Promise will be resolved with the value of this Promise"
            assertEquals("Value matches", def.getPromise().getValue(), recover.getValue());
        }
    }

    private void testRecoverWith(String r) throws Exception {
        Deferred<String> def = new Deferred<String>();

        Promise<? extends String> recover = def.getPromise().recoverWith(
                new Function<Promise<?>, Promise<? extends String>>() {
                    @Override
                    public Promise<String> apply(Promise<?> promise) {
                        try {
                            @SuppressWarnings({"not thrown", "all"})
                            String msg = promise.getFailure().getMessage();
                            if (msg == null) {
                                throw new Nasty(null);
                            }
                            if (msg.equals("null")) {
                                return null;
                            }
                            return Promises.resolved("recover:" + msg);

                        } catch (InterruptedException e) {
                            return null;
                        }
                    }
                });

        Throwable fail = new Throwable(r);

        if (null == r || "fail".equals(r) || "null".equals(r)) {
            def.fail(fail);
        } else {
            def.resolve(r);
        }
        assertTrue("RecoverWith resolved", recover.isDone());

        if ("fail".equals(r)) {
            // "recover Promise will be resolved with the recovery value"
            assertEquals("Recovery value matches", "recover:" + r, recover.getValue());
        } else if ("null".equals(r)) {
            // "recover Promise will be failed with the failure of this Promise"
            assertEquals("Recovery failed matches", fail, recover.getFailure());
        } else if (r == null) {
            @SuppressWarnings({"not thrown", "all"})
            Throwable failure = recover.getFailure();
            assertTrue("Failure instance Nasty", failure instanceof Nasty);
        } else {
            // "the returned Promise will be resolved with the value of this Promise"
            assertEquals("Value matches", def.getPromise().getValue(), recover.getValue());
        }

    }

    void testFallback(String r, String f) throws Exception {
        Deferred<String> def = new Deferred<String>();
        Promise<String> promise = def.getPromise();
        Deferred<String> fallback = new Deferred<String>();
        Promise<String> result = promise.fallbackTo(fallback.getPromise());

        Throwable fail = new Throwable(r);
        Throwable fail2 = new Throwable(f + f);

        if ("fail".equals(r)) {
            if ("fail".equals(f)) {
                fallback.fail(fail2);
            } else {
                fallback.resolve(f);
            }
            def.fail(fail);
        } else {
            def.resolve(r);
        }
        assertTrue("result resolved", result.isDone());

        if ("fail".equals(r)) {
            if ("fail".equals(f)) {
                assertEquals("Failure matches", fail, result.getFailure());
            } else {
                assertEquals("Fallback matches", f, result.getValue());
            }
        } else {
            assertEquals("Value matches", r, result.getValue());
        }
    }
}
