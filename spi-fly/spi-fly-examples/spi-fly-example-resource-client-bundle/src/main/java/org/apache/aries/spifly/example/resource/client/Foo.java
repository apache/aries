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
package org.apache.aries.spifly.example.resource.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

class Foo {
    static void doit() throws Exception {
        System.out.println("*** About to invoke getThreadContextClassLoader().getResource()");

        URL r = Thread.currentThread().getContextClassLoader().getResource("/org/apache/aries/spifly/test/blah.txt");
        System.out.println("*** Found resource: " + r);
        System.out.println("*** First line of content: " + new BufferedReader(new InputStreamReader(r.openStream())).readLine());
    }
}
