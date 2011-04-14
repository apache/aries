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
package org.apache.aries.proxy.weaving;

import java.util.concurrent.Callable;

import org.apache.aries.proxy.InvocationListener;

public interface WovenProxy {
  
  /**
   * @return true if this instance has a non null dispatcher or listener
   */
  public boolean org_apache_aries_proxy_weaving_WovenProxy_isProxyInstance();
  
  /**
   * @return the dispatcher, or null if no dispatcher is set
   */
  public Callable<Object> org_apache_aries_proxy_weaving_WovenProxy_unwrap();
  
  /**
   * @return A new proxy instance that can be used for delegation. Note that this object should
   *         not be used without setting a dispatcher!
   */
  public WovenProxy org_apache_aries_proxy_weaving_WovenProxy_createNewProxyInstance(
      Callable<Object> dispatcher, InvocationListener listener);
}