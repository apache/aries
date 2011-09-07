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

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.aries.jpa.container.context.JTAPersistenceContextManager;
import org.apache.aries.util.tracker.SingleServiceTracker;
import org.apache.aries.util.tracker.SingleServiceTracker.SingleServiceListener;
import org.apache.openejb.persistence.EntityManagerTxKey;
import org.apache.openejb.persistence.JtaEntityManagerRegistry;
import org.osgi.framework.BundleContext;

public class AriesPersistenceContextIntegration extends
    JtaEntityManagerRegistry {
  
  private static final AtomicReference<AriesPersistenceContextIntegration> INSTANCE =
    new AtomicReference<AriesPersistenceContextIntegration>();
  
  private final SingleServiceTracker<JTAPersistenceContextManager> ariesJTARegistry;
  
  private AriesPersistenceContextIntegration(BundleContext ctx) {
    super(OSGiTransactionManager.get());
    ariesJTARegistry = new SingleServiceTracker<JTAPersistenceContextManager>
        (ctx, JTAPersistenceContextManager.class, new DummySingleServiceListener());
    ariesJTARegistry.open();
  }
  
  public static void init(BundleContext ctx) {
    AriesPersistenceContextIntegration apci = new AriesPersistenceContextIntegration(ctx);
    if(!!!INSTANCE.compareAndSet(null, apci))
      apci.destroy();
  }
  
  public static AriesPersistenceContextIntegration get() {
    return INSTANCE.get();
  }
  
  public void destroy() {
    INSTANCE.set(null);
    ariesJTARegistry.close();
  }
  
  @Override
  public EntityManager getEntityManager(EntityManagerFactory emf, Map props,
      boolean extended, String unitName) throws IllegalStateException {

    if(!!!isTransactionActive())
      return super.getEntityManager(emf, props, extended, unitName);
    
    JTAPersistenceContextManager mgr = ariesJTARegistry.getService();
    
    if(mgr == null)
      throw new IllegalStateException("No JTAPersistenceContextManager service available");
    
    //Check if we, or OpenEJB, already have a context
    EntityManager ariesEM = mgr.getExistingPersistenceContext(emf);
    EntityManager openEjbEM = (EntityManager) OSGiTransactionManager.get().
                                  getResource(new EntityManagerTxKey(emf));
    
    if(ariesEM == null) {
      if(openEjbEM == null) {
        //If both are null then it's easier to let OpenEJB win and push the PC into Aries
        openEjbEM = super.getEntityManager(emf, props, extended, unitName);
      }
      mgr.manageExistingPersistenceContext(emf, openEjbEM);
      ariesEM = openEjbEM;
    } else {
      //We have an Aries EM, if OpenEJB doesn't then sort it out, if it does they should be the same
      if(openEjbEM == null){
        if(extended) {
          throw new IllegalStateException("We already have an active TX scope PersistenceContext, so we can't" +
            "create an extended one");
        } else {
          OSGiTransactionManager.get().putResource(new EntityManagerTxKey(emf), ariesEM);
          openEjbEM = ariesEM;
        }
      } else {
        //If both non null and not equal then something bad has happened
        if(openEjbEM != ariesEM) {
          throw new IllegalStateException("OpenEJB has been cheating. They have a different EntityManager to Aries");
        }
      }
    }
    
    //We could return either ariesEM or openEjbEM at this point
    return ariesEM;
  }

  private static final class DummySingleServiceListener implements SingleServiceListener {

    public void serviceFound() {}

    public void serviceLost() {}

    public void serviceReplaced() {}
  }
}
