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

package org.apache.aries.transaction;

import java.util.concurrent.TimeUnit;
import javax.transaction.xa.Xid;

import org.apache.geronimo.transaction.manager.XidFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class XidFactoryImplTest {

    @Test
    public void testLong() {
        byte[] buffer = new byte[64];
        long l1 = 1343120074022l;
        XidFactoryImpl.insertLong(l1, buffer, 4);
        long l2 = XidFactoryImpl.extractLong(buffer, 4);
        assertEquals(l1, l2);

        l1 = 1343120074022l - TimeUnit.DAYS.toMillis(15);
        XidFactoryImpl.insertLong(l1, buffer, 4);
        l2 = XidFactoryImpl.extractLong(buffer, 4);
        assertEquals(l1, l2);
    }

    @Test
    public void testAriesFactory() throws Exception {
        XidFactory factory = new XidFactoryImpl("hi".getBytes());
        Xid id1 = factory.createXid();
        Xid id2 = factory.createXid();

        assertFalse("Should not match new: " + id1, factory.matchesGlobalId(id1.getGlobalTransactionId()));
        assertFalse("Should not match new: " + id2, factory.matchesGlobalId(id2.getGlobalTransactionId()));

        Xid b_id1 = factory.createBranch(id1, 1);
        Xid b_id2 = factory.createBranch(id2, 1);

        assertFalse("Should not match new branch: " + b_id1, factory.matchesBranchId(b_id1.getBranchQualifier()));
        assertFalse("Should not match new branch: " + b_id2, factory.matchesBranchId(b_id2.getBranchQualifier()));

        Thread.sleep(5);

        XidFactory factory2 = new XidFactoryImpl("hi".getBytes());
        assertTrue("Should match old: " + id1, factory2.matchesGlobalId(id1.getGlobalTransactionId()));
        assertTrue("Should match old: " + id2, factory2.matchesGlobalId(id2.getGlobalTransactionId()));

        assertTrue("Should match old branch: " + b_id1, factory2.matchesBranchId(b_id1.getBranchQualifier()));
        assertTrue("Should match old branch: " + b_id2, factory2.matchesBranchId(b_id2.getBranchQualifier()));
    }

}
