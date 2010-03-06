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

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

import org.apache.aries.jndi.services.ServiceHelper;

/**
 * A JNDI context for looking stuff up from the service registry.
 */
public class ServiceRegistryContext implements Context
{
  private static final String ARIES_SERVICES = "aries:services/";
  /** The name parser for the service registry name space */
  private NameParser parser = new OsgiNameParser();
  /** The environment for this context */
  private Map<String, Object> env;
  
  /**
   * Why Mr Java this class does indeed take a fine copy of the provided 
   * environment. One might imagine that it is worried that the provider is
   * not to be trusted.
   * 
   * @param environment
   */
  @SuppressWarnings("unchecked")
  public ServiceRegistryContext(Hashtable<?, ?> environment)
  {
    env = new HashMap<String, Object>();
    env.putAll((Map<? extends String, ? extends Object>) environment);
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

  public NamingEnumeration<NameClassPair> list(final Name name) throws NamingException
  {
    return new ServiceRegistryListContext(env, convert(name)).list("");
  }

  public NamingEnumeration<NameClassPair> list(String name) throws NamingException
  {
    return list(parser.parse(name));
  }

  public NamingEnumeration<Binding> listBindings(final Name name) throws NamingException
  {
    return new ServiceRegistryListContext(env, convert(name)).listBindings("");
  }

  public NamingEnumeration<Binding> listBindings(String name) throws NamingException
  {
    return listBindings(parser.parse(name));
  }

  public Object lookup(Name name) throws NamingException
  {
    Object result;
    
    OsgiName validName = convert(name);
    
    String pathFragment = validName.getSchemePath();
    String serviceName = validName.getServiceName();
    String schemeName = validName.getScheme();
    
    if (OsgiName.FRAMEWORK_PATH.equals(pathFragment) && "bundleContext".equals(validName.getServiceName())) {
      result = ServiceHelper.getBundleContext(env);
    } else if ((OsgiName.SERVICE_PATH.equals(pathFragment) && OsgiName.OSGI_SCHEME.equals(schemeName)) ||
               (OsgiName.SERVICES_PATH.equals(pathFragment) && OsgiName.ARIES_SCHEME.equals(schemeName))) {
      result = ServiceHelper.getService(validName.getInterface(), validName.getFilter(), serviceName, null, true, env);
    } else if (OsgiName.SERVICE_LIST_PATH.equals(pathFragment)) {
      result = new ServiceRegistryListContext(env, validName);
    } else {
      result = null;
    }
    
    if (result == null) {
      throw new NameNotFoundException(name.toString());
    }
    
    return result;
  }

  private OsgiName convert(Name name) throws InvalidNameException
  {
    OsgiName result;
    
    if (name instanceof OsgiName) {
      result = (OsgiName) name;
    } else {
      result = new OsgiName(name);
    }
    
    return result;
  }

  public Object lookup(String name) throws NamingException
  {
    return lookup(parser.parse(name));
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