/**
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

package org.apache.aries.samples.bank.cc;

import org.apache.aries.samples.bank.api.CreditCheckService;

public class CreditCheckServiceImpl implements CreditCheckService {

  private static final int MIN = -10000;
  private static final int MAX = 10000;
  
  @Override
  public double risk(String name, int assets, int liabilities) {
    int equity = assets - liabilities;
    double risk = 1.0;
    if (equity <= MIN)
      risk = 1.0;
    else if (equity >= MAX)
      risk = 0.0;
    else
      risk = ((double)(MAX-equity)) / (MAX-MIN);
    System.out.println("EJB: CreditCheck.risk("+name+","+assets+","+liabilities+") = "+risk);
    return risk;
  }

}
