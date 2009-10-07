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

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>This class represents a method call that has been or is expected to be 
 *   made. It encapsulates the class that the call was made on, the method
 *   that was invoked and the arguments passed.</p>
 */
public final class MethodCall
{
  /** An empty object array */
  private static Object[] EMPTY_OBJECT_ARRAY = new Object[0];
  /** The name of the class invoked */
  private String _className;
  /** The array of interfaces implemented by the class */
  private Class<?>[] _interfaces = new Class[0];
  /** The method invoked */
  private String _methodName;
  /** The arguments passed */
  private Object[] _arguments = EMPTY_OBJECT_ARRAY;
  /** The object invoked */
  private Object _invokedObject;
  /** A list of comparators to use, instead of the objects .equals methods */
  private static Map<Class<?>, Comparator<?>> equalsHelpers = new HashMap<Class<?>, Comparator<?>>();

  /* ------------------------------------------------------------------------ */
  /* MethodCall method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This constructor allows a MethodCall to be created when the class can be
   * located statically, rather than dynamically.
   * 
   * @param clazz      The class.
   * @param methodName The method name.
   * @param arguments  The arguments.
   */
  public MethodCall(Class<?> clazz, String methodName, Object ... arguments)
  {
    _className = clazz.getName();
    _methodName = methodName;
    _arguments = arguments;
  }

  /* ------------------------------------------------------------------------ */
  /* MethodCall method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method is used by the Skeleton in order create an instance of a 
   * MethodCall representing an invoked interface.
   * 
   * NOTE: If possible changing this so the constructor does not need to be
   * default visibility would be good, given the problems with default 
   * visibility. 
   * 
   * @param invokedObject The object that was invoked.
   * @param methodName    The name of the method invoked.
   * @param arguments     The arguments passed.
   */
  MethodCall(Object invokedObject, String methodName, Object ... arguments)
  {
    _className = invokedObject.getClass().getName();
    _interfaces = invokedObject.getClass().getInterfaces();
    _methodName = methodName;

    this._arguments = (arguments == null) ? EMPTY_OBJECT_ARRAY : arguments;
    
    _invokedObject = invokedObject;
  }

  /* ------------------------------------------------------------------------ */
  /* getArguments method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method returns the arguments.
   * 
   * @return The arguments.
   */
  public Object[] getArguments()
  {
    return _arguments;
  }

  /* ------------------------------------------------------------------------ */
  /* getClassName method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * Returns the name of the class the method was invoked or was defined on.
   * 
   * @return the classname.
   */
  public String getClassName()
  {
    return _className;
  }

  /* ------------------------------------------------------------------------ */
  /* getMethodName method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * Returns the name of the method that was (or will be) invoked.
   * 
   * @return the method name
   */
  public String getMethodName()
  {
    return _methodName;
  }

  /* ------------------------------------------------------------------------ */
  /* checkClassName method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method checks that the class names specified in the method call are
   * compatible, i.e. one is a superclass of the other.
   * 
   * @param one The first method call.
   * @param two The second method call.
   * @return    true if the classes can be assigned to each other.
   */
  private boolean checkClassName(MethodCall one, MethodCall two)
  {
    // TODO make this stuff work better.
    if (one._className.equals("java.lang.Object"))
    {
      return true;
    }
    else if (two._className.equals("java.lang.Object"))
    {
      return true;
    }
    else if (one._className.equals(two._className))
    {
      return true;
    }
    else
    {
      // check the other class name is one of the implemented interfaces
      boolean result = false;
        
      for (int i = 0; i < two._interfaces.length; i++)
      {
        if (two._interfaces[i].getName().equals(one._className))
        {
          result = true;
          break;
        }
      }
        
      if (!result)
      {
        for (int i = 0; i < one._interfaces.length; i++)
        {
          if (one._interfaces[i].getName().equals(two._className))
          {
            result = true;
            break;
          }
        }
      }
        
      return result;
    }
  }
  
