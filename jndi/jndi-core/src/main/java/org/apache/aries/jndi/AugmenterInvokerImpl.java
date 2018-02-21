/*
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
package org.apache.aries.jndi;

import org.apache.aries.jndi.spi.AugmenterInvoker;
import org.apache.aries.jndi.spi.EnvironmentAugmentation;
import org.apache.aries.jndi.spi.EnvironmentUnaugmentation;
import org.apache.aries.jndi.startup.Activator;
import org.osgi.framework.BundleContext;

import java.util.Hashtable;


public class AugmenterInvokerImpl implements AugmenterInvoker {

    private final BundleContext context;

    public AugmenterInvokerImpl(BundleContext context) {
        this.context = context;
    }

    public void augmentEnvironment(Hashtable<?, ?> environment) {
        for (EnvironmentAugmentation svc : Activator.getServices(context, EnvironmentAugmentation.class)) {
            svc.augmentEnvironment(environment);
        }
    }

    public void unaugmentEnvironment(Hashtable<?, ?> environment) {
        for (EnvironmentUnaugmentation svc : Activator.getServices(context, EnvironmentUnaugmentation.class)) {
            svc.unaugmentEnvironment(environment);
        }
    }
}
