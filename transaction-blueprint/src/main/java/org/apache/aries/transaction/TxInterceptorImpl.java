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

import java.lang.reflect.Method;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.transaction.exception.TransactionRollbackException;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TxInterceptorImpl implements Interceptor {
    private static final Logger _logger =
        LoggerFactory.getLogger("org.apache.aries.transaction");

    private TransactionManager tm;
    private TxComponentMetaDataHelper metaDataHelper;

    public int getRank()
    {
      // TODO Auto-generated method stub
      return 0;
    }

    public void postCallWithException(ComponentMetadata cm, Method m,
        Exception ex, Object preCallToken)
     {
       if (preCallToken instanceof TransactionToken)
       {
         final TransactionToken token = (TransactionToken)preCallToken;
         try { 
            token.ts.finish(tm, token.t);
         }
         catch (Exception e)
         {
           // we do not throw the exception since there already is one, but we need to log it
           _logger.error("An exception has occured.", e);
         }
       } else {
         // TODO: what now?
       }
    }

    public void postCallWithReturn(ComponentMetadata cm, Method m,
        Object returnType, Object preCallToken) throws Exception
    {
      if (preCallToken instanceof TransactionToken)
      {
        final TransactionToken token = (TransactionToken)preCallToken;
        try { 
           token.ts.finish(tm, token.t);
        }
        catch (Exception e)
        {
          _logger.error("An exception has occured.", e);
          throw new TransactionRollbackException(e);
        }
      }
      else {
        // TODO: what now?
      }
    }

    public Object preCall(ComponentMetadata cm, Method m,
        Object... parameters) throws Throwable  {
      // extract bundleId, componentName and method name
      // then lookup using metadatahelper
      // build transtrategy and call begin
      // store resulting tx and strategy in return object
      // which will be passed to postInvoke call
      final String methodName = m.getName();
        
      final String strategy = metaDataHelper.getComponentMethodTxStrategy(cm, methodName);

      Transaction t;
      TransactionStrategy txStrategy = TransactionStrategy.REQUIRED;
      if (strategy != null)
      {
        txStrategy = TransactionStrategy.fromValue(strategy);
      }

      t = txStrategy.begin(tm);

      // now construct return object from txStrategy and t
      return new TransactionToken(t, txStrategy);
    }

    public final void setTransactionManager(TransactionManager manager)
    {
      tm = manager;
    }

    public final void setTxMetaDataHelper(TxComponentMetaDataHelper transactionEnhancer)
    {
      this.metaDataHelper = transactionEnhancer;
    }  

    private static class TransactionToken
    {
       private Transaction t;
       private TransactionStrategy ts;
       private TransactionToken(Transaction t, TransactionStrategy ts)
       {
         this.t = t;
         this.ts = ts;
       }
    }
}
