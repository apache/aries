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

import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

public class TranStrategyTest {

    TransactionManager tm;
    Transaction t;
    private IMocksControl c;
   
    @Before
    public void clean()
    {
      c = createControl();
      tm = c.createMock(TransactionManager.class);
      t = c.createMock(Transaction.class);
    }
    
    @Test
    public void testMandatoryBegin() throws Exception
    {
      // MANDATORY strategy should throw IllegalStateException when tran manager 
      // status is Status.STATUS_NO_TRANSACTION it should not return null.
      expect(tm.getStatus()).andReturn(Status.STATUS_NO_TRANSACTION);

      try {
        assertNotNull("TransactionStrategy.MANDATORY.begin(tm) returned null when manager " +
              "status is STATUS_NO_TRANSACTION", TransactionAttribute.MANDATORY.begin(tm).getActiveTransaction());
      } catch (IllegalStateException ise) {
          // Expected to be in here
      } catch (Exception e) {
          fail("TransactionStrategy.MANDATORY.begin() threw an unexpected exception when tran manager status is STATUS_NO_TRANSACTION");
      }
          
      // MANDATORY strategy should return null for all tran manager states other
      // than Status.STATUS_NO_TRANSACTION.
      int[] invalids = new int[]{ Status.STATUS_COMMITTED, Status.STATUS_COMMITTING, Status.STATUS_MARKED_ROLLBACK,
          Status.STATUS_ACTIVE, Status.STATUS_PREPARED, Status.STATUS_PREPARING, Status.STATUS_ROLLEDBACK, 
          Status.STATUS_ROLLING_BACK, Status.STATUS_UNKNOWN };
      
      for (int i = 0; i < invalids.length ; i++) {
        c.reset();
        expect(tm.getStatus()).andReturn(invalids[i]);
        expect(tm.getTransaction()).andReturn(null);
        c.replay();
        try {
          Transaction tran = TransactionAttribute.MANDATORY.begin(tm).getActiveTransaction();
          assertNull("TransactionStrategy.MANDATORY.begin() did not return null when manager status value is " + invalids[i], tran);
        } catch (Exception ise) {
          fail("TransactionStrategy.MANDATORY.begin() threw Exception when manager status value is " + invalids[i]);
        }
        c.verify();
      }
    }
     
    @Test
    public void testMandatoryFinish()
    {
      try {
        TransactionToken tranToken = new TransactionToken(t, null, TransactionAttribute.MANDATORY);
        TransactionAttribute.MANDATORY.finish(tm, tranToken);
      } catch (Exception e) {
          fail("TransactionStrategy.MANDATORY.finish() threw an unexpected exception");
      }
    }
    
    
    @Test
    public void testNeverBegin() throws Exception
    {
        // NEVER strategy should throw IllegalStateException when tran manager 
        // status is Status.STATUS_ACTIVE it should not return null.
        expect(tm.getStatus()).andReturn(Status.STATUS_ACTIVE);

        try {
            assertNotNull("TransactionStrategy.NEVER.begin() returned null when manager status is STATUS_ACTIVE", TransactionAttribute.NEVER.begin(tm));
        } catch (IllegalStateException ise) {
            // Expect to be in here
        } catch (Exception e) {
            fail("TransactionStrategy.NEVER.begin() threw an unexpected exception when tran manager status is STATUS_ACTIVE");
        }

        // NEVER strategy should return null for all tran manager states other
        // than Status.STATUS_ACTIVE.
        int[] invalids = new int[]{ Status.STATUS_COMMITTED, Status.STATUS_COMMITTING, Status.STATUS_MARKED_ROLLBACK,
                                    Status.STATUS_NO_TRANSACTION, Status.STATUS_PREPARED, Status.STATUS_PREPARING, Status.STATUS_ROLLEDBACK, 
                                    Status.STATUS_ROLLING_BACK, Status.STATUS_UNKNOWN };

        for (int i = 0; i < invalids.length ; i++) {
            c.reset();
            expect(tm.getStatus()).andReturn(invalids[i]);
            expect(tm.getTransaction()).andReturn(null).anyTimes();
            c.replay();
            try {
                assertNull("TransactionStrategy.NEVER.begin() did not return null when manager status value is " + invalids[i], TransactionAttribute.NEVER.begin(tm).getActiveTransaction());
            } catch (Exception ise) {
                fail("TransactionStrategy.NEVER.begin() threw unexpected exception when manager status value is " + invalids[i]);
            } 
            c.verify();
        }

    }
    
