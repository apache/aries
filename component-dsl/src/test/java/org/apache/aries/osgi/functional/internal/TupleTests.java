/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.osgi.functional.internal;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Carlos Sierra Andrés
 */
public class TupleTests {

    @Test
    public void testCrossRelatedTuples() {
        AtomicInteger aTerminated = new AtomicInteger(0);
        AtomicInteger bTerminated = new AtomicInteger(0);

        Tuple<String> a = Tuple.create("A");
        a.onTermination(aTerminated::incrementAndGet);

        Tuple<String> b = Tuple.create("B");
        b.onTermination(bTerminated::incrementAndGet);

        a.addRelatedTuple(b);
        b.addRelatedTuple(a);

        a.terminate();

        assertEquals(1, aTerminated.get());
        assertEquals(1, bTerminated.get());
    }

    @Test
    public void testRelatedTuples() {
        AtomicBoolean aTerminated = new AtomicBoolean(false);
        AtomicBoolean bTerminated = new AtomicBoolean(false);

        Tuple<String> a = Tuple.create("A");
        a.onTermination(() -> aTerminated.set(true));

        Tuple<String> b = Tuple.create("B");
        b.onTermination(() -> bTerminated.set(true));

        a.addRelatedTuple(b);

        a.terminate();

        assertTrue(aTerminated.get());
        assertTrue(bTerminated.get());
    }

    @Test
    public void testTerminators() {
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        Tuple<Integer> tuple = Tuple.create(0);

        tuple.onTermination(() -> atomicBoolean.set(true));

        tuple.terminate();

        assertTrue(atomicBoolean.get());
    }
}
