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
package org.apache.aries.blueprint.proxy;

public class ProxyTestClassSuperWithNoDefaultOrProtectedAccess
{
  static {
    System.out.println("The time is: " + System.currentTimeMillis());
  }
  
  public void bMethod()
  {
    aPrivateMethod();
  }

  public void bProMethod()
  {

  }

  public void bDefMethod()
  {

  }

  private void aPrivateMethod()
  {

  }
  
  public Object getTargetObject() {
    return null;
  }
  
  @SuppressWarnings("unused")
private void doTarget() {
    Object o = getTargetObject();
    if(this != o)
      ((ProxyTestClassSuperWithNoDefaultOrProtectedAccess)o).doTarget();
  }

}
