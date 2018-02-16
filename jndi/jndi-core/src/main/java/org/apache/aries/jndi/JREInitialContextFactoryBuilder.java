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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;

public class JREInitialContextFactoryBuilder implements InitialContextFactoryBuilder {

    public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment)
        throws NamingException {
        final String contextFactoryClass = (String) environment.get(Context.INITIAL_CONTEXT_FACTORY);
        if (contextFactoryClass != null) {
            return AccessController.doPrivileged(new PrivilegedAction<InitialContextFactory>() {
                public InitialContextFactory run() {
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends InitialContextFactory> clazz = (Class<? extends InitialContextFactory>) ClassLoader.
                            getSystemClassLoader().loadClass(contextFactoryClass);
                        return InitialContextFactory.class.cast(clazz.newInstance());
                    } catch (Exception e) {
                        return null;
                    }
                }
            });
        }
        return null;
    }   
}