    @Test
    public void testNeverFinish()
    {
      try {
        TransactionToken tranToken = new TransactionToken(null, null, TransactionAttribute.NEVER);
        TransactionAttribute.NEVER.finish(tm, tranToken);
      } catch (Exception e) {
          fail("TransactionStrategy.NEVER.finish() threw an unexpected exception");
      }
    }
    
    @Test
    public void testNotSupportedBegin() throws Exception
    {
      // NOT_SUPPORTED strategy should suspend an active transaction
      // and _NOT_ begin a new one
      expect(tm.getStatus()).andReturn(Status.STATUS_ACTIVE);
      expect(tm.suspend()).andReturn(null);
      c.replay();
      TransactionAttribute.NOTSUPPORTED.begin(tm);
      c.verify();
       
      // For all situations where there is no active transaction the
      // NOT_SUPPORTED strategy should return null
      int[] invalids = new int[]{ Status.STATUS_COMMITTED, Status.STATUS_COMMITTING, Status.STATUS_MARKED_ROLLBACK,
          Status.STATUS_NO_TRANSACTION, Status.STATUS_PREPARED, Status.STATUS_PREPARING, Status.STATUS_ROLLEDBACK, 
          Status.STATUS_ROLLING_BACK, Status.STATUS_UNKNOWN };
      
      for (int i = 0; i < invalids.length ; i++) {
          c.reset();
          expect(tm.getStatus()).andReturn(invalids[i]);
          expect(tm.getTransaction()).andReturn(null).anyTimes();
          c.replay();
        try {
          assertNull("TransactionStrategy.NOT_SUPPORTED.begin() did not return null when manager status value is " + invalids[i], TransactionAttribute.NOTSUPPORTED.begin(tm).getActiveTransaction());
        } catch (Exception ise) {
            fail("TransactionStrategy.NOT_SUPPORTED.begin() threw unexpected exception when manager status value is " + invalids[i]);
        } 
        c.verify();
      }
     
    }
    
    @Test
    public void testNotSupportedFinish()
    {
      // If finish is called with a previously active transaction, then
      // we expect this transaction to be resumed for a NOT_SUPPORTED strategy
      try {
        tm.resume(t);
        EasyMock.expectLastCall();
        c.replay();
        TransactionToken tranToken = new TransactionToken(null, t, TransactionAttribute.NOTSUPPORTED);
        TransactionAttribute.NOTSUPPORTED.finish(tm, tranToken);
        c.verify();
        
        c.reset();
        tranToken = new TransactionToken(null, null, TransactionAttribute.NOTSUPPORTED);
        TransactionAttribute.NOTSUPPORTED.finish(tm, tranToken);
      } catch (Exception e) {
          fail("TransactionStrategy.NOT_SUPPORTED.finish() threw unexpected exception, " + e);
      }
    }
    
    @Test
    public void testRequiredBegin() throws Exception
    {
      // If there is no previously active transaction when the REQUIRED strategy
      // is invoked then we expect a call to begin one
      expect(tm.getStatus()).andReturn(Status.STATUS_NO_TRANSACTION);
      expect(tm.getTransaction()).andReturn(null);
      tm.begin();
      expectLastCall();
      c.replay();
      TransactionAttribute.REQUIRED.begin(tm);
      c.verify();
      c.reset();
       
      // For all cases where there is a transaction we expect REQUIRED to return null
      int[] invalids = new int[]{ Status.STATUS_COMMITTED, Status.STATUS_COMMITTING, Status.STATUS_MARKED_ROLLBACK,
          Status.STATUS_ACTIVE, Status.STATUS_PREPARED, Status.STATUS_PREPARING, Status.STATUS_ROLLEDBACK, 
          Status.STATUS_ROLLING_BACK, Status.STATUS_UNKNOWN };
      
      for (int i = 0; i < invalids.length ; i++) {
          c.reset();
          expect(tm.getStatus()).andReturn(invalids[i]);
          expect(tm.getTransaction()).andReturn(null);
          c.replay();
        try {
          assertNull("TransactionStrategy.REQUIRED.begin() did not return null when manager status value is " + invalids[i], TransactionAttribute.REQUIRED.begin(tm).getActiveTransaction());
        } catch (Exception ise) {
            fail("TransactionStrategy.REQUIRED.begin() threw unexpected exception when manager status value is " + invalids[i]);
        } 
        c.verify();
      }
    }
    
