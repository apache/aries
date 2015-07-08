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

import java.sql.SQLException;

import javax.inject.Inject;

import org.apache.aries.transaction.test.TestBean;
import org.junit.Test;
import org.ops4j.pax.exam.util.Filter;

public class RequiresNewTranAttributeTest extends AbstractIntegrationTest {
    @Inject @Filter("(tranAttribute=RequiresNew)") 
    TestBean rnBean;
    
    @Inject @Filter("(tranAttribute=Required)") 
    TestBean rBean;

    
  @Test
  public void testRequiresNew() throws Exception {
      //Test with client transaction - a container transaction is used to insert the row
      int initialRows = rnBean.countRows();
      
      tran.begin();
      rnBean.insertRow("testWithClientTran", 1);
      tran.rollback();
      
      int finalRows = rnBean.countRows();
      assertTrue("Initial rows: " + initialRows + ", Final rows: " + finalRows, finalRows - initialRows == 1);
      
      //Test with client transaction and application exception - the container transaction is committed,
      //the user transaction is not affected.
      initialRows = rnBean.countRows();
      
      tran.begin();
      rBean.insertRow("testWithClientTranAndWithAppException", 1);
      
      try {
          rnBean.insertRow("testWithClientTranAndWithAppException", 2, new SQLException("Dummy exception"));
      } catch (SQLException e) {
          e.printStackTrace();
      }
      
      tran.commit();
      
      finalRows = rnBean.countRows();
      assertTrue("Initial rows: " + initialRows + ", Final rows: " + finalRows, finalRows - initialRows == 2);
      
      //Test with client transaction and runtime exception - the container transaction is rolled back,
      //the user transaction is not affected
      initialRows = rnBean.countRows();
      
      tran.begin();
      rBean.insertRow("testWithClientTranAndWithRuntimeException", 1);
      
      try {
          rnBean.insertRow("testWithClientTranAndWithRuntimeException", 2, new RuntimeException("Dummy exception"));
      } catch (RuntimeException e) {
          e.printStackTrace();
      }
      
      tran.commit();
      
      finalRows = rnBean.countRows();
      assertTrue("Initial rows: " + initialRows + ", Final rows: " + finalRows, finalRows - initialRows == 1);
      
      //Test without client transaction - a container transaction is used to insert the row
      initialRows = rnBean.countRows();
      
      rnBean.insertRow("testWithoutClientTran", 1);
      
      finalRows = rnBean.countRows();
      assertTrue("Initial rows: " + initialRows + ", Final rows: " + finalRows, finalRows - initialRows == 1);
      
      //Test without client transaction and with application exception - the container transaction is committed
      initialRows = rnBean.countRows();
      
      try {
          rnBean.insertRow("testWithoutClientTranAndWithAppException", 1, new SQLException("Dummy exception"));
      } catch (Exception e) {
          e.printStackTrace();
      }
      
      finalRows = rnBean.countRows();
      assertTrue("Initial rows: " + initialRows + ", Final rows: " + finalRows, finalRows - initialRows == 1);
      
      //Test without client transaction and with runtime exception - the container transaction is rolled back
      initialRows = rnBean.countRows();
      
      try {
          rnBean.insertRow("testWithoutClientTranAndWithRuntimeException", 1, new RuntimeException("Dummy exception"));
      } catch (Exception e) {
          e.printStackTrace();
      }
      
      finalRows = rnBean.countRows();
      assertTrue("Initial rows: " + initialRows + ", Final rows: " + finalRows, finalRows - initialRows == 0);
  }
}
