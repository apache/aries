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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>This class contains some return type handlers that provides some default behavior.</p>
 */
public class DefaultReturnTypeHandlers
{
  /** A handler for Longs */
  public static final ReturnTypeHandler LONG_HANDLER = new ReturnTypeHandler() {
    public Object handle(Class<?> class1, Skeleton parent)
    {
      return Long.valueOf(0);
    }
  };
  /** A handler for Integers */
  public static final ReturnTypeHandler INT_HANDLER = new ReturnTypeHandler() {
    public Object handle(Class<?> class1, Skeleton parent)
    {
      return Integer.valueOf(0);
    }
  };
  /** A handler for Shorts */
  public static final ReturnTypeHandler SHORT_HANDLER = new ReturnTypeHandler() {
    public Object handle(Class<?> class1, Skeleton parent)
    {
      return Short.valueOf((short)0);
    }
  };
  /** A handler for Bytes */
  public static final ReturnTypeHandler BYTE_HANDLER = new ReturnTypeHandler() {
    public Object handle(Class<?> class1, Skeleton parent)
    {
      return Byte.valueOf((byte)0);
    }
  };
  /** A handler for Characters */
  public static final ReturnTypeHandler CHAR_HANDLER = new ReturnTypeHandler() {
    public Object handle(Class<?> class1, Skeleton parent)
    {
      return Character.valueOf(' ');
    }
  };
  /** A handler for Strings */
  public static final ReturnTypeHandler STRING_HANDLER = new ReturnTypeHandler() {
    public Object handle(Class<?> class1, Skeleton parent)
    {
      return "";
    }
  };
  /** A handler for Lists */
  public static final ReturnTypeHandler LIST_HANDLER = new ReturnTypeHandler() {
    public Object handle(Class<?> class1, Skeleton parent)
    {
      return new ArrayList<Object>();
    }
  };
  /** A handler for Maps */
  public static final ReturnTypeHandler MAP_HANDLER = new ReturnTypeHandler() {
    public Object handle(Class<?> class1, Skeleton parent)
    {
      return new HashMap<Object,Object>();
    }
  };
  /** A handler for Setss */
  public static final ReturnTypeHandler SET_HANDLER = new ReturnTypeHandler() {
    public Object handle(Class<?> class1, Skeleton parent)
    {
      return new HashSet<Object>();
    }
  };
  /** A handler for Floats */
  public static final ReturnTypeHandler FLOAT_HANDLER = new ReturnTypeHandler() {
    public Object handle(Class<?> class1, Skeleton parent)
    {
      return new Float(0.0f);
    }
  };
  /** A handler for Doubles */
  public static final ReturnTypeHandler DOUBLE_HANDLER = new ReturnTypeHandler() {
    public Object handle(Class<?> class1, Skeleton parent)
    {
      return new Double(0.0d);
    }
  };
  /** A handler for Booleans */
  public static final ReturnTypeHandler BOOLEAN_HANDLER = new ReturnTypeHandler() {
    public Object handle(Class<?> class1, Skeleton parent)
    {
      return Boolean.FALSE;
    }
  };
  /**
   * Register all the default handlers against the specified skeleton.
   * 
   * @param s the skeleton
   */
  public static void registerDefaultHandlers(Skeleton s)
  {
    s.registerReturnTypeHandler(Double.class, DOUBLE_HANDLER);
    s.registerReturnTypeHandler(Float.class, FLOAT_HANDLER);
    s.registerReturnTypeHandler(Long.class, LONG_HANDLER);
    s.registerReturnTypeHandler(Integer.class, INT_HANDLER);
    s.registerReturnTypeHandler(Short.class, SHORT_HANDLER);
    s.registerReturnTypeHandler(Byte.class, BYTE_HANDLER);
    s.registerReturnTypeHandler(Boolean.class, BOOLEAN_HANDLER);
    s.registerReturnTypeHandler(Character.class, CHAR_HANDLER);
    s.registerReturnTypeHandler(String.class, STRING_HANDLER);
    s.registerReturnTypeHandler(List.class, LIST_HANDLER);
    s.registerReturnTypeHandler(Map.class, MAP_HANDLER);
    s.registerReturnTypeHandler(Set.class, SET_HANDLER);
    s.registerReturnTypeHandler(double.class, DOUBLE_HANDLER);
    s.registerReturnTypeHandler(float.class, FLOAT_HANDLER);
    s.registerReturnTypeHandler(long.class, LONG_HANDLER);
    s.registerReturnTypeHandler(int.class, INT_HANDLER);
    s.registerReturnTypeHandler(short.class, SHORT_HANDLER);
    s.registerReturnTypeHandler(byte.class, BYTE_HANDLER);
    s.registerReturnTypeHandler(char.class, CHAR_HANDLER);
    s.registerReturnTypeHandler(boolean.class, BOOLEAN_HANDLER);
  }
}