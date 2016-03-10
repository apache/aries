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
package org.apache.aries.blueprint.plugin.test;

import org.apache.aries.blueprint.plugin.AnnotatedService;
import org.ops4j.pax.cdi.api.OsgiService;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class MyBean5 {

    ServiceA serviceA1;

    ServiceA serviceA2;

    ServiceB serviceB;

    int bla;

    ServiceC myReference;
    ServiceC myReference2;

    ServiceA serviceAAnnotated;

    @Inject
    public MyBean5(@Named("my2") ServiceA serviceA1,
                   ServiceA serviceA2,
                   ServiceB serviceB,
                   @Value("100") int bla,
                   @OsgiService(filter = "myRef") @Named("ser1") ServiceC myReference,
                   @OsgiService(filter = "(mode=123)") @Named("ser2") ServiceC myReference2,
                   @AnnotatedService ServiceA serviceAAnnotated) {
        this.serviceA1 = serviceA1;
        this.serviceA2 = serviceA2;
        this.serviceB = serviceB;
        this.bla = bla;
        this.myReference = myReference;
        this.myReference2 = myReference2;
        this.serviceAAnnotated = serviceAAnnotated;
    }
}
