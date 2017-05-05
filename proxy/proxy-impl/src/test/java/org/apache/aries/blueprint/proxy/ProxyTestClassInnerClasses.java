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
package org.apache.aries.blueprint.proxy;


public class ProxyTestClassInnerClasses {

  public static class ProxyTestClassStaticInner {
    public String sayHello() {
      return "Hello";
    }
  }
  
  public class ProxyTestClassInner {
    
    public String sayGoodbye() {
      return "Goodbye";
    }
  }
  
  public class ProxyTestClassUnweavableInnerParent {
    
    public ProxyTestClassUnweavableInnerParent(int i) {
      
    }
    
    public String wave() {
      return "Wave";
    }
  }
  
  public class ProxyTestClassUnweavableInnerChild extends 
       ProxyTestClassUnweavableInnerParent {
    
      public ProxyTestClassUnweavableInnerChild() {
        super(1);
      }
      public ProxyTestClassUnweavableInnerChild(int i) {
        super(i);
      }

      public String leave() {
        return "Gone";
      }
  }
}
