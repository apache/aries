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

package org.apache.aries.component.dsl.internal;

import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.Publisher;
import org.apache.aries.component.dsl.test.DSLTest;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.aries.component.dsl.OSGi.just;
import static org.junit.Assert.assertEquals;

/**
 * @author Carlos Sierra Andrés
 */
@Ignore
public class ProbeTests {

    static BundleContext bundleContext =
        FrameworkUtil.getBundle(DSLTest.class).getBundleContext();

    @Test
    public void testTupleTermination() {
        AtomicReference<String> result = new AtomicReference<>("");

        ProbeImpl<String> probeA = new ProbeImpl<>();
        AtomicReference<ProbeImpl<String>> probeBreference = new AtomicReference<>();

        OSGi<String> program =
            probeA.flatMap(a ->
            OSGi.onClose(
                () -> result.accumulateAndGet("Hello", (x, y) -> x.replace(y, ""))).
            flatMap(__ -> {
                ProbeImpl<String> probeB = new ProbeImpl<>();

                probeBreference.set(probeB);

                return probeB.flatMap(b ->
                    OSGi.onClose(
                        () -> result.accumulateAndGet(", World", (x, y) -> x.replace(y, ""))).
                        then(
                            OSGi.just(a + b)));
            }

        ));

        program.run(bundleContext, newValue -> {
            result.set(newValue);

            return OSGi.NOOP;
        });

        Publisher<? super String> opA = probeA.getPublisher();

        Runnable sentA = opA.apply("Hello");

        Publisher<? super String> opB = probeBreference.get().getPublisher();

        sentA.run();

        Runnable sentB = opB.apply(", World");
        sentB.run();

        assertEquals("", result.get());

        program.run(bundleContext, newValue -> {
            result.set(newValue);

            return OSGi.NOOP;
        });

        opA = probeA.getPublisher();
        sentA = opA.apply("Hello");

        opB = probeBreference.get().getPublisher();
        sentB = opB.apply(", World");

        assertEquals("Hello, World", result.get());

        sentA.run();
        sentB.run();

        assertEquals("", result.get());
    }

    @Test
    public void testProbe() {
        AtomicInteger result = new AtomicInteger();

        ProbeImpl<Integer> probeA = new ProbeImpl<>();

        OSGi<Integer> just10 = OSGi.just(10);

        OSGi<Integer> program = probeA.flatMap(a ->
            OSGi.onClose(result::incrementAndGet).then(
                just10.flatMap(b ->
                    OSGi.onClose(result::incrementAndGet).then(
                        OSGi.just(a + b)
                    ))));

        program.run(bundleContext, newValue -> {
            result.set(newValue);

            return OSGi.NOOP;
        });

        assertEquals(0, result.get());

        Publisher<? super Integer> opA = probeA.getPublisher();

        Runnable sentA = opA.apply(5);
        assertEquals(15, result.get());

        sentA.run();
        assertEquals(17, result.get());

        sentA = opA.apply(10);
        assertEquals(20, result.get());

        sentA.run();
        assertEquals(22, result.get());
    }

}