    @Test
    public void testRequiredFinish() throws Exception
    {
      // In the REQUIRED strategy we expect a call to rollback when a call to finish()
      // is made with a tran where the tran manager status shows Status.STATUS_MARKED_ROLLBACK
        expect(tm.getStatus()).andReturn(Status.STATUS_MARKED_ROLLBACK);
      
        TransactionToken tranToken = new TransactionToken(t, null, TransactionAttribute.REQUIRED, true);
        tm.rollback();
        expectLastCall();
        c.replay();
        TransactionAttribute.REQUIRED.finish(tm, tranToken);
        c.verify();
        
        int[] invalids = new int[]{ Status.STATUS_COMMITTED, Status.STATUS_COMMITTING, Status.STATUS_NO_TRANSACTION,
            Status.STATUS_ACTIVE, Status.STATUS_PREPARED, Status.STATUS_PREPARING, Status.STATUS_ROLLEDBACK, 
            Status.STATUS_ROLLING_BACK, Status.STATUS_UNKNOWN };
        
        // For all other tran manager states we expect a call to commit
        for (int i = 0; i < invalids.length ; i++) {
          c.reset();
          expect(tm.getStatus()).andReturn(invalids[i]);
          tm.commit();
          expectLastCall();
          c.replay();
          TransactionAttribute.REQUIRED.finish(tm, tranToken);
          c.verify();
        }
        
        // If null is passed instead of a tran we expect nothing to happen
        c.reset();
        c.replay();
        tranToken = new TransactionToken(null, null, TransactionAttribute.REQUIRED);
        c.verify();
        TransactionAttribute.REQUIRED.finish(tm, tranToken);
      
    }
    
    @Test
    public void testRequiresNew_BeginActiveTran() throws Exception
    {
      // Suspend case (no exception from tm.begin())
      expect(tm.getStatus()).andReturn(Status.STATUS_ACTIVE);
      
      // In the case of the REQUIRES_NEW strategy we expect an active tran to be suspended
      // a new new transaction to begin
      expect(tm.suspend()).andReturn(null);
      expectLastCall();
      tm.begin();
      expectLastCall();
      expect(tm.getTransaction()).andReturn(null);
      c.replay();
      TransactionAttribute.REQUIRESNEW.begin(tm);
      c.verify();
    }
    
    @Test
    public void testRequiresNew_BeginNoActiveTran() throws Exception
    {
      // No active tran cases (no exception from tm.begin())

      int[] manStatus = new int[]{ Status.STATUS_COMMITTED, Status.STATUS_COMMITTING, Status.STATUS_MARKED_ROLLBACK,
          Status.STATUS_NO_TRANSACTION, Status.STATUS_PREPARED, Status.STATUS_PREPARING, Status.STATUS_ROLLEDBACK, 
          Status.STATUS_ROLLING_BACK, Status.STATUS_UNKNOWN };
      
      // For all cases where the tran manager state is _not_ Status.STATUS_ACTIVE 
      // we expect a call to begin a new tran, no call to suspend and null to be
      // returned from TransactionStrategy.REQUIRES_NEW.begin(tm)
      for (int i = 0; i < manStatus.length ; i++) {
          c.reset();
          expect(tm.getStatus()).andReturn(manStatus[i]);
          expect(tm.getTransaction()).andReturn(null);
          tm.begin();
          expectLastCall();
          c.replay();
        try {
          assertNull("TransactionStrategy.REQUIRES_NEW.begin() did not return null when manager status value is " + manStatus[i], TransactionAttribute.REQUIRESNEW.begin(tm).getActiveTransaction());
        } catch (Exception ise) {
            fail("TransactionStrategy.REQUIRES_NEW.begin() threw unexpected exception when manager status value is " + manStatus[i]);
        } 
        c.verify();
      }
     
    }
    
    @Test
    public void testRequiresNew_TmExceptions() throws Exception
    {
      int[] allStates = new int[]{ Status.STATUS_COMMITTED, Status.STATUS_COMMITTING, Status.STATUS_NO_TRANSACTION,
          Status.STATUS_ACTIVE, Status.STATUS_PREPARED, Status.STATUS_PREPARING, Status.STATUS_ROLLEDBACK, 
          Status.STATUS_MARKED_ROLLBACK, Status.STATUS_ROLLING_BACK, Status.STATUS_UNKNOWN };
      
      // SystemException and NotSupportedException from tm.begin()
      Set<Exception> ees = new HashSet<Exception>();
      ees.add(new SystemException("KABOOM!"));
      ees.add(new NotSupportedException("KABOOM!"));
      
      // Loop through all states states twice changing the exception thrown
      // from tm.begin()
      for (int i = 0 ; i < allStates.length ; i++ ) {
        Iterator<Exception> iterator = ees.iterator();
        while (iterator.hasNext()) {
          Exception e = iterator.next();
          c.reset();
          expect(tm.getStatus()).andReturn(allStates[i]);
          expect(tm.getTransaction()).andReturn(null).anyTimes();
          tm.begin();
          expectLastCall().andThrow(e);
          requiresNewExceptionCheck(tm, allStates[i]);
        }
      }
    }
    
