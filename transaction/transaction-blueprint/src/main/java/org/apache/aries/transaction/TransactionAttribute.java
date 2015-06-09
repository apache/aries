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

import org.apache.aries.transaction.annotations.TransactionPropagationType;

public enum TransactionAttribute {
    MANDATORY
    {
      public TransactionToken begin(TransactionManager man) throws SystemException
      {
        if (man.getStatus() == Status.STATUS_NO_TRANSACTION) {
          throw new IllegalStateException(Constants.MESSAGES.getMessage("tran.not.found"));
        }

        return new TransactionToken(man.getTransaction(), null, MANDATORY);
      }
    },
    NEVER
    {
      public TransactionToken begin(TransactionManager man) throws SystemException
      {
        if (man.getStatus() == Status.STATUS_ACTIVE) {
          throw new IllegalStateException(Constants.MESSAGES.getMessage("tran.found.never"));
        }

        return new TransactionToken(null, null, NEVER);
      }
    },
    NOTSUPPORTED
    {
      public TransactionToken begin(TransactionManager man) throws SystemException
      {
        if (man.getStatus() == Status.STATUS_ACTIVE) {
          return new TransactionToken(null, man.suspend(), this);
        }

        return new TransactionToken(null, null, NOTSUPPORTED);
      }

      public void finish(TransactionManager man, TransactionToken tranToken) throws SystemException,
          InvalidTransactionException, IllegalStateException
      {
        Transaction tran = tranToken.getSuspendedTransaction();
        if (tran != null) {
          man.resume(tran);
        }
      }
    },
    REQUIRED
    {
      public TransactionToken begin(TransactionManager man) throws SystemException, NotSupportedException
      {
        if (man.getStatus() == Status.STATUS_NO_TRANSACTION) {
          man.begin();
          return new TransactionToken(man.getTransaction(), null, REQUIRED, true);
        }

        return new TransactionToken(man.getTransaction(), null, REQUIRED);
      }

      public void finish(TransactionManager man, TransactionToken tranToken) throws SystemException,
          InvalidTransactionException, IllegalStateException, SecurityException, RollbackException,
          HeuristicMixedException, HeuristicRollbackException
      {
        if (tranToken.isCompletionAllowed()) {
          if (man.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
            man.rollback();
          } else {
            man.commit();
          }
        }
      }
    },
    REQUIRESNEW
    {
      public TransactionToken begin(TransactionManager man) throws SystemException, NotSupportedException,
          InvalidTransactionException, IllegalStateException
      {
         Transaction suspendedTransaction = (man.getStatus() == Status.STATUS_ACTIVE) ? man.suspend() : null;

        try {
          man.begin();
        } catch (SystemException e) {
          man.resume(suspendedTransaction);
          throw e;
        } catch (NotSupportedException e) {
          man.resume(suspendedTransaction);
          throw e;
        }
        return new TransactionToken(man.getTransaction(), suspendedTransaction, REQUIRESNEW, true);
      }

      public void finish(TransactionManager man, TransactionToken tranToken) throws SystemException,
          InvalidTransactionException, IllegalStateException, SecurityException, RollbackException,
          HeuristicMixedException, HeuristicRollbackException
      {
        if (tranToken.isCompletionAllowed()) {
          if (man.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
            man.rollback();
          } else {
            man.commit();
          }
        }

        Transaction tran = tranToken.getSuspendedTransaction();
        if (tran != null) {
          man.resume(tran);
        }
      }
    },
    SUPPORTS
    {
      public TransactionToken begin(TransactionManager man) throws SystemException, NotSupportedException,
          InvalidTransactionException, IllegalStateException
      {
          if (man.getStatus() == Status.STATUS_ACTIVE) {
              return new TransactionToken(man.getTransaction(), null, SUPPORTS);
          }

          return new TransactionToken(null, null, SUPPORTS);
      }
    };

    public static TransactionAttribute fromValue(TransactionPropagationType type)
    {
      return valueOf(type.name().toUpperCase());
    }

    public TransactionToken begin(TransactionManager man) throws SystemException, NotSupportedException,
        InvalidTransactionException, IllegalStateException
    {
      return null;
    }

    public void finish(TransactionManager man, TransactionToken tranToken) throws SystemException,
        InvalidTransactionException, IllegalStateException, SecurityException, RollbackException,
        HeuristicMixedException, HeuristicRollbackException
    {

    }
}
