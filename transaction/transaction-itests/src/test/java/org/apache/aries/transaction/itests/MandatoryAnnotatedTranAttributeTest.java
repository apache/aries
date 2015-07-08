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

import java.sql.SQLException;

import javax.inject.Inject;
import javax.transaction.RollbackException;

import org.apache.aries.transaction.test.TestBean;
import org.junit.Test;
import org.ops4j.pax.exam.util.Filter;

public class MandatoryAnnotatedTranAttributeTest extends AbstractIntegrationTest {
    @Inject @Filter("(tranAttribute=MandatoryAnnotated)") 
    TestBean bean;
  
  @Test
  public void testMandatory() throws Exception {
      String prefix = "MTAT";
      //Test with client transaction - the user transaction is used to insert a row
      int initialRows = bean.countRows();
      
      tran.begin();
      bean.insertRow(prefix + "testWithClientTran", 1);
      tran.commit();
      
      int finalRows = bean.countRows();
      assertTrue("Initial rows: " + initialRows + ", Final rows: " + finalRows, finalRows - initialRows == 1);
  
      //Test with client transaction and application exception - the user transaction is not rolled back
      initialRows = bean.countRows();
      
      tran.begin();
      bean.insertRow(prefix + "testWithClientTranAndWithAppException", 1);
      
      try {
          bean.insertRow(prefix + "testWithClientTranAndWithAppException", 2, new SQLException("Dummy exception"));
      } catch (SQLException e) {
          e.printStackTrace();
      }
      
      tran.commit();
      
      finalRows = bean.countRows();
      assertTrue("Initial rows: " + initialRows + ", Final rows: " + finalRows, finalRows - initialRows == 2);
      
      //Test with client transaction and runtime exception - the user transaction is rolled back
      initialRows = bean.countRows();
      
      tran.begin();
      bean.insertRow(prefix + "testWithClientTranAndWithRuntimeException", 1);
      
      try {
          bean.insertRow(prefix + "testWithClientTranAndWithRuntimeException", 2, new RuntimeException("Dummy exception"));
      } catch (RuntimeException e) {
          e.printStackTrace();
      }
      
      try {
          tran.commit();
          fail("RollbackException not thrown");
      } catch (RollbackException e) {
          e.printStackTrace();
      }
      
      finalRows = bean.countRows();
      assertTrue("Initial rows: " + initialRows + ", Final rows: " + finalRows, finalRows - initialRows == 0);
      
      //Test without client transaction - an exception is thrown because a transaction is mandatory
      try {
          bean.insertRow(prefix + "testWithoutClientTran", 1);
          fail("IllegalStateException not thrown");
      } catch (IllegalStateException e) {
          e.printStackTrace();
      }
  }
}
