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

import org.apache.aries.osgi.functional.OSGi;
import org.apache.aries.osgi.functional.SentEvent;
import org.apache.aries.osgi.functional.test.DSLTest;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.apache.aries.osgi.functional.OSGi.just;
import static org.apache.aries.osgi.functional.OSGi.onClose;
import static org.junit.Assert.assertEquals;

/**
 * @author Carlos Sierra Andrés
 */
public class ProbeTests {

    static BundleContext bundleContext =
        FrameworkUtil.getBundle(DSLTest.class).getBundleContext();

    @Test
    public void testProbe() {
        AtomicInteger result = new AtomicInteger();

        ProbeImpl<Integer> probeA = new ProbeImpl<>();

        Function<Integer, SentEvent<Integer>> opA = probeA.getOperation();
        OSGi<Integer> just10 = just(10);

        OSGi<Integer> program = probeA.flatMap(a ->
            onClose(result::incrementAndGet).then(
                just10.flatMap(b ->
                    onClose(result::incrementAndGet).then(
                        just(a + b)
                    ))));

        program.run(bundleContext, result::set);
        assertEquals(0, result.get());

        SentEvent<Integer> sentA = opA.apply(5);
        assertEquals(15, result.get());

        sentA.terminate();
        assertEquals(17, result.get());

        sentA.terminate();
        assertEquals(17, result.get());

        sentA = opA.apply(10);
        assertEquals(20, result.get());

        sentA.terminate();
        assertEquals(22, result.get());
    }

}
