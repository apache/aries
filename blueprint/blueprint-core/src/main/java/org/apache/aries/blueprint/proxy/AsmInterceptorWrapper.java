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
package org.apache.aries.blueprint.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.aries.blueprint.Interceptor;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsmInterceptorWrapper
{
  private static final Logger LOGGER = LoggerFactory.getLogger(AsmInterceptorWrapper.class);
  final static String LOG_ENTRY = "Method entry: {}, args {}";
  final static String LOG_EXIT = "Method exit: {}, returning {}";
  final static String LOG_EXCEPTION = "Caught exception";

  public static Object createProxyObject(ClassLoader cl, ComponentMetadata cm,
      List<Interceptor> interceptors, Callable<Object> delegate, Class<?>... classesToProxy) throws UnableToProxyException
  {

    LOGGER.debug(LOG_ENTRY, "createProxyObject", new Object[] { cl, cm, interceptors, delegate,
        classesToProxy });

    Object proxyObject = null;

      if (classesToProxy.length == 1 && !classesToProxy[0].isInterface()) {

        Class<?> classToProxy = classesToProxy[0];
        LOGGER.debug("Single class to proxy: {}", classToProxy.getName());

        boolean isProxy = isProxyClass(classToProxy);
        LOGGER.debug("Class already a proxy: {}", isProxy);

        if (isProxy) {
          try {
            LOGGER.debug("Get a new instance of existing proxy class");
            /*
             * the class is already a proxy, we should just invoke
             * the constructor to get a new instance of the proxy
             * with a new Collaborator using the specified delegate
             */
            proxyObject = classToProxy.getConstructor(InvocationHandler.class).newInstance(
                new Collaborator(cm, interceptors, delegate));
            LOGGER.debug("New proxy object instance {}", proxyObject);
          } catch (InvocationTargetException e) {
            LOGGER.debug(LOG_EXCEPTION, e);
          } catch (NoSuchMethodException e) {
            LOGGER.debug(LOG_EXCEPTION, e);
          } catch (InstantiationException e) {
            LOGGER.debug(LOG_EXCEPTION, e);
          } catch (IllegalArgumentException e) {
            LOGGER.debug(LOG_EXCEPTION, e);
          } catch (SecurityException e) {
            LOGGER.debug(LOG_EXCEPTION, e);
          } catch (IllegalAccessException e) {
            LOGGER.debug(LOG_EXCEPTION, e);
          }
        } else {
          // we should generate a subclass proxy of the given class
          LOGGER.debug("Generating a subclass proxy for: {}", classToProxy.getName());
          proxyObject = createSubclassProxy(classToProxy, cm, interceptors, delegate);
        }

      } else {
        // we had more than one class specified or only an interface
        LOGGER.debug("Multiple classes or interface(s) to proxy: {}", classesToProxy);
        // if we just have interfaces and no classes we default to using
        // the interface proxy because we can't dynamically
        // subclass more than one interface
        // unless we have a class
        // that implements all of them

        // default to not subclass
        boolean useSubclassProxy = false;

        // loop through the classes checking if they are java interfaces
        // if we find any class that isn't an interface we need to use
        // the subclass proxy
        Set<Class<?>> notInterfaces = new HashSet<Class<?>>();
        for (Class<?> clazz : classesToProxy) {
          if (!clazz.isInterface()) {
            useSubclassProxy = true;
            notInterfaces.add(clazz);
          }
        }

        if (useSubclassProxy) {
          LOGGER.debug("Going to use subclass proxy");
          // if we need to use the subclass proxy then we need to find
          // the most specific class
          Class<?> classToProxy = null;
          int deepest = 0;
          // for each of the classes find out how deep it is in the
          // hierarchy
          for (Class<?> clazz : notInterfaces) {
            Class<?> nextHighestClass = clazz;
            int depth = 0;
            do {
              nextHighestClass = nextHighestClass.getSuperclass();
              depth++;
            } while (nextHighestClass != null);
            if (depth > deepest) {
              // if we find a class deeper than the one we already
              // had
              // it becomes the new most specific
              deepest = depth;
              classToProxy = clazz;
            }
          }
          LOGGER.debug("Most specific class to proxy: {}", classToProxy);
          proxyObject = createSubclassProxy(classToProxy, cm, interceptors, delegate);
        } else {
          LOGGER.debug("Going to use interface proxy");
          proxyObject = Proxy.newProxyInstance(cl, classesToProxy, new Collaborator(cm,
              interceptors, delegate));
        }

      }

//    LOGGER.debug(LOG_EXIT, "createProxyObject", proxyObject);

    return proxyObject;
  }

  private static Object createSubclassProxy(Class<?> classToProxy, ComponentMetadata cm,
      List<Interceptor> interceptors, Callable<Object> delegate) throws UnableToProxyException
  {
    LOGGER.debug(LOG_ENTRY, "createSubclassProxy", new Object[] { classToProxy, cm, interceptors,
        delegate });
    LOGGER.debug("Generating a subclass proxy for: {}", classToProxy.getName());
    try {
      Object proxyObject = ProxySubclassGenerator.newProxySubclassInstance(classToProxy,
          new Collaborator(cm, interceptors, delegate));

//      LOGGER.debug("Generated subclass proxy object: {}", proxyObject);
//      LOGGER.debug(LOG_EXIT, "createSubclassProxy", proxyObject);
      return proxyObject;
    } catch (UnableToProxyException e) {
      LOGGER.debug(LOG_EXCEPTION, e);
      LOGGER.debug(LOG_EXIT, "createSubclassProxy", e);
      throw e;
    }
  }

  static boolean isProxyClass(Class<?> clazz)
  {
    LOGGER.debug(LOG_ENTRY, "isProxyClass", new Object[] { clazz });
    boolean isProxyObject = false;
    isProxyObject = ProxySubclassGenerator.isProxySubclass(clazz);
    LOGGER.debug(LOG_EXIT, "isProxyClass", isProxyObject);
    return isProxyObject;
  }

  static Object unwrapObject(Object o) throws Exception
  {
    LOGGER.debug(LOG_ENTRY, "unwrapObject", new Object[] { o });
    InvocationHandler ih = null;
    Object unwrappedObject = null;
    if (ProxySubclassGenerator.isProxySubclass(o.getClass())) {
      ih = ProxySubclassGenerator.getInvocationHandler(o);
    } else {
      ih = Proxy.getInvocationHandler(o);
    }
    if (ih instanceof Collaborator) {
      unwrappedObject = ((Collaborator) ih).object.call();
    }
//    LOGGER.debug(LOG_EXIT, "unwrapObject", unwrappedObject);
    return unwrappedObject;
  }

    public static <T> Callable<T> passThrough(T t) {
        return new PassThrough<T>(t);
    }

    public static class PassThrough<T> implements Callable<T> {
        private final T t;
        public PassThrough(T t) {
            this.t = t;
        }
        public T call() throws Exception {
            return t;
        }
    }
}
