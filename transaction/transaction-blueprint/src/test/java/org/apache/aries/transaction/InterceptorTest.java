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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.IOException;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Test;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;

public class InterceptorTest {

    @Test
    public void testRollbackOnException() throws Throwable {
        runPostCall(false);
        runPostCall(true);
    }

    private void runPostCall(boolean failCoordination) throws Throwable {
        postCallWithTransaction(new IllegalStateException(), true, failCoordination);
        postCallWithTransaction(new Error(), true, failCoordination);
        postCallWithTransaction(new Exception(), false, failCoordination);
        postCallWithTransaction(new IOException(), false, failCoordination);
    }
    
    private CoordinationException coordinationException(Throwable th) {
        Coordination coordination = EasyMock.createMock(Coordination.class);
        expect(coordination.getId()).andReturn(1l);
        expect(coordination.getName()).andReturn("Test");
        replay(coordination);
        CoordinationException cex = new CoordinationException("Simulating exception", 
                                                              coordination , 
                                                              CoordinationException.FAILED,
                                                              th);
        return cex;
    }
    
    private void postCallWithTransaction(Throwable th, boolean expectRollback, boolean failCoordination) throws Throwable {
        IMocksControl c = EasyMock.createControl();
        TxInterceptorImpl sut = new TxInterceptorImpl();
        sut.setTransactionManager(c.createMock(TransactionManager.class));
        Transaction tran = c.createMock(Transaction.class);
        
        if (expectRollback) {
            tran.setRollbackOnly();
            EasyMock.expectLastCall();
        }
        Coordination coordination = c.createMock(Coordination.class);
        coordination.end();
        if (failCoordination) {
            EasyMock.expectLastCall().andThrow(coordinationException(th));
        } else {
            EasyMock.expectLastCall();
        }
        
        c.replay();
        TransactionToken tt = new TransactionToken(tran, null, TransactionAttribute.REQUIRED);
        
        tt.setCoordination(coordination );
        sut.postCallWithException(null, this.getClass().getMethods()[0], th, tt);
        c.verify();
    }
    
}
