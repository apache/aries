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

import java.util.Arrays;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class OSGiInitialContextFactoryBuilder implements InitialContextFactoryBuilder, InitialContextFactory {

	private BundleContext _context;
	
	public OSGiInitialContextFactoryBuilder(BundleContext context) {	
		_context = context;
	}
	
	public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment) 
	    throws NamingException {
	    return this;
	}
  
	public Context getInitialContext(Hashtable<?, ?> environment) 
	    throws NamingException {
	    
	    // TODO: use caller's bundle context
	    
	    Context initialContext = null;
	    
	    String contextFactoryClass = (String) environment.get(Context.INITIAL_CONTEXT_FACTORY);
	    if (contextFactoryClass == null) {
	        // 1. get ContextFactory using builder
	        initialContext = getInitialContextUsingBuilder(_context, environment);
	        
	        // 2. lookup all ContextFactory services
	        if (initialContext == null) {
	            String filter = "(&(objectClass=javax.naming.spi.InitialContextFactory))";
	            ServiceReference[] references = null;
	            try {
	                references = _context.getAllServiceReferences(InitialContextFactory.class.getName(), filter);
	            } catch (InvalidSyntaxException e) {
	                NamingException ex = new NamingException("Bad filter: " + filter);
	                ex.initCause(e);    
	                throw ex;
	            }
	            if (references != null) {
	                for (int i = 0; i < references.length && initialContext == null; i++) {
	                    ServiceReference reference = references[i];	                    
	                    InitialContextFactory factory = (InitialContextFactory) _context.getService(reference);
	                    try {
	                        initialContext = factory.getInitialContext(environment);
	                    } finally {	                  
	                        _context.ungetService(reference);
	                    }
	                }
	            }
	        }
	        
	        if (initialContext == null) {
	            // TODO: only url based lookups are allowed
	            return new DelegateContext(environment);
	        } else {
	            return new DelegateContext(initialContext);
	        }
	    } else {
	        // 1. lookup ContextFactory using the factory class
	        String filter = "(&(objectClass=javax.naming.spi.InitialContextFactory)(objectClass=" + contextFactoryClass + "))";
	        ServiceReference[] references = null;
	        try {
	            references = _context.getServiceReferences(InitialContextFactory.class.getName(), filter);
	        } catch (InvalidSyntaxException e) {
	            NamingException ex = new NamingException("Bad filter: " + filter);
	            ex.initCause(e);    
	            throw ex;
	        }
	        
	        if (references != null && references.length > 0) {
	            Arrays.sort(references);
	            ServiceReference factoryReference = references[0];
	            InitialContextFactory factory = (InitialContextFactory)_context.getService(factoryReference);
	            try {
	                initialContext = factory.getInitialContext(environment);
	            } finally {
	                _context.ungetService(factoryReference);
	            }
	        }	        
	        
	        // 2. get ContextFactory using builder
	        if (initialContext == null) {
	            initialContext = getInitialContextUsingBuilder(_context, environment);
	        }
	        
	        if (initialContext == null) {
	            throw new NoInitialContextException("We could not find an InitialContextFactory to use");
	        } else {
	            return new DelegateContext(initialContext);
	        }
	    }	   
	}
	
	private static Context getInitialContextUsingBuilder(BundleContext context, Hashtable<?, ?> environment) 
        throws NamingException {
	    InitialContextFactory factory = getInitialContextFactoryBuilder(context, environment);
	    return (factory == null) ? null : factory.getInitialContext(environment);
	}
	
	private static InitialContextFactory getInitialContextFactoryBuilder(BundleContext context, Hashtable<?, ?> environment) 
	    throws NamingException {
	    InitialContextFactory factory = null;
	    try {
	        ServiceReference[] refs = context.getAllServiceReferences(InitialContextFactoryBuilder.class.getName(), null);
	        if (refs != null) {
	            Arrays.sort(refs);
	            for (int i = 0; i < refs.length && factory == null; i++) {
	                ServiceReference ref = refs[i];	                
	                InitialContextFactoryBuilder builder = (InitialContextFactoryBuilder) context.getService(ref);
	                try {
	                    factory = builder.createInitialContextFactory(environment);
	                } finally {	              
	                    context.ungetService(ref);
	                }
	            }
	        }	        
	    } catch (InvalidSyntaxException e) {
	        // ignore - should never happen
	    }
	    return factory;
	}
}
