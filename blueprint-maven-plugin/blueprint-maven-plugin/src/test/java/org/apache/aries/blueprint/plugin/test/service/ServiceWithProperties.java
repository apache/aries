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
package org.apache.aries.blueprint.plugin.test.service;

import org.apache.aries.blueprint.annotation.service.Service;
import org.apache.aries.blueprint.annotation.service.ServiceProperty;

import javax.inject.Singleton;

@Service(properties = {
        @ServiceProperty(name = "oneValue", values = "test"),
        @ServiceProperty(name = "intValue", values = "1", type = Integer.class),
        @ServiceProperty(name = "longArray", values = {"1", "2", "3"}, type = Long.class),
        @ServiceProperty(name = "emptyArray", values = {}),
        @ServiceProperty(name = "stringArray", values = {"a", "b"}),
})
@Singleton
public class ServiceWithProperties implements Service1, Service2 {
}
