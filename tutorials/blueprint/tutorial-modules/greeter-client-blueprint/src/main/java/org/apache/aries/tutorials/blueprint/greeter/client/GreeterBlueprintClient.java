/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.tutorials.blueprint.greeter.client;

import javax.inject.Inject;

import org.apache.aries.blueprint.annotation.bean.Bean;
import org.apache.aries.blueprint.annotation.config.ConfigProperty;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.apache.aries.tutorials.blueprint.greeter.api.GreeterMessageService;

@Bean(initMethod = "doRequests")
public class GreeterBlueprintClient {
    private final String clientID;
    private GreeterMessageService greeterMessageService;

    public GreeterBlueprintClient(@ConfigProperty("Blueprint Client") String clientID) {
        this.clientID = clientID;
    }

    @Inject
    @Reference(timeout = 10000)
    public void setGreeterMessageService(GreeterMessageService gms) {
        this.greeterMessageService = gms;
    }

    public void doRequests() {
        System.out.println("Invoking injected service...");
        printResult(greeterMessageService.getGreetingMessage());
    }

    private void printResult(String response) {
        System.out.println("**********************************");
        System.out.println("*" + clientID);
        System.out.println("*" + response);
        System.out.println("**********************************");
    }
}
