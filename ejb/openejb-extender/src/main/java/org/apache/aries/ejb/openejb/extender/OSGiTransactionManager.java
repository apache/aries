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

import java.util.concurrent.atomic.AtomicReference;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.apache.aries.util.tracker.SingleServiceTracker;
import org.apache.aries.util.tracker.SingleServiceTracker.SingleServiceListener;
import org.osgi.framework.BundleContext;

public class OSGiTransactionManager implements TransactionManager,
    TransactionSynchronizationRegistry, SingleServiceListener {

  private static class NoTransactionManagerException extends SystemException { 
    public NoTransactionManagerException() {
      super("No Transaction Manager is available");
    }
  }
  
  private static class NoTransactionSynchronizationRegistryException extends RuntimeException { 
    public NoTransactionSynchronizationRegistryException() {
      super("No Transaction Synchronization Registry is available");
    }
  }
  
  private final SingleServiceTracker<TransactionManager> tmTracker;
  private final SingleServiceTracker<TransactionSynchronizationRegistry> tsrTracker;
  
  private final AtomicReference<TransactionManager> tm = 
    new AtomicReference<TransactionManager>();
  
  private final AtomicReference<TransactionSynchronizationRegistry> tsr = 
    new AtomicReference<TransactionSynchronizationRegistry>();
  
  private static final AtomicReference<OSGiTransactionManager> INSTANCE =
    new AtomicReference<OSGiTransactionManager>();
  
  private OSGiTransactionManager(BundleContext ctx) {
    tmTracker = new SingleServiceTracker<TransactionManager>(ctx, TransactionManager.class, this);
    tsrTracker = new SingleServiceTracker<TransactionSynchronizationRegistry>(ctx, 
        TransactionSynchronizationRegistry.class, this);
    
    tmTracker.open();
    tsrTracker.open();
  }
  
  private final TransactionManager getTM() throws SystemException {
    TransactionManager tManager = tm.get();
    
    if(tManager == null) {
      throw new NoTransactionManagerException();
    }
    return tManager;
  }
  
  private final TransactionSynchronizationRegistry getTSR() {
    TransactionSynchronizationRegistry tSReg = tsr.get();
    
    if(tSReg == null) {
      throw new NoTransactionSynchronizationRegistryException();
    }
    return tSReg;
  }
  
  public static OSGiTransactionManager get() {
    return INSTANCE.get();
  }
  
  public static void init(BundleContext ctx) {
    OSGiTransactionManager oTM = new OSGiTransactionManager(ctx);
    if(!!!INSTANCE.compareAndSet(null, oTM))
      oTM.destroy();
  }
  
  public void destroy() {
    tmTracker.close();
    tsrTracker.close();
  }
  
  public void serviceFound() {
    update();
  }

  public void serviceLost() {
    update();
  }

  public void serviceReplaced() {
    update();
  }
  
  private void update() {
    tm.set(tmTracker.getService());
    tsr.set(tsrTracker.getService());
  }
  
  public void begin() throws NotSupportedException, SystemException {
    getTM().begin();
  }
  
  public void commit() throws HeuristicMixedException,
      HeuristicRollbackException, IllegalStateException, RollbackException,
      SecurityException, SystemException {
    getTM().commit();
  }
  
  public int getStatus() throws SystemException {
    return getTM().getStatus();
  }
  
  public Transaction getTransaction() throws SystemException {
    return getTM().getTransaction();
  }
  
  public void resume(Transaction arg0) throws IllegalStateException,
      InvalidTransactionException, SystemException {
    getTM().resume(arg0);
  }
  
  public void rollback() throws IllegalStateException, SecurityException,
      SystemException {
    getTM().rollback();
  }
  
  public void setRollbackOnly() throws IllegalStateException {
    getTSR().setRollbackOnly();
  }
  
  public void setTransactionTimeout(int arg0) throws SystemException {
    getTM().setTransactionTimeout(arg0);
  }
  
  public Transaction suspend() throws SystemException {
    return getTM().suspend();
  }
  
  public Object getResource(Object arg0) {
    return getTSR().getResource(arg0);
  }
  
  public boolean getRollbackOnly() {
    return getTSR().getRollbackOnly();
  }
  
  public Object getTransactionKey() {
    return getTSR().getTransactionKey();
  }
  
  public int getTransactionStatus() {
    return getTSR().getTransactionStatus();
  }
  
  public void putResource(Object arg0, Object arg1) {
    getTSR().putResource(arg0, arg1);
  }
  
  public void registerInterposedSynchronization(Synchronization arg0) {
    getTSR().registerInterposedSynchronization(arg0);
  }
}


