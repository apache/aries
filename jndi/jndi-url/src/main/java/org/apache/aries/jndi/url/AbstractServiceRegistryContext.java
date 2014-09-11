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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jndi.JNDIConstants;

import org.apache.aries.jndi.spi.AugmenterInvoker;

public abstract class AbstractServiceRegistryContext implements Context
{

  protected BundleContext callerContext;
  /** The environment for this context */
  protected Map<String, Object> env;
  /** The name parser for the service registry name space */
  protected NameParser parser = new OsgiNameParser();
  private static final String ARIES_SERVICES = "aries:services/";

  private static AugmenterInvoker augmenterInvoker = null;

  @SuppressWarnings("unchecked")
  public AbstractServiceRegistryContext(BundleContext callerContext, Hashtable<?, ?> environment)
  {
    env = new HashMap<String, Object>();
    env.putAll((Map<? extends String, ? extends Object>) environment);
    // ARIES-397:, If the caller has provided a BundleContext
    // in the hashtable, use this in preference to callerContext
    if (augmenterInvoker == null && callerContext != null) {
      ServiceReference augmenterSR = callerContext.getServiceReference(AugmenterInvoker.class.getName());
      if (augmenterSR != null) augmenterInvoker = (AugmenterInvoker) callerContext.getService(augmenterSR);
    }
    if (augmenterInvoker != null) augmenterInvoker.augmentEnvironment(environment);
    BundleContext bc = (BundleContext) env.get(JNDIConstants.BUNDLE_CONTEXT);
    if (augmenterInvoker != null) augmenterInvoker.unaugmentEnvironment(environment);
    if (bc != null) { 
      this.callerContext = bc;
    } else { 
      this.callerContext = callerContext;    
    }
  }

  @SuppressWarnings("unchecked")
  public AbstractServiceRegistryContext(BundleContext callerContext, Map<?, ?> environment)
  {
    env = new HashMap<String, Object>();
    env.putAll((Map<? extends String, ? extends Object>) environment);
    Hashtable<String, Object> environmentHT = new Hashtable<String,Object>();
    environmentHT.putAll(env);
    // ARIES-397: If the caller has provided a BundleContext
    // in the hashtable, use this in preference to callerContext
    if (augmenterInvoker == null && callerContext != null) {
      ServiceReference augmenterSR = callerContext.getServiceReference(AugmenterInvoker.class.getName());
      if (augmenterSR != null) augmenterInvoker = (AugmenterInvoker) callerContext.getService(augmenterSR);
    }
    if (augmenterInvoker != null) augmenterInvoker.augmentEnvironment(environmentHT); 
    BundleContext bc = (BundleContext) env.get(JNDIConstants.BUNDLE_CONTEXT);
    if (augmenterInvoker != null) augmenterInvoker.unaugmentEnvironment(environmentHT);
    if (bc != null) { 
      this.callerContext = bc;
    } else { 
      this.callerContext = callerContext;    
    }
  }

  public Object addToEnvironment(String propName, Object propVal) throws NamingException
  {
    return env.put(propName, propVal);
  }

  public void bind(Name name, Object obj) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public void bind(String name, Object obj) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public void close() throws NamingException
  {
    env = null;
    parser = null;
  }

  public Name composeName(Name name, Name prefix) throws NamingException
  {
    String result = prefix + "/" + name;
  
    String ns = ARIES_SERVICES;
    
    if (result.startsWith(ns)) {
      ns = "";
    }
    
    return parser.parse(ns + result);
  }

  public String composeName(String name, String prefix) throws NamingException
  {
    String result = prefix + "/" + name;
  
    String ns = ARIES_SERVICES;
    
    if (result.startsWith(ns)) {
      ns = "";
    }
    
    parser.parse(ns + result);
    
    return result;
  }

  public Context createSubcontext(Name name) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public Context createSubcontext(String name) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public void destroySubcontext(Name name) throws NamingException
  {
    //No-op we don't support sub-contexts in our context   
  }

  public void destroySubcontext(String name) throws NamingException
  {
    //No-op we don't support sub-contexts in our context
    
  }

  public Hashtable<?, ?> getEnvironment() throws NamingException
  {
    Hashtable<Object, Object> environment = new Hashtable<Object, Object>();
    environment.putAll(env);
    return environment;
  }

  public String getNameInNamespace() throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public NameParser getNameParser(Name name) throws NamingException
  {
    return parser;
  }

  public NameParser getNameParser(String name) throws NamingException
  {
    return parser;
  }

  public Object lookupLink(Name name) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public Object lookupLink(String name) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public void rebind(Name name, Object obj) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public void rebind(String name, Object obj) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public Object removeFromEnvironment(String propName) throws NamingException
  {
    return env.remove(propName);
  }

  public void rename(Name oldName, Name newName) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public void rename(String oldName, String newName) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public void unbind(Name name) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public void unbind(String name) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

}
