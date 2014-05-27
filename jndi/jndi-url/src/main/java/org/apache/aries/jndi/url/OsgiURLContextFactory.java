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
package org.apache.aries.jndi.url;

import java.util.Hashtable;

import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.BundleContext;

/**
 * A factory for the aries JNDI context
 */
public class OsgiURLContextFactory implements ObjectFactory {
    
    private BundleContext callerContext;
    
    public OsgiURLContextFactory(BundleContext callerContext) {
        this.callerContext = callerContext;
    }
    
    public Object getObjectInstance(Object obj,
                                    Name name,
                                    Context nameCtx,
                                    Hashtable<?, ?> environment) throws Exception {
        if (obj == null) {
            return new ServiceRegistryContext(callerContext, environment);
        } else if (obj instanceof String) {
        	return findAny(environment, (String)obj);
        } else if (obj instanceof String[]) {
            return findAny(environment, (String[]) obj);
        } else {
            return null;
        }
    }

    /**
     * Try each URL until either lookup succeeds or they all fail
     */
	private Object findAny(Hashtable<?, ?> environment, String ... urls)
			throws ConfigurationException, NamingException {
		if (urls.length == 0) {
		    throw new ConfigurationException("0");
		}
		Context context = new ServiceRegistryContext(callerContext, environment);
		try {
		    NamingException ne = null;
		    for (int i = 0; i < urls.length; i++) {
		        try {
		            return context.lookup(urls[i]);
		        } catch (NamingException e) {
		            ne = e;
		        }
		    }
		    throw ne;
		} finally {
		    context.close();
		}
	}

}
