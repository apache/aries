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
package org.apache.aries.unittest.mocks;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.apache.aries.unittest.mocks.annotations.InjectSkeleton;
import org.apache.aries.unittest.mocks.annotations.Singleton;

/**
 * <p>The Skeleton class is an implementation of the 
 *   <code>java.lang.reflect.InvocationHandler</code> that can be used for
 *   dynamic mock objects.
 * </p>
 * 
 * <ol>
 *   <li>The static newMock methods can be used to create completely new mock 
 *     objects backed by an entirely new skeleton.
 *   </li>
 *   <li>The static getSkeleton method can be used to obtain the skeleton 
 *     backing a given mock.
 *   </li>
 *   <li>The createMock methods can be used to create a new mock object based on
 *     the skeleton that is invoked.
 *   </li>
 *   <li>The registerMethodCallHandler method can be used to register a handler
 *     that will be invoked when a method is called.
 *   </li>
 *   <li>The registerReturnTypeHandler method can be used to register a handler
 *     that will be invoked when a method with a specific return type is 
 *     invoked. It should be noted that registered ReturnTypeHandlers will be
 *     invoked only if a method call handler has not been registered for the
 *     method that was invoked.
 *   </li>
 *   <li>The setReturnValue method can be used to set a value that will be
 *     returned when a method is invoked.
 *   </li>
 *   <li>The checkCalls methods can be used to determine if the methods in the
 *     list should have been called. They return a boolean to indicate if the
 *     expected calls occurred.
 *   </li>
 *   <li>The assertCalls method performs the same operation as the checkCalls, 
 *     but throws an junit.framework.AssertionFailedError if the calls don't
 *     match. This intended for use within the junit test framework
 *   </li> 
 *   <li>If no method call or return type handlers have been registered for a
 *     call then if the return type is an interface then a mock that implements
 *     that interface will be returned, otherwise null will be returned.
 *   </li>
 * </ol>
 */
public final class Skeleton implements InvocationHandler
{
  /** A list of calls made on this skeleton */
  private List<MethodCall> _methodCalls;
  /** The invocation handler to call after MethodCall and ReturnType handlers */
  private DefaultInvocationHandler default_Handler;
  /** The method call handlers */
  private Map<MethodCall, MethodCallHandler> _callHandlers;
  /** The type handlers */
  private Map<Class<?>, ReturnTypeHandler> _typeHandlers;
  /** The parameter map */
  private Map<String, Object> _mockParameters;
  /** A Map of mock objects to Maps of properties */
  private Map<Object, Map<String, Object>> _objectProperties;
  /** A Map of exception notification listeners */
  private Map<Class<?>, List<ExceptionListener>> _notificationListeners;
  /** The template class used to create this Skeleton, may be null */
  private Object _template;
  /** Cached template objects */
  private static ConcurrentMap<Object, SoftReference<Object>> _singletonMocks = new ConcurrentHashMap<Object, SoftReference<Object>>();

  // Constructors
  
  /* ------------------------------------------------------------------------ */
  /* Skeleton constructor                                    
  /* ------------------------------------------------------------------------ */
  /**
   * constructs the skeleton with the default method call handlers and the
   * default return type handlers.
   */
  private Skeleton()
  {
    reset();
  }
  
  // Static methods create methods
  
  /* ------------------------------------------------------------------------ */
  /* newMock method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method returns a completely new mock object backed by a new skeleton
   * object. It is equivalent to 
   * <code>new Skeleton().createMock(interfaceClazzes)</code>
   * 
   * @param interfaceClazzes the classes the mock should implement
   * @return the new mock object.
   */
  public final static Object newMock(Class<?> ... interfaceClazzes)
  {
    return new Skeleton().createMock(interfaceClazzes);
  }
  
  /* ------------------------------------------------------------------------ */
  /* newMock method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method returns a completely new mock object backed by a new skeleton
   * object. It is equivalent to 
   * <code>new Skeleton().createMock(interfaceClazzes)</code>
   * 
   * @param <T>            The object type.
   * @param interfaceClazz the classes the mock should implement
   * @return the new mock object.
   */
  public final static <T> T newMock(Class<T> interfaceClazz)
  {
    return interfaceClazz.cast(new Skeleton().createMock(interfaceClazz));
  }
  
