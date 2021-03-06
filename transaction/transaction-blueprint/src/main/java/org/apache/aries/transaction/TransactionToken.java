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
package org.apache.aries.transaction;

import javax.transaction.Transaction;

import org.osgi.service.coordinator.Coordination;

public class TransactionToken
{
   private Transaction activeTransaction;
   private Transaction suspendedTransaction;
   private TransactionAttribute transactionAttribute;
   private boolean isCompletionAllowed;
   private boolean requiresNewCoordination;
   private Coordination coordination;
   
   public TransactionToken(Transaction activeTransaction, Transaction suspendedTransaction,
           TransactionAttribute transactionAttribute)
   {
    this(activeTransaction, suspendedTransaction, transactionAttribute, false);
   }

   TransactionToken(Transaction activeTransaction, Transaction suspendedTransaction,
           TransactionAttribute transactionAttribute, boolean isCompletionAllowed)
   {
       this(activeTransaction, suspendedTransaction, transactionAttribute, isCompletionAllowed, true);
   }

   TransactionToken(Transaction activeTransaction, Transaction suspendedTransaction,
           TransactionAttribute transactionAttribute, boolean isCompletionAllowed,
            boolean requiresNewCoordination)
   {
       this.activeTransaction = activeTransaction;
       this.suspendedTransaction = suspendedTransaction;
       this.transactionAttribute = transactionAttribute;
       this.isCompletionAllowed = isCompletionAllowed;
       this.requiresNewCoordination = requiresNewCoordination;
   }

   public Transaction getActiveTransaction() {
       return activeTransaction;
   }

   public Transaction getSuspendedTransaction() {
       return suspendedTransaction;
   }

   public void setSuspendedTransaction(Transaction suspendedTransaction) {
       this.suspendedTransaction = suspendedTransaction;
   }

   public TransactionAttribute getTransactionAttribute() {
       return transactionAttribute;
   }

   public void setTransactionStrategy(TransactionAttribute transactionAttribute) {
       this.transactionAttribute = transactionAttribute;
   }

   public boolean isCompletionAllowed() {
       return isCompletionAllowed;
   }

   public boolean requiresNewCoordination() {
       return requiresNewCoordination;
   }

   public Coordination getCoordination() {
    return coordination;
   }
   
   public void setCoordination(Coordination coordination) {
    this.coordination = coordination;
   }
}