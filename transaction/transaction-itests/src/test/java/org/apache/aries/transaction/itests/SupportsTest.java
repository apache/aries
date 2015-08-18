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

import javax.inject.Inject;

import org.apache.aries.transaction.test.TestBean;
import org.junit.Test;
import org.ops4j.pax.exam.util.Filter;

public class SupportsTest extends AbstractIntegrationTest {
    @Inject
    @Filter("(tranAttribute=Supports)")
    TestBean bean;

    /**
     * Test with client transaction - the insert succeeds because the bean delegates to
     * another bean with a transaction strategy of Mandatory, and the user transaction
     * is delegated
     * @throws Exception
     */
    @Test
    public void testDelegatedInsertWithClientTransaction() throws Exception {
        assertDelegateInsert();
    }
    
    /**
     * Test with client transaction and application exception - the user transaction is not rolled back
     * @throws Exception
     */
    @Test
    public void testInsertWithAppExceptionCommitted() throws Exception {
        assertInsertWithAppExceptionCommitted();
    }
    
    /**
     * Test with client transaction and runtime exception - the user transaction is rolled back
     * @throws Exception
     */
    @Test
    public void testInsertWithRuntimeExceptionRolledBack() throws Exception {
        assertInsertWithRuntimeExceptionRolledBack();
    }
    
    /**
     * Test without client transaction - the insert fails because the bean delegates to
     * another bean with a transaction strategy of Mandatory, and no transaction is available
     * @throws Exception
     */
    @Test
    public void testDelegatedWithoutClientTransactionFails() throws Exception {
        clientTransaction = false;
        assertDelegateInsertFails();
    }

    @Override
    protected TestBean getBean() {
        return bean;
    }
}
