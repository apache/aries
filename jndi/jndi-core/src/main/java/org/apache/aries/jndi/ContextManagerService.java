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
import org.osgi.service.jndi.JNDIContextManager;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import java.util.*;

public class ContextManagerService implements JNDIContextManager {

    private Set<Context> contexts = Collections.synchronizedSet(new HashSet<Context>());
    private BundleContext callerContext;

    public ContextManagerService(BundleContext callerContext) {
        this.callerContext = callerContext;
    }

    public void close() {
        synchronized (contexts) {
            for (Context context : contexts) {
                try {
                    context.close();
                } catch (NamingException e) {
                    // ignore
                }
            }
            contexts.clear();
        }
    }

    public Context newInitialContext() throws NamingException {
        return newInitialContext(new Hashtable<Object, Object>());
    }

    public Context newInitialContext(Map<?, ?> environment) throws NamingException {
        return getInitialContext(environment);
    }

    public DirContext newInitialDirContext() throws NamingException {
        return newInitialDirContext(new Hashtable<Object, Object>());
    }

    public DirContext newInitialDirContext(Map<?, ?> environment) throws NamingException {
        return DirContext.class.cast(getInitialContext(environment));
    }

    private Context getInitialContext(Map<?, ?> environment) throws NamingException {
        Hashtable<?, ?> env = Utils.toHashtable(environment);
        Context context = ContextHelper.getInitialContext(callerContext, env);
        contexts.add(context);
        return context;
    }

}
