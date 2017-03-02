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

import org.apache.aries.blueprint.annotation.config.Config;
import org.apache.aries.blueprint.annotation.config.ConfigProperty;
import org.apache.aries.blueprint.annotation.config.DefaultProperty;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

@Config(
        pid = "org.apache.aries.my",
        placeholderPrefix = "$[",
        placeholderSuffix = "]",
        defaults = {
                @DefaultProperty(key = "title", value = "My Title"),
                @DefaultProperty(key = "test2", value = "v2")
        }
)
@Singleton
public class BeanWithConfig {
    @ConfigProperty("$[title]")
    String title;

    @Produces
    @Named("producedWithConfigProperty")
    public MyProducedWithConstructor createBean(@ConfigProperty("1000") long test) {
        return null;
    }
}
