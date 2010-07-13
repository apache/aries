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

import static org.junit.Assert.fail;

import org.apache.aries.transaction.test.TestBean;
import org.junit.Test;

public class InvalidTranAttributeTest extends AbstractIntegrationTest {
  
  @Test
  public void testInvalid() throws Exception {
      TestBean bean = getOsgiService(TestBean.class, "(tranAttribute=Invalid)", DEFAULT_TIMEOUT);
      
      //Test without client transaction - an exception is thrown because the bean is not
      //configured correctly, i.e. multiple transaction elements match to the same method
      //name.
      try {
          bean.insertRow("testWithoutClientTran", 1);
          fail("IllegalStateException not thrown");
      } catch (IllegalStateException e) {
          e.printStackTrace();
      }
  }
}
