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
package org.apache.aries.ejb.openejb.extender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.openejb.AppContext;
import org.osgi.framework.ServiceRegistration;

public class RunningApplication {

  private final AppContext ctx;
  private final Collection<ServiceRegistration<?>> regs = 
    Collections.synchronizedCollection(new ArrayList<ServiceRegistration<?>>());
  
  public RunningApplication(AppContext context) {
    this.ctx = context;
  }

  public AppContext getCtx() {
    return ctx;
  }

  public Collection<ServiceRegistration<?>> getRegs() {
    return regs;
  }
  
  public void addRegs(Collection<ServiceRegistration<?>> toAdd) {
    regs.addAll(toAdd);
  }
}