    private void requiresNewExceptionCheck(TransactionManager tm, int managerStatus) throws Exception
    {
      // If an ACTIVE tran is already present we expect a call to suspend this tran.
      // All states should call begin(), whereupon we receive our exception resulting
      // in calls to resume(t)
      if (managerStatus == Status.STATUS_ACTIVE) {
          expect(tm.suspend()).andReturn(null);
      }
      tm.resume(EasyMock.anyObject(Transaction.class));
      expectLastCall();
      c.replay();
      try {
          TransactionAttribute.REQUIRESNEW.begin(tm);
      } catch (SystemException se) {
          // Expect to be in here
      } catch (NotSupportedException nse) {
          // or to be in here
      } catch (Exception thrownE) {
          fail("TransactionStrategy.REQUIRES_NEW.begin() threw unexpected exception when manager status is " + managerStatus);
      } finally {
          // If Status.STATUS_ACTIVE
      }
      c.verify();
      c.reset();
    }
    
    @Test
    public void testRequiresNew_Finish() throws Exception
    {
        int[] allStates = new int[]{ Status.STATUS_COMMITTED, Status.STATUS_COMMITTING, Status.STATUS_NO_TRANSACTION,
                                     Status.STATUS_ACTIVE, Status.STATUS_PREPARED, Status.STATUS_PREPARING, Status.STATUS_ROLLEDBACK, 
                                     Status.STATUS_MARKED_ROLLBACK, Status.STATUS_ROLLING_BACK, Status.STATUS_UNKNOWN };

        // Loop through all states calling TransactionStrategy.REQUIRES_NEW.finish
        // passing tran manager and a tran, then passing tran manager and null
        for (int i = 0 ; i < allStates.length ; i++ ) {
            c.reset();
            expect(tm.getStatus()).andReturn(allStates[i]);
            requiresNew_assertion(tm, allStates[i]);
            tm.resume(EasyMock.anyObject(Transaction.class));
            expectLastCall();
            c.replay();
            try {
                TransactionToken tranToken = new TransactionToken(t, t, TransactionAttribute.REQUIRESNEW, true);
                TransactionAttribute.REQUIRESNEW.finish(tm, tranToken);
            } catch (Exception e) {
                fail("TransactionStrategy.REQUIRES_NEW.finish() threw unexpected exception when manager status is " + allStates[i]);
            }
            c.verify();
            c.reset();
            try {
                expect(tm.getStatus()).andReturn(allStates[i]);
                requiresNew_assertion(tm, allStates[i]);
                c.replay();
                TransactionToken tranToken = new TransactionToken(t, null, TransactionAttribute.REQUIRESNEW, true);
                TransactionAttribute.REQUIRESNEW.finish(tm, tranToken);
            } catch (Throwable e) {
                e.printStackTrace();
                fail("TransactionStrategy.REQUIRES_NEW.finish() threw unexpected exception when manager status is " + allStates[i]);
            } finally {
            }
            c.verify();
        }

    }

    // If tran manager status reports Status.STATUS_MARKED_ROLLBACK we expect
    // a call to rollback ... otherwise we expect a call to commit
    private void requiresNew_assertion(TransactionManager tm, int status) throws Exception
    {
      if (status == Status.STATUS_MARKED_ROLLBACK) {
          tm.rollback();
      }
      else {
          tm.commit();
      }
      expectLastCall();
    }
    
    @Test
    public void testSupports() throws Exception
    {
      expect(tm.getTransaction()).andReturn(null);
      expect(tm.getStatus()).andReturn(Status.STATUS_ACTIVE);
      c.replay();
      assertNull("TransTransactionStrategy.SUPPORTS.begin(tm) did not return null", TransactionAttribute.SUPPORTS.begin(tm).getActiveTransaction());
      c.verify();
    }
}
