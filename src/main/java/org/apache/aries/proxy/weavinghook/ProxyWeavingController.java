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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.proxy.weavinghook;

import org.osgi.framework.hooks.weaving.WovenClass;

/**
 * Services of this interface are used by the ProxyManager's weaving implementation to
 * decide if a specific bundle should be subject to weaving. 
 * 
 * <p>If multiple ProxyWeavingController are registered all will be consulted to decide 
 *   whether to weave or not. As soon as one service says to weave a class then
 *   it will be woven and following services may not be consulted.
 * </p>
 */
public interface ProxyWeavingController
{
  /**
   * Returns true if the class should be subject to proxy weaving. If it returns
   * false then the class will not be weaved. The result of this method is immutable
   * for a given bundle. That means repeated calls given the same bundle MUST 
   * return the same response.
   * 
   * @param classToWeave the class that is a candidate to be weaved.
   * @param helper       a helper calss to allow the implementation to make intelligent weaving decisions.
   * @return true if it should be woven, false otherwise.
   */
  public boolean shouldWeave(WovenClass classToWeave, WeavingHelper helper);
}