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
 * <p>Return type handlers return objects that implement the specified class.</p>
 */
public interface ReturnTypeHandler
{

  /**
   * This method is called when a method call handler has not been registered
   * and an object of a specific type needs to be returned. The handle method
   * is called along with the type that is required.
   * 
   * @param clazz  the class to create an object for
   * @param parent the skeleton requesting the class.
   * @return       an instance of the class, or something that can be assigned to it.
   * @throws Exception if a failure occurs.
   */
  public Object handle(Class<?> clazz, Skeleton parent) throws Exception;
}