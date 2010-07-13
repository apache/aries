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

import java.io.IOException;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Test;

public class InterceptorTest {
    private Transaction t;
    
    @Test
    public void testRollbackOnException() {
        TxInterceptorImpl sut = new TxInterceptorImpl();
        sut.setTransactionManager(Skeleton.newMock(TransactionManager.class));
        
        sut.postCallWithException(null, null, new IllegalStateException(), newTranToken());
        assertRolledBack();
        sut.postCallWithException(null, null, new Error(), newTranToken());
        assertRolledBack();

        sut.postCallWithException(null, null, new Exception(), newTranToken());
        assertNotRolledBack();
        sut.postCallWithException(null, null, new IOException(), newTranToken());
        assertNotRolledBack();
    }
    
    private void assertNotRolledBack() {
        Skeleton.getSkeleton(t).assertNotCalled(new MethodCall(Transaction.class, "setRollbackOnly"));
    }
    
    private void assertRolledBack() {
        Skeleton.getSkeleton(t).assertCalled(new MethodCall(Transaction.class, "setRollbackOnly"));
    }
    
    private TransactionToken newTranToken() {
        t = Skeleton.newMock(Transaction.class);
        return new TransactionToken(t, null, TransactionAttribute.REQUIRED);
    }
}
