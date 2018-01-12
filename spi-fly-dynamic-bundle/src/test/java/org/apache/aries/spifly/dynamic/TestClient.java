/**
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
package org.apache.aries.spifly.dynamic;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.aries.mytest.MySPI;

public class TestClient {
	/**
	 * Load using a constant as parameter.
	 */
    public Set<String> test(String input) {
        Set<String> results = new HashSet<String>();

        ServiceLoader<MySPI> loader = ServiceLoader.load(MySPI.class);
        for (MySPI mySPI : loader) {
            results.add(mySPI.someMethod(input));
        }
        return results;
    }
    
	/**
	 * Load using a variable as parameter.
	 */
    public Set<String> testService(String input, Class<MySPI> service) {
        Set<String> results = new HashSet<String>();

        // Try to irritate TCCLSetterVisitor by forcing an (irrelevant) LDC.
        @SuppressWarnings("unused")
		Class<?> causeLDC = String.class;
        
        ServiceLoader<MySPI> loader = ServiceLoader.load(service);
        for (MySPI mySPI : loader) {
            results.add(mySPI.someMethod(input));
        }
        return results;
    }
}
