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

import org.apache.aries.jndi.startup.Activator;
import org.osgi.framework.BundleContext;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import java.util.Hashtable;

public class OSGiInitialContextFactoryBuilder implements InitialContextFactoryBuilder, InitialContextFactory {

    public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment) {
        return this;
    }

    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
//IC see: https://issues.apache.org/jira/browse/ARIES-1068
        Activator.getAugmenterInvoker().augmentEnvironment(environment);
        BundleContext context = Utils.getBundleContext(environment, InitialContext.class);
        if (context == null) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1786
            throw new NoInitialContextException("The calling code's BundleContext could not be determined.");
        }
        Activator.getAugmenterInvoker().unaugmentEnvironment(environment);
        return ContextHelper.getInitialContext(context, environment);
    }
}