  /* ------------------------------------------------------------------------ */
  /* equals method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * Returns true if and only if the two object represent the same call.
   * 
   * @param obj The object to be compared.
   * @return    true if the specified object is the same as this.
   */
  @Override
  public boolean equals(Object obj)
  {

    if (obj == null) return false;

    if (obj == this) return true;
    
    if (obj instanceof MethodCall)
    {
      MethodCall other = (MethodCall)obj;

      if (!checkClassName(this, other)) 
      {
        return false;
      }
      
      if (!other._methodName.equals(this._methodName)) return false;
      if (other._arguments.length != this._arguments.length) return false;
      
      for (int i = 0; i < this._arguments.length; i++)
      {
        boolean thisArgNull = this._arguments[i] == null;
        boolean otherArgClazz = other._arguments[i] instanceof Class;
        boolean otherArgNull = other._arguments[i] == null;
        boolean thisArgClazz = this._arguments[i] instanceof Class;
        
        if (thisArgNull)
        {
          if (otherArgNull)
          {
            // This is OK
          }
          else if (otherArgClazz)
          {
            // This is also OK
          }
          else
          {
            return false;
          }
          // this argument is OK.
        }
        else if (otherArgNull)
        {
          if (thisArgClazz)
          {
            // This is OK
          }
          else
          {
            return false;
          }
          // this argument is OK.
        }
        else if (otherArgClazz)
        {
          if (thisArgClazz)
          {
            Class<?> otherArgClass = (Class<?>) other._arguments[i];
            Class<?> thisArgClass = (Class<?>) this._arguments[i];
          
            if (otherArgClass.equals(Class.class) || thisArgClass.equals(Class.class))
            {
              // do nothing
            } else if (!(otherArgClass.isAssignableFrom(thisArgClass) ||
                thisArgClass.isAssignableFrom(otherArgClass)))
            {
              return false;
            }
          }
          else
          {
            Class<?> clazz = (Class<?>)other._arguments[i];
            if (clazz.isPrimitive())
            {
              if (clazz.equals(byte.class))
              {
                return this._arguments[i].getClass().equals(Byte.class);
              }
              else if (clazz.equals(boolean.class))
              {
                return this._arguments[i].getClass().equals(Boolean.class);
              }
              else if (clazz.equals(short.class))
              {
                return this._arguments[i].getClass().equals(Short.class);
              }
              else if (clazz.equals(char.class))
              {
                return this._arguments[i].getClass().equals(Character.class);
              }
              else if (clazz.equals(int.class))
              {
                return this._arguments[i].getClass().equals(Integer.class);
              }
              else if (clazz.equals(long.class))
              {
                return this._arguments[i].getClass().equals(Long.class);
              }
              else if (clazz.equals(float.class))
              {
                return this._arguments[i].getClass().equals(Float.class);
              }
              else if (clazz.equals(double.class))
              {
                return this._arguments[i].getClass().equals(Double.class);
              }
            }
            else
            {
              if (!clazz.isInstance(this._arguments[i]))
              {
                return false;
              }
            }
          }
        }
        else if (thisArgClazz)
        {
          Class<?> clazz = (Class<?>)this._arguments[i];
          if (clazz.isPrimitive())
          {
            if (clazz.equals(byte.class))
            {
              return other._arguments[i].getClass().equals(Byte.class);
            }
            else if (clazz.equals(boolean.class))
            {
              return other._arguments[i].getClass().equals(Boolean.class);
            }
            else if (clazz.equals(short.class))
            {
              return other._arguments[i].getClass().equals(Short.class);
            }
            else if (clazz.equals(char.class))
            {
              return other._arguments[i].getClass().equals(Character.class);
            }
            else if (clazz.equals(int.class))
            {
              return other._arguments[i].getClass().equals(Integer.class);
            }
            else if (clazz.equals(long.class))
            {
              return other._arguments[i].getClass().equals(Long.class);
            }
            else if (clazz.equals(float.class))
            {
              return other._arguments[i].getClass().equals(Float.class);
            }
            else if (clazz.equals(double.class))
            {
              return other._arguments[i].getClass().equals(Double.class);
            }
          }
          else
          {
            if (!clazz.isInstance(other._arguments[i]))
            {
              return false;
            }
          }
        }
        else if (this._arguments[i] instanceof Object[] && other._arguments[i] instanceof Object[])
        {
          return equals((Object[])this._arguments[i], (Object[])other._arguments[i]);
        }
        else 
        {
          int result = compareUsingComparators(this._arguments[i], other._arguments[i]);
          
          if (result == 0) continue;
          else if (result == 1) return false;
          else if (!!!this._arguments[i].equals(other._arguments[i])) return false;
        }
      }
    }

    return true;
  }

