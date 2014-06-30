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

import org.apache.openejb.BeanContext.BusinessLocalBeanHome;
import org.apache.openejb.BeanContext.BusinessLocalHome;
import org.apache.openejb.BeanContext.BusinessRemoteHome;
import org.apache.openejb.core.ivm.BaseEjbProxyHandler;
import org.apache.openejb.util.proxy.InvocationHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class EJBServiceFactory implements ServiceFactory<Object> {

  private static enum Type {LOCAL, LOCAL_NO_IFACE, REMOTE;}
  
  private final BusinessLocalBeanHome localBeanHome;
  private final BusinessLocalHome localHome;
  private final BusinessRemoteHome remoteHome;
  
  private final Type type;
  
  public EJBServiceFactory(BusinessLocalBeanHome home) {
    this.localBeanHome = home;
    type = Type.LOCAL_NO_IFACE;
    this.remoteHome = null;
    this.localHome = null;
  }

  public EJBServiceFactory(BusinessLocalHome home) {
    this.localHome = home;
    type = Type.LOCAL;
    this.remoteHome = null;
    this.localBeanHome = null;
  }

  public EJBServiceFactory(BusinessRemoteHome home) {
    this.remoteHome = home;
    type = Type.REMOTE;
    this.localHome = null;
    this.localBeanHome = null;
  }

  public Object getService(Bundle bundle,
      ServiceRegistration<Object> registration) {
    switch(type) {
      case LOCAL :
        return localHome.create();
      case LOCAL_NO_IFACE :
        return localBeanHome.create();
      case REMOTE : {
        InvocationHandler ih = AriesProxyService.get().getInvocationHandler(remoteHome);
        
        if(ih instanceof BaseEjbProxyHandler) {
          ((BaseEjbProxyHandler)ih).setIntraVmCopyMode(false);
        }
        return remoteHome.create();
      }
      default :
        throw new IllegalArgumentException("Unknown EJB type " + type);
    }
  }

  public void ungetService(Bundle bundle,
      ServiceRegistration<Object> registration, Object service) {
  }
}
