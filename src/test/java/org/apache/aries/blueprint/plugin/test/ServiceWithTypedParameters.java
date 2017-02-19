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

import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.ops4j.pax.cdi.api.Properties;
import org.ops4j.pax.cdi.api.Property;

import javax.inject.Singleton;

@OsgiServiceProvider
@Properties({
        @Property(name = "test1", value = "test"),
        @Property(name = "test2:Integer", value = "15"),
        @Property(name = "test3:java.lang.Boolean", value = "true"),
        @Property(name = "test4:[]", value = "val1|val2"),
        @Property(name = "test5:Short[]", value = "1|2|3"),
        @Property(name = "test6:java.lang.Double[]", value = "1.5|0.8|-7.1")
})
@Singleton
public class ServiceWithTypedParameters {
}
