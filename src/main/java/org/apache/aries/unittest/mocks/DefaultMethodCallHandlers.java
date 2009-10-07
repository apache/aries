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

/**
 * <p>This class contains method call handlers for some default method handling.
 * </p>
 *
 * <p>This class provides handlers for the toString, equals and hashCode
 *   methods. They reproduce the default Object implementations for dynamic
 *   mock objects, these can be overridden.
 * </p>
 */
public class DefaultMethodCallHandlers
{
  /** A MethodCall representing the equals method */
  private static MethodCall _equals;
  /** A MethodCall representing the toString method */
  private static MethodCall _toString;
  /** A MethodCall representing the hashCode method */
  private static MethodCall _hashCode;

  /* ------------------------------------------------------------------------ */
  /* static initializer
  /* ------------------------------------------------------------------------ */
  static
  {
    _equals = new MethodCall(Object.class, "equals", new Object[] {Object.class});
    _toString = new MethodCall(Object.class, "toString", new Object[0]);
    _hashCode = new MethodCall(Object.class, "hashCode", new Object[0]);
  }

  /**
   * The Default MethodCallHandler for the equals method, performs an == check.
   */
  public static final MethodCallHandler EQUALS_HANDLER = new MethodCallHandler()
  {
    public Object handle(MethodCall methodCall, Skeleton parent) throws Exception
    {
      Object obj = methodCall.getInvokedObject();
      Object toObj = methodCall.getArguments()[0];
      
      if (toObj == null) return false;
      
      if(parent.getTemplateObject() != null){
        try {
          if(Skeleton.isSkeleton(toObj) &&Skeleton.getSkeleton(toObj).getTemplateObject() != null){
            return parent.getTemplateObject().equals(Skeleton.getSkeleton(toObj).getTemplateObject());
          } else {
            return false;
          }
        } catch (IllegalArgumentException iae) {
          return parent.getTemplateObject().equals(toObj);
        }
      }
        

      return obj == toObj ? Boolean.TRUE : Boolean.FALSE;
    }
  };

  /**
   * The Default MethodCallHandler for the toString method, reproduces
   * <classname>@<hashCode>
   */
  public static final MethodCallHandler TOSTRING_HANDLER = new MethodCallHandler()
  {
    public Object handle(MethodCall methodCall, Skeleton parent) throws Exception
    {
      if(parent.getTemplateObject() != null)
        return parent.getTemplateObject().toString();
      Object obj = methodCall.getInvokedObject();
      return obj.getClass().getName() + "@" + System.identityHashCode(obj);
    }
  };

  /**
   * The Default MethodCallHandler for the hashCode method, returns the
   * identity hashCode.
   */
  public static final MethodCallHandler HASHCODE_HANDLER = new MethodCallHandler()
  {
    public Object handle(MethodCall methodCall, Skeleton parent) throws Exception
    {
      if(parent.getTemplateObject() != null)
        return parent.getTemplateObject().hashCode();
      
      return Integer.valueOf(System.identityHashCode(methodCall.getInvokedObject()));
    }
  };

  /* ------------------------------------------------------------------------ */
  /* registerDefaultHandlers method
  /* ------------------------------------------------------------------------ */
  /**
   * This method registers the DefaultMethodCall Handlers with the specified
   * skeleton.
   *
   * @param s a skeleton
   */
  public static void registerDefaultHandlers(Skeleton s)
  {
    s.registerMethodCallHandler(_equals, EQUALS_HANDLER);
    s.registerMethodCallHandler(_toString, TOSTRING_HANDLER);
    s.registerMethodCallHandler(_hashCode, HASHCODE_HANDLER);
  }

  /* ------------------------------------------------------------------------ */
  /* isDefaultMethodCall method
  /* ------------------------------------------------------------------------ */
  /**
   * This method returns true if and only if the specified call represents a
   * default method call.
   *
   * @param call the call
   * @return see above.
   */
  public static boolean isDefaultMethodCall(MethodCall call)
  {
    return _toString.equals(call) || _equals.equals(call) || _hashCode.equals(call);
  }
}