  /**
   * It is often the case that only a subset of methods on an interface are needed, but
   * those methods that are needed are quite complex. In this case a static mock forces
   * you into implementing lots of methods you do not need, and produces problems when
   * new methods are added to the interface being implemented. This method can essentially
   * be used to complete the interface implementation. The object passed in is an instance
   * of a class that implements a subset of the methods on the supplied interface. It does
   * not need to implement the interface itself. The returned object will implement the full
   * interface and delegate to the methods on the templateObject where necessary.
   * 
   * @param <T>            The object type.
   * @param template       The template object for the mock
   * @param interfaceClazz The interface to implement
   * @return An implementation of the interface that delegates (where appropraite) onto the template.  
   */
  public final static <T> T newMock(final Object template, Class<T> interfaceClazz)
  {
    Class<?> templateClass = template.getClass();
    
    if (templateClass.getAnnotation(Singleton.class) != null) {
      SoftReference<Object> mock = _singletonMocks.get(template);
      if (mock != null) {
        Object theMock = mock.get();
        if (theMock == null) {
          _singletonMocks.remove(template);
        } else if (interfaceClazz.isInstance(theMock)) {
          return interfaceClazz.cast(theMock);
        } 
      }
    }
    
    Skeleton s = new Skeleton();
    s._template = template;
    
    for (Method m : interfaceClazz.getMethods()) {
      try {
        final Method m2 = templateClass.getMethod(m.getName(), m.getParameterTypes());
        
        MethodCall mc = new MethodCall(interfaceClazz, m.getName(), (Object[])m.getParameterTypes());
        s.registerMethodCallHandler(mc, new MethodCallHandler()
        {
          public Object handle(MethodCall methodCall, Skeleton parent) throws Exception
          {
            
            try {
              m2.setAccessible(true);
              return m2.invoke(template, methodCall.getArguments());
            } catch (InvocationTargetException ite) {
              if(ite.getTargetException() instanceof Exception)
                throw (Exception)ite.getTargetException();
              else throw new Exception(ite.getTargetException());
            }
          }
        });
      } catch (NoSuchMethodException e) {
        // do nothing here, it is a method not on the interface so ignore it.
      }
    }
    
    Field[] fs = template.getClass().getFields();
    
    for (Field f : fs) {
      InjectSkeleton sk = f.getAnnotation(InjectSkeleton.class);
      
      if (sk != null) {
        f.setAccessible(true);
        try {
          f.set(template, s);
        } catch (IllegalArgumentException e) {
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      }
    }
      
    Object o = s.createMock(interfaceClazz);
    _singletonMocks.put(template, new SoftReference<Object>(o) {
      @Override
      public boolean enqueue()
      {
        _singletonMocks.remove(template);
        
        System.out.println("Done cleanup");
        
        return super.enqueue();
      }
    });
    return interfaceClazz.cast(o);
  }
  
  // static mock query methods
  
  /* ------------------------------------------------------------------------ */
  /* getSkeleton method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method returns the Skeleton backing the supplied mock object. If the
   * supplied object is not a mock an IllegalArgumentException will be thrown.
   * 
   * @param mock the mock object
   * @return the skeleton backing the mock object
   * @throws IllegalArgumentException thrown if the object is not a mock.
   */
  public final static Skeleton getSkeleton(Object mock) 
    throws IllegalArgumentException
  {
    InvocationHandler ih = Proxy.getInvocationHandler(mock);
    if (ih instanceof Skeleton)
    {
      return (Skeleton)ih;
    }
    throw new IllegalArgumentException("The supplied proxy (" + mock + ") was not an Aries dynamic mock ");
  }
  
  /* ------------------------------------------------------------------------ */
  /* isSkeleton method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method returns true if and only the provided object is backed by a
   * Skeleton. Another way to think about this is if it returns true then a 
   * call to getSkeleton will not result in an IllegalArgumentException, and is
   * guaranteed to return a Skeleton.
   * 
   * @param mock the mock to test.
   * @return     true if it is backed by a skeleton.
   */
  public final static boolean isSkeleton(Object mock)
  {
    if (Proxy.isProxyClass(mock.getClass())) {
      InvocationHandler ih = Proxy.getInvocationHandler(mock);
      
      return (ih instanceof Skeleton);
    }
    
    return false;
  }

  // InvocationHandler defined methods.
  
  /* ------------------------------------------------------------------------ */
  /* invoke method                                    
  /* ------------------------------------------------------------------------ */
  /** 
   * This method is invoked by the mock objects. It constructs a MethodCall
   * object representing the call and adds it to the list of calls that were
   * made. (It should be noted that if the method is toString, hashCode or
   * equals then they are not added to the list.) It then calls a registered
   * MethodCallHandler, if a MethodCallHandler is not registered then a 
   * ReturnTypeHandler is invoked. If a ReturnTypeHandler is not invoked then
   * the registered default InvocationHandler is called. By default the Skeleton
   * is constructed with a DefaultInvocationHandler. If the invoked method has
   * an interface as a return type then the DefaultInvocationHandler will return
   * a new mock implementing that interface. If the return type is a class null
   * will be returned.
   * 
   * @param targetObject The mock object that was invoked.
   * @param calledMethod The method that was called.
   * @param arguments    The arguments that were passed.
   * @return             The return of the method invoked.
   * @throws Throwable   Any exception thrown.
   */
  public Object invoke(Object targetObject, Method calledMethod, Object[] arguments)
      throws Throwable
  {
    String methodName = calledMethod.getName();
    MethodCall call = new MethodCall(targetObject, methodName, arguments);

    if (!DefaultMethodCallHandlers.isDefaultMethodCall(call))
    {
      _methodCalls.add(call);
    }

    Object result;
    
    try
    {
      if (_callHandlers.containsKey(call))
      {
        MethodCallHandler handler =  _callHandlers.get(call);
        result = handler.handle(call, this);
      }
      else if (isReadWriteProperty(targetObject.getClass(), calledMethod))
      {
        String propertyName = methodName.substring(3);
        if (methodName.startsWith("get") || methodName.startsWith("is"))
        {
          if (methodName.startsWith("is")) propertyName = methodName.substring(2);
          
          Map<String, Object> properties = _objectProperties.get(targetObject);
          if (properties == null)
          {
            properties = new HashMap<String, Object>();
            _objectProperties.put(targetObject, properties);
          }
          
          if (properties.containsKey(propertyName))
          {
            result = properties.get(propertyName);
          }
          else if (_typeHandlers.containsKey(calledMethod.getReturnType()))
          {
            result = createReturnTypeProxy(calledMethod.getReturnType());
          }
          else
          {
            result = default_Handler.invoke(targetObject, calledMethod, arguments);
          }          
        }
        // Must be a setter.
        else
        {
          Map<String, Object> properties = _objectProperties.get(targetObject);
          if (properties == null)
          {
            properties = new HashMap<String, Object>();
            _objectProperties.put(targetObject, properties);
          }
          
          properties.put(propertyName, arguments[0]);
          result = null;
        }
      }
      else if (_typeHandlers.containsKey(calledMethod.getReturnType()))
      {
        result = createReturnTypeProxy(calledMethod.getReturnType());
      }
      else
      {
        result = default_Handler.invoke(targetObject, calledMethod, arguments);
      }
    }
    catch (Throwable t)
    {
      Class<?> throwableType = t.getClass();
      List<ExceptionListener> listeners = _notificationListeners.get(throwableType);
      if (listeners != null)
      {
        for (ExceptionListener listener : listeners)
        {
          listener.exceptionNotification(t);
        }
      }
      
      throw t;
    }
    
    return result;
  }

  // MethodCall registration methods
  
  /* ------------------------------------------------------------------------ */
  /* registerMethodCallHandler method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method registers a MethodCallHandler for the specified MethodCall.
   * 
   * @param call    The method that was called.
   * @param handler The MethodCallHandler.
   */
  public void registerMethodCallHandler(MethodCall call, MethodCallHandler handler)
  {
    deRegisterMethodCallHandler(call);
    _callHandlers.put(call, handler);
  }
  
  /* ------------------------------------------------------------------------ */
  /* deRegisterMethodCallHandler method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method removes a registered MethodCallHandler for the specified 
   * MethodCall.
   * 
   * @param call the specified MethodCall
   */
  public void deRegisterMethodCallHandler(MethodCall call)
  {
    _callHandlers.remove(call);
  }
  
  /* ------------------------------------------------------------------------ */
  /* reset method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method resets the skeleton to the state it was in prior just after it
   * was constructed. 
   */
  public void reset()
  {
    _methodCalls = new LinkedList<MethodCall>();
    _callHandlers = new HashMap<MethodCall, MethodCallHandler>();
    _typeHandlers = new HashMap<Class<?>, ReturnTypeHandler>();
    DefaultReturnTypeHandlers.registerDefaultHandlers(this);
    DefaultMethodCallHandlers.registerDefaultHandlers(this);
    default_Handler = new DefaultInvocationHandler(this);
    _mockParameters = new HashMap<String, Object>();
    _objectProperties = new HashMap<Object, Map<String, Object>>();
    _notificationListeners = new HashMap<Class<?>, List<ExceptionListener>>();
  }
  
  /* ------------------------------------------------------------------------ */
  /* clearMethodCalls method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method clears the method calls list for the skeleton
   */
  public void clearMethodCalls()
  {
    _methodCalls = new LinkedList<MethodCall>();
  }
  
  
  /* ------------------------------------------------------------------------ */
  /* setReturnValue method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This is a convenience method for registering a method call handler where
   * a specific value should be returned when a method is called, rather than 
   * some logic needs to be applied. The value should be an object or the object
   * version of the primitive for the methods return type, so if the method
   * returns short the value must be an instance of java.lang.Short, not 
   * java.lang.Integer.   
   * 
   * @param call  the method being called.
   * @param value the value to be returned when that method is called.
   */
  public void setReturnValue(MethodCall call, final Object value)
  {
    Class<?> clazz;
    try {
      clazz = Class.forName(call.getClassName());
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("This should be impossible as we have already seen this class loaded");
    }
    
    
      Method[] methods = clazz.getMethods();
      
      methods: for (Method m : methods) {
        if(!!!m.getName().equals(call.getMethodName()))
          continue methods;
        
        Object[] args = call.getArguments();
        Class<?>[] parms = m.getParameterTypes();
        
        if (args.length == parms.length) {
          for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Class && args[i].equals(parms[i])) {
            } else if (parms[i].isInstance(args[i])) {
            } else {
              continue methods;
            }
          }
          
          Class<?> returnType = m.getReturnType();
          if (returnType.isPrimitive()) {
            if (returnType == boolean.class) returnType = Boolean.class;
            else if (returnType == byte.class) returnType = Byte.class;
            else if (returnType == short.class) returnType = Short.class;
            else if (returnType == char.class) returnType = Character.class;
            else if (returnType == int.class) returnType = Integer.class;
            else if (returnType == long.class) returnType = Long.class;
            else if (returnType == float.class) returnType = Float.class;
            else if (returnType == double.class) returnType = Double.class;
          }
          
          if (value != null && !!!returnType.isInstance(value)) {
            throw new IllegalArgumentException("The object cannot be returned by the requested method: " + call);
          } else break methods; 
        }
      }
    
    
    
