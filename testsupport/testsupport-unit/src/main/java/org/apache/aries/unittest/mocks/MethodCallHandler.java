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
 * Implementations of this interface perform function when a method is called. The
 * handler is provided details of the method called along with the skeleton used
 * for the call.
 */
public interface MethodCallHandler
{
  /**
   * @param methodCall the method that was called
   * @param parent     the skeleton it was called on
   * @return           an object to be returned (optional)
   * @throws Exception an exception in case of failure.
   */
  public Object handle(MethodCall methodCall, Skeleton parent) throws Exception;
}
