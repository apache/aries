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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jndi;

import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

public class DelegateContext implements Context
{
  private Hashtable<Object, Object> env = new Hashtable<Object, Object>();
  private Context defaultContext;
  private static ConcurrentMap<String, Context> urlContexts = new ConcurrentHashMap<String, Context>();

  public DelegateContext(Hashtable<?, ?> theEnv)
  {
    env.putAll(theEnv);
  }
  
  public DelegateContext(Context ctx) throws NamingException
	{

  		defaultContext = ctx;
			env.putAll(ctx.getEnvironment());

	}

	public Object addToEnvironment(String propName, Object propVal) throws NamingException
  {
    Context ctx = getDefaultContext();
    
    if (ctx != null) ctx.addToEnvironment(propName, propVal);
    
    return env.put(propName, propVal);
  }

  public void bind(Name name, Object obj) throws NamingException
  {
    findContext(name).bind(name, obj);
  }

  public void bind(String name, Object obj) throws NamingException
  {
    findContext(name).bind(name, obj);
  }

  public void close() throws NamingException
  {
    if (defaultContext != null) defaultContext.close();
    env.clear();
  }

  public Name composeName(Name name, Name prefix) throws NamingException
  {
    return findContext(name).composeName(name, prefix);
  }

  public String composeName(String name, String prefix) throws NamingException
  {
    return findContext(name).composeName(name, prefix);
  }

  public Context createSubcontext(Name name) throws NamingException
  {
    return findContext(name).createSubcontext(name);
  }

  public Context createSubcontext(String name) throws NamingException
  {
    return findContext(name).createSubcontext(name);
  }

  public void destroySubcontext(Name name) throws NamingException
  {
    findContext(name).destroySubcontext(name);
  }

  public void destroySubcontext(String name) throws NamingException
  {
    findContext(name).destroySubcontext(name);
  }

  public Hashtable<?, ?> getEnvironment() throws NamingException
  {
    Hashtable<Object, Object> theEnv = new Hashtable<Object, Object>();
    theEnv.putAll(env);
    return theEnv;
  }

  public String getNameInNamespace() throws NamingException
  {
    return getDefaultContext().getNameInNamespace();
  }

  public NameParser getNameParser(Name name) throws NamingException
  {
    return findContext(name).getNameParser(name);
  }

  public NameParser getNameParser(String name) throws NamingException
  {
    return findContext(name).getNameParser(name);
  }

  public NamingEnumeration<NameClassPair> list(Name name) throws NamingException
  {
    return findContext(name).list(name);
  }

  public NamingEnumeration<NameClassPair> list(String name) throws NamingException
  {
    return findContext(name).list(name);
  }

  public NamingEnumeration<Binding> listBindings(Name name) throws NamingException
  {
    return findContext(name).listBindings(name);
  }

  public NamingEnumeration<Binding> listBindings(String name) throws NamingException
  {
    return findContext(name).listBindings(name);
  }

  public Object lookup(Name name) throws NamingException
  {
    return findContext(name).lookup(name);
  }

  public Object lookup(String name) throws NamingException
  {
    return findContext(name).lookup(name);
  }

  public Object lookupLink(Name name) throws NamingException
  {
    return findContext(name).lookupLink(name);
  }

  public Object lookupLink(String name) throws NamingException
  {
    return findContext(name).lookupLink(name);
  }

  public void rebind(Name name, Object obj) throws NamingException
  {
    findContext(name).rebind(name, obj);
  }

  public void rebind(String name, Object obj) throws NamingException
  {
    findContext(name).rebind(name, obj);
  }

  public Object removeFromEnvironment(String propName) throws NamingException
  {
    Context ctx = getDefaultContext();
    
    if (ctx != null) ctx.removeFromEnvironment(propName);
    
    return env.remove(propName);
  }

  public void rename(Name oldName, Name newName) throws NamingException
  {
    findContext(oldName).rename(oldName, newName);
  }

  public void rename(String oldName, String newName) throws NamingException
  {
    findContext(oldName).rename(oldName, newName);
  }

  public void unbind(Name name) throws NamingException
  {
    findContext(name).unbind(name);
  }

  public void unbind(String name) throws NamingException
  {
    findContext(name).unbind(name);
  }

  protected Context findContext(Name name) throws NamingException
  {
    return findContext(name.toString());
  }

  
  protected Context findContext(String name) throws NamingException
  {
  	Context toReturn = null;
  	
  	if (name.contains(":")) {
      toReturn = getURLContext(name);
    } else {
      toReturn =  getDefaultContext();
    }
    
  	if (toReturn != null)
  	{
  		String packages = System.getProperty(Context.URL_PKG_PREFIXES, null);
  		
  		if (packages != null)
  		{
  			toReturn.addToEnvironment(Context.URL_PKG_PREFIXES, packages);	
  		}
  	}
  	
    return toReturn;
  }

  private Context getDefaultContext() throws NamingException
  {
    if (defaultContext == null) {
      defaultContext = ContextHelper.createContext(env);
    }
    return defaultContext;
  }

  private Context getURLContext(String name) throws NamingException
  {
    Context ctx = null;
    
    int index = name.indexOf(':');
    
    if (index != -1) {
      String scheme = name.substring(0, index);
      
      ctx = ContextHelper.createURLContext(scheme, env);
    }
    
    if (ctx == null) {
      ctx = getDefaultContext();
    }
    
    return ctx;
  }
  
  /**
   * This method allows the caller to set the default context if one is to hand.
   * Normally the context would be lazily constructed upon first use, but this
   * if one already exists when this is created it can be pushed in.
   * 
   * @param ctx the context to use.
   */
  public void setDefaultContext(Context ctx)
  {
    defaultContext = ctx;
  }
  
  /**
   * This method allows URL contexts to be pushed in. We should probably be 
   * pushing in references to the ObjectFactory for the url Object Factory, but
   * for now lets just push the context itself in.
   * 
   * @param url
   * @param ctx
   */
  public void setURLContext(String url, Context ctx)
  {
    urlContexts.put(url, ctx);
  }
}