    registerMethodCallHandler(call, new MethodCallHandler()
    {
      public Object handle(MethodCall methodCall, Skeleton parent) throws Exception
      {
        return value;
      }
    });
  }

  /* ------------------------------------------------------------------------ */
  /* setThrow method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This is a convenience method for registering a method call handler where
   * a specific exception should be thrown when the method is called, rather
   * than some logic needs to be applied.
   * 
   * @param call         the method being called
   * @param thingToThrow the exception to throw.
   */
  public void setThrows(MethodCall call, final Exception thingToThrow)
  {
    registerMethodCallHandler(call, new MethodCallHandler()
    {
      public Object handle(MethodCall methodCall, Skeleton parent) throws Exception
      {
        thingToThrow.fillInStackTrace();
        throw thingToThrow;
      }
    });
  }

  /* ------------------------------------------------------------------------ */
  /* setThrow method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This is a convenience method for registering a method call handler where
   * a specific exception should be thrown when the method is called, rather
   * than some logic needs to be applied.
   * 
   * @param call         the method being called
   * @param thingToThrow the exception to throw.
   */
  public void setThrows(MethodCall call, final Error thingToThrow)
  {
    registerMethodCallHandler(call, new MethodCallHandler()
    {
      public Object handle(MethodCall methodCall, Skeleton parent) throws Exception
      {
        thingToThrow.fillInStackTrace();
        throw thingToThrow;
      }
    });
  }

  // ReturnType registration methods
  
  /* ------------------------------------------------------------------------ */
  /* registerReturnTypeHandler method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method registers a ReturnTypeHandler for the specified class.
   * 
   * @param clazz   The class to be handled.
   * @param handler The ReturnTypeHandler
   */
  public void registerReturnTypeHandler(Class<?> clazz, ReturnTypeHandler handler)
  {
    deRegisterReturnTypeHandler(clazz);
    _typeHandlers.put(clazz, handler);
  }
  
  /* ------------------------------------------------------------------------ */
  /* deRegisterReturnTypeHandler method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method removes a registration for a ReturnTypeHandler for the 
   * specified class.
   * 
   * @param clazz The class to deregister the handler for.
   */
  public void deRegisterReturnTypeHandler(Class<?> clazz)
  {
    _typeHandlers.remove(clazz);
  }
  
  // Exception notification methods
  
  /* ------------------------------------------------------------------------ */
  /* registerExceptionListener method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method registers an ExceptionListener when the specified Exception is
   * thrown.
   * 
   * @param throwableType The type of the Throwable
   * @param listener      The listener.
   */
  public void registerExceptionListener(Class<?> throwableType, ExceptionListener listener)
  {
    List<ExceptionListener> l = _notificationListeners.get(throwableType);
    if (l == null)
    {
      l = new ArrayList<ExceptionListener>();
      _notificationListeners.put(throwableType, l);
    }
    l.add(listener);
  }
  
  // parameter related methods

  /* ------------------------------------------------------------------------ */
  /* setParameter method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method allows a parameter to be set. It is intended to be used by
   * MethodCallHandlers and ReturnTypeHandlers.
   * 
   * @param key   The key
   * @param value The value
   */
  public void setParameter(String key, Object value)
  {
    _mockParameters.put(key, value);
  }
  
  /* ------------------------------------------------------------------------ */
  /* getParameter method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method allows a parameter to be retrieved.
   * 
   * @param key the key the parameter was set using
   * @return the parameter
   */
  public Object getParameter(String key)
  {
    return _mockParameters.get(key);
  }
  
  /* ------------------------------------------------------------------------ */
  /* getTemplateObject method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @return the template object, if one was used when initializing this skeleton.
   */
  public Object getTemplateObject()
  {
    return _template;
  }
  
  // default InvocationHandler related methods
  
  /**
   * @param defaultHandler The defaultHandler to set.
   */
  public void setDefaultHandler(DefaultInvocationHandler defaultHandler)
  {
    this.default_Handler = defaultHandler;
  }
  
  // MethodCall list check methods
  
  /* ------------------------------------------------------------------------ */
  /* checkCalls method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method checks that the calls in the list occurred. If the addCalls
   * boolean is true then their must be an exact match. If the allCalls boolean
   * is false then the calls in the list must occur in that order, but other 
   * calls can be in between. 
   * 
   * @param calls    The expected calls list
   * @param allCalls true if an exact match comparison should be performed
   * @return         true if they the expected calls match.
   */
  public boolean checkCalls(List<MethodCall> calls, boolean allCalls)
  {
    boolean found = false;
    if (allCalls)
    {
      return calls.equals(_methodCalls);
    }
    else
    {
      Iterator<MethodCall> actual = _methodCalls.iterator();
      for (MethodCall expectedCall : calls)
      {
        found = false;
        actual: while (actual.hasNext())
        {
          MethodCall actualCall = actual.next();
          if (actualCall.equals(expectedCall))
          {
            found = true;
            break actual;
          }
        }
      }
    }
    
    return found;
  }

  /* ------------------------------------------------------------------------ */
  /* checkCall method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * Checks that the specified method has been called on this skeleton
   * 
   * @param call the call that should have been called.
   * @return     true if the MethodCall occurs in the list.
   */
  public boolean checkCall(MethodCall call)
  {
    return this._methodCalls.contains(call);
  }
  
  // MethodCall list assert methods
  
  /* ------------------------------------------------------------------------ */
  /* assertCalls method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method checks that the MethodCalls objects in the given list were
   * made and throws an AssertionFailedError if they were not. If allCalls is
   * true the given list and the calls list must be identical. If allCalls is
   * false other calls could have been made on the skeleton in between ones
   * specified in the list.
   * 
   * @param calls the list of calls
   * @param allCalls whether an exact match between the lists is required
   * @throws AssertionFailedError if a failure has occurred.
   */
  public void assertCalled(List<MethodCall> calls, boolean allCalls) throws AssertionFailedError
  {
    if (allCalls)
    {
      if ((calls == null) && (_methodCalls == null)) return;

      if (calls == null)
      {
        throw new AssertionFailedError("expected null, but was " + _methodCalls);
      }

      if (_methodCalls == null)
      {
        throw new AssertionFailedError("expected:" + calls + " but was null");
      }

      if (calls.equals(_methodCalls)) return;

      // OK compare lists and decide on differences - initially all the lists are different
      int startOfDifferences = 0;
      // Remove the common start of sequence
      boolean lastItemSame = true;

      for (int i = 0; i < calls.size() && i < _methodCalls.size() && lastItemSame; i++)
      {
        if ((calls.get(i) == null) && (_methodCalls.get(i) == null))
        {
          lastItemSame = true;
        } 
        else if ((calls.get(i) == null) || (_methodCalls.get(i) == null))
        {
          lastItemSame = false;
        } 
        else
        {
          lastItemSame = calls.get(i).equals(_methodCalls.get(i));
        }

        if (lastItemSame) startOfDifferences++;

      }//for
      // Now remove the common bit at the end
      int endOfDifferencesInExpected = calls.size();
      int endOfDifferencesInReceived = _methodCalls.size();
      lastItemSame = true;

      while ((endOfDifferencesInExpected > startOfDifferences)
        && (endOfDifferencesInReceived > startOfDifferences)
        && lastItemSame)
      {
        int ap = endOfDifferencesInExpected - 1;
        int bp = endOfDifferencesInReceived - 1;

        if ((calls.get(ap) == null) && (_methodCalls.get(bp) == null))
        {
          lastItemSame = true;
        } 
        else if ((calls.get(ap) == null) || (_methodCalls.get(bp) == null))
        {
          lastItemSame = false;
        } 
        else
        {
          lastItemSame = calls.get(ap).equals(_methodCalls.get(bp));
        }

        if (lastItemSame)
        {
          endOfDifferencesInExpected--;
          endOfDifferencesInReceived--;
        }

      }//while

      String failureText;
      // OK, now build the failureText
      if (endOfDifferencesInExpected == startOfDifferences)
      {
        failureText =
            "Expected calls and actual calls differed because "
            + _methodCalls.subList(startOfDifferences, endOfDifferencesInReceived)
            + " inserted after element "
            + startOfDifferences;

      } 
      else if (endOfDifferencesInReceived == startOfDifferences)
      {
        failureText =
            "Expected calls and actual calls differed  because "
            + calls.subList(startOfDifferences, endOfDifferencesInExpected)
            + " missing after element "
            + startOfDifferences;

      } 
      else
      {
        if ((endOfDifferencesInExpected == startOfDifferences + 1)
          && (endOfDifferencesInReceived == startOfDifferences + 1))
        {

          failureText =
              "Expected calls and actual calls differed  because element "
              + startOfDifferences
              + " is different (calls:"
              + calls.get(startOfDifferences)
              + " but was:"+_methodCalls.get(startOfDifferences)+") ";

        } 
        else if (endOfDifferencesInExpected == startOfDifferences + 1)
        {

            failureText =
                "Expected calls and actual calls differed  because element "
                + startOfDifferences
                + " ("
                + calls.get(startOfDifferences)
                + ") has been replaced by "
                + _methodCalls.subList(startOfDifferences, endOfDifferencesInReceived);
        } 
        else
        {
          failureText =
                "Expected calls and actual calls differed  because elements between "
                + startOfDifferences
                + " and "
                + (endOfDifferencesInExpected - 1)
                + " are different (expected:"
                + calls.subList(startOfDifferences, endOfDifferencesInExpected)
                + " but was:"
                + _methodCalls.subList(startOfDifferences, endOfDifferencesInReceived)
                + ")";
        }//if
      }//if

      throw new AssertionFailedError(failureText + " expected:" + calls + " but was:" + _methodCalls);
    }
    else
    {
      Iterator<MethodCall> expected = calls.iterator();
      Iterator<MethodCall> actual = _methodCalls.iterator();
      while (expected.hasNext())
      {
        boolean found = false;
        MethodCall expectedCall = expected.next();
        MethodCall actualCall = null;
        
        actual: while (actual.hasNext())
        {
          actualCall = actual.next();
          if (actualCall.equals(expectedCall))
          {
            found = true;
            break actual;
          }
        }
        
        if (!found)
        {
          throw new AssertionFailedError( "The method call " + 
                                          expectedCall + 
                                          " was expected but has not occurred (actual calls = "+_methodCalls+")");
        }
      }
    }
  }
  
  /* ------------------------------------------------------------------------ */
  /* assertCall method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This does the same as checkCall, but throws an 
   * junit.framework.AssertionFailedError if the call did not occur.
   * 
   * @param call the call that was expected
   */
  public void assertCalled(MethodCall call)
  {
    if (!checkCall(call))
    {
      throw new AssertionFailedError("The method call " + 
                                      call + 
                                      " was expected but has not occurred. Actual calls: " + getMethodCallsAsString());
    }
  }
  
  /* ------------------------------------------------------------------------ */
  /* assertCalledExactNumberOfTimes method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method asserts that the method specified in the call parameter has 
   * been called the number of times specified by numberOfCalls. If 
   * numberOfCalls is zero this method is equivalent to assertNotCalled.
   * 
   * @param call          The call that was made.
   * @param numberOfCalls The number of times the call should have been made.
   */
  public void assertCalledExactNumberOfTimes(MethodCall call, int numberOfCalls)
  {
    int callCount = 0;
    
    for (MethodCall callMade : _methodCalls)
    {
      if (callMade.equals(call))
      {
        callCount++;
      }
    }
    
    if (numberOfCalls != callCount)
    {
      throw new AssertionFailedError("The method call " + call + 
          " should have been called " + numberOfCalls + 
          " time(s), but was called " + callCount + " time(s)");
    }
  }
  
  /* ------------------------------------------------------------------------ */
  /* assertNotCalled method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method throws an junit.framework.AssertionFailedError if the specified
   * call was invoked on the skeleton. 
   * 
   * @param call the call to check.
   */
  public void assertNotCalled(MethodCall call)
  {
    Assert.assertFalse( "The method call " + 
                        call + 
                        " occurred in the skeleton " + 
                        this.toString(), checkCall(call));
  }
  
  /* ------------------------------------------------------------------------ */
  /* assertMockNotCalled method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method throws an junit.framework.AssertionFailedError if the skeleton
   * has had any methods invoked on it. 
   */
  public void assertSkeletonNotCalled()
  {
    Assert.assertEquals("The skeleton " + this.toString() + 
        " has had the following method invoked on it " + getMethodCallsAsString(), 
        0, _methodCalls.size());
  }

  // Instance mock creation methods
  
  /* ------------------------------------------------------------------------ */
  /* createMock method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * Creates a new Mock using this skeleton backing it.
   * 
   * @param interfaceClasses an array of interface the mock should implement.
   * @return the mock
   */
  public Object createMock(Class<?> ... interfaceClasses)
  {
    ClassLoader cl;
    
    if (interfaceClasses.length > 0) cl = interfaceClasses[0].getClassLoader();
    else cl = Thread.currentThread().getContextClassLoader();
    
    return Proxy.newProxyInstance(cl, interfaceClasses, this);
  }
  
  /* ------------------------------------------------------------------------ */
  /* createMock method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * Creates a new Mock using this skeleton backing it.
   * 
   * @param <T> The object type
   * @param interfaceClass an array of interface the mock should implement.
   * @return the mock
   */
  public <T> T createMock(Class<T> interfaceClass)
  {
    return interfaceClass.cast(Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[] {interfaceClass}, this));
  }
  
  /* ------------------------------------------------------------------------ */
  /* invokeReturnTypeHandlers method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method invokes the return type proxy for the specified class. If a
   * ReturnTypeHandler for that type has not been registered then if the class
   * represents an interface a new mock will be returned, backed by this 
   * skeleton, otherwise null will be returned.
   * 
   * @param type       the type to be invoked.
   * @return           the returned object.
   * @throws Exception if an error occurs when invoking the return type handler.
   */
  public Object invokeReturnTypeHandlers(Class<?> type) throws Exception
  {
    if (_typeHandlers.containsKey(type))
    {
      ReturnTypeHandler rth =  _typeHandlers.get(type);
      return rth.handle(type, this);
    }
    else if (type.isInterface())
    {
      return createMock(type);
    }
    else
    {
      return null;
    }
  }

  // Miscellaneous methods that have been deprecated and will be removed.
  
  /* ------------------------------------------------------------------------ */
  /* createReturnTypeProxy method
  /* ------------------------------------------------------------------------ */
  /**
   * create a proxy for given return type. 
   *
   * @deprecated use invokeReturnTypeHandlers instead
   *  
   * @param type               The return type for which a handler is required
   * @return ReturnTypeHandler The return type handler
   * @throws Exception         Thrown if an exception occurs.
   */
  /* ------------------------------------------------------------------------ */
  @Deprecated
  private final Object createReturnTypeProxy(Class<?> type) throws Exception
  {
    return invokeReturnTypeHandlers(type);
  }
  
  /* ------------------------------------------------------------------------ */
  /* isReadWriteProperty method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method returns true if the method passed a getter for a read write
   * java bean property. This is worked out by checking that a setter and getter
   * exist for the property and that the setter and getter take and return 
   * exactly the same time.
   * 
   * @param objClass The object the read write method has been invoked on.
   * @param method   The method to be checked.
   * @return         true if it is a getter or setter for a read write property.
   */
  private boolean isReadWriteProperty(Class<?> objClass, Method method)
  {
    String methodName = method.getName();
    String propertyName = methodName.substring(3);
    Class<?>[] parameters = method.getParameterTypes();
    Class<?> clazz;
    boolean result = false;
    
    if (methodName.startsWith("get") && parameters.length == 0)
    {
      clazz = method.getReturnType();
      try
      {
        objClass.getMethod("set" + propertyName, clazz);
        result = true;
      }
      catch (NoSuchMethodException e)
      {
        if (isPrimitive(clazz))
        {
          clazz = getOtherForm(clazz);
          try
          {
            objClass.getMethod("set" + propertyName, clazz);
            result = true;
          }
          catch (NoSuchMethodException e1)
          {
            
          }
        }
      }
    }
    else if (methodName.startsWith("is") && parameters.length == 0)
    {
      clazz = method.getReturnType();
      if (clazz.equals(Boolean.class) || clazz.equals(boolean.class))
      {
        propertyName = methodName.substring(2);
        try
        {
          objClass.getMethod("set" + propertyName, clazz);
          result = true;
        }
        catch (NoSuchMethodException e)
        {
          
          if (isPrimitive(clazz))
          {
            clazz = getOtherForm(clazz);
            try
            {
              objClass.getMethod("set" + propertyName, clazz);
              result = true;
            }
            catch (NoSuchMethodException e1)
            {
              
            }
          }
        }
      }
    }
    else if (methodName.startsWith("set") && parameters.length == 1)
    {
      clazz = parameters[0];
      
      try
      {
        Method getter = objClass.getMethod("get" + propertyName, new Class[0]);
        result = checkClasses(getter.getReturnType(), clazz);
      }
      catch (NoSuchMethodException e)
      {
        if (isPrimitive(clazz))
        {
          clazz = getOtherForm(clazz);
          try
          {
            Method getter = objClass.getMethod("get" + propertyName, new Class[0]);
            result = checkClasses(getter.getReturnType(), clazz);
          }
          catch (NoSuchMethodException e1)
          {
            if (clazz.equals(Boolean.class) || clazz.equals(boolean.class))
            {
              try
              {
                Method getter = objClass.getMethod("is" + propertyName, new Class[0]);
                result = checkClasses(getter.getReturnType(), clazz);
              }
              catch (NoSuchMethodException e2)
              {
                clazz = getOtherForm(clazz);
                try
                {
                  Method getter = objClass.getMethod("is" + propertyName, new Class[0]);
                  result = checkClasses(getter.getReturnType(), clazz);
                }
                catch (NoSuchMethodException e3)
                {
                }
              }
            }
          }
        }
      }
    }
    
    return result;
  }
  
  
  /* ------------------------------------------------------------------------ */
  /* isPrimitive method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method returns true if the class object represents a primitive or the
   * object version of a primitive.
   * 
   * @param clazz The class to be checked.
   * @return      true if it is a primitive or a wrapper.
   */
  private boolean isPrimitive(Class<?> clazz)
  {
    boolean result = false;
    
    if (clazz.isPrimitive())
    {
      result = true;
    }
    else
    {
      result =  clazz.equals(Boolean.class) || clazz.equals(Byte.class) || 
                clazz.equals(Short.class) || clazz.equals(Character.class) ||
                clazz.equals(Integer.class) || clazz.equals(Long.class) ||
                clazz.equals(Float.class) || clazz.equals(Double.class);
    }
    
    return result;
  }
  
  /* ------------------------------------------------------------------------ */
  /* getOtherForm method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method takes a class representing either a primitive or an object 
   * wrapper. If the class is a primitive type the object wrapper class is 
   * returned. If the class is an object wrapper class the primitive type is
   * returned.
   * 
   * @param clazz
   * @return the class representing the primitive object wrapper.
   */
  private Class<?> getOtherForm(Class<?> clazz)
  {
    Class<?> result = null;
    
    if (clazz.equals(boolean.class)) result = Boolean.class;
    else if (clazz.equals(byte.class)) result = Byte.class;
    else if (clazz.equals(short.class)) result = Short.class;
    else if (clazz.equals(char.class)) result = Character.class;
    else if (clazz.equals(int.class)) result = Integer.class;
    else if (clazz.equals(long.class)) result = Long.class;
    else if (clazz.equals(float.class)) result = Float.class;
    else if (clazz.equals(double.class)) result = Double.class;
    else if (clazz.equals(Boolean.class)) result = boolean.class;
    else if (clazz.equals(Byte.class)) result = byte.class;
    else if (clazz.equals(Short.class)) result = short.class;
    else if (clazz.equals(Character.class)) result = char.class;
    else if (clazz.equals(Integer.class)) result = int.class;
    else if (clazz.equals(Long.class)) result = long.class;
    else if (clazz.equals(Float.class)) result = float.class;
    else if (clazz.equals(Double.class)) result = double.class;
    
    return result;
  }
  
  /* ------------------------------------------------------------------------ */
  /* checkClasses method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method returns true if the two classes are the same or if one is 
   * primitive that the other is a primitive wrapper.
   * 
   * @param type1
   * @param type2
   * @return true if the classes are compatible.
   */
  private boolean checkClasses(Class<?> type1, Class<?> type2)
  {
    boolean result = false;
    
    if ((type1.isPrimitive() && type2.isPrimitive()) ||
        (!type1.isPrimitive() && !type2.isPrimitive()))
    {
      result = type1.equals(type2);
    }
    else
    {
      result =  (type1.equals(boolean.class)  && type2.equals(Boolean.class))   ||
                (type1.equals(byte.class)     && type2.equals(Byte.class))      ||
                (type1.equals(short.class)    && type2.equals(Short.class))     ||
                (type1.equals(char.class)     && type2.equals(Character.class)) ||
                (type1.equals(int.class)      && type2.equals(Integer.class))   ||
                (type1.equals(long.class)     && type2.equals(Long.class))      ||
                (type1.equals(float.class)    && type2.equals(Float.class))     ||
                (type1.equals(double.class)   && type2.equals(Double.class))    ||
                (type2.equals(boolean.class)  && type1.equals(Boolean.class))   ||
                (type2.equals(byte.class)     && type1.equals(Byte.class))      ||
                (type2.equals(short.class)    && type1.equals(Short.class))     ||
                (type2.equals(char.class)     && type1.equals(Character.class)) ||
                (type2.equals(int.class)      && type1.equals(Integer.class))   ||
                (type2.equals(long.class)     && type1.equals(Long.class))      ||
                (type2.equals(float.class)    && type1.equals(Float.class))     ||
                (type2.equals(double.class)   && type1.equals(Double.class));
    }
    
    return result;
  }

  /* ------------------------------------------------------------------------ */
  /* getMethodCallsAsString method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method builds a String that contains the method calls that have been
   * made on this skeleton. It puts each call on a separate line.
   * 
   * @return the string representation of the method call.
   */
  private String getMethodCallsAsString()
  {
    StringBuilder builder = new StringBuilder();
    
    for (MethodCall call : _methodCalls)
    {
      builder.append(call);
      builder.append("\r\n");
    }
    
    return builder.toString();
  }
  
}
