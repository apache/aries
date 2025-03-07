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
package org.apache.aries.blueprint.plugin.test.osgiserviceprovider;

import org.apache.aries.blueprint.plugin.test.MyProduced;
import org.apache.aries.blueprint.plugin.test.interfaces.ServiceA;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.ops4j.pax.cdi.api.Properties;
import org.ops4j.pax.cdi.api.Property;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class MyFactoryBeanAsService {
    
    @Produces
    @Named("producedForService")
    @OsgiServiceProvider
    public MyProduced createBeanWithServiceExpose1() {
        return null;
    }

    @Produces
    @Named("producedForServiceWithOneInterface")
    @OsgiServiceProvider(classes = MyProduced.class)
    public MyProduced createBeanWithServiceExpose2() {
        return null;
    }

    @Produces
    @Named("producedForServiceWithTwoInterfaces")
    @OsgiServiceProvider(classes = {MyProduced.class, ServiceA.class})
    public MyProduced createBeanWithServiceExpose3() {
        return null;
    }

    @Produces
    @Named("producedForServiceWithProperties")
    @OsgiServiceProvider
    @Properties({
        @Property(name = "n1", value = "v1"),
        @Property(name = "n2", value = "v2"),
        @Property(name = "service.ranking", value = "100")
    })
    public MyProduced createBeanWithServiceExpose4() {
        return null;
    }
}