  /**
   * Compare two arrays calling out to the custom comparators and handling
   * AtomicIntegers nicely.
   * 
   * TODO remove the special casing for AtomicInteger.
   * 
   * @param arr1 
   * @param arr2
   * @return true if the arrays are equals, false otherwise.
   */
  private boolean equals(Object[] arr1, Object[] arr2)
  {
    if (arr1.length != arr2.length) return false;
    
    for (int k = 0; k < arr1.length; k++) {
      if (arr1[k] == arr2[k]) continue;
      if (arr1[k] == null && arr2[k] != null) return false;
      if (arr1[k] != null && arr2[k] == null) return false;
      
      int result = compareUsingComparators(arr1[k], arr2[k]);
      
      if (result == 0) continue;
      else if (result == 1) return false;
      
      if (arr1[k] instanceof AtomicInteger && arr2[k] instanceof AtomicInteger && 
          ((AtomicInteger)arr1[k]).intValue() == ((AtomicInteger)arr2[k]).intValue()) 
        continue;

      if (!!!arr1[k].equals(arr2[k])) return false;

    }
    
    return true;
  }
  
  /**
   * Attempt to do the comparison using the comparators. This logic returns:
   * 
   * <ul>
   *   <li>0 if they are equal</li>
   *   <li>1 if they are not equal</li>
   *   <li>-1 no comparison was run</li>
   * </ul>
   * 
   * @param o1 The first object.
   * @param o2 The second object.
   * @return 0, 1 or -1 depending on whether the objects were equal, not equal or no comparason was run.
   */
  private int compareUsingComparators(Object o1, Object o2)
  {
    if (o1.getClass() == o2.getClass()) {
      @SuppressWarnings("unchecked")
      Comparator<Object> compare = (Comparator<Object>) equalsHelpers.get(o1.getClass());
      
      if (compare != null) {
        if (compare.compare(o1, o2) == 0) return 0;
        else return 1;
      }
    }
    
    return -1;
  }
  
  /* ------------------------------------------------------------------------ */
  /* hashCode method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * Returns the hashCode (obtained by returning the hashCode of the 
   * methodName).
   * 
   * @return The hashCode
   */
  @Override
  public int hashCode()
  {
    return _methodName.hashCode();
  }

  /* ------------------------------------------------------------------------ */
  /* toString method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * Returns a string representation of the method call.
   * 
   * @return string representation.
   */
  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append(this._className);
    buffer.append('.');
    buffer.append(this._methodName);
    buffer.append("(");

    for (int i = 0; i < this._arguments.length; i++)
    {
      if (this._arguments[i] != null)
      {
        if (this._arguments[i] instanceof Class)
        {
          buffer.append(((Class<?>)this._arguments[i]).getName());
        }
        else if (Proxy.isProxyClass(this._arguments[i].getClass()))
        {
          // If the object is a dynamic proxy, just use the proxy class name to avoid calling toString on the proxy
          buffer.append(this._arguments[i].getClass().getName());
        }
        else if (this._arguments[i] instanceof Object[])
        {
          buffer.append(Arrays.toString((Object[])this._arguments[i]));
        }
        else
        {  
          buffer.append(String.valueOf(this._arguments[i]));
        }
      } 
      else
      {  
        buffer.append("null");
      }

      if (i + 1 < this._arguments.length)
        buffer.append(", ");
    }

    buffer.append(")");
    String string = buffer.toString();
    return string;
  }

  /* ------------------------------------------------------------------------ */
  /* getInterfaces method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method returns the list of interfaces implemented by the class that
   * was called.
   * 
   * @return Returns the interfaces.
   */
  public Class<?>[] getInterfaces()
  {
    return this._interfaces;
  }
  
  /* ------------------------------------------------------------------------ */
  /* getInvokedObject method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method returns the invoked object.
   * 
   * @return The object that was invoked or null if an expected call.
   */
  public Object getInvokedObject()
  {
    return _invokedObject;
  }
  
  /* ------------------------------------------------------------------------ */
  /* registerEqualsHelper method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * The native equals for an object may not provide the behaviour required by
   * the tests. As an example AtomicInteger does not define a .equals, but tests
   * may wish to compare it being passed in a method call for equality. This
   * method allows a Comparator to be specified for any type and the Comparator
   * will be used to determine equality in place of the .equals method.
   * 
   * <p>The Comparator must not throw exceptions, and must return 0 for equality
   *   or any other integer for inequality.
   * </p>
   * 
   * @param <T>        the type of the class and comparator.
   * @param type       the type of the class for which the comparator will be called.
   * @param comparator the comparator to call.
   */
  public static <T> void registerEqualsHelper(Class<T> type, Comparator<T> comparator)
  {
    equalsHelpers.put(type, comparator);
  }
  
  /* ------------------------------------------------------------------------ */
  /* removeEqualsHelper method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method removes any registered comparator specified for the given type.
   * 
   * @param type the type to remove the comparator from.
   */
  public static void removeEqualsHelper(Class<?> type)
  {
    equalsHelpers.remove(type);
  }
}