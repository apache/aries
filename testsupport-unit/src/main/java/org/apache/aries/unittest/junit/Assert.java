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
package org.apache.aries.unittest.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A library of useful assertion routines.
 */
public class Assert
{
  /**
   * This method checks that the two objects have the same hashCode. If the
   * equalsObjects parameter is true then the objects must also be .equals equal
   * if the equalsObject parameter is false then they must not be .equals equal.
   * 
   * @param <T> 
   * @param obj  the first object.
   * @param obj2 the second object.
   * @param equalObjects whether the objects are also equal.
   */
  public static <T> void assertHashCodeEquals(T obj, T obj2, boolean equalObjects)
  {
    assertEquals("The hashCodes were different, bad, bad, bad", obj.hashCode(), obj2.hashCode());
    assertEquals("The two objects are not equal", equalObjects, obj.equals(obj2));
  }

  /**
   * This method makes sure that the hashCodes are not equal. And that they
   * are not .equals. This is because two objects of the same type cannot be
   * .equals if they have different hashCodes.
   * 
   * @param <T> 
   * @param obj
   * @param obj2
   */
  public static <T> void assertHashCodeNotEquals(T obj, T obj2)
  {
    assertFalse("The the two hashCodes should be different: " + obj.hashCode() + ", " + obj2.hashCode(), obj.hashCode() == obj2.hashCode());
    assertFalse("The two objects not equal", obj.equals(obj2));
  }
  
  /**
   * This method asserts that the equals contract is upheld by type T.
   * 
   * @param <T>
   * @param info    an instance of T
   * @param info2   a different instance of T that is .equal to info
   * @param info3   an instance of T that is not equal to info
   */
  public static <T> void assertEqualsContract(T info, T info2, T info3)
  {
    Object other = new Object();
    if (info.getClass() == Object.class) other = "A string";
    
    assertEquals(info, info);
    assertFalse(info.equals(null));
    assertTrue("Equals should be commutative", info.equals(info2) == info2.equals(info) && info2.equals(info3) == info3.equals(info2));
    assertTrue("If two objects are equal, then they must both be equal (or not equal) to a third", info.equals(info3) == info2.equals(info3));
    assertFalse("An object should not equal an object of a disparate type", info.equals(other));
  }
}