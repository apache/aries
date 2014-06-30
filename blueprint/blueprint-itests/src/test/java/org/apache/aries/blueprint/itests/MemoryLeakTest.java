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
package org.apache.aries.blueprint.itests;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.provision;

import java.io.InputStream;

import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

public class MemoryLeakTest extends AbstractBlueprintIntegrationTest {

    @Test
    public void testScheduledExecMemoryLeak() throws Exception {
        Bundle b = context().getBundleByName("test.bundle");
        
        long startFreeMemory = getFreeMemory();
        
        // 3000 iterations on a Mac 1.6 JVM leaks 30+ mb, 2000 leaks a bit more than 20, 
        // 10000 iterations would be close to OutOfMemory however by that stage the test runs very slowly
        for (int i=0; i<1500; i++) {
            b.start();
            // give the container some time to operate, otherwise it probably won't even get to create a future
            Thread.sleep(10);
            b.stop();
        }
        
        long endFreeMemory = getFreeMemory();
        
        long lossage = startFreeMemory - endFreeMemory;
        System.out.println("We lost: " + lossage);
        // increase the lossage value as it may depends of the JDK
        assertTrue("We lost: " + lossage, lossage < 77000000);
    }

    private long getFreeMemory() {
        for (int i=0; i<16; i++) System.gc();
        return Runtime.getRuntime().freeMemory();
    }

    private InputStream memoryLeakTestBundle() {
        return TinyBundles.bundle()
        .add("OSGI-INF/blueprint/blueprint.xml", this.getClass().getResource("/bp2.xml"))
        .set(Constants.BUNDLE_SYMBOLICNAME, "test.bundle")
        .build();
    }

    @org.ops4j.pax.exam.Configuration
    public Option[] configuration() {
        return new Option[] {
            baseOptions(),
            Helper.blueprintBundles(),
            provision(memoryLeakTestBundle())
        };
    }

}
