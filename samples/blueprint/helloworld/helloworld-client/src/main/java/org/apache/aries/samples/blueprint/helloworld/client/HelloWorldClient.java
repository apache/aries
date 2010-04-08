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
package org.apache.aries.samples.blueprint.helloworld.client;

import org.apache.aries.samples.blueprint.helloworld.api.HelloWorldService;

public class HelloWorldClient {

        HelloWorldService helloWorldService = null;

        public void startUp() {
                System.out.println("========>>>>Client HelloWorld: About to execute a method from the Hello World service");
                helloWorldService.hello();
                System.out.println("========>>>>Client HelloWorld: ... if you didn't just see a Hello World message something went wrong");
        }

        public HelloWorldService getHelloWorldService() {
                return helloWorldService;
        }

        public void setHelloWorldService(HelloWorldService helloWorldService) {
                this.helloWorldService = helloWorldService;

        }

}

