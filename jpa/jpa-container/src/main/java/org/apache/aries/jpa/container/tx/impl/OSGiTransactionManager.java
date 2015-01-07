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
package org.apache.aries.jpa.container.tx.impl;

import java.util.concurrent.atomic.AtomicReference;

import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.aries.jpa.container.impl.NLS;
import org.apache.aries.util.tracker.SingleServiceTracker;
import org.apache.aries.util.tracker.SingleServiceTracker.SingleServiceListener;
import org.osgi.framework.BundleContext;

/**
 *  Used to avoid a dependency on javax.transaction.TransactionManager 
 */
public class OSGiTransactionManager implements SingleServiceListener {

  private static final AtomicReference<OSGiTransactionManager> INSTANCE = 
    new AtomicReference<OSGiTransactionManager>();
  
  private final SingleServiceTracker<TransactionManager> tracker;
  
  @SuppressWarnings("unchecked")
  public static void init(BundleContext ctx) {
    
    try {
      Class<TransactionManager> txMgrClass = (Class<TransactionManager>) Class.forName(
          "javax.transaction.TransactionManager", false, 
          OSGiTransactionManager.class.getClassLoader());
    
      OSGiTransactionManager otm = new OSGiTransactionManager(txMgrClass, ctx);
      if(!!!INSTANCE.compareAndSet(null, otm))
        otm.destroy();
    } catch (ClassNotFoundException cnfe) {
      //No op
    }
  }
  
  public static OSGiTransactionManager get() {
    return INSTANCE.get();
  }
  
  private OSGiTransactionManager(Class<TransactionManager> txMgrClass, BundleContext ctx) {
      tracker = new SingleServiceTracker<TransactionManager>(ctx, txMgrClass, this);
      tracker.open();
  }
  
  private TransactionManager getTransactionManager() {
    TransactionManager txMgr = tracker.getService();
    
    if(txMgr == null)
      throw new IllegalStateException(NLS.MESSAGES.getMessage("unable.to.get.tx.mgr"));
    
    return txMgr;
  }
  
  public void destroy() {
    tracker.close();
  }
  
  public int getStatus() throws SystemException {
    return getTransactionManager().getStatus();
  }

  public void resume(Transaction arg0) throws IllegalStateException,
      InvalidTransactionException, SystemException {
    getTransactionManager().resume(arg0);
  }

  public void setRollbackOnly() throws IllegalStateException, SystemException {
    getTransactionManager().setRollbackOnly();
  }

  public Object getTransaction() throws SystemException {
    // TODO Auto-generated method stub
    return getTransactionManager().getTransaction();
  }

  @Override
  public void serviceFound() {
    // TODO Auto-generated method stub
    
  }


  @Override
  public void serviceLost() {
    // TODO Auto-generated method stub
    
  }


  @Override
  public void serviceReplaced() {
    // TODO Auto-generated method stub
    
  }
}
