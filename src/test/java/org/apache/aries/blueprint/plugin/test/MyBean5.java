/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin.test;

import org.apache.aries.blueprint.plugin.AnnotatedService;
import org.apache.aries.blueprint.plugin.test.interfaces.ServiceA;
import org.apache.aries.blueprint.plugin.test.interfaces.ServiceB;
import org.apache.aries.blueprint.plugin.test.interfaces.ServiceC;
import org.ops4j.pax.cdi.api.OsgiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@DependsOn("myBean6")
public class MyBean5 {

    @Inject
    public MyBean5(@Named("my2") ServiceA serviceA1,
                   ServiceA serviceA2,
                   ServiceB serviceB,
                   @Value("100") int bla,
                   @OsgiService(filter = "myRef") @Named("ser1") ServiceC myReference,
                   @OsgiService(filter = "(mode=123)") @Named("ser2") ServiceC myReference2,
                   @AnnotatedService ServiceA serviceAAnnotated,
                   @Named("produced2") MyProduced myProduced) {
    }

    public MyBean5() {
    }
}
