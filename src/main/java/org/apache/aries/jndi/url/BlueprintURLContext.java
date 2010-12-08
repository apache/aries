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

package org.apache.aries.jndi.url;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.ServiceUnavailableException;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.NoSuchComponentException;

public class BlueprintURLContext implements Context {
  private static final String BLUEPRINT_NAMESPACE = "blueprint:comp/";
  private Bundle _callersBundle;
  private Map<String, Object> _env;
  private NameParser _parser = new BlueprintNameParser();
  private BlueprintContainer _blueprintContainer;
  private BlueprintName _parentName;
  
  // listBindings wants a NamingEnumeration<Binding>
  // list wants a NamingEnumeration<NameClassPair>
  // Both are very similar. As per ServiceRegistryListContext we delegate to a closure to do the final processing
  
  private interface ComponentProcessor<T> { 
    T get (Binding b);
  }

  private static class BlueprintComponentNamingEnumeration<T> implements NamingEnumeration<T>
  {
    private Binding[] blueprintIdToComponentBindings;
    private int position = 0;
    private ComponentProcessor<T> processor;
    
    public BlueprintComponentNamingEnumeration (BlueprintContainer bpc, ComponentProcessor<T> p) 
    { 
      @SuppressWarnings("unchecked")
      Set<String> componentIds = bpc.getComponentIds();
      blueprintIdToComponentBindings = new Binding[componentIds.size()];
      Iterator<String> idIterator= componentIds.iterator();
      for (int i=0; i < blueprintIdToComponentBindings.length; i++) { 
        String id = idIterator.next();
        Object o = bpc.getComponentInstance(id);
        blueprintIdToComponentBindings[i] = new Binding (id, o);
      }
      processor = p;
    }
    

    @Override
    public boolean hasMoreElements()
    {
      return position < blueprintIdToComponentBindings.length;
    }

    @Override
    public T nextElement()
    {
      if (!hasMoreElements()) throw new NoSuchElementException();
      Binding bindingToProcess = blueprintIdToComponentBindings[position];
      position++;
      T result = processor.get(bindingToProcess);
      return result;
    }

    @Override
    public T next() throws NamingException
    {
      return nextElement();
    }

    @Override
    public boolean hasMore() throws NamingException
    {
      return hasMoreElements();
    }

    @Override
    public void close() throws NamingException
    {
      // Nothing to do
    }
    
  }
  
  @SuppressWarnings("unchecked")
  public BlueprintURLContext(Bundle callersBundle, Hashtable<?, ?> env) throws ServiceUnavailableException 
  {
    _callersBundle = callersBundle;
    _env = new HashMap<String, Object>();
    _env.putAll((Map<? extends String, ? extends Object>) env);
    _parentName = null;
    ServiceReference bpContainerRef = getBlueprintContainerRef(_callersBundle);
    if (bpContainerRef != null) { 
      _blueprintContainer = (BlueprintContainer) _callersBundle.getBundleContext().getService(bpContainerRef);
    } else { 
      throw new ServiceUnavailableException ();
    }
  }
  
  public BlueprintURLContext (Bundle callersBundle, BlueprintName parentName, Map<String, Object> env, 
      BlueprintContainer bpc) { 
    _callersBundle = callersBundle;
    _parentName = parentName;
    _env = env;
    _blueprintContainer = bpc;
    
  }

  @Override
  public Object addToEnvironment(String propName, Object propVal)
      throws NamingException
  {
    return _env.put(propName, propVal);
  }

  @Override
  public void bind(Name n, Object o) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  @Override
  public void bind(String s, Object o) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  @Override
  public void close() throws NamingException
  {
    _env = null;
  }

  @Override
  public Name composeName(Name name, Name prefix) throws NamingException
  {
    String result = prefix + "/" + name;
    String ns = BLUEPRINT_NAMESPACE;
    if (result.startsWith(ns)) {
      ns = "";
    }
    return _parser.parse(ns + result);
  }

  @Override
  public String composeName(String name, String prefix) throws NamingException
  {
    String result = prefix + "/" + name;
    String ns = BLUEPRINT_NAMESPACE;
    if (result.startsWith(ns)) {
      ns = "";
    }
    _parser.parse(ns + result);
    return result;
  }

  @Override
  public Context createSubcontext(Name n) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  @Override
  public Context createSubcontext(String s) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  @Override
  public void destroySubcontext(Name n) throws NamingException
  {
    // No-op we don't support sub-contexts in our context
  }

