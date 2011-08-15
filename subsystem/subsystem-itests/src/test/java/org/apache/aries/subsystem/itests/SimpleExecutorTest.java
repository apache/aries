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
package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.Executor;

import org.apache.aries.subsystem.executor.SimpleExecutor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

@RunWith(JUnit4TestRunner.class)
public class SimpleExecutorTest extends SubsystemTest {    
    @Test
    public void test() throws Exception {        
        // obtain executor service
        Executor executor = getOsgiService(Executor.class);
        assertNotNull("Executor should not be null", executor);
        System.out.println("Able to get Executor service");
        
        // check the service is of the right implementation type
        assertEquals(SimpleExecutor.class.getName(), executor.getClass().getName());
        System.out.println("The Executor service is of type: " + SimpleExecutor.class.getName());

    }
}
