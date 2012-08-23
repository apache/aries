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
package org.apache.aries.proxy.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.aries.proxy.ProxyManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ProxyManagerActivator implements BundleActivator 
{
  private static final boolean ASM_PROXY_SUPPORTED;
  private AbstractProxyManager managerService;
  
  static
  {
    boolean classProxy = false;
    try {
      // Try load load a asm class (to make sure it's actually available
      // then create the asm factory
      Class.forName("org.objectweb.asm.ClassVisitor");
      classProxy = true;
    } catch (Throwable t) {
    }
    
    ASM_PROXY_SUPPORTED = classProxy;
  }
  
  public void start(BundleContext context)
  {
    if (ASM_PROXY_SUPPORTED) {
      managerService = new AsmProxyManager();
      
      try {
        //if ASM is available then we should also try weaving
        Class<?> cls = Class.forName("org.apache.aries.proxy.impl.weaving.ProxyWeavingHook");
        Dictionary<String,String> props = new Hashtable<String,String>();
        // SubsystemResource.java also uses this constant. 
        //   While it could be turned into a static final constant, note that this
        //   is also a non-standard workaround in the absence of a solution in the spec. 
        // See the associated OSGi spec bug. 
        props.put("osgi.woven.packages", "org.apache.aries.proxy.weaving,org.apache.aries.proxy");
        context.registerService("org.osgi.framework.hooks.weaving.WeavingHook",
            cls.getConstructor(BundleContext.class).newInstance(context), props);
      } catch (Throwable t) {
        //We don't care about this, we just won't have registered the hook
      }
      
    } else {
      managerService = new JdkProxyManager();
    }
    
    context.registerService(ProxyManager.class.getName(), managerService, null);
  }

  public void stop(BundleContext context)
  {
  }
}