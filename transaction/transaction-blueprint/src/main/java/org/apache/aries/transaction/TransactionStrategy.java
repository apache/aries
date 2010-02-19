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

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

public enum TransactionStrategy {
    MANDATORY
    {
      public Transaction begin(TransactionManager man) throws SystemException
      {
        if (man.getStatus() == Status.STATUS_NO_TRANSACTION) {
          throw new IllegalStateException("No tran on thread");
        }

        return null;
      }
    },
    NEVER
    {
      public Transaction begin(TransactionManager man) throws SystemException
      {
        if (man.getStatus() == Status.STATUS_ACTIVE) {
          throw new IllegalStateException("Tran on thread");
        }

        return null;
      }
    },
    NOT_SUPPORTED
    {
      public Transaction begin(TransactionManager man) throws SystemException
      {
        if (man.getStatus() == Status.STATUS_ACTIVE) {
          return man.suspend();
        }

        return null;
      }

      public void finish(TransactionManager man, Transaction tran) throws SystemException,
          InvalidTransactionException, IllegalStateException
      {
        if (tran != null) {
          man.resume(tran);
        }
      }
    },
    REQUIRED
    {
      public Transaction begin(TransactionManager man) throws SystemException, NotSupportedException
      {
        if (man.getStatus() == Status.STATUS_NO_TRANSACTION) {
          man.begin();
          return man.getTransaction();
        }

        return null;
      }

      public void finish(TransactionManager man, Transaction tran) throws SystemException,
          InvalidTransactionException, IllegalStateException, SecurityException, RollbackException,
          HeuristicMixedException, HeuristicRollbackException
      {
        if (tran != null) {
          if (man.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
            man.rollback();
          } else {
            man.commit();
          }
        }
      }
    },
    REQUIRES_NEW
    {
      public Transaction begin(TransactionManager man) throws SystemException, NotSupportedException,
          InvalidTransactionException, IllegalStateException
      {
        Transaction result;
        if (man.getStatus() == Status.STATUS_ACTIVE) {
          result = man.suspend();
        } else {
          result = null;
        }

        try {
          man.begin();
        } catch (SystemException e) {
          man.resume(result);
          throw e;
        } catch (NotSupportedException e) {
          man.resume(result);
          throw e;
        }
        return result;
      }

      public void finish(TransactionManager man, Transaction tran) throws SystemException,
          InvalidTransactionException, IllegalStateException, SecurityException, RollbackException,
          HeuristicMixedException, HeuristicRollbackException
      {
        if (man.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
          man.rollback();
        } else {
          man.commit();
        }

        if (tran != null) {
          man.resume(tran);
        }
      }
    },
    SUPPORTS;

    public static TransactionStrategy fromValue(String value)
    {
      return valueOf(value.toUpperCase());
    }

    public Transaction begin(TransactionManager man) throws SystemException, NotSupportedException,
        InvalidTransactionException, IllegalStateException
    {
      return null;
    }

    public void finish(TransactionManager man, Transaction tran) throws SystemException,
        InvalidTransactionException, IllegalStateException, SecurityException, RollbackException,
        HeuristicMixedException, HeuristicRollbackException
    {

    }
}
