/*  Licensed to the Apache Software Foundation (ASF) under one or more
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
package org.apache.aries.transaction.itests;

import static junit.framework.Assert.assertEquals;

import java.sql.SQLException;

import javax.inject.Inject;

import org.apache.aries.transaction.test.TestBean;
import org.junit.Test;
import org.ops4j.pax.exam.util.Filter;

public class RequiresNewTest extends AbstractIntegrationTest {
    @Inject
    @Filter("(tranAttribute=RequiresNew)")
    TestBean rnBean;

    @Inject
    @Filter("(tranAttribute=Required)")
    TestBean rBean;

    /**
     * Test with client transaction - a container transaction is used to insert the row,
     * user transaction roll back has no influence
     * @throws Exception
     */
    @Test
    public void testClientTransactionRollback() throws Exception {
        int initialRows = counter.countRows();
        tran.begin();
        rnBean.insertRow("testWithClientTran", 1, null);
        tran.rollback();
        int finalRows = counter.countRows();
        assertEquals("Added rows", 1, finalRows - initialRows);
    }
    
    /**
     * Test with client transaction and application exception - the container transaction is committed,
     * the user transaction is not affected.
     * @throws Exception
     */
    @Test
    public void testClientTransactionAndApplicationException() throws Exception {
        int initialRows = counter.countRows();
        tran.begin();
        rBean.insertRow("testWithClientTranAndWithAppException", 1, null);
        try {
            rnBean.insertRow("testWithClientTranAndWithAppException", 2, new SQLException("Dummy exception"));
        } catch (SQLException e) {
            // Ignore expected
        }
        tran.commit();
        int finalRows = counter.countRows();
        assertEquals("Added rows", 2, finalRows - initialRows);

    }

    /**
     * Test with client transaction and runtime exception - the container transaction is rolled back,
     * the user transaction is not affected
     * @throws Exception
     */
    @Test
    public void testClientTransactionAndRuntimeException() throws Exception {
        int initialRows = counter.countRows();
        tran.begin();
        rBean.insertRow("testWithClientTranAndWithRuntimeException", 1, null);
        try {
            rnBean.insertRow("testWithClientTranAndWithRuntimeException", 2, new RuntimeException("Dummy exception"));
        } catch (RuntimeException e) {
         // Ignore expected
        }
        tran.commit();
        int finalRows = counter.countRows();
        assertEquals("Added rows", 1, finalRows - initialRows);
    }
    
    /**
     * Test without client transaction - a container transaction is used to insert the row
     * @throws Exception
     */
    //@Test
    public void testNoClientTransaction() throws Exception {
        clientTransaction = false;
        assertInsertSuccesful();
        testClientTransactionAndApplicationException();
        testClientTransactionAndRuntimeException();
    }

    @Override
    protected TestBean getBean() {
        return rnBean;
    }
}
