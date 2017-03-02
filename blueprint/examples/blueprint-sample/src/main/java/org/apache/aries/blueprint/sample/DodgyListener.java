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
package org.apache.aries.blueprint.sample;

import java.util.Set;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class DodgyListener {
  private BundleContext ctx;
  
  public void setBundleContext(BundleContext ctx) {
    this.ctx = ctx;
  }
  
  public void bind(Set a, Map props) {
    System.out.println("Attempting to provoke deadlock");
    
    Thread t = new Thread() {
      public void run() {
    	// we pretend to be another bundle (otherwise we'll deadlock in Equinox itself :(
    	BundleContext otherCtx = ctx.getBundle(0).getBundleContext();  
    	  
        ServiceReference ref = otherCtx.getServiceReference("java.util.List");
        otherCtx.getService(ref);
      }
    };
    t.start();
    
    // let the other thread go first
    try {
      Thread.sleep(100);
    } catch (Exception e) {}
    
    ServiceReference ref = ctx.getServiceReference("java.util.List");
    ctx.getService(ref);
  }
}