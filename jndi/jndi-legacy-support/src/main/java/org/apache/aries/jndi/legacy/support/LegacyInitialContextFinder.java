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
package org.apache.aries.jndi.legacy.support;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;

/**
 * Some OSGi based server runtimes, such as jetty OSGi and virgo rely on the thread context classloader
 * to make their JNDI InitialContextFactory's available in OSGi, rather than relying on the OSGi JNDI spec.
 * This is a little bizare, but perhaps is just a point in time statement. In any case to support them
 * using Aries JNDI we have this ICFB which uses the Thread context classloader. We don't ship it in the
 * jndi uber bundle because it is only for these runtimes which haven't caught up with the latest OSGi specs.
 * Normally we want to enourage the use of the OSGi spec, but this is a backstop for those wanting to use
 * Aries JNDI and one of these runtimes.
 *
 */
public class LegacyInitialContextFinder implements InitialContextFactoryBuilder {

    public InitialContextFactory createInitialContextFactory(
            Hashtable<?, ?> environment) throws NamingException {
        String icf = (String) environment.get(Context.INITIAL_CONTEXT_FACTORY);
        if (icf != null) {
            ClassLoader cl;
            if (System.getSecurityManager() != null) {
                cl = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) Thread.currentThread()::getContextClassLoader);
            } else {
                cl = Thread.currentThread().getContextClassLoader();
            }

            try {
                Class<?> icfClass = Class.forName(icf, false, cl);
                if (InitialContextFactory.class.isAssignableFrom(icfClass)) {
                    return (InitialContextFactory) icfClass.newInstance();
                }
            } catch (ClassNotFoundException e) {
                // If the ICF doesn't exist this is expected. Should return null so the next builder is queried.
            } catch (InstantiationException e) {
                // If the ICF couldn't be created just ignore and return null.
            } catch (IllegalAccessException e) {
                // If the default constructor is private, just ignore and return null.
            }
        }

        return null;
    }

}
