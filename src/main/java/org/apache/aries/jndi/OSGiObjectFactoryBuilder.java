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

import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.directory.Attributes;
import javax.naming.spi.DirObjectFactory;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.ObjectFactoryBuilder;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class OSGiObjectFactoryBuilder implements ObjectFactoryBuilder, ObjectFactory, DirObjectFactory
{
  /** The bundle context we use for accessing the SR */
  private static BundleContext context;
  
  public static void setBundleContext(BundleContext ctx)
  {
    context = ctx;
  }

  public ObjectFactory createObjectFactory(Object obj, Hashtable<?, ?> environment)
      throws NamingException
  {
    return this;
  }

  public Object getObjectInstance(Object obj, Name name, Context nameCtx,
      Hashtable<?, ?> environment) throws Exception
  {
  	
    Reference ref = null;

    if (obj instanceof Reference) {
      ref = (Reference) obj;
    } else if (obj instanceof Referenceable) {
      ref = ((Referenceable)obj).getReference();
    }

    Object result = ref;

    if (ref != null) {
      String className = ref.getFactoryClassName();

      if (className != null) {
        result = getObjectInstanceUsingClassName(obj, className, obj, name, nameCtx, environment);
      } else {
        result = getObjectInstanceUsingRefAddress(ref.getAll(), obj, name, nameCtx, environment);

        if (result == null || result == obj) {
          result = getObjectInstanceUsingAllObjectFactories(obj, name, nameCtx, environment);
        }
      }
    }

    if (result == null) result = ref;
    
    return result;
  }

  private Object getObjectInstanceUsingAllObjectFactories(Object obj, Name name, Context nameCtx,
      Hashtable<?, ?> environment)
  {
    Object result = obj;
    try {
      ServiceReference[] refs = context.getAllServiceReferences(ObjectFactory.class.getName(), null);

      if (refs != null) {
        for (ServiceReference ref : refs) {
          ObjectFactory factory = (ObjectFactory) context.getService(ref);
  
          if (factory != null) {
            try {
              result = factory.getObjectInstance(obj, name, nameCtx, environment);
            } catch (Exception e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
  
            context.ungetService(ref);
  
            // if the result comes back and is not null and not the reference
            // object then we should return the result, so break out of the
            // loop we are in.
            if (result != null && result != obj) break;
          }
        }
      }
    } catch (InvalidSyntaxException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    if (result == null) result = obj;
  	
    return result;
  }

  private Object getObjectInstanceUsingRefAddress(Enumeration<RefAddr> addresses, Object obj, Name name,
      Context nameCtx, Hashtable<?, ?> environment)
  {
    Object result = obj;

    while (addresses.hasMoreElements()) {
      RefAddr address = addresses.nextElement();
      if (address instanceof StringRefAddr && "URL".equals(address.getType())) {
        String urlScheme = (String)address.getContent();
        ObjectFactory factory = null;
        ServiceReference ref = null;
        try {
          ServiceReference[] services = context.getServiceReferences(ObjectFactory.class.getName(), "(|(osgi.jndi.urlScheme=" + urlScheme + ")(urlScheme=" + urlScheme + "))");

          if (services != null) {
            ref = services[0];
            factory = (ObjectFactory) context.getService(ref);
          }
        } catch (InvalidSyntaxException e) {
          e.printStackTrace();
        }

        if (factory != null) {
          try {
            result = factory.getObjectInstance(obj, name, nameCtx, environment);
          } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }

        if (ref != null) context.ungetService(ref);
        
        // if the result is not null and references a different object from
        // the obj then we have resolved the object, so we stop searching.
        if (result != null && result != obj) break;
      }
    }

    return result;
  }

  private Object getObjectInstanceUsingClassName(Object reference, String className, Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment)
  {
    Object result = obj;

    ObjectFactory factory = null;
    ServiceReference ref = null;

    if (className != null) {
      try {
        ServiceReference[] refs = context.getAllServiceReferences(className, null);

        if (refs != null) {
          for (ServiceReference ref1 : refs) {
            factory = (ObjectFactory) context.getService(ref1);

            if (factory != null) {
              ref = ref1;
              break;
            }
          }
        }
      } catch (InvalidSyntaxException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    if (factory == null) {
      try {
        ServiceReference[] refs = context.getServiceReferences(ObjectFactoryBuilder.class.getName(), null);

        if (refs != null) {
          for (ServiceReference ofRef : refs) {
            ObjectFactoryBuilder builder = (ObjectFactoryBuilder) context.getService(ofRef);
            try {
              factory = builder.createObjectFactory(reference, environment);
            } catch (NamingException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }

            context.ungetService(ofRef);
            if (factory != null) {
              break;
            }
          }
        }
      } catch (InvalidSyntaxException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    if (factory != null) {
      try {
        result = factory.getObjectInstance(obj, name, nameCtx, environment);
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } finally {
        if (ref != null) context.ungetService(ref);
      }
    }

    return result;
  }
  
  /**
   * when we get called by DirectoryManager#getObjectInstance if we can't find the object 
   * instance, we just need to return the passed in refInfo  
   */
  public Object getObjectInstance(Object refInfo, Name name, Context nameCtx, 
                                  Hashtable<?, ?> environment, Attributes attrs) throws Exception {
      Object result = getObjectInstance(refInfo, name, nameCtx, environment);
      return result == null ? refInfo : result;
  }

}