  @Override
  public void destroySubcontext(String s) throws NamingException
  {
    // No-op we don't support sub-contexts in our context
  }

  @Override
  public Hashtable<?, ?> getEnvironment() throws NamingException
  {
    Hashtable<Object, Object> environment = new Hashtable<Object, Object>();
    environment.putAll(_env);
    return environment;
  }

  @Override
  public String getNameInNamespace() throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  @Override
  public NameParser getNameParser(Name n) throws NamingException
  {
    return _parser;
  }

  @Override
  public NameParser getNameParser(String s) throws NamingException
  {
    return _parser;
  }

  @Override
  public NamingEnumeration<NameClassPair> list(Name name) throws NamingException
  {
    return list(name.toString());
  }

  @Override
  public NamingEnumeration<NameClassPair> list(String s) throws NamingException
  {
    NamingEnumeration<NameClassPair> result = new BlueprintComponentNamingEnumeration<NameClassPair>(_blueprintContainer, new ComponentProcessor<NameClassPair>() {
      @Override
      public NameClassPair get(Binding b)
      {
        NameClassPair result = new NameClassPair (b.getName(), b.getClassName());
        return result;
      } 
    });
    return result;
  }

  @Override
  public NamingEnumeration<Binding> listBindings(Name name) throws NamingException
  {
    return listBindings(name.toString());
  }

  @Override
  public NamingEnumeration<Binding> listBindings(String name)
      throws NamingException
  {
    NamingEnumeration<Binding> result = new BlueprintComponentNamingEnumeration<Binding>(_blueprintContainer, new ComponentProcessor<Binding>() {
      @Override
      public Binding get(Binding b)
      {
        return b;
      } 
    });
    return result;
  }

  @Override
  public Object lookup(Name name) throws NamingException
  {
    BlueprintName bpName;
    if (name instanceof BlueprintName) { 
      bpName = (BlueprintName) name; 
    } else if (_parentName != null) { 
      bpName = new BlueprintName (_parentName.toString() + "/" + name.toString());
    } else { 
      bpName = (BlueprintName) _parser.parse(name.toString());
    }

    Object result;
    if (bpName.hasComponent()) { 
      String componentId = bpName.getComponentId();
      ServiceReference bpContainerRef = getBlueprintContainerRef(_callersBundle);
      BlueprintContainer bpc; 
      if (bpContainerRef != null) { 
        bpc = (BlueprintContainer) _callersBundle.getBundleContext().getService(bpContainerRef);
      } else { 
        throw new NamingException();
      }
      
      try { 
        result = bpc.getComponentInstance(componentId);
      } catch (NoSuchComponentException nsce) { 
        throw new NamingException (nsce.getMessage());
      } finally {
        _callersBundle.getBundleContext().ungetService(bpContainerRef);
     } 
   } else { 
     result = new BlueprintURLContext (_callersBundle, bpName, _env, _blueprintContainer);
   }
   return result;
  }

  @Override
  public Object lookup(String name) throws NamingException
  {
    if (_parentName != null) {
      name = _parentName.toString() + "/" + name;
    }
    Object result = lookup (_parser.parse(name));
    return result;
  }

  @Override
  public Object lookupLink(Name n) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  @Override
  public Object lookupLink(String s) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  @Override
  public void rebind(Name n, Object o) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  @Override
  public void rebind(String s, Object o) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  @Override
  public Object removeFromEnvironment(String propName) throws NamingException
  {
    return _env.remove(propName);
  }

  @Override
  public void rename(Name nOld, Name nNew) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  @Override
  public void rename(String sOld, String sNew) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  @Override
  public void unbind(Name n) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  @Override
  public void unbind(String s) throws NamingException
  {
    throw new OperationNotSupportedException();
  }
  
  private ServiceReference getBlueprintContainerRef(Bundle b)
  {
    ServiceReference[] refs = b.getRegisteredServices();
    ServiceReference result = null;
    outer: for (ServiceReference r : refs) { 
      String[] objectClasses = (String[]) r.getProperty(Constants.OBJECTCLASS);
      for (String objectClass : objectClasses) { 
        if (objectClass.equals(BlueprintContainer.class.getName())) { 
          // Arguably we could put an r.isAssignableTo(jndi-url-bundle, BlueprintContainer.class.getName()) 
          // check here. But if you've got multiple, class-space inconsistent instances of blueprint in 
          // your environment, you've almost certainly got other problems. 
          result = r;
          break outer;
        }
      }
    }
    return result;
  }

}
