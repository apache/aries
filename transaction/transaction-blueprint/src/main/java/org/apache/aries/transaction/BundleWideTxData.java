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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.aries.transaction.annotations.TransactionPropagationType;

public final class BundleWideTxData
{
  private TransactionPropagationType value;
  private final List<Pattern> methodList = new ArrayList<Pattern>();
  private final List<Pattern> beanList = new ArrayList<Pattern>();
  
  public BundleWideTxData(TransactionPropagationType value,
          String method, String bean) {
      if (value == null)
        this.value = TransactionPropagationType.Required;
      else
        this.value = value;
      setupPatterns(method, bean);  
  }
 
  private void setupPatterns(String method, String bean) {
	  // when bean or method is not specified, the value is same as "*".
	  if (method == null || method.length() == 0) {
		  method = "*";
	  }
	  if (bean == null || bean.length() == 0) {
		  bean = "*";
	  }
	  
      String[] names = method.split("[, \t]");
      
      for (int i = 0; i < names.length; i++) {
          Pattern p = Pattern.compile(names[i].replaceAll("\\*", ".*"));
          this.methodList.add(p);
      }
      
      names = bean.split("[, \t]");
      
      for (int i = 0; i < names.length; i++) {
          Pattern p = Pattern.compile(names[i].replaceAll("\\*", ".*"));
          this.beanList.add(p);
      }
  }
  public TransactionPropagationType getValue() {
      return this.value;
  }
  
  public List<Pattern> getMethod() {
      return this.methodList;
  }
  
  public List<Pattern> getBean() {
      return this.beanList;
  }
}
