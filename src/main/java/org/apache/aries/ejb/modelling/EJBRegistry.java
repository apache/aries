/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.ejb.modelling;

/**
 * A registry of located Session EJBs
 */
public interface EJBRegistry {
  /**
   * Add a view of a session EJB, e.g. a local home, remote business interface etc.
   * 
   * @param ejbName The ejb name
   * @param ejbType The ejb type (e.g. stateless)
   * @param interfaceName The fully qualified Java type name for this view
   * @param remote
   */
  public void addEJBView(String ejbName, String ejbType, String interfaceName,
      boolean remote);
}
