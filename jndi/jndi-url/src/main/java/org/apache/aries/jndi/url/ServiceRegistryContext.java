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
import java.util.Map;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.aries.jndi.services.ServiceHelper;

/**
 * A JNDI context for looking stuff up from the service registry.
 */
public class ServiceRegistryContext extends AbstractServiceRegistryContext implements Context
{
  /** The parent name, if one is provided, of this context */
  private OsgiName parentName;

  /**
   * Why Mr Java this class does indeed take a fine copy of the provided 
   * environment. One might imagine that it is worried that the provider is
   * not to be trusted.
   * 
   * @param environment
   */
  public ServiceRegistryContext(Hashtable<?, ?> environment)
  {
    super(environment);
  }

  public ServiceRegistryContext(OsgiName validName, Map<String, Object> env) 
  {
    super(env);
    parentName = validName;
  }

  public NamingEnumeration<NameClassPair> list(final Name name) throws NamingException
  {
    return new ServiceRegistryListContext(env, convert(name)).list("");
  }

  public NamingEnumeration<NameClassPair> list(String name) throws NamingException
  {
    return list(parse(name));
  }

  public NamingEnumeration<Binding> listBindings(final Name name) throws NamingException
  {
    return new ServiceRegistryListContext(env, convert(name)).listBindings("");
  }

  public NamingEnumeration<Binding> listBindings(String name) throws NamingException
  {
    return listBindings(parse(name));
  }

  public Object lookup(Name name) throws NamingException
  {
    Object result;
    
    OsgiName validName = convert(name);
    
    String pathFragment = validName.getSchemePath();
    String schemeName = validName.getScheme();
    
    if (validName.hasInterface()) {
      if (OsgiName.FRAMEWORK_PATH.equals(pathFragment) && "bundleContext".equals(validName.getServiceName())) {
        result = ServiceHelper.getBundleContext(env);
      } else if ((OsgiName.SERVICE_PATH.equals(pathFragment) && OsgiName.OSGI_SCHEME.equals(schemeName)) ||
                 (OsgiName.SERVICES_PATH.equals(pathFragment) && OsgiName.ARIES_SCHEME.equals(schemeName))) {
        result = ServiceHelper.getService(validName, null, true, env);
      } else if (OsgiName.SERVICE_LIST_PATH.equals(pathFragment)) {
        result = new ServiceRegistryListContext(env, validName);
      } else {
        result = null;
      }
    } else {
      result = new ServiceRegistryContext(validName, env);
    }
    
    if (result == null) {
      throw new NameNotFoundException(name.toString());
    }
    
    return result;
  }

  private OsgiName convert(Name name) throws NamingException
  {
    OsgiName result;
    
    if (name instanceof OsgiName) {
      result = (OsgiName) name;
    } else {
      if (parentName != null) {
        result = new OsgiName(parentName.toString() + "/" + name.toString());
      } else {
        result = (OsgiName)parser.parse(name.toString());
      }
    }
    
    return result;
  }
  
  private Name parse(String name) throws NamingException
  {
    if (parentName != null) {
        name = parentName.toString() + "/" + name;
    }
    
    return parser.parse(name);
  }
  
  public Object lookup(String name) throws NamingException
  {
    return lookup(parse(name));
  }
}
