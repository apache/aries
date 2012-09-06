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
package beans.integration.impl;

import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.transaction.TransactionSynchronizationRegistry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import beans.integration.Tx;

@Singleton
public class TxSingleton implements Tx {

  private TransactionSynchronizationRegistry getTSR() {
    BundleContext ctx = FrameworkUtil.getBundle(TxSingleton.class).getBundleContext();
    return (TransactionSynchronizationRegistry) ctx.getService(
        ctx.getServiceReference("javax.transaction.TransactionSynchronizationRegistry"));
  }
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public Object getNoTransactionId() throws Exception {
    return getTSR().getTransactionKey();
  }
  
  @TransactionAttribute(TransactionAttributeType.SUPPORTS)
  public Object getMaybeTransactionId() throws Exception {
    return getTSR().getTransactionKey();
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public Object getTransactionId() throws Exception {
    return getTSR().getTransactionKey();
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Object getNewTransactionId() throws Exception {
    return getTSR().getTransactionKey();
  }
}
