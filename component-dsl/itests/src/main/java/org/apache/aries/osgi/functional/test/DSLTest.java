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

package org.apache.aries.osgi.functional.test;

import org.apache.aries.osgi.functional.OSGi;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class DSLTest {

    static BundleContext bundleContext = FrameworkUtil.getBundle(
        DSLTest.class).getBundleContext();

    @Test
    public void testJust() {
        AtomicInteger atomicInteger = new AtomicInteger(0);

        OSGi<Integer> just = OSGi.just(25);

        assertEquals(0, atomicInteger.get());

        just.run(bundleContext, atomicInteger::set);

        assertEquals(25, atomicInteger.get());
    }

}
