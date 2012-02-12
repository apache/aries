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
 * This provides helper methods to allow a ProxyWeavingController to make
 * sensible decisions without needing to know how the ProxyManager has implemented
 * the weaving support.
 * 
 * @noimplement
 */
public interface WeavingHelper
{
  /** 
   * Tests to see if the provided class has been woven for proxying.
   * 
   * @param c the class to test
   * @return true if it is woven, false otherwise.
   */
  public boolean isWoven(Class<?> c);
  /**
   * Tests to see if the super class of the provided WovenClass has
   * been woven to support proxying.
   * 
   * @param wovenClass the class whose parent should be tested.
   * @return true if it is woven, false otherwise.
   */
  public boolean isSuperClassWoven(WovenClass wovenClass);
}