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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.transaction.UserTransaction;

import org.apache.aries.transaction.test.TestBean;
import org.junit.Test;

public class NeverTranAttributeTest extends AbstractIntegrationTest {
  
  @Test
  public void testNever() throws Exception {
      TestBean bean = context().getService(TestBean.class, "(tranAttribute=Never)");
      UserTransaction tran = context().getService(UserTransaction.class);
      
      //Test with client transaction - an exception is thrown because transactions are not allowed
      int initialRows = bean.countRows();
      
      tran.begin();
      
      try {
          bean.insertRow("testWithClientTran", 1);
          fail("IllegalStateException not thrown");
      } catch (IllegalStateException e) {
          e.printStackTrace();
      }
      
      tran.commit();
      
      int finalRows = bean.countRows();
      assertTrue("Initial rows: " + initialRows + ", Final rows: " + finalRows, finalRows - initialRows == 0);
      
      //Test without client transaction - the insert fails because the bean delegates to another
      //bean with a transaction strategy of Mandatory, and no transaction is available
      initialRows = bean.countRows();

      try {
          bean.insertRow("testWithoutClientTran", 1, true);
          fail("IllegalStateException not thrown");
      } catch (IllegalStateException e) {
          e.printStackTrace();
      }
      
      finalRows = bean.countRows();
      assertTrue("Initial rows: " + initialRows + ", Final rows: " + finalRows, finalRows - initialRows == 0);
  }
  
}
