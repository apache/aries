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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

public class URLContextProvider extends ContextProvider {
    private final ObjectFactory factory;
    private final Hashtable<?, ?> environment;

    public URLContextProvider(BundleContext bc, ServiceReference<?> reference, ObjectFactory factory, Hashtable<?, ?> environment) {
        super(bc, reference);
        this.factory = factory;
        this.environment = environment;
    }

    @Override
    public Context getContext() throws NamingException {
        try {
            return (Context) factory.getObjectInstance(null, null, null, environment);
        } catch (Exception e) {
            NamingException ne = new NamingException();
            ne.initCause(e);
            throw ne;
        }
    }
